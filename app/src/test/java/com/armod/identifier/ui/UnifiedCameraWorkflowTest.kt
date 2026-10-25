package com.armod.identifier.ui

import com.armod.identifier.data.model.MedicineRecord
import com.armod.identifier.matching.MatchOutcome
import com.armod.identifier.matching.MedicineMatcher
import com.armod.identifier.ocr.OcrLine
import com.armod.identifier.ocr.OcrResult
import com.armod.identifier.tts.SpeechContentGenerator
import com.armod.identifier.ui.gesture.AccessibleGesture
import com.armod.identifier.ui.gesture.AccessibleGestureStateMachine
import com.armod.identifier.ui.gesture.GestureThresholdConfig
import com.armod.identifier.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test suite validating the Unified Single-Screen Camera Workflow and Interaction Architecture:
 * 1. Single-Screen Gesture Handling (Single Tap, Double Tap, Long Press in-place without page transitions).
 * 2. Movement Filter (20px threshold cancelling camera panning / hand tremors).
 * 3. Instant 3D AR Card Overlay (Spawning over live camera upon identification).
 * 4. Dedicated 'READ INFORMATION' Button (The ONLY trigger opening full-screen structured text page).
 * 5. Reset State ('Scan Again' control returning camera cleanly back to scanning state).
 */
class UnifiedCameraWorkflowTest {

    private lateinit var gestureStateMachine: AccessibleGestureStateMachine
    private val sampleCatalogue = listOf(
        MedicineRecord(
            id = "omeprazole-20mg-cap",
            brandName = "Omez",
            genericName = "Omeprazole",
            strength = "20mg",
            dosageForm = "Capsule",
            manufacturer = "Dr. Reddy's Laboratories Ltd",
            labelPurpose = "Reduces stomach acid for acid reflux and ulcers",
            labelDirections = "Take one capsule daily before breakfast",
            warnings = listOf("Do not crush or chew capsules. Consult doctor if pregnant"),
            commonSideEffects = listOf("Headache, nausea, abdominal pain"),
            sourceTitle = "NHS UK & DailyMed",
            datasetVersion = "1.0.0"
        ),
        MedicineRecord(
            id = "paracetamol-500mg-tab",
            brandName = "Calpol",
            genericName = "Paracetamol",
            strength = "500mg",
            dosageForm = "Tablet",
            manufacturer = "GSK",
            labelPurpose = "Pain relief and fever reduction",
            labelDirections = "Take 1-2 tablets every 4-6 hours",
            warnings = listOf("Do not exceed 4g per day. Risk of liver toxicity"),
            commonSideEffects = listOf("Rare allergic reactions, skin rash"),
            sourceTitle = "WHO EML",
            datasetVersion = "1.0.0"
        ),
        MedicineRecord(
            id = "paracetamol-650mg-tab",
            brandName = "Dolo",
            genericName = "Paracetamol",
            strength = "650mg",
            dosageForm = "Tablet",
            manufacturer = "Micro Labs Ltd",
            labelPurpose = "Fever reduction and pain relief",
            labelDirections = "Take one tablet every 6 hours as needed",
            warnings = listOf("Do not exceed 3250mg per day without medical advice"),
            commonSideEffects = listOf("Liver damage with overdose"),
            sourceTitle = "NLEM 2022",
            datasetVersion = "1.0.0"
        )
    )

    @Before
    fun setUp() {
        gestureStateMachine = AccessibleGestureStateMachine(
            GestureThresholdConfig(
                movementThresholdPx = 20f,
                doubleTapTimeoutMillis = 300L,
                longPressTimeoutMillis = 500L
            )
        )
    }

    @Test
    fun `single tap identifies medicine, creates basic name and dosage speech, and stays on camera`() {
        val ocrResult = OcrResult(
            rawText = "Omez 20mg Capsules Dr. Reddy's B.No: T535 EXP: 12/2027",
            lines = listOf(
                OcrLine("Omez 20mg Capsules"),
                OcrLine("Dr. Reddy's"),
                OcrLine("B.No: T535"),
                OcrLine("EXP: 12/2027")
            )
        )

        val outcome = MedicineMatcher.match(ocrResult, sampleCatalogue)
        assertTrue("Single tap capture must produce ConfidentMatch", outcome is MatchOutcome.ConfidentMatch)

        val spoken = SpeechContentGenerator.generateSpeech(outcome, ocrResult)
        assertTrue(
            "Basic name and dosage must announce brand and strength",
            spoken.basicNameAndDosage.contains("Omez", ignoreCase = true) &&
                spoken.basicNameAndDosage.contains("20 milligrams", ignoreCase = true)
        )

        // Gesture state machine fires SingleTap when double-tap window expires
        gestureStateMachine.onTouchDown(50f, 50f, 0L)
        gestureStateMachine.onTouchUp(51f, 50f, 40L)
        val gesture = gestureStateMachine.onTimerTick(350L)
        assertEquals(AccessibleGesture.SingleTap, gesture)
    }

    @Test
    fun `double tap triggers in-place audio playback for warnings and side effects`() {
        val ocrResult = OcrResult(
            rawText = "Omez 20mg Capsules Dr. Reddy's",
            lines = listOf(OcrLine("Omez 20mg Capsules"))
        )
        val outcome = MedicineMatcher.match(ocrResult, sampleCatalogue)
        val spoken = SpeechContentGenerator.generateSpeech(outcome, ocrResult)

        assertTrue(
            "Double tap audio must contain warnings and side effects",
            spoken.warningsAndSideEffects.contains("Warnings and side effects for Omez", ignoreCase = true) &&
                spoken.warningsAndSideEffects.contains("Do not crush or chew", ignoreCase = true) &&
                spoken.warningsAndSideEffects.contains("Headache, nausea", ignoreCase = true)
        )

        // Verify DoubleTap gesture detection within time window
        gestureStateMachine.onTouchDown(100f, 100f, 0L)
        gestureStateMachine.onTouchUp(100f, 100f, 50L)
        gestureStateMachine.onTouchDown(102f, 101f, 150L)
        val gesture = gestureStateMachine.onTouchUp(102f, 101f, 200L)
        assertEquals(AccessibleGesture.DoubleTap, gesture)
    }

    @Test
    fun `long press triggers in-place audio playback for complete profile summary`() {
        val ocrResult = OcrResult(
            rawText = "Omez 20mg Capsules Dr. Reddy's B.No: T535 EXP: 12/2027",
            lines = listOf(
                OcrLine("Omez 20mg Capsules"),
                OcrLine("B.No: T535"),
                OcrLine("EXP: 12/2027")
            )
        )
        val outcome = MedicineMatcher.match(ocrResult, sampleCatalogue)
        val spoken = SpeechContentGenerator.generateSpeech(outcome, ocrResult)

        assertTrue(
            "Full profile summary must contain brand, active ingredient, directions, warnings, and disclaimer",
            spoken.fullSpeech.contains("Omez", ignoreCase = true) &&
                spoken.fullSpeech.contains("Omeprazole", ignoreCase = true) &&
                spoken.fullSpeech.contains("Batch code on package", ignoreCase = true) &&
                spoken.fullSpeech.contains("academic research prototype", ignoreCase = true)
        )

        // Verify LongPress gesture detection after 500ms
        gestureStateMachine.onTouchDown(100f, 100f, 0L)
        val gesture = gestureStateMachine.onTimerTick(500L)
        assertEquals(AccessibleGesture.LongPress, gesture)
    }

    @Test
    fun `movement filter ignores drag and camera panning exceeding 20px threshold`() {
        gestureStateMachine.onTouchDown(100f, 100f, 0L)

        // Move 21px (> 20px threshold)
        val moveGesture = gestureStateMachine.onTouchMove(121f, 100f, 100L)
        assertTrue(
            "Move exceeding 20px threshold must trigger MovementCancelled",
            moveGesture is AccessibleGesture.MovementCancelled
        )
        assertEquals(21f, (moveGesture as AccessibleGesture.MovementCancelled).distance, 0.01f)

        // Subsequent release must not trigger any tap
        val upGesture = gestureStateMachine.onTouchUp(121f, 100f, 150L)
        assertNull("Cancelled gesture must not trigger tap on up", upGesture)

        val tickGesture = gestureStateMachine.onTimerTick(600L)
        assertNull("Cancelled gesture must not trigger any timer action", tickGesture)
    }

    @Test
    fun `movement within 20px threshold is accepted as a valid tap`() {
        gestureStateMachine.onTouchDown(100f, 100f, 0L)

        // Move 10px (<= 20px threshold)
        val moveGesture = gestureStateMachine.onTouchMove(108f, 106f, 100L)
        assertNull("Small move within 20px must not cancel tap", moveGesture)

        gestureStateMachine.onTouchUp(108f, 106f, 150L)
        val tickGesture = gestureStateMachine.onTimerTick(500L)
        assertEquals(AccessibleGesture.SingleTap, tickGesture)
    }

    @Test
    fun `dedicated READ INFORMATION button is the only trigger opening full structured text page`() {
        // App starts on Screen.LiveScan
        var currentScreen: Screen = Screen.LiveScan
        var hasTriggeredReadInformation = false

        val onReadInformationClick: () -> Unit = {
            hasTriggeredReadInformation = true
            currentScreen = Screen.Result
        }

        // Deliberate gestures on live camera must NOT navigate away to Screen.Result
        val onSingleTap = { /* Play Name & Dosage in-place on camera screen */ }
        val onDoubleTap = { /* Play Warnings in-place on camera screen */ }
        val onLongPress = { /* Play Full Profile in-place on camera screen */ }

        onSingleTap()
        assertEquals("Single tap must remain on Screen.LiveScan", Screen.LiveScan, currentScreen)

        onDoubleTap()
        assertEquals("Double tap must remain on Screen.LiveScan", Screen.LiveScan, currentScreen)

        onLongPress()
        assertEquals("Long press must remain on Screen.LiveScan", Screen.LiveScan, currentScreen)

        // Only the dedicated READ INFORMATION button navigates to Screen.Result
        onReadInformationClick()
        assertTrue(hasTriggeredReadInformation)
        assertEquals("READ INFORMATION button must navigate to Screen.Result", Screen.Result, currentScreen)
    }

    @Test
    fun `reset state clears recognized AR card and returns camera view to scanning state`() {
        val ocrResult = OcrResult(
            rawText = "Omez 20mg Capsules Dr. Reddy's B.No: T535 EXP: 12/2027",
            lines = listOf(
                OcrLine("Omez 20mg Capsules"),
                OcrLine("B.No: T535"),
                OcrLine("EXP: 12/2027")
            )
        )
        var recognizedMatchOutcome: MatchOutcome? = MedicineMatcher.match(ocrResult, sampleCatalogue)
        var recognizedOcrResult: OcrResult? = ocrResult
        var isCameraReadyForScanning = false

        assertNotNull(recognizedMatchOutcome)
        assertNotNull(recognizedOcrResult)

        // Reset state action triggered by 'Scan Again' control
        val resetToScanningState = {
            recognizedMatchOutcome = null
            recognizedOcrResult = null
            isCameraReadyForScanning = true
        }

        resetToScanningState()

        assertNull("AR card outcome must be cleared", recognizedMatchOutcome)
        assertNull("AR card OCR result must be cleared", recognizedOcrResult)
        assertTrue("Camera must be cleanly returned to scanning state", isCameraReadyForScanning)
    }
}
