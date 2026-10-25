package com.armod.identifier.ocr

/**
 * Rectangular coordinates for OCR bounding boxes.
 */
data class OcrBoundingBox(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

/**
 * Individual recognized line of text.
 */
data class OcrLine(
    val text: String,
    val boundingBox: OcrBoundingBox? = null,
    val confidence: Float? = null
)

/**
 * Grouped text block detected by OCR.
 */
data class OcrBlock(
    val text: String,
    val lines: List<OcrLine> = emptyList(),
    val boundingBox: OcrBoundingBox? = null
)

/**
 * Complete representation of on-device OCR output.
 * Preserves raw text, block structure, and line coordinates for downstream matching.
 */
data class OcrResult(
    val rawText: String,
    val blocks: List<OcrBlock> = emptyList(),
    val lines: List<OcrLine> = emptyList(),
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val timestampMillis: Long = System.currentTimeMillis()
) {
    val isEmpty: Boolean get() = rawText.isBlank()
    val lineCount: Int get() = lines.size
    val blockCount: Int get() = blocks.size

    /**
     * Returns cleaned, non-empty text lines with normalized spacing.
     */
    fun getNormalizedLines(): List<String> = lines
        .map { it.text.trim() }
        .filter { it.isNotEmpty() }
}
