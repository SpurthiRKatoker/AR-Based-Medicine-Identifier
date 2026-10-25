package com.armod.identifier.gesture

import com.armod.identifier.ui.gesture.AccessibleGesture
import com.armod.identifier.ui.gesture.AccessibleGestureStateMachine
import com.armod.identifier.ui.gesture.GestureThresholdConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AccessibleGestureStateMachineTest {

    private lateinit var stateMachine: AccessibleGestureStateMachine
    private val config = GestureThresholdConfig(
        movementThresholdPx = 20f,
        doubleTapTimeoutMillis = 300L,
        longPressTimeoutMillis = 500L
    )

    @Before
    fun setUp() {
        stateMachine = AccessibleGestureStateMachine(config)
    }

    @Test
    fun `single tap triggers when touch is released and window expires without second tap`() {
        stateMachine.onTouchDown(100f, 100f, 0L)
        val upGesture = stateMachine.onTouchUp(102f, 101f, 50L) // Minor 2px release
        assertNull("Up should not immediately fire single tap while waiting for double tap window", upGesture)

        // At 200ms (still within window)
        val tickEarly = stateMachine.onTimerTick(200L)
        assertNull(tickEarly)

        // At 301ms (double tap window expired)
        val tickExpired = stateMachine.onTimerTick(350L)
        assertEquals(AccessibleGesture.SingleTap, tickExpired)
    }

    @Test
    fun `double tap triggers when second tap occurs within short time window`() {
        // First tap
        stateMachine.onTouchDown(100f, 100f, 0L)
        stateMachine.onTouchUp(100f, 100f, 50L)

        // Second tap at 150ms (< 300ms window)
        stateMachine.onTouchDown(102f, 101f, 150L)
        val gesture = stateMachine.onTouchUp(103f, 102f, 200L)

        assertEquals(AccessibleGesture.DoubleTap, gesture)
    }

    @Test
    fun `double tap outside time window emits first single tap instead of double tap`() {
        // First tap
        stateMachine.onTouchDown(100f, 100f, 0L)
        stateMachine.onTouchUp(100f, 100f, 50L)

        // Wait past window
        val firstTap = stateMachine.onTimerTick(360L)
        assertEquals(AccessibleGesture.SingleTap, firstTap)

        // Second tap arrives at 400ms (> 300ms)
        stateMachine.onTouchDown(100f, 100f, 400L)
        val secondTapUp = stateMachine.onTouchUp(100f, 100f, 450L)
        assertNull(secondTapUp)

        // Double tap window for second tap passes
        val secondTap = stateMachine.onTimerTick(760L)
        assertEquals(AccessibleGesture.SingleTap, secondTap)
    }

    @Test
    fun `long press triggers when touch held past 500ms duration`() {
        stateMachine.onTouchDown(100f, 100f, 0L)

        // Check at 200ms
        val earlyTick = stateMachine.onTimerTick(200L)
        assertNull(earlyTick)

        // Check at 500ms
        val longPressTick = stateMachine.onTimerTick(500L)
        assertEquals(AccessibleGesture.LongPress, longPressTick)

        // Release after long press should not fire single tap
        val upGesture = stateMachine.onTouchUp(100f, 100f, 600L)
        assertNull(upGesture)
    }

    @Test
    fun `movement exceeding pixel threshold cancels deliberate tap action`() {
        stateMachine.onTouchDown(100f, 100f, 0L)

        // Accidental drag or camera panning: move 25px in X (> 20px threshold)
        val moveGesture = stateMachine.onTouchMove(125f, 100f, 100L)
        assertTrue("Move exceeding threshold should emit MovementCancelled", moveGesture is AccessibleGesture.MovementCancelled)
        assertEquals(25f, (moveGesture as AccessibleGesture.MovementCancelled).distance, 0.01f)

        // Releasing should not fire any tap
        val upGesture = stateMachine.onTouchUp(125f, 100f, 150L)
        assertNull(upGesture)

        // Timer expiration should not fire single tap
        val tickGesture = stateMachine.onTimerTick(500L)
        assertNull(tickGesture)
    }

    @Test
    fun `movement exceeding pixel threshold cancels long press`() {
        stateMachine.onTouchDown(100f, 100f, 0L)

        // Drag 30px
        stateMachine.onTouchMove(100f, 130f, 200L)

        // Holding past 500ms should NOT trigger long press because movement was exceeded
        val tickGesture = stateMachine.onTimerTick(550L)
        assertNull("Cancelled drag must not trigger long press", tickGesture)
    }

    @Test
    fun `minor tremor below threshold allows deliberate single tap to proceed`() {
        stateMachine.onTouchDown(100f, 100f, 0L)

        // Physiological hand tremor: 5px movement (< 20px threshold)
        val moveGesture = stateMachine.onTouchMove(104f, 103f, 30L)
        assertNull("Tremor within threshold should not cancel gesture", moveGesture)

        stateMachine.onTouchUp(105f, 104f, 60L)

        // Timer expires
        val tapGesture = stateMachine.onTimerTick(400L)
        assertEquals(AccessibleGesture.SingleTap, tapGesture)
    }

    @Test
    fun `cancel explicitly resets any active tracking`() {
        stateMachine.onTouchDown(100f, 100f, 0L)
        stateMachine.onTouchUp(100f, 100f, 50L) // Pending single tap

        stateMachine.cancel()

        // Timer ticks past 300ms
        val tickGesture = stateMachine.onTimerTick(400L)
        assertNull("Cancelled state machine should not emit gesture", tickGesture)
    }
}
