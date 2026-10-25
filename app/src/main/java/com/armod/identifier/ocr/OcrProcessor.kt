package com.armod.identifier.ocr

import androidx.camera.core.ImageProxy

/**
 * Interface for on-device OCR processors.
 */
interface OcrProcessor {
    /**
     * Analyzes the given ImageProxy frame on a background thread and returns recognized text.
     * Guarantees that ImageProxy is closed after processing.
     */
    suspend fun processImage(imageProxy: ImageProxy): OcrResult

    /**
     * Releases underlying OCR engine resources.
     */
    fun close()
}
