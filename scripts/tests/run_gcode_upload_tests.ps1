param(
    [string]$GcodeFile = '',
    [string[]]$GcodeFiles = @(),
    [string[]]$AnycubicGcodeFiles = @()
)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
Push-Location $projectRoot
try {
    $outputDirectory = Join-Path $projectRoot 'app/build/gcode-upload-tests'
    # Keep this directory limited to the Java test classes. The older shared
    # "classes" folder also contained copied app classes and could shadow the
    # freshly compiled Kotlin classes on the test classpath.
    $testClassesDirectory = Join-Path $outputDirectory 'classes-current'
    New-Item -ItemType Directory -Force -Path $testClassesDirectory | Out-Null

    $classpathFile = $env:ORCA_TEST_CLASSPATH_FILE
    if (-not [string]::IsNullOrWhiteSpace($classpathFile) -and (Test-Path -LiteralPath $classpathFile -PathType Leaf)) {
        $dependencyPaths = @(Get-Content -LiteralPath $classpathFile | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
        Write-Host 'Using the dependency classpath prepared by the grouped verification run.'
    } else {
        $gradleOutput = & .\gradlew.bat -q -I scripts/tests/init.gradle :app:compileDebugKotlin :app:compileDebugJavaWithJavac :app:printClasspath
        if ($LASTEXITCODE -ne 0) { throw "Gradle classpath resolution failed with exit code $LASTEXITCODE" }
        $dependencyPaths = @($gradleOutput | Where-Object { $_ -like 'CLASSPATH_ENTRY:*' } |
            ForEach-Object { $_.Substring('CLASSPATH_ENTRY:'.Length) })
    }
    if (-not $dependencyPaths) { throw 'Gradle did not return compile classpath entries' }

    $appJavaClasses = Join-Path $projectRoot 'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'
    $appKotlinClasses = Join-Path $projectRoot 'app/build/tmp/kotlin-classes/debug'
    $classpath = (@($testClassesDirectory, $appJavaClasses, $appKotlinClasses) + $dependencyPaths) -join [IO.Path]::PathSeparator
    $sources = @(
        'scripts/tests/GCodeToolMapperTest.java',
        'scripts/tests/AnycubicGCodeProcessorTest.java',
        'scripts/tests/UploadFileValidatorTest.java',
        'scripts/tests/PrinterWebOriginPolicyTest.java',
        'scripts/tests/GCodeIntegrityValidatorTest.java',
        'scripts/tests/FilamentPaletteSyncPolicyTest.java'
    )
    & javac -d $testClassesDirectory -cp $classpath @sources
    if ($LASTEXITCODE -ne 0) { throw "javac failed with exit code $LASTEXITCODE" }

    & java -cp $classpath scripts.tests.GCodeToolMapperTest
    if ($LASTEXITCODE -ne 0) { throw "GCodeToolMapperTest failed with exit code $LASTEXITCODE" }

    & java -cp $classpath scripts.tests.FilamentPaletteSyncPolicyTest
    if ($LASTEXITCODE -ne 0) { throw "FilamentPaletteSyncPolicyTest failed with exit code $LASTEXITCODE" }

    & java -cp $classpath scripts.tests.UploadFileValidatorTest
    if ($LASTEXITCODE -ne 0) { throw "UploadFileValidatorTest failed with exit code $LASTEXITCODE" }

    & java -cp $classpath scripts.tests.PrinterWebOriginPolicyTest
    if ($LASTEXITCODE -ne 0) { throw "PrinterWebOriginPolicyTest failed with exit code $LASTEXITCODE" }

    $validationFiles = @((@($GcodeFiles) + @($GcodeFile)) |
        Where-Object { -not [string]::IsNullOrWhiteSpace($_) -and (Test-Path -LiteralPath $_ -PathType Leaf) } |
        Select-Object -Unique)
    if ($validationFiles.Count -gt 0) {
        & java -cp $classpath scripts.tests.GCodeIntegrityValidatorTest @validationFiles
    } else {
        & java -cp $classpath scripts.tests.GCodeIntegrityValidatorTest
    }
    if ($LASTEXITCODE -ne 0) { throw "GCodeIntegrityValidatorTest failed with exit code $LASTEXITCODE" }

    $anycubicFiles = @((@($AnycubicGcodeFiles) + @($GcodeFile)) |
        Where-Object { -not [string]::IsNullOrWhiteSpace($_) -and (Test-Path -LiteralPath $_ -PathType Leaf) } |
        Select-Object -Unique)
    if ($anycubicFiles.Count -gt 0) {
        & java -cp $classpath scripts.tests.AnycubicGCodeProcessorTest @anycubicFiles
    } else {
        & java -cp $classpath scripts.tests.AnycubicGCodeProcessorTest
    }
    if ($LASTEXITCODE -ne 0) { throw "AnycubicGCodeProcessorTest failed with exit code $LASTEXITCODE" }
} finally {
    Pop-Location
}
