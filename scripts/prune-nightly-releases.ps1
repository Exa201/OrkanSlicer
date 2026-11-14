[CmdletBinding(SupportsShouldProcess = $true)]
param(
    [string]$Repository = 'Exa201/OrkanSlicer-Nightly-2.5',
    [ValidateRange(2, 100)]
    [int]$Retain = 5
)

$releases = gh release list --repo $Repository --limit 100 --json tagName,name,createdAt,isPrerelease,isDraft | ConvertFrom-Json
$nightlies = @($releases | Where-Object { $_.isPrerelease -and -not $_.isDraft } | ForEach-Object {
    $versionMatch = [regex]::Match($_.tagName, '^v(\d+\.\d+\.\d+)-nightly(?:\.|$)')
    if (-not $versionMatch.Success) { throw "Nightly release tag has no semantic app version: $($_.tagName)" }
    [pscustomobject]@{
        tagName = $_.tagName
        name = $_.name
        createdAt = $_.createdAt
        version = [version]$versionMatch.Groups[1].Value
    }
} | Sort-Object version -Descending)
$expired = @($nightlies | Select-Object -Skip $Retain)

if ($expired.Count -eq 0) {
    Write-Host "Found $($nightlies.Count) published Nightly prerelease(s); nothing to remove."
    exit 0
}

Write-Host "Keeping the newest $Retain Nightly prereleases:"
$nightlies | Select-Object -First $Retain | ForEach-Object { Write-Host "  $($_.tagName) $($_.name)" }
foreach ($release in $expired) {
    $target = "$Repository/$($release.tagName)"
    if ($PSCmdlet.ShouldProcess($target, 'Delete old Nightly release and tag')) {
        gh release delete $release.tagName --repo $Repository --cleanup-tag --yes
        if ($LASTEXITCODE -ne 0) { throw "Failed to delete $($release.tagName)" }
        Write-Host "Deleted $($release.tagName)"
    }
}
