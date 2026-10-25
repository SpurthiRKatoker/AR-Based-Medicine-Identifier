package com.armod.identifier.ocr

/**
 * State representing the OCR recognition pipeline lifecycle.
 */
sealed interface OcrState {
    /**
     * Ready for new capture; no recognition currently running.
     */
    data object Idle : OcrState

    /**
     * Background OCR processing actively executing ("Reading label…").
     */
    data class Processing(val startTimeMillis: Long = System.currentTimeMillis()) : OcrState

    /**
     * Successfully recognized readable text from the frame.
     */
    data class Success(val result: OcrResult) : OcrState

    /**
     * Recognition ran but detected no readable text (e.g. blank surface, blurry frame).
     */
    data class Empty(val message: String) : OcrState

    /**
     * Recognition or hardware failure during processing.
     */
    data class Error(val message: String, val canRetry: Boolean = true) : OcrState
}
