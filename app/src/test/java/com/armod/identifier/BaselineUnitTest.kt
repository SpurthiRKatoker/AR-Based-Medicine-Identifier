package com.armod.identifier

import com.armod.identifier.util.TextNormaliser
import org.junit.Assert.assertEquals
import org.junit.Test

class BaselineUnitTest {
    @Test
    fun normalisation_stripsWhitespaceAndLowercases() {
        val input = "  Paracetamol   500MG  "
        val expected = "paracetamol 500mg"
        assertEquals(expected, TextNormaliser.normalise(input))
    }
}
