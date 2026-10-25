package com.armod.identifier.ocr

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * On-device ML Kit Text Recognition v2 processor.
 *
 * Privacy & Offline Guarantee:
 * - Uses bundled on-device Latin script model (com.google.mlkit:text-recognition:16.0.1).
 * - Zero network calls, zero remote servers, zero Google Play Services model downloads.
 * - ImageProxy is guaranteed to be closed immediately after processing to prevent frame buffer leaks.
 */
class MlKitOcrProcessor(
    private val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
) : OcrProcessor {

    @OptIn(ExperimentalGetImage::class)
    override suspend fun processImage(imageProxy: ImageProxy): OcrResult = withContext(Dispatchers.Default) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            throw IllegalArgumentException("ImageProxy contains null media image")
        }

        val rotation = imageProxy.imageInfo.rotationDegrees
        val width = imageProxy.width
        val height = imageProxy.height
        val inputImage = InputImage.fromMediaImage(mediaImage, rotation)

        suspendCancellableCoroutine { continuation ->
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val result = mapVisionText(visionText, width, height)
                    continuation.resume(result)
                }
                .addOnFailureListener { error ->
                    continuation.resumeWithException(error)
                }
                .addOnCompleteListener {
                    try {
                        imageProxy.close()
                    } catch (_: Exception) {}
                }
        }
    }

    /**
     * Maps ML Kit Text object to pure domain OcrResult with blocks, lines, and bounding boxes.
     */
    fun mapVisionText(visionText: Text, width: Int, height: Int): OcrResult {
        val blocks = visionText.textBlocks.map { block ->
            val blockBox = block.boundingBox?.let { box ->
                OcrBoundingBox(
                    left = box.left,
                    top = box.top,
                    right = box.right,
                    bottom = box.bottom
                )
            }

            val lines = block.lines.map { line ->
                val lineBox = line.boundingBox?.let { box ->
                    OcrBoundingBox(
                        left = box.left,
                        top = box.top,
                        right = box.right,
                        bottom = box.bottom
                    )
                }
                val confidence = line.confidence
                OcrLine(
                    text = line.text,
                    boundingBox = lineBox,
                    confidence = confidence
                )
            }

            OcrBlock(
                text = block.text,
                lines = lines,
                boundingBox = blockBox
            )
        }

        val allLines = blocks.flatMap { it.lines }

        return OcrResult(
            rawText = visionText.text,
            blocks = blocks,
            lines = allLines,
            imageWidth = width,
            imageHeight = height
        )
    }

    override fun close() {
        try {
            recognizer.close()
        } catch (_: Exception) {}
    }
}
