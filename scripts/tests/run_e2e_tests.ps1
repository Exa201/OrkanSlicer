$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
Push-Location $projectRoot
try {
    $testOutput = Join-Path $projectRoot 'app/build/headless-e2e-tests'
    New-Item -ItemType Directory -Force -Path $testOutput | Out-Null
    $testOutputRoot = (Resolve-Path -LiteralPath $testOutput).Path
    $testClasses = Join-Path $testOutputRoot 'classes'
    $jsonMockClasses = Join-Path $testOutputRoot 'json-mocks'
    if (Test-Path -LiteralPath $testClasses -PathType Container) {
        $resolvedTestClasses = (Resolve-Path -LiteralPath $testClasses).Path
        if (-not $resolvedTestClasses.StartsWith($testOutputRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
            throw "Refusing to clear a test class directory outside the generated test output: $resolvedTestClasses"
        }
        Remove-Item -LiteralPath $resolvedTestClasses -Recurse -Force
    }
    New-Item -ItemType Directory -Force -Path $testClasses | Out-Null
    New-Item -ItemType Directory -Force -Path $jsonMockClasses | Out-Null

    $sdkLine = Get-Content -LiteralPath 'local.properties' |
        Where-Object { $_ -match '^sdk\.dir=' } |
        Select-Object -First 1
    if (-not $sdkLine) { throw 'sdk.dir is missing from local.properties' }
    $sdkPath = $sdkLine.Substring($sdkLine.IndexOf('=') + 1).Replace('\:', ':').Replace('\\', '\')
    $platforms = Get-ChildItem -LiteralPath (Join-Path $sdkPath 'platforms') -Directory |
        Where-Object { $_.Name -match '^android-\d+$' } |
        Sort-Object { [int]($_.Name -replace '^android-', '') }
    if (-not $platforms) { throw "No Android SDK platforms found under $sdkPath" }
    $androidJar = Join-Path $platforms[-1].FullName 'android.jar'
    if (-not (Test-Path -LiteralPath $androidJar -PathType Leaf)) { throw "Android API jar not found: $androidJar" }

    Write-Host "Android API: $androidJar"
    Write-Host 'Auditing native profile setting routes...'
    $pythonCommand = if ($env:PYTHON) { $env:PYTHON } else { (Get-Command python -ErrorAction SilentlyContinue).Source }
    if (-not $pythonCommand) { throw 'Python was not found. Set PYTHON to the interpreter path or add python to PATH.' }
    & $pythonCommand (Join-Path $PSScriptRoot '../check_profile_setting_routes.py')
    if ($LASTEXITCODE -ne 0) { throw "Profile setting route audit failed with exit code $LASTEXITCODE" }

    $classpathFile = $env:ORCA_TEST_CLASSPATH_FILE
    if (-not [string]::IsNullOrWhiteSpace($classpathFile) -and (Test-Path -LiteralPath $classpathFile -PathType Leaf)) {
        $dependencyPaths = @(Get-Content -LiteralPath $classpathFile | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
        Write-Host 'Using the dependency classpath prepared by the grouped verification run.'
    } else {
        $gradleOutput = & .\gradlew.bat -q -I scripts/tests/init.gradle :app:compileDebugJavaWithJavac :app:printClasspath
        if ($LASTEXITCODE -ne 0) { throw "Gradle classpath resolution failed with exit code $LASTEXITCODE" }
        $dependencyPaths = @($gradleOutput | Where-Object { $_ -like 'CLASSPATH_ENTRY:*' } |
            ForEach-Object { $_.Substring('CLASSPATH_ENTRY:'.Length) })
    }
    if (-not $dependencyPaths) { throw 'Gradle did not return compile classpath entries' }

    $appClasses = Join-Path $projectRoot 'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'
    $appKotlinClasses = Join-Path $projectRoot 'app/build/tmp/kotlin-classes/debug'
    if (-not (Test-Path -LiteralPath $appKotlinClasses -PathType Container)) {
        throw "Debug Kotlin classes not found: $appKotlinClasses"
    }
    $classpath = (@($testClasses, $appClasses, $appKotlinClasses) + $dependencyPaths + @($androidJar)) -join [IO.Path]::PathSeparator

    $mockSources = @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'mocks') -Recurse -Filter '*.java' |
        ForEach-Object { $_.FullName })
    & javac -encoding UTF-8 -d $testClasses -cp $classpath @mockSources
    if ($LASTEXITCODE -ne 0) { throw "Compiling E2E mocks failed with exit code $LASTEXITCODE" }

    $suiteSources = @(
        (Join-Path $PSScriptRoot 'E2ETestSuite.java'),
        (Join-Path $PSScriptRoot 'E2ETestRunner.java'),
        (Join-Path $PSScriptRoot 'KotlinJavaInteropContractTest.java')
    )
    & javac -encoding UTF-8 -d $testClasses -cp $classpath @suiteSources
    if ($LASTEXITCODE -ne 0) { throw "Compiling E2E suite failed with exit code $LASTEXITCODE" }

    Write-Host 'Running headless OrcaMobile suite...'
    & java -cp $classpath scripts.tests.E2ETestRunner
    $headlessExitCode = $LASTEXITCODE
    if ($headlessExitCode -eq 0) {
        $jsonMockSources = @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'json-mocks') -Recurse -Filter '*.java' |
            ForEach-Object { $_.FullName })
        & javac -encoding UTF-8 -d $jsonMockClasses -cp $classpath @jsonMockSources
        if ($LASTEXITCODE -ne 0) { throw "Compiling JVM JSON compatibility models failed with exit code $LASTEXITCODE" }
        $interopClasspath = (@($jsonMockClasses, $testClasses, $appClasses, $appKotlinClasses) + $dependencyPaths + @($androidJar)) -join [IO.Path]::PathSeparator
        & java -cp $interopClasspath scripts.tests.KotlinJavaInteropContractTest
        $interopExitCode = $LASTEXITCODE
    } else {
        $interopExitCode = 1
    }

    Write-Host 'Running upload and filament routing regressions...'
    $uploadExitCode = 0
    try {
        & (Join-Path $PSScriptRoot 'run_gcode_upload_tests.ps1')
    } catch {
        Write-Error "Upload regression suite failed: $_" -ErrorAction Continue
        $uploadExitCode = 1
    }

    Write-Host 'Running lithophane STL geometry regressions...'
    $lithophaneExitCode = 0
    try {
        # This focused test needs current production geometry classes; the broader suite uses its Native mock first.
        $geometryClasspath = (@($appClasses, $appKotlinClasses, $testClasses) + $dependencyPaths + @($androidJar)) -join [IO.Path]::PathSeparator
        $lithophaneSource = Join-Path $PSScriptRoot 'LithophaneStlWriterTest.java'
        & javac -encoding UTF-8 -d $testClasses -cp $geometryClasspath $lithophaneSource
        if ($LASTEXITCODE -ne 0) { throw "Compiling lithophane tests failed with exit code $LASTEXITCODE" }
        & java -cp $geometryClasspath scripts.tests.LithophaneStlWriterTest
        if ($LASTEXITCODE -ne 0) { throw "Lithophane tests failed with exit code $LASTEXITCODE" }
        $inputSource = Join-Path $PSScriptRoot 'LithophaneInputTest.java'
        & javac -encoding UTF-8 -d $testClasses -cp $geometryClasspath $inputSource
        if ($LASTEXITCODE -ne 0) { throw "Compiling lithophane input tests failed with exit code $LASTEXITCODE" }
        & java -cp $geometryClasspath scripts.tests.LithophaneInputTest
        if ($LASTEXITCODE -ne 0) { throw "Lithophane input tests failed with exit code $LASTEXITCODE" }
    } catch {
        Write-Error "Lithophane geometry tests failed: $_" -ErrorAction Continue
        $lithophaneExitCode = 1
    }

    Write-Host 'Running printable fastener geometry regressions...'
    $fastenerExitCode = 0
    try {
        $geometryClasspath = (@($appClasses, $appKotlinClasses, $testClasses) + $dependencyPaths + @($androidJar)) -join [IO.Path]::PathSeparator
        $fastenerSource = Join-Path $PSScriptRoot 'FastenerStlWriterTest.java'
        & javac -encoding UTF-8 -d $testClasses -cp $geometryClasspath $fastenerSource
        if ($LASTEXITCODE -ne 0) { throw "Compiling fastener tests failed with exit code $LASTEXITCODE" }
        & java -cp $geometryClasspath scripts.tests.FastenerStlWriterTest
        if ($LASTEXITCODE -ne 0) { throw "Fastener tests failed with exit code $LASTEXITCODE" }
    } catch {
        Write-Error "Fastener geometry tests failed: $_" -ErrorAction Continue
        $fastenerExitCode = 1
    }

    if ($headlessExitCode -ne 0 -or $interopExitCode -ne 0 -or $uploadExitCode -ne 0 -or $lithophaneExitCode -ne 0 -or $fastenerExitCode -ne 0) {
        throw "Test run incomplete: headless suite exit=$headlessExitCode, Kotlin/Java ABI exit=$interopExitCode, upload regressions exit=$uploadExitCode, lithophane geometry exit=$lithophaneExitCode, fastener geometry exit=$fastenerExitCode"
    }
} finally {
    Pop-Location
}
