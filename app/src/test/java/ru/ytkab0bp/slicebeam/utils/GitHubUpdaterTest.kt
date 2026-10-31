package ru.ytkab0bp.slicebeam.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class GitHubUpdaterTest {
    @Test
    fun comparesNumericVersionComponentsInsteadOfLexicographically() {
        assertEquals(1, GitHubUpdater.compareVersions("0.8.10", "0.8.9"))
        assertEquals(-1, GitHubUpdater.compareVersions("0.9.0", "0.10.0"))
    }

    @Test
    fun treatsMissingTrailingComponentsAsZero() {
        assertEquals(0, GitHubUpdater.compareVersions("0.8", "v0.8.0"))
        assertEquals(0, GitHubUpdater.compareVersions("v1.2.0", "1.2"))
    }

    @Test
    fun ignoresBuildAndPrereleaseSuffixWhenComparingReleaseNumber() {
        assertEquals(0, GitHubUpdater.compareVersions("v0.8.404-beta+abc", "0.8.404"))
    }

    @Test
    fun invalidVersionDoesNotCompareAsANewerRelease() {
        assertEquals(-1, GitHubUpdater.compareVersions("not-a-version", "0.8.404"))
    }

    @Test
    fun nightlySelectionIgnoresStableDraftAndInstalledReleases() {
        val releases = listOf(
            NightlyReleaseCandidate("v0.8.99", prerelease = false, draft = false),
            NightlyReleaseCandidate("v0.8.421", prerelease = true, draft = true),
            NightlyReleaseCandidate("v0.8.418", prerelease = true, draft = false),
            NightlyReleaseCandidate("v0.8.420", prerelease = true, draft = false),
        )

        assertEquals("v0.8.420", GitHubUpdater.selectNightlyRelease(releases, "0.8.419")?.tagName)
        assertEquals(null, GitHubUpdater.selectNightlyRelease(releases, "0.8.420"))
    }
}
