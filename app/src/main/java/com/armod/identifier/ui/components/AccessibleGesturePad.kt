package com.armod.identifier.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armod.identifier.R
import com.armod.identifier.ui.gesture.accessibleTouchGestures

/**
 * Visual & accessible gesture pad component with movement/tremor filtering and tactile feedback.
 *
 * Mappings:
 * - Single Tap: Triggers [onSingleTap] (Basic Item Name & Dosage).
 * - Double Tap: Triggers [onDoubleTap] (Warnings & Side Effects).
 * - Long Press: Triggers [onLongPress] (Complete Profile Summary).
 * - Movement Filtering: Accidental drags, camera panning, or tremors (> 20px) cancel the gesture.
 */
@Composable
fun AccessibleGesturePad(
    modifier: Modifier = Modifier,
    onSingleTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val haptic = LocalHapticFeedback.current

    var recentGestureStatus by remember { mutableStateOf<String?>(null) }
    var isCancelledStatus by remember { mutableStateOf(false) }

    val statusSingleTap = stringResource(R.string.gesture_status_single_tap)
    val statusDoubleTap = stringResource(R.string.gesture_status_double_tap)
    val statusLongPress = stringResource(R.string.gesture_status_long_press)
    val statusCancelled = stringResource(R.string.gesture_status_cancelled)

    val padBg = if (isDark) Color(0xFF132337) else Color(0xFFE3F2FD)
    val padBorder = if (isDark) Color(0xFF64B5F6) else Color(0xFF1976D2)
    val textColor = if (isDark) Color(0xFFE2E8F0) else Color(0xFF0D47A1)
    val hintColor = if (isDark) Color(0xFF90CAF9) else Color(0xFF1565C0)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .accessibleTouchGestures(
                onSingleTap = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    recentGestureStatus = statusSingleTap
                    isCancelledStatus = false
                    onSingleTap()
                },
                onDoubleTap = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    recentGestureStatus = statusDoubleTap
                    isCancelledStatus = false
                    onDoubleTap()
                },
                onLongPress = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    recentGestureStatus = statusLongPress
                    isCancelledStatus = false
                    onLongPress()
                },
                onMovementCancelled = { _ ->
                    recentGestureStatus = statusCancelled
                    isCancelledStatus = true
                }
            )
            .semantics {
                heading()
                contentDescription = "Accessible touch gesture pad. Tap once for name and dosage, double tap for warnings and side effects, hold for full monograph."
            },
        colors = CardDefaults.cardColors(containerColor = padBg),
        border = BorderStroke(2.dp, padBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = padBorder,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = stringResource(R.string.gesture_pad_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = textColor
                    )
                }

                // Filter active badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFBBDEFB))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.gesture_movement_filter_hint),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = hintColor
                    )
                }
            }

            // Gesture Instruction Rows
            GestureGuideRow(
                icon = "👆",
                label = stringResource(R.string.gesture_single_tap),
                textColor = textColor
            )
            GestureGuideRow(
                icon = "✌️",
                label = stringResource(R.string.gesture_double_tap),
                textColor = textColor
            )
            GestureGuideRow(
                icon = "⏱️",
                label = stringResource(R.string.gesture_long_press),
                textColor = textColor
            )

            // Dynamic Feedback Pill
            recentGestureStatus?.let { status ->
                Spacer(modifier = Modifier.height(2.dp))
                val statusBg = if (isCancelledStatus) {
                    if (isDark) Color(0xFF4C1D1D) else Color(0xFFFFCDD2)
                } else {
                    if (isDark) Color(0xFF14532D) else Color(0xFFC8E6C9)
                }
                val statusFg = if (isCancelledStatus) {
                    if (isDark) Color(0xFFFFCDD2) else Color(0xFFB71C1C)
                } else {
                    if (isDark) Color(0xFFDCFCE7) else Color(0xFF1B5E20)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusBg)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = status,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusFg
                    )
                }
            }
        }
    }
}

@Composable
private fun GestureGuideRow(
    icon: String,
    label: String,
    textColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = icon,
            fontSize = 18.sp
        )
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}
