package com.armod.identifier.ocr

import com.armod.identifier.util.TextNormaliser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure JVM Unit tests for OCR mapping, line extraction, text normalisation, and state handling.
 *
 * Limitation note: Google ML Kit hardware image recognition requires an Android runtime environment
 * (Camera hardware / OpenGL / Android context). Therefore, pure JVM unit tests cover text mapping,
 * sanitisation, regex extraction, and state representation, while live camera image recognition
 * is verified directly on the physical Android device (Redmi Note 11).
 */
class OcrPipelineUnitTest {

    @Test
    fun textNormaliser_stripsWhitespaceAndLowercases() {
        val noisyInput = "   Paracetamol    500   MG   \t\n"
        val normalised = TextNormaliser.normalise(noisyInput)
        assertEquals("paracetamol 500 mg", normalised)

        val cetirizineInput = "  CETIRIZINE   10   mg  "
        assertEquals("cetirizine 10 mg", TextNormaliser.normalise(cetirizineInput))
    }

    @Test
    fun textNormaliser_cleansOcrLines_handlesNoiseAndLinebreaks() {
        val rawOcr = """
            
            Crocin 500
            ---
            Paracetamol 500 mg
               
            B. No. ABC123
            Expiry: 08/2027
            ...
        """.trimIndent()

        val cleanedLines = TextNormaliser.cleanOcrLines(rawOcr)
        assertEquals(4, cleanedLines.size)
        assertEquals("Crocin 500", cleanedLines[0])
        assertEquals("Paracetamol 500 mg", cleanedLines[1])
        assertEquals("B. No. ABC123", cleanedLines[2])
        assertEquals("Expiry: 08/2027", cleanedLines[3])
    }

    @Test
    fun textNormaliser_extractsExpiryCandidate_fromVariousFormats() {
        val sample1 = "Crocin 500 mg Tablet\nExpiry: 08/2027\nBatch No. 1234"
        assertEquals("08/2027", TextNormaliser.extractExpiryCandidate(sample1))

        val sample2 = "Paracetamol IP 650 mg EXP. 11/26 MFG. 12/23"
        assertEquals("11/26", TextNormaliser.extractExpiryCandidate(sample2))

        val sample3 = "Cetirizine 10 mg EXP DATE: 05/2029"
        assertEquals("05/2029", TextNormaliser.extractExpiryCandidate(sample3))

        val sample4 = "USE BY 12/2025 KEEP OUT OF REACH OF CHILDREN"
        assertEquals("12/2025", TextNormaliser.extractExpiryCandidate(sample4))

        val sampleNoExpiry = "Only Paracetamol 500 mg with no dates"
        assertNull(TextNormaliser.extractExpiryCandidate(sampleNoExpiry))
    }

    @Test
    fun textNormaliser_extractsBatchCandidate_fromVariousFormats() {
        val sample1 = "Crocin 500 mg\nBatch No. ABC123\nExp: 08/2027"
        assertEquals("ABC123", TextNormaliser.extractBatchCandidate(sample1))

        val sample2 = "LOT: B789X MFG 2024"
        assertEquals("B789X", TextNormaliser.extractBatchCandidate(sample2))

        val sample3 = "B.No. 44521"
        assertEquals("44521", TextNormaliser.extractBatchCandidate(sample3))

        val sampleNoBatch = "Paracetamol 500 mg without batch"
        assertNull(TextNormaliser.extractBatchCandidate(sampleNoBatch))
    }

    @Test
    fun ocrResult_emptyAndWhitespace_reportsEmptyCorrectly() {
        val emptyResult = TextNormaliser.createSyntheticOcrResult("")
        assertTrue("Blank result must be empty", emptyResult.isEmpty)
        assertEquals(0, emptyResult.lineCount)
        assertEquals(0, emptyResult.blockCount)

        val whitespaceResult = TextNormaliser.createSyntheticOcrResult("   \n\t  \n  ")
        assertTrue("Whitespace-only result must be empty", whitespaceResult.isEmpty)
        assertEquals(0, whitespaceResult.lineCount)
    }

    @Test
    fun ocrResult_syntheticResult_retainsBlocksLinesAndDimensions() {
        val rawText = "Paracetamol 500 mg\nCrocin\nBatch No. ABC123\nExpiry: 08/2027"
        val result = TextNormaliser.createSyntheticOcrResult(
            rawText = rawText,
            imageWidth = 2448,
            imageHeight = 3264
        )

        assertFalse(result.isEmpty)
        assertEquals(4, result.lineCount)
        assertEquals(1, result.blockCount)
        assertEquals(2448, result.imageWidth)
        assertEquals(3264, result.imageHeight)

        val normalizedLines = result.getNormalizedLines()
        assertEquals(4, normalizedLines.size)
        assertEquals("Paracetamol 500 mg", normalizedLines[0])
        assertEquals("Crocin", normalizedLines[1])
        assertEquals("Batch No. ABC123", normalizedLines[2])
        assertEquals("Expiry: 08/2027", normalizedLines[3])
    }

    @Test
    fun ocrState_hierarchy_representsPipelineStates() {
        val idle: OcrState = OcrState.Idle
        assertEquals(OcrState.Idle, idle)

        val processing: OcrState = OcrState.Processing()
        assertTrue(processing is OcrState.Processing)

        val syntheticResult = TextNormaliser.createSyntheticOcrResult("Paracetamol 500 mg")
        val success: OcrState = OcrState.Success(syntheticResult)
        assertTrue(success is OcrState.Success)
        assertEquals("Paracetamol 500 mg", (success as OcrState.Success).result.rawText)

        val empty: OcrState = OcrState.Empty("No readable text found")
        assertTrue(empty is OcrState.Empty)
        assertEquals("No readable text found", (empty as OcrState.Empty).message)

        val error: OcrState = OcrState.Error("Engine initialization error", canRetry = true)
        assertTrue(error is OcrState.Error)
        assertTrue((error as OcrState.Error).canRetry)
    }
}
