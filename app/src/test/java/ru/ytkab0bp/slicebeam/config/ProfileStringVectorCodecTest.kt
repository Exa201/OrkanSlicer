package ru.ytkab0bp.slicebeam.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileStringVectorCodecTest {
    @Test
    fun parsesPlainAndEmptyEntries() {
        assertEquals(listOf("first", "second"), ProfileStringVectorCodec.parse("first;second"))
        assertEquals(listOf("", "first", ""), ProfileStringVectorCodec.parse(";first;"))
        assertEquals(emptyList<String>(), ProfileStringVectorCodec.parse(""))
    }

    @Test
    fun roundTripsQuotedSeparatorsAndWhitespace() {
        val values = listOf("plain", "two words", "semi;colon", "tab\tinside", "")
        assertEquals(values, ProfileStringVectorCodec.parse(ProfileStringVectorCodec.serialize(values)))
    }

    @Test
    fun roundTripsQuotesBackslashesAndNewlines() {
        val values = listOf("a\\b", "say \"hello\"", "line one\nline two", "return\r")
        assertEquals(values, ProfileStringVectorCodec.parse(ProfileStringVectorCodec.serialize(values)))
    }

    @Test
    fun quotesSingleEmptyValueAndKeepsEmptyVectorDistinct() {
        assertEquals("\"\"", ProfileStringVectorCodec.serialize(listOf("")))
        assertEquals(listOf(""), ProfileStringVectorCodec.parse("\"\""))
        assertEquals("", ProfileStringVectorCodec.serialize(emptyList()))
        assertEquals(emptyList<String>(), ProfileStringVectorCodec.parse(""))
    }

    @Test
    fun rejectsMalformedQuotedInput() {
        assertNull(ProfileStringVectorCodec.parse("\"unterminated"))
        assertNull(ProfileStringVectorCodec.parse("\"quoted\"unexpected"))
        assertNull(ProfileStringVectorCodec.parse("\"dangling\\"))
    }
}
