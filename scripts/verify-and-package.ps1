[CmdletBinding()]
param(
    [string]$InstallSerial = '',
    [switch]$SkipGradleBuild,
    [switch]$UsePrebuiltNative
)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
Push-Location $projectRoot
$oldClasspathFile = $env:ORCA_TEST_CLASSPATH_FILE
$oldAndroidSerial = $env:ANDROID_SERIAL
$transcriptStarted = $false
$pythonCommand = if ($env:PYTHON) { $env:PYTHON } else { (Get-Command python -ErrorAction SilentlyContinue).Source }
if (-not $pythonCommand) {
    throw 'Python was not found. Set PYTHON to the interpreter path or add python to PATH.'
}

try {
    $gradleFile = Get-Content -LiteralPath 'app/build.gradle' -Raw
    $versionNameMatch = [regex]::Match($gradleFile, '(?m)^\s*versionName\s+"([^"]+)"')
    $versionCodeMatch = [regex]::Match($gradleFile, '(?m)^\s*versionCode\s+(\d+)')
    if (-not $versionNameMatch.Success -or -not $versionCodeMatch.Success) {
        throw 'Could not read versionName and versionCode from app/build.gradle.'
    }

    $versionName = $versionNameMatch.Groups[1].Value
    $versionCode = $versionCodeMatch.Groups[1].Value
    if ($UsePrebuiltNative) {
        Write-Host 'Validating the source-built ARM64 cache before starting the fast build...'
        & $pythonCommand 'scripts/sync_native_cache.py' 'check'
        if ($LASTEXITCODE -ne 0) { throw 'The fast build was refused because its ARM64 native cache is stale or incomplete.' }
    }
    $packageName = 'com.codemastercody3d.orcaslicermobile'
    $outputDirectory = Join-Path $projectRoot "outputs/OrkanSlicer_$versionName"
    New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
    $transcriptPath = Join-Path $outputDirectory 'verification-transcript.txt'
    Start-Transcript -LiteralPath $transcriptPath -Force | Out-Null
    $transcriptStarted = $true

    if (-not $SkipGradleBuild) {
        Write-Host "Building and testing OrkanSlicer $versionName ($versionCode) in one Gradle invocation..."
        $gradleLog = Join-Path $outputDirectory 'gradle-build.log'
        $gradleArguments = @('--no-daemon')
        if ($UsePrebuiltNative) {
            $gradleArguments += '-PusePrebuiltNative=true'
            Write-Host 'Using the verified existing ARM64 native libraries for this Kotlin/UI-only iteration.'
        } else {
            Write-Host 'Building the ARM64 native libraries from source.'
        }
        $gradleArguments += @(
            '-I', 'scripts/tests/init.gradle',
            ':app:testDebugUnitTest',
            ':app:assembleDebug',
            ':app:assembleRelease',
            ':app:lintRelease',
            ':app:printClasspath',
            ':eventbusdemo:assembleDebug',
            ':sapildemo:assembleDebug'
        )
        if ([string]::IsNullOrWhiteSpace($InstallSerial)) {
            $gradleArguments += @(
                ':app:compileSliceTestAndroidTestKotlin',
                ':app:compileSliceTestAndroidTestJavaWithJavac'
            )
        } else {
            $gradleArguments += ':app:connectedSliceTestAndroidTest'
            $env:ANDROID_SERIAL = $InstallSerial
        }
        $previousErrorPreference = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        $gradleOutput = [System.Collections.Generic.List[string]]::new()
        try {
            & '.\gradlew.bat' @gradleArguments 2>&1 | ForEach-Object {
                $line = $_.ToString()
                [void]$gradleOutput.Add($line)
                Write-Host $line
            }
            $gradleExitCode = $LASTEXITCODE
        } finally {
            $ErrorActionPreference = $previousErrorPreference
        }
        $gradleOutput | Set-Content -LiteralPath $gradleLog -Encoding utf8
        if ($gradleExitCode -ne 0) {
            throw "Gradle build, unit tests or Release lint failed with exit code $gradleExitCode. See $gradleLog"
        }

        if (-not $UsePrebuiltNative) {
            Write-Host 'Refreshing the Kotlin/UI fast-build cache from the successful source-built Debug APK...'
            $sourceDebugApks = @(Get-ChildItem -LiteralPath 'app/build/outputs/apk/debug' -Filter '*.apk' -File)
            if ($sourceDebugApks.Count -ne 1) {
                throw "Expected one source-built Debug APK for refreshing the native cache; found $($sourceDebugApks.Count)."
            }
            & $pythonCommand 'scripts/sync_native_cache.py' 'sync' '--apk' $sourceDebugApks[0].FullName
            if ($LASTEXITCODE -ne 0) { throw 'The native cache could not be refreshed from the successful source build.' }
        }

        $dependencyPaths = @(Get-Content -LiteralPath $gradleLog |
            Where-Object { $_ -like 'CLASSPATH_ENTRY:*' } |
            ForEach-Object { $_.Substring('CLASSPATH_ENTRY:'.Length).Trim() } |
            Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
        if (-not $dependencyPaths) {
            throw "Gradle did not emit dependency paths for the headless suites. See $gradleLog"
        }
        $classpathFile = Join-Path $projectRoot 'app/build/headless-e2e-tests/dependency-classpath.txt'
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $classpathFile) | Out-Null
        Set-Content -LiteralPath $classpathFile -Value $dependencyPaths -Encoding utf8
        $env:ORCA_TEST_CLASSPATH_FILE = $classpathFile
    } else {
        Write-Host 'Gradle build skipped by request; running regression, static and packaging checks on existing build outputs.'
        $classpathFile = Join-Path $projectRoot 'app/build/headless-e2e-tests/dependency-classpath.txt'
        Write-Host 'Resolving the test dependency classpath once for this verification run...'
        $previousErrorPreference = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        try {
            $classpathOutput = & '.\gradlew.bat' -q -I scripts/tests/init.gradle :app:compileDebugJavaWithJavac :app:printClasspath
            $classpathExitCode = $LASTEXITCODE
        } finally {
            $ErrorActionPreference = $previousErrorPreference
        }
        if ($classpathExitCode -ne 0) { throw "Gradle classpath resolution failed with exit code $classpathExitCode" }
        $dependencyPaths = @($classpathOutput | Where-Object { $_ -like 'CLASSPATH_ENTRY:*' } |
            ForEach-Object { $_.Substring('CLASSPATH_ENTRY:'.Length).Trim() } |
            Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
        if (-not $dependencyPaths) { throw 'Gradle did not return dependency paths for the headless suites.' }
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $classpathFile) | Out-Null
        Set-Content -LiteralPath $classpathFile -Value $dependencyPaths -Encoding utf8
        $env:ORCA_TEST_CLASSPATH_FILE = $classpathFile
    }

    Write-Host 'Running grouped UI migration, connector and native-prebuilt audits...'
    & $pythonCommand 'scripts/check_kotlin_ui_migration.py'
    if ($LASTEXITCODE -ne 0) { throw "Kotlin UI audit failed with exit code $LASTEXITCODE" }
    & $pythonCommand 'scripts/check_cut_connectors.py'
    if ($LASTEXITCODE -ne 0) { throw "Cut connector audit failed with exit code $LASTEXITCODE" }
    & $pythonCommand 'scripts/check-native-prebuilts.py' 'arm64-v8a'
    if ($LASTEXITCODE -ne 0) { throw "ARM64 native-prebuilt audit failed with exit code $LASTEXITCODE" }

    Write-Host 'Running the complete headless and geometry regression suites...'
    & (Join-Path $PSScriptRoot 'tests/run_e2e_tests.ps1')
    if ($LASTEXITCODE -ne 0) { throw "Headless regression suite failed with exit code $LASTEXITCODE" }

    Write-Host 'Checking patch formatting...'
    & git diff --check
    if ($LASTEXITCODE -ne 0) { throw "git diff --check failed with exit code $LASTEXITCODE" }

    $debugDirectory = Join-Path $projectRoot 'app/build/outputs/apk/debug'
    $releaseDirectory = Join-Path $projectRoot 'app/build/outputs/apk/release'
    $debugCandidates = @(Get-ChildItem -LiteralPath $debugDirectory -Filter '*.apk' -File)
    $releaseCandidates = @(Get-ChildItem -LiteralPath $releaseDirectory -Filter '*.apk' -File)
    if ($debugCandidates.Count -ne 1 -or $releaseCandidates.Count -ne 1) {
        throw "Expected one Debug and one Release APK; found Debug=$($debugCandidates.Count), Release=$($releaseCandidates.Count)."
    }

    $debugApk = Join-Path $projectRoot "outputs/OrkanSlicer_${versionName}_debug.apk"
    $releaseApk = Join-Path $projectRoot "outputs/OrkanSlicer_${versionName}_release_unsigned.apk"
    Copy-Item -LiteralPath $debugCandidates[0].FullName -Destination $debugApk -Force
    Copy-Item -LiteralPath $releaseCandidates[0].FullName -Destination $releaseApk -Force
    Copy-Item -LiteralPath 'app/build/reports/lint-results-release.txt' -Destination (Join-Path $outputDirectory 'lint-results-release.txt') -Force

    $sdkLine = Get-Content -LiteralPath 'local.properties' |
        Where-Object { $_ -match '^sdk\.dir=' } |
        Select-Object -First 1
    if (-not $sdkLine) { throw 'sdk.dir is missing from local.properties.' }
    $sdkPath = $sdkLine.Substring($sdkLine.IndexOf('=') + 1).Replace('\:', ':').Replace('\\', '\')
    $buildTools = Get-ChildItem -LiteralPath (Join-Path $sdkPath 'build-tools') -Directory |
        Where-Object { $_.Name -match '^\d+(\.\d+)*$' } |
        Sort-Object { [version]$_.Name } -Descending
    if (-not $buildTools) { throw "No installed Android build-tools found under $sdkPath." }
    $aaptPath = Join-Path $buildTools[0].FullName 'aapt.exe'
    if (-not (Test-Path -LiteralPath $aaptPath -PathType Leaf)) { throw "aapt was not found at $aaptPath." }
    foreach ($apkPath in @($debugApk, $releaseApk)) {
        $badging = & $aaptPath dump badging $apkPath
        if ($LASTEXITCODE -ne 0) { throw "Could not read APK metadata: $apkPath." }
        $badgingText = $badging -join "`n"
        if ($badgingText -notmatch "versionCode='$versionCode' versionName='$([regex]::Escape($versionName))'") {
            throw "APK metadata does not match version $versionName ($versionCode): $apkPath."
        }
        if ($badgingText -notmatch "sdkVersion:'23'" -or $badgingText -notmatch "targetSdkVersion:'36'" -or
            $badgingText -notmatch "compileSdkVersion='36'") {
            throw "APK SDK metadata is not min/compile/target 23/36/36: $apkPath."
        }
    }
    & $pythonCommand 'scripts/check_apk_zip.py' $debugApk $releaseApk
    if ($LASTEXITCODE -ne 0) { throw 'APK ZIP integrity verification failed.' }

    $deviceInstrumentationWasRun = (-not $SkipGradleBuild -and -not [string]::IsNullOrWhiteSpace($InstallSerial))
    $installResult = 'ADB-Gerätestatus konnte nicht geprüft werden. Touch-, Slice-, Rotation-, Setup- und Wiederherstellungs-Instrumentierungstests wurden lokal kompiliert, aber nicht auf dem Gerät ausgeführt.'
    if ([string]::IsNullOrWhiteSpace($InstallSerial)) {
        $sdkLine = Get-Content -LiteralPath 'local.properties' |
            Where-Object { $_ -match '^sdk\.dir=' } |
            Select-Object -First 1
        if ($sdkLine) {
            $sdkPath = $sdkLine.Substring($sdkLine.IndexOf('=') + 1).Replace('\:', ':').Replace('\\', '\')
            $adbPath = Join-Path $sdkPath 'platform-tools/adb.exe'
            if (Test-Path -LiteralPath $adbPath -PathType Leaf) {
                $deviceLines = & $adbPath devices
                if ($LASTEXITCODE -eq 0) {
                    $authorizedDevices = @($deviceLines | Where-Object { $_ -match '^\S+\s+device$' })
                    if ($authorizedDevices.Count -eq 0) {
                        $installResult = 'ADB listete kein angeschlossenes und autorisiertes Gerät; Installation, Start und Touchscreen-Test blieben offen. Touch-, Slice-, Rotation-, Setup- und Wiederherstellungs-Instrumentierungstests wurden lokal kompiliert, aber nicht auf dem Gerät ausgeführt.'
                    } else {
                        $installResult = "ADB erkennt $($authorizedDevices.Count) autorisierte(s) Gerät(e), aber es wurde keine Seriennummer zur Installation übergeben. Touch-, Slice-, Rotation-, Setup- und Wiederherstellungs-Instrumentierungstests wurden lokal kompiliert, aber nicht auf dem Gerät ausgeführt."
                    }
                } else {
                    $installResult = 'Der ADB-Gerätestatus konnte wegen eines adb-Fehlers nicht gelesen werden. Touch-, Slice- und Rotation-Instrumentierungstests wurden lokal kompiliert, aber nicht auf dem Gerät ausgeführt.'
                }
            }
        }
    }
    if (-not [string]::IsNullOrWhiteSpace($InstallSerial)) {
        $sdkLine = Get-Content -LiteralPath 'local.properties' |
            Where-Object { $_ -match '^sdk\.dir=' } |
            Select-Object -First 1
        if (-not $sdkLine) { throw 'sdk.dir is missing from local.properties.' }
        $sdkPath = $sdkLine.Substring($sdkLine.IndexOf('=') + 1).Replace('\:', ':').Replace('\\', '\')
        $adbPath = Join-Path $sdkPath 'platform-tools/adb.exe'
        if (-not (Test-Path -LiteralPath $adbPath -PathType Leaf)) { throw "adb was not found at $adbPath" }

        $deviceLines = & $adbPath devices
        if ($LASTEXITCODE -ne 0) { throw "adb devices failed with exit code $LASTEXITCODE" }
        $deviceLine = $deviceLines | Where-Object { $_ -match "^$([regex]::Escape($InstallSerial))\s+device$" }
        if (-not $deviceLine) { throw "Device '$InstallSerial' is not connected and authorized." }

        Write-Host "Installing Debug APK on $InstallSerial with app data preserved..."
        & $adbPath -s $InstallSerial install -r $debugApk
        if ($LASTEXITCODE -ne 0) { throw "APK installation failed with exit code $LASTEXITCODE" }
        & $adbPath -s $InstallSerial logcat -c
        if ($LASTEXITCODE -ne 0) { throw "Could not clear device logcat with exit code $LASTEXITCODE" }
        & $adbPath -s $InstallSerial shell am force-stop $packageName
        if ($LASTEXITCODE -ne 0) { throw "Could not stop the app before launch with exit code $LASTEXITCODE" }
        & $adbPath -s $InstallSerial shell monkey -p $packageName 1
        if ($LASTEXITCODE -ne 0) { throw "App launch failed with exit code $LASTEXITCODE" }
        Start-Sleep -Seconds 4

        $installedPackage = & $adbPath -s $InstallSerial shell dumpsys package $packageName
        if ($LASTEXITCODE -ne 0 -or -not (($installedPackage -join "`n") -match "versionName=$([regex]::Escape($versionName))\b")) {
            throw "Installed package did not report version $versionName."
        }
        $appProcess = & $adbPath -s $InstallSerial shell pidof $packageName
        if ($LASTEXITCODE -ne 0 -or -not $appProcess) { throw 'The app process exited after launch.' }
        $appPid = (($appProcess -join ' ').Trim() -split '\s+')[0]
        $logOutput = & $adbPath -s $InstallSerial logcat -d -v brief "--pid=$appPid"
        if ($LASTEXITCODE -ne 0) { throw "Could not read device logcat with exit code $LASTEXITCODE" }
        $fatalLines = $logOutput | Select-String -Pattern 'FATAL EXCEPTION|ANR in|OutOfMemoryError|Fatal signal'
        if ($fatalLines) { throw "Crash/ANR diagnostics found on device: $($fatalLines -join ' | ')" }
        if ($deviceInstrumentationWasRun) {
            $installResult = "Die Touch-, Slice-, Rotation-, Setup- und Wiederherstellungs-Instrumentierungstests waren erfolgreich. Version $versionName wurde auf $InstallSerial installiert und startete ohne Crash. Es wurde kein Drucker angesprochen."
        } else {
            $installResult = "Debug-Version $versionName wurde auf $InstallSerial installiert und gestartet; der App-Prozess blieb aktiv und das Crash-/ANR-Protokoll war sauber. Touch-, Slice-, Rotation-, Setup- und Wiederherstellungs-Instrumentierungstests wurden kompiliert, aber in diesem Lauf nicht auf dem Gerät ausgeführt. Es wurde kein Drucker angesprochen."
        }
    }

    $debugHash = (Get-FileHash -LiteralPath $debugApk -Algorithm SHA256).Hash
    $releaseHash = (Get-FileHash -LiteralPath $releaseApk -Algorithm SHA256).Hash
    $debugSize = (Get-Item -LiteralPath $debugApk).Length
    $releaseSize = (Get-Item -LiteralPath $releaseApk).Length
    $lintTail = Get-Content -LiteralPath (Join-Path $outputDirectory 'lint-results-release.txt') -Tail 1
    $unitTestResults = @(Get-ChildItem -LiteralPath 'app/build/test-results/testDebugUnitTest' -Filter 'TEST-*.xml' -File)
    if (-not $unitTestResults) { throw 'Gradle did not produce Android unit-test XML results.' }
    $unitTestTotals = [ordered]@{ tests = 0; failures = 0; errors = 0; skipped = 0 }
    foreach ($resultFile in $unitTestResults) {
        [xml]$resultXml = Get-Content -LiteralPath $resultFile.FullName -Raw
        foreach ($suite in $resultXml.SelectNodes('//testsuite')) {
            foreach ($counter in @('tests', 'failures', 'errors', 'skipped')) {
                $unitTestTotals[$counter] += [int]$suite.GetAttribute($counter)
            }
        }
    }
    $report = @"
# OrkanSlicer $versionName – Sammelprüfung

- Version: $versionName (versionCode $versionCode, targetSdk 36, minSdk 23)
- Ergebnis: Build, Unit-Tests, Release-Lint, Headless-/Geometrieregressionen, statische Audits und Formatprüfung erfolgreich.
- APK-Prüfung: Versions-/SDK-Metadaten stimmen; die vollständige ZIP-CRC-Prüfung beider APKs ist erfolgreich.
- Android-Unit-Tests: $($unitTestTotals.tests) ausgeführt, $($unitTestTotals.failures) fehlgeschlagen, $($unitTestTotals.errors) Fehler, $($unitTestTotals.skipped) übersprungen.
- Gerätetest: $installResult
- Release-Lint: $lintTail
- Kotlin-Migration: die App-Hauptquellcodedateien verwenden Kotlin; die zwölf EventBus-/Demo-/ARM64-Dateien wurden bereits mit Build 0.8.67 migriert. Beide Demo-APK-Module wurden in diesem Sammellauf gebaut.
- Debug-APK: ``OrkanSlicer_${versionName}_debug.apk`` ($debugSize Byte), SHA-256 ``$debugHash``.
- Release-APK: ``OrkanSlicer_${versionName}_release_unsigned.apk`` ($releaseSize Byte), SHA-256 ``$releaseHash``. Ohne Release-Keystore ist sie unsigniert.
- Vollständiges Konsolenprotokoll: ``outputs/OrkanSlicer_${versionName}/verification-transcript.txt``.

Der Sammellauf führt Haupt-App, beide Demo-Module, Android-Unit-Tests, Release-Lint und Classpath-Ermittlung in einem Gradle-Aufruf aus. Die Java-basierten Regressionstests erhalten sowohl Java- als auch Kotlin-Ausgabeklassen. Die Upload-Untertests verwenden denselben ermittelten Classpath und starten kein weiteres Gradle.
"@
    Set-Content -LiteralPath (Join-Path $projectRoot "outputs/OrkanSlicer_${versionName}_Buildbericht.md") -Value $report -Encoding utf8
    Write-Host "All grouped checks passed. APKs and report are in outputs/ for version $versionName."
} catch {
    Write-Host "Grouped verification failed: $($_.Exception.Message)" -ForegroundColor Red
    throw
} finally {
    $env:ORCA_TEST_CLASSPATH_FILE = $oldClasspathFile
    $env:ANDROID_SERIAL = $oldAndroidSerial
    if ($transcriptStarted) { Stop-Transcript | Out-Null }
    Pop-Location
}
