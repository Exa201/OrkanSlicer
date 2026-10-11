package ru.ytkab0bp.slicebeam.view

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnackbarSwipePolicyTest {
    @Test
    fun aFastRightFlingDismissesPastTheVisibleCard() {
        val release = SnackbarSwipePolicy.release(
            lifetimeMs = 2500,
            velocityX = 1500f,
            viewWidth = 320,
            marginPx = 24,
        )

        assertTrue(release.dismiss)
        assertEquals(344f, release.targetX)
    }

    @Test
    fun aFastLeftFlingDismissesPastTheVisibleCard() {
        val release = SnackbarSwipePolicy.release(
            lifetimeMs = 5000,
            velocityX = -1800f,
            viewWidth = 320,
            marginPx = 24,
        )

        assertTrue(release.dismiss)
        assertEquals(-344f, release.targetX)
    }

    @Test
    fun aSlowSwipeReturnsToItsOriginalPosition() {
        val release = SnackbarSwipePolicy.release(
            lifetimeMs = 2500,
            velocityX = 1499.9f,
            viewWidth = 320,
            marginPx = 24,
        )

        assertFalse(release.dismiss)
        assertEquals(0f, release.targetX)
    }

    @Test
    fun persistentTaggedProgressCannotBeDismissedBySwiping() {
        val release = SnackbarSwipePolicy.release(
            lifetimeMs = 0,
            velocityX = 2400f,
            viewWidth = 320,
            marginPx = 24,
        )

        assertFalse(release.dismiss)
        assertEquals(0f, release.targetX)
    }

    @Test
    fun invalidGeometryDoesNotDismissTheSnackbar() {
        val release = SnackbarSwipePolicy.release(
            lifetimeMs = 2500,
            velocityX = 2400f,
            viewWidth = 0,
            marginPx = 24,
        )

        assertFalse(release.dismiss)
        assertEquals(0f, release.targetX)
    }
}
