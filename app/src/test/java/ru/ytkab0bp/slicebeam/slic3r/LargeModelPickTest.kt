package ru.ytkab0bp.slicebeam.slic3r

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test
import ru.ytkab0bp.slicebeam.utils.Vec3d

class LargeModelPickTest {
    private val min = Vec3d(-10.0, -10.0, 0.0)
    private val max = Vec3d(10.0, 10.0, 2.0)

    @Test fun frontHitReturnsEntryFace() {
        val hit = requireNotNull(LargeModelPick.hit(Vec3d(0.0, 0.0, 20.0), Vec3d(0.0, 0.0, -1.0), min, max))
        assertEquals(2.5, hit.position.z, 1e-9)
        assertEquals(1.0, hit.normal.z, 1e-9)
    }

    @Test fun missAndParallelRayDoNotSelectModel() {
        assertNull(LargeModelPick.hit(Vec3d(30.0, 0.0, 20.0), Vec3d(0.0, 0.0, -1.0), min, max))
        assertNull(LargeModelPick.hit(Vec3d(30.0, 0.0, 20.0), Vec3d(0.0, 1.0, 0.0), min, max))
    }

    @Test fun originInsideBoxReturnsExitFace() {
        val hit = requireNotNull(LargeModelPick.hit(Vec3d(0.0, 0.0, 1.0), Vec3d(1.0, 0.0, 0.0), min, max))
        assertEquals(10.5, hit.position.x, 1e-9)
        assertEquals(1.0, hit.normal.x, 1e-9)
    }

    @Test fun thinFlatModelCanStillBeSelected() {
        val hit = LargeModelPick.hit(
            Vec3d(0.0, 0.0, 5.0), Vec3d(0.0, 0.0, -1.0),
            Vec3d(-1.0, -1.0, 0.0), Vec3d(1.0, 1.0, 0.0),
        )
        assertNotNull(hit)
    }
}
