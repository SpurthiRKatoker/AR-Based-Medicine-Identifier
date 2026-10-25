package com.armod.identifier.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * High-contrast, minimal framing guide for medicine package positioning.
 * Follows Document 04 (UI/UX Design Brief):
 * - Keeps viewfinder mostly unobstructed
 * - Avoids complex reticles or visual noise
 * - Clear, high-contrast corner brackets
 */
@Composable
fun CameraFramingOverlay(
    modifier: Modifier = Modifier,
    bracketColor: Color = Color(0xFF145DA0), // Primary Blue
    scrimColor: Color = Color(0x66000000)   // Subtle scrim outside target area
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Framing box dimensions: 80% width, 45% height, vertically centered slightly above middle
        val boxWidth = width * 0.82f
        val boxHeight = height * 0.48f
        val left = (width - boxWidth) / 2f
        val top = (height - boxHeight) * 0.42f
        val right = left + boxWidth
        val bottom = top + boxHeight

        // Scrim path covering outside of the framing box
        val backgroundPath = Path().apply {
            addRect(Rect(0f, 0f, width, height))
            val cutOut = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(left, top, right, bottom),
                        cornerRadius = CornerRadius(24f, 24f)
                    )
                )
            }
            op(this, cutOut, androidx.compose.ui.graphics.PathOperation.Difference)
        }
        drawPath(backgroundPath, color = scrimColor)

        // Corner bracket parameters
        val bracketLength = 48.dp.toPx()
        val strokeWidth = 4.dp.toPx()

        // Top-Left Corner
        drawLine(
            color = bracketColor,
            start = Offset(left, top + bracketLength),
            end = Offset(left, top),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = bracketColor,
            start = Offset(left, top),
            end = Offset(left + bracketLength, top),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Top-Right Corner
        drawLine(
            color = bracketColor,
            start = Offset(right - bracketLength, top),
            end = Offset(right, top),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = bracketColor,
            start = Offset(right, top),
            end = Offset(right, top + bracketLength),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Bottom-Left Corner
        drawLine(
            color = bracketColor,
            start = Offset(left, bottom - bracketLength),
            end = Offset(left, bottom),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = bracketColor,
            start = Offset(left, bottom),
            end = Offset(left + bracketLength, bottom),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Bottom-Right Corner
        drawLine(
            color = bracketColor,
            start = Offset(right - bracketLength, bottom),
            end = Offset(right, bottom),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = bracketColor,
            start = Offset(right, bottom),
            end = Offset(right, bottom - bracketLength),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}
