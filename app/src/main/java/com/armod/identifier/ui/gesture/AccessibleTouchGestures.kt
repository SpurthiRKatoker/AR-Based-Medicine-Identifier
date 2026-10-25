package com.armod.identifier.ui.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.hypot

/**
 * Custom Compose pointer modifier implementing accessible touch gestures with movement/tremor filtering.
 *
 * Behavioral Rules:
 * - Single Tap: Triggers [onSingleTap] (basic item name and dosage).
 * - Double Tap: Triggers [onDoubleTap] (warnings and side effects).
 * - Long Press: Triggers [onLongPress] (complete profile summary).
 * - Movement Filtering: If finger/pointer moves beyond [config.movementThresholdPx] (accidental drag,
 *   camera panning, or hand tremor), the gesture is immediately cancelled.
 */
fun Modifier.accessibleTouchGestures(
    enabled: Boolean = true,
    config: GestureThresholdConfig = GestureThresholdConfig(),
    onSingleTap: () -> Unit,
    onDoubleTap: () -> Unit,
    onLongPress: () -> Unit,
    onMovementCancelled: ((distance: Float) -> Unit)? = null
): Modifier = if (!enabled) this else pointerInput(config, enabled) {
    coroutineScope {
        var lastTapTime = 0L
        var lastTapPosition = Offset.Zero
        var pendingSingleTapJob: Job? = null

        awaitEachGesture {
            // 1. Await pointer down
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
            val downTime = System.currentTimeMillis()
            val downPos = down.position
            var isMovementExceeded = false
            var isLongPressTriggered = false

            // Check if this tap down is part of a potential double-tap sequence
            val timeSinceLastTap = downTime - lastTapTime
            val distSinceLastTap = hypot(downPos.x - lastTapPosition.x, downPos.y - lastTapPosition.y)
            val isPotentialDoubleTap = (pendingSingleTapJob?.isActive == true) &&
                (timeSinceLastTap <= config.doubleTapTimeoutMillis) &&
                (distSinceLastTap <= config.movementThresholdPx)

            if (!isPotentialDoubleTap) {
                pendingSingleTapJob?.cancel()
                pendingSingleTapJob = null
            }

            // Launch long-press timer coroutine
            val longPressJob = launch {
                delay(config.longPressTimeoutMillis)
                if (isActive && !isMovementExceeded) {
                    isLongPressTriggered = true
                    pendingSingleTapJob?.cancel()
                    pendingSingleTapJob = null
                    onLongPress()
                }
            }

            // 2. Track pointer moves until release or cancellation
            var isPointerDown = true
            while (isPointerDown) {
                val event = awaitPointerEvent(pass = PointerEventPass.Main)
                val currentChange = event.changes.firstOrNull { it.id == down.id }

                if (currentChange == null || !currentChange.pressed) {
                    isPointerDown = false
                } else {
                    val currentPos = currentChange.position
                    val distance = hypot(currentPos.x - downPos.x, currentPos.y - downPos.y)

                    if (distance > config.movementThresholdPx) {
                        if (!isMovementExceeded) {
                            isMovementExceeded = true
                            longPressJob.cancel()
                            pendingSingleTapJob?.cancel()
                            pendingSingleTapJob = null
                            onMovementCancelled?.invoke(distance)
                        }
                    }
                }
            }

            // Clean up long press job
            longPressJob.cancel()

            // 3. Pointer released (Up)
            val upTime = System.currentTimeMillis()
            if (!isMovementExceeded && !isLongPressTriggered) {
                if (isPotentialDoubleTap) {
                    // Second tap occurred within time window and without exceeding movement
                    pendingSingleTapJob?.cancel()
                    pendingSingleTapJob = null
                    lastTapTime = 0L
                    onDoubleTap()
                } else {
                    // First tap registered; schedule single tap after double-tap window expires
                    lastTapTime = upTime
                    lastTapPosition = downPos
                    pendingSingleTapJob?.cancel()
                    pendingSingleTapJob = launch {
                        delay(config.doubleTapTimeoutMillis)
                        if (isActive) {
                            onSingleTap()
                            pendingSingleTapJob = null
                        }
                    }
                }
            }
        }
    }
}
