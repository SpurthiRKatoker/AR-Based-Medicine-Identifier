package com.armod.identifier.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executor

/**
 * Manages CameraX lifecycle binding, preview stream, and on-device image capture.
 *
 * Strict Privacy & Offline Architecture:
 * Captured frames are processed strictly in-memory on-device and safely closed.
 * Zero network requests, zero file uploads, zero telemetry.
 */
class CameraCaptureManager(private val context: Context) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null

    /**
     * Binds CameraX Preview and ImageCapture use cases to the given LifecycleOwner and SurfaceProvider.
     */
    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
        onReady: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = surfaceProvider
                }

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )

                onReady()
            } catch (e: Exception) {
                onError(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Captures a single image frame on-device.
     * The captured ImageProxy is immediately closed after extracting frame metadata,
     * ensuring zero memory leaks and zero unauthorized data retention.
     */
    fun captureImage(
        executor: Executor,
        onCaptured: (width: Int, height: Int, rotation: Int) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val capture = imageCapture ?: run {
            onError(IllegalStateException("Camera capture is not initialized or bound"))
            return
        }

        capture.takePicture(
            executor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val width = image.width
                        val height = image.height
                        val rotation = image.imageInfo.rotationDegrees
                        onCaptured(width, height, rotation)
                    } finally {
                        image.close()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    onError(exception)
                }
            }
        )
    }

    /**
     * Captures a single image frame and delivers the ImageProxy directly to the OCR pipeline.
     * The consumer is responsible for processing and closing the ImageProxy.
     */
    fun captureImageProxy(
        executor: Executor,
        onCaptured: (ImageProxy) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val capture = imageCapture ?: run {
            onError(IllegalStateException("Camera capture is not initialized or bound"))
            return
        }

        capture.takePicture(
            executor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    onCaptured(image)
                }

                override fun onError(exception: ImageCaptureException) {
                    onError(exception)
                }
            }
        )
    }

    /**
     * Unbinds all CameraX use cases and releases camera hardware resources.
     */
    fun unbind() {
        try {
            cameraProvider?.unbindAll()
        } catch (_: Exception) {}
        cameraProvider = null
        imageCapture = null
    }
}
