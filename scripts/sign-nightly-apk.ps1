[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$ApkPath,
    [string]$OutputPath,
    [string]$KeystorePath,
    [string]$KeyAlias = 'androiddebugkey'
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$apk = (Resolve-Path -LiteralPath $ApkPath).Path
if (-not $OutputPath) {
    $OutputPath = Join-Path (Split-Path -Parent $apk) (([IO.Path]::GetFileNameWithoutExtension($apk)) + '-signed.apk')
}
if (-not $KeystorePath) {
    $KeystorePath = Join-Path $env:USERPROFILE '.android/debug.keystore'
}
if (-not (Test-Path -LiteralPath $KeystorePath -PathType Leaf)) {
    throw "Signing keystore not found: $KeystorePath"
}
if ([string]::IsNullOrWhiteSpace($env:ORKAN_NIGHTLY_SIGNING_PASSWORD)) {
    throw 'Set ORKAN_NIGHTLY_SIGNING_PASSWORD in the process environment before signing.'
}

$sdkLine = Get-Content -LiteralPath (Join-Path $repoRoot 'local.properties') |
    Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
if (-not $sdkLine) { throw 'sdk.dir is missing from local.properties.' }
$sdkPath = $sdkLine.Substring($sdkLine.IndexOf('=') + 1).Replace('\:', ':').Replace('\\', '\')
$buildTools = Get-ChildItem -LiteralPath (Join-Path $sdkPath 'build-tools') -Directory |
    Where-Object { $_.Name -match '^\d+(\.\d+)*$' } |
    Sort-Object { [version]$_.Name } -Descending | Select-Object -First 1
if (-not $buildTools) { throw "Android build-tools not found under $sdkPath" }
$apksigner = Join-Path $buildTools.FullName 'apksigner.bat'
$aapt = Join-Path $buildTools.FullName 'aapt.exe'

& $apksigner sign --ks $KeystorePath --ks-key-alias $KeyAlias `
    --ks-pass env:ORKAN_NIGHTLY_SIGNING_PASSWORD --key-pass env:ORKAN_NIGHTLY_SIGNING_PASSWORD `
    --out $OutputPath $apk
if ($LASTEXITCODE -ne 0) { throw 'apksigner could not sign the APK.' }

$verification = (& $apksigner verify --verbose --print-certs $OutputPath 2>&1 | Out-String)
if ($LASTEXITCODE -ne 0 -or $verification -notmatch '(?m)^Verifies\s*$') {
    throw "APK signature verification failed:`n$verification"
}
$certificateMatch = [regex]::Match($verification, '(?m)^Signer #1 certificate SHA-256 digest:\s*([0-9a-fA-F:]+)')
if (-not $certificateMatch.Success) { throw 'Could not read the APK signer certificate fingerprint.' }
$certificate = $certificateMatch.Groups[1].Value.Replace(':', '').ToLowerInvariant()
$expectedCertificate = '6254b703f8440e375448b8ec213ef2dc63ebd6c4094ae83eb25f39c2cf426d12'
if ($certificate -ne $expectedCertificate) {
    throw "Unexpected signing certificate $certificate; expected historical OrkanSlicer signer $expectedCertificate."
}

$badging = (& $aapt dump badging $OutputPath | Select-Object -First 1)
if ($LASTEXITCODE -ne 0) { throw 'Could not read signed APK manifest.' }
Write-Output $badging
Write-Output "Signature verified: $certificate"
Write-Output "Signed APK: $OutputPath"
