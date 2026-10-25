package com.armod.identifier.camera

import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * CameraX PreviewView composable.
 * Hosts hardware-accelerated preview rendering with automatic lifecycle integration.
 */
@Composable
fun CameraPreviewView(
    captureManager: CameraCaptureManager,
    modifier: Modifier = Modifier,
    onReady: () -> Unit = {},
    onError: (Throwable) -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.PERFORMANCE

                captureManager.bindCamera(
                    lifecycleOwner = lifecycleOwner,
                    surfaceProvider = this.surfaceProvider,
                    onReady = onReady,
                    onError = onError
                )
            }
        }
    )

    DisposableEffect(lifecycleOwner) {
        onDispose {
            captureManager.unbind()
        }
    }
}
