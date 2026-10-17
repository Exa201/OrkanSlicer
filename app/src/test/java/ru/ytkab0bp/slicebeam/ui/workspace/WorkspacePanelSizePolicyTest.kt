package ru.ytkab0bp.slicebeam.ui.workspace

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspacePanelSizePolicyTest {
    @Test
    fun fixedPortraitPanelIsBoundedByComposeAvailableDp() {
        assertEquals(316f, WorkspacePanelSizePolicy.resolve(WorkspacePanelDimension.Fixed(316f), 720f), 0f)
        assertEquals(280f, WorkspacePanelSizePolicy.resolve(WorkspacePanelDimension.Fixed(316f), 280f), 0f)
    }

    @Test
    fun fractionAndFillUseComposeAvailableSpace() {
        assertEquals(360f, WorkspacePanelSizePolicy.resolve(WorkspacePanelDimension.Fraction(0.5f), 720f), 0f)
        assertEquals(720f, WorkspacePanelSizePolicy.resolve(WorkspacePanelDimension.FillAvailable, 720f), 0f)
    }

    @Test
    fun clampedFractionPreservesCutPanelSizingAcrossViewportSizes() {
        val portrait = WorkspacePanelDimension.ClampedFraction(1f / 3f, minDp = 240f, maxDp = 300f)
        assertEquals(240f, WorkspacePanelSizePolicy.resolve(portrait, 600f), 0.01f)
        assertEquals(300f, WorkspacePanelSizePolicy.resolve(portrait, 1200f), 0.01f)
        assertEquals(180f, WorkspacePanelSizePolicy.resolve(portrait, 180f), 0.01f)

        val landscape = WorkspacePanelDimension.ClampedFraction(0.42f, maxDp = 360f)
        assertEquals(336f, WorkspacePanelSizePolicy.resolve(landscape, 800f), 0.01f)
        assertEquals(360f, WorkspacePanelSizePolicy.resolve(landscape, 1200f), 0.01f)
    }

    @Test
    fun invalidFractionsAndAvailableSizesStayInsideBounds() {
        assertEquals(0f, WorkspacePanelSizePolicy.resolve(WorkspacePanelDimension.Fraction(-1f), 720f), 0f)
        assertEquals(720f, WorkspacePanelSizePolicy.resolve(WorkspacePanelDimension.Fraction(2f), 720f), 0f)
        assertEquals(0f, WorkspacePanelSizePolicy.resolve(WorkspacePanelDimension.Fixed(100f), -4f), 0f)
        assertEquals(0f, WorkspacePanelSizePolicy.resolve(WorkspacePanelDimension.Fixed(Float.NaN), 720f), 0f)
        assertEquals(0f, WorkspacePanelSizePolicy.resolve(WorkspacePanelDimension.Fraction(Float.NaN), 720f), 0f)
    }
}
