package com.example.englishit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckerTest {
    @Test fun caseInsensitiveAndTrim() {
        assertTrue(AnswerChecker.check("  Starts ", listOf("starts", "begins")))
    }
    @Test fun acceptsAlternatives() {
        assertTrue(AnswerChecker.check("ends", listOf("finishes", "ends")))
    }
    @Test fun ignoresTrailingPunctuationAndSpaces() {
        assertTrue(AnswerChecker.check("What time does it start ?", listOf("What time does it start")))
        assertTrue(AnswerChecker.check("the   most  reliable.", listOf("the most reliable")))
    }
    @Test fun rejectsWrongAndEmpty() {
        assertFalse(AnswerChecker.check("", listOf("is")))
        assertFalse(AnswerChecker.check("are", listOf("is")))
    }
    @Test fun srsBoxes() {
        assertEquals(1, Srs.nextBox(0, true))
        assertEquals(5, Srs.nextBox(5, true))
        assertEquals(0, Srs.nextBox(4, false))
        assertEquals(10L, Srs.dueDay(10L, 0))
        assertEquals(12L, Srs.dueDay(10L, 2))
    }
}
