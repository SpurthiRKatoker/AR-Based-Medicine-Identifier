package com.armod.identifier.ui.gesture

import kotlin.math.hypot

/**
 * Recognised accessible gestures.
 */
sealed interface AccessibleGesture {
    /**
     * Single Tap: Triggers playback of basic item name and dosage.
     */
    data object SingleTap : AccessibleGesture

    /**
     * Double Tap: Triggers playback of warnings and side effects within short time window.
     */
    data object DoubleTap : AccessibleGesture

    /**
     * Long Press: Triggers playback of the complete profile summary when held past threshold duration.
     */
    data object LongPress : AccessibleGesture

    /**
     * Cancelled due to pointer movement exceeding the pixel threshold (camera panning, drags, or tremors).
     */
    data class MovementCancelled(val distance: Float) : AccessibleGesture
}

/**
 * Configurable thresholds for accessible gesture recognition and movement filtering.
 */
data class GestureThresholdConfig(
    val movementThresholdPx: Float = 20f,
    val doubleTapTimeoutMillis: Long = 300L,
    val longPressTimeoutMillis: Long = 500L
)

/**
 * Pure Kotlin state machine for accessible touch gestures with movement/tremor filtering.
 *
 * Rules:
 * - Single Tap: Triggers playback of basic item name and dosage.
 * - Double Tap (within short time window): Triggers playback of warnings and side effects.
 * - Long Press (held past threshold duration): Triggers playback of the complete profile summary.
 * - Movement Filtering: If movement exceeds [movementThresholdPx], cancels gesture action.
 * - 100% pure Kotlin; can be thoroughly unit-tested without Android framework or instrumentation.
 */
class AccessibleGestureStateMachine(
    val config: GestureThresholdConfig = GestureThresholdConfig()
) {
    private var isPressed: Boolean = false
    private var isMovementExceeded: Boolean = false
    private var isLongPressTriggered: Boolean = false

    private var downX: Float = 0f
    private var downY: Float = 0f
    private var downTimeMillis: Long = 0L

    private var hasPendingSingleTap: Boolean = false
    private var lastTapUpTimeMillis: Long = 0L
    private var lastTapUpX: Float = 0f
    private var lastTapUpY: Float = 0f

    /**
     * Called when a pointer down event is registered.
     */
    fun onTouchDown(x: Float, y: Float, currentTimeMillis: Long): AccessibleGesture? {
        isPressed = true
        isMovementExceeded = false
        isLongPressTriggered = false
        downX = x
        downY = y
        downTimeMillis = currentTimeMillis

        // If there was a pending single tap, check if this down is within double-tap distance tolerance
        if (hasPendingSingleTap) {
            val distFromLastTap = hypot(x - lastTapUpX, y - lastTapUpY)
            if (distFromLastTap > config.movementThresholdPx) {
                // Too far from last tap to be a double tap; commit previous single tap and reset
                hasPendingSingleTap = false
                return AccessibleGesture.SingleTap
            }
        }
        return null
    }

    /**
     * Called when a pointer movement is registered.
     */
    fun onTouchMove(x: Float, y: Float, currentTimeMillis: Long): AccessibleGesture? {
        if (!isPressed || isMovementExceeded) return null

        val distance = hypot(x - downX, y - downY)
        if (distance > config.movementThresholdPx) {
            isMovementExceeded = true
            hasPendingSingleTap = false
            return AccessibleGesture.MovementCancelled(distance)
        }

        // Check if long press duration has been reached while moving within tolerance
        if (!isLongPressTriggered && (currentTimeMillis - downTimeMillis) >= config.longPressTimeoutMillis) {
            isLongPressTriggered = true
            hasPendingSingleTap = false
            return AccessibleGesture.LongPress
        }

        return null
    }

    /**
     * Called when a periodic timer ticks (to check for long press hold or double tap window expiration).
     */
    fun onTimerTick(currentTimeMillis: Long): AccessibleGesture? {
        // 1. Long press trigger check while still pressed
        if (isPressed && !isMovementExceeded && !isLongPressTriggered) {
            if ((currentTimeMillis - downTimeMillis) >= config.longPressTimeoutMillis) {
                isLongPressTriggered = true
                hasPendingSingleTap = false
                return AccessibleGesture.LongPress
            }
        }

        // 2. Pending single tap expiration check when released
        if (!isPressed && hasPendingSingleTap) {
            if ((currentTimeMillis - lastTapUpTimeMillis) >= config.doubleTapTimeoutMillis) {
                hasPendingSingleTap = false
                return AccessibleGesture.SingleTap
            }
        }

        return null
    }

    /**
     * Called when a pointer up / release event is registered.
     */
    fun onTouchUp(x: Float, y: Float, currentTimeMillis: Long): AccessibleGesture? {
        if (!isPressed) return null
        isPressed = false

        if (isMovementExceeded || isLongPressTriggered) {
            return null
        }

        val distance = hypot(x - downX, y - downY)
        if (distance > config.movementThresholdPx) {
            isMovementExceeded = true
            hasPendingSingleTap = false
            return AccessibleGesture.MovementCancelled(distance)
        }

        if (hasPendingSingleTap) {
            val timeSinceLastTap = currentTimeMillis - lastTapUpTimeMillis
            val distSinceLastTap = hypot(x - lastTapUpX, y - lastTapUpY)
            if (timeSinceLastTap <= config.doubleTapTimeoutMillis && distSinceLastTap <= config.movementThresholdPx) {
                hasPendingSingleTap = false
                return AccessibleGesture.DoubleTap
            }
        }

        // Start pending window for double-tap detection
        hasPendingSingleTap = true
        lastTapUpTimeMillis = currentTimeMillis
        lastTapUpX = x
        lastTapUpY = y
        return null
    }

    /**
     * Cancels any active gesture tracking immediately.
     */
    fun cancel() {
        isPressed = false
        isMovementExceeded = false
        isLongPressTriggered = false
        hasPendingSingleTap = false
    }

    val isTracking: Boolean
        get() = isPressed || hasPendingSingleTap
}
