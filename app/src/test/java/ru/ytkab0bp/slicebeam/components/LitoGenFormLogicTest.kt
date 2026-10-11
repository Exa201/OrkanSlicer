package ru.ytkab0bp.slicebeam.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LitoGenFormLogicTest {
    @Test
    fun defaultsAndLocalizedDecimalInputsAreValid() {
        val defaults = options()
        assertNull(LitoGenFormLogic.validate(defaults))
        assertEquals(60.5f, LitoGenFormLogic.parseNumber("60,5"))
        assertEquals(60.5f, LitoGenFormLogic.parseNumber(" 60.5 "))
    }

    @Test
    fun invalidSizeAndNonFiniteValuesAreRejected() {
        assertEquals(LitoGenValidation.SIZE, LitoGenFormLogic.validate(options(widthMm = 34.9f)))
        assertEquals(LitoGenValidation.SIZE, LitoGenFormLogic.validate(options(heightMm = Float.NaN)))
    }

    @Test
    fun thicknessBoundsAndMinimumToMaximumOrderAreEnforced() {
        assertEquals(LitoGenValidation.THICKNESS, LitoGenFormLogic.validate(options(minThicknessMm = .79f)))
        assertEquals(LitoGenValidation.THICKNESS, LitoGenFormLogic.validate(options(maxThicknessMm = 1.9f)))
        assertEquals(LitoGenValidation.THICKNESS, LitoGenFormLogic.validate(options(minThicknessMm = 3f, maxThicknessMm = 3f)))
    }

    @Test
    fun toneMappingAndMeshDetailStayWithinSupportedRanges() {
        assertEquals(LitoGenValidation.BRIGHTNESS, LitoGenFormLogic.validate(options(brightness = 51)))
        assertEquals(LitoGenValidation.CONTRAST, LitoGenFormLogic.validate(options(contrast = 49)))
        assertEquals(LitoGenValidation.DETAIL, LitoGenFormLogic.validate(options(detailIndex = 4)))
        assertNull(LitoGenFormLogic.validate(options(brightness = -50, contrast = 180, detailIndex = 3)))
    }

    private fun options(
        widthMm: Float = 60f,
        heightMm: Float = 60f,
        minThicknessMm: Float = 1.2f,
        maxThicknessMm: Float = 4f,
        brightness: Int = 0,
        contrast: Int = 100,
        invert: Boolean = false,
        detailIndex: Int = 1,
    ) = LitoGenOptions(widthMm, heightMm, minThicknessMm, maxThicknessMm, brightness, contrast, invert, detailIndex)
}
