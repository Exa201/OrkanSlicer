package ru.ytkab0bp.slicebeam.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class TextTagFormLogicTest {
    @Test fun defaultTextTagIsValidAndKeepsExistingDefaults() {
        val values = TextTagFormLogic.defaults()
        assertEquals("OrkanSlicer", values.text)
        assertEquals("80", TextTagFormLogic.normalized(values).width)
        assertNull(TextTagFormLogic.validate(values))
    }

    @Test fun emptyTextIsRejectedAndLongTextIsLimitedToTwentyFourCharacters() {
        assertSame(TextTagIssue.EMPTY_TEXT, TextTagFormLogic.validate(TextTagFormLogic.defaults().copy(text = "  ")))
        assertEquals(24, TextTagFormLogic.normalized(TextTagFormLogic.defaults().copy(text = "x".repeat(40))).text.length)
    }

    @Test fun invalidDimensionsUseTheExistingDefaultsAndRanges() {
        val normalized = TextTagFormLogic.normalized(TextTagFormLogic.defaults().copy(
            width = "300", height = "15", fontSize = "invalid", baseThickness = "12",
            raisedHeight = "0.2", holeDiameter = "40",
        ))
        assertEquals("180", normalized.width)
        assertEquals("18", normalized.height)
        assertEquals("10", normalized.fontSize)
        assertEquals("8", normalized.baseThickness)
        assertEquals("0.4", normalized.raisedHeight)
        assertEquals("9.9", normalized.holeDiameter)
    }
}
