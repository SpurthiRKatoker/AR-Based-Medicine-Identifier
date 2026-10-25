package com.armod.identifier.util

import com.armod.identifier.ocr.OcrBlock
import com.armod.identifier.ocr.OcrLine
import com.armod.identifier.ocr.OcrResult

/**
 * Text normalisation and extraction helper functions for OCR sanitisation and downstream matching.
 * Provides pure JVM methods that can be tested reliably without device hardware.
 */
object TextNormaliser {

    private val EXPIRY_REGEX = Regex(
        pattern = """\b(?:EXPIRY|EXP\.?\s*DATE|EXP\.|EXP|USE BY|USE BEFORE|UB)[ :.-]*([0-9]{2}[/-][0-9]{2,4})\b""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    private val BATCH_REGEX = Regex(
        pattern = """\b(?:BATCH\s*NO\.?|BATCH|LOT\.?|B\.?\s*NO\.?)\s*[:.-]*\s*([A-Z0-9]+(?:-[A-Z0-9]+)*)\b""",
        options = setOf(RegexOption.IGNORE_CASE)
    )

    /**
     * Standardizes text by trimming, lowercasing, and collapsing multiple whitespace characters.
     */
    fun normalise(input: String): String {
        return input.trim().lowercase().replace(Regex("""\s+"""), " ")
    }

    /**
     * Cleans raw OCR text into discrete non-empty lines, stripping noisy punctuation and trailing spaces.
     */
    fun cleanOcrLines(rawText: String): List<String> {
        if (rawText.isBlank()) return emptyList()

        return rawText.lines()
            .map { it.trim() }
            .filter { line ->
                // Filter out lines that are purely punctuation or single stray characters
                line.isNotBlank() && line.any { it.isLetterOrDigit() }
            }
    }

    /**
     * Attempts to find a printed medicine expiry date pattern in OCR text.
     * Returns the date string (e.g. "08/2027", "11/26") or null if not detected.
     */
    fun extractExpiryCandidate(text: String): String? {
        val match = EXPIRY_REGEX.find(text)
        return match?.groupValues?.getOrNull(1)
    }

    /**
     * Attempts to find a printed batch/lot number pattern in OCR text.
     * Returns the batch code (e.g. "ABC123") or null if not detected.
     */
    fun extractBatchCandidate(text: String): String? {
        val match = BATCH_REGEX.find(text)
        return match?.groupValues?.getOrNull(1)
    }

    /**
     * Creates a structured OcrResult from raw text strings (useful for synthetic testing and mock pipelines).
     */
    fun createSyntheticOcrResult(
        rawText: String,
        imageWidth: Int = 1920,
        imageHeight: Int = 1080
    ): OcrResult {
        val lines = cleanOcrLines(rawText).map { OcrLine(text = it) }
        val blocks = if (lines.isNotEmpty()) {
            listOf(OcrBlock(text = rawText, lines = lines))
        } else {
            emptyList()
        }

        return OcrResult(
            rawText = rawText,
            blocks = blocks,
            lines = lines,
            imageWidth = imageWidth,
            imageHeight = imageHeight
        )
    }
}
