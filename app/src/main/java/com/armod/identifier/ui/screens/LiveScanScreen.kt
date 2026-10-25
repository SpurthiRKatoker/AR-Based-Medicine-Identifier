package com.armod.identifier.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.armod.identifier.R
import com.armod.identifier.camera.CameraCaptureManager
import com.armod.identifier.camera.CameraFramingOverlay
import com.armod.identifier.camera.CameraPreviewView
import com.armod.identifier.camera.CameraState
import com.armod.identifier.data.model.MedicineRecord
import com.armod.identifier.matching.MatchOutcome
import com.armod.identifier.matching.MedicineMatcher
import com.armod.identifier.ocr.OcrProcessor
import com.armod.identifier.ocr.OcrResult
import com.armod.identifier.tts.SpeechContentGenerator
import com.armod.identifier.tts.TextToSpeechManager
import com.armod.identifier.tts.TtsState
import com.armod.identifier.ui.gesture.GestureThresholdConfig
import com.armod.identifier.ui.gesture.SpeechTarget
import com.armod.identifier.ui.gesture.accessibleTouchGestures
import com.armod.identifier.ui.theme.PrimaryBlue
import com.armod.identifier.ui.theme.StatusError
import com.armod.identifier.ui.theme.StatusSuccess
import com.armod.identifier.util.TextNormaliser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Screen S04 / Unified Camera & AR Overlay Screen.
 *
 * Implements Unified Single-Screen Camera Workflow:
 * - Single-Screen Gesture Handling: All gestures (Single Tap, Double Tap, Long Press) operate directly
 *   on the main camera screen without triggering unwanted page transitions.
 * - Single Tap: Captures current frame, identifies medicine on-device, speaks Basic Name and Dosage,
 *   and immediately spawns the 3D AR Card Overlay right on the live camera view.
 * - Double Tap: In-place audio playback for Warnings and Side Effects.
 * - Long Press: In-place audio playback for Complete Profile Summary.
 * - Movement Filter: Ignores touch movement exceeding the 20px tremor/drag threshold to prevent
 *   accidental triggers during camera panning.
 * - Instant AR Card Overlay: Appears directly on the main camera screen anchored to the package/surface
 *   without forcing user away from live camera view.
 * - Dedicated 'READ INFORMATION' Button: Prominently displayed on the camera overlay screen; the ONLY
 *   trigger that opens the full-screen structured text page.
 * - Reset State ('Scan Again' control): Clears AR card, resets tracking visuals, and returns camera view
 *   cleanly back to the scanning state.
 */
@Composable
fun LiveScanScreen(
    ocrProcessor: OcrProcessor,
    medicineCatalogue: List<MedicineRecord> = emptyList(),
    onReadInformationClick: ((OcrResult, MatchOutcome) -> Unit)? = null,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    initialOcrResult: OcrResult? = null,
    initialMatchOutcome: MatchOutcome? = null,
    onOcrResult: ((OcrResult, SpeechTarget?) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val captureManager = remember { CameraCaptureManager(context) }
    var cameraState by remember { mutableStateOf<CameraState>(CameraState.Initializing) }
    var isReadingLabel by remember { mutableStateOf(false) }

    var recognizedOcrResult by remember { mutableStateOf<OcrResult?>(initialOcrResult) }
    var recognizedMatchOutcome by remember { mutableStateOf<MatchOutcome?>(initialMatchOutcome) }

    var gestureFeedbackText by remember { mutableStateOf<String?>(null) }
    var ttsState by remember { mutableStateOf<TtsState>(TtsState.Initializing) }
    val ttsManager = remember {
        TextToSpeechManager(context) { newState ->
            ttsState = newState
        }
    }

    val spokenResult = remember(recognizedMatchOutcome, recognizedOcrResult) {
        val outcome = recognizedMatchOutcome
        val result = recognizedOcrResult
        if (outcome != null && result != null) {
            SpeechContentGenerator.generateSpeech(outcome, result)
        } else null
    }

    val statusSingleTap = stringResource(R.string.gesture_status_single_tap)
    val statusDoubleTap = stringResource(R.string.gesture_status_double_tap)
    val statusLongPress = stringResource(R.string.gesture_status_long_press)
    val statusCancelled = stringResource(R.string.camera_panning_cancelled)

    // Lifecycle: stop TTS on pause, release on dispose
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                ttsManager.stop()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            ttsManager.stop()
            ttsManager.release()
            captureManager.unbind()
        }
    }

    // Reset state function to clear AR card and return camera to scanning state
    val resetToScanningState: () -> Unit = {
        ttsManager.stop()
        recognizedMatchOutcome = null
        recognizedOcrResult = null
        gestureFeedbackText = null
        isReadingLabel = false
    }

    // Back handling: if AR card is showing, reset to scanning state; otherwise navigate back
    BackHandler {
        if (recognizedMatchOutcome != null) {
            resetToScanningState()
        } else {
            ttsManager.stop()
            onBackClick()
        }
    }

    // Trigger on-device capture, OCR, and catalog matching with target voice readout
    val triggerCaptureAndIdentify: (SpeechTarget) -> Unit = { speechTarget ->
        if (cameraState is CameraState.Ready && !isReadingLabel) {
            isReadingLabel = true
            captureManager.captureImageProxy(
                executor = ContextCompat.getMainExecutor(context),
                onCaptured = { imageProxy ->
                    coroutineScope.launch(Dispatchers.Default) {
                        try {
                            val ocr = ocrProcessor.processImage(imageProxy)
                            val outcome = if (medicineCatalogue.isNotEmpty()) {
                                MedicineMatcher.match(ocr, medicineCatalogue)
                            } else {
                                MatchOutcome.InsufficientText("Catalogue loading…")
                            }
                            val speech = SpeechContentGenerator.generateSpeech(outcome, ocr)

                            withContext(Dispatchers.Main) {
                                isReadingLabel = false
                                recognizedOcrResult = ocr
                                recognizedMatchOutcome = outcome

                                // In-place audio playback based on triggering gesture
                                when (speechTarget) {
                                    SpeechTarget.BasicNameAndDosage -> {
                                        gestureFeedbackText = statusSingleTap
                                        ttsManager.speak(speech.basicNameAndDosage)
                                    }
                                    SpeechTarget.WarningsAndSideEffects -> {
                                        gestureFeedbackText = statusDoubleTap
                                        ttsManager.speak(speech.warningsAndSideEffects)
                                    }
                                    SpeechTarget.FullProfile -> {
                                        gestureFeedbackText = statusLongPress
                                        ttsManager.speak(speech.fullSpeech)
                                    }
                                }

                                // Optional backward-compatible callback
                                onOcrResult?.invoke(ocr, speechTarget)
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                isReadingLabel = false
                                cameraState = CameraState.Error(
                                    message = e.localizedMessage ?: "OCR recognition failed"
                                )
                            }
                        }
                    }
                },
                onError = { error ->
                    isReadingLabel = false
                    cameraState = CameraState.Error(
                        message = error.localizedMessage ?: "Image capture failed"
                    )
                }
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (val state = cameraState) {
            is CameraState.Error -> {
                // Recoverable Camera Error View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(id = R.string.camera_error),
                                style = MaterialTheme.typography.titleLarge,
                                color = StatusError,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            if (state.canRetry) {
                                Button(
                                    onClick = {
                                        cameraState = CameraState.Initializing
                                        resetToScanningState()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.retry),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                            OutlinedButton(
                                onClick = onBackClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.back),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                // Live Hardware Camera View with Movement Filtering & Accessible Gestures
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .accessibleTouchGestures(
                            enabled = (state is CameraState.Ready && !isReadingLabel),
                            config = GestureThresholdConfig(movementThresholdPx = 20f),
                            onSingleTap = {
                                val currentSpoken = spokenResult
                                if (recognizedMatchOutcome != null && currentSpoken != null) {
                                    // In-place speech playback for Basic Name & Dosage
                                    gestureFeedbackText = statusSingleTap
                                    ttsManager.speak(currentSpoken.basicNameAndDosage)
                                } else {
                                    // Capture current frame, identify, speak Name & Dosage, spawn 3D AR card
                                    triggerCaptureAndIdentify(SpeechTarget.BasicNameAndDosage)
                                }
                            },
                            onDoubleTap = {
                                val currentSpoken = spokenResult
                                if (recognizedMatchOutcome != null && currentSpoken != null) {
                                    // In-place speech playback for Warnings & Side Effects
                                    gestureFeedbackText = statusDoubleTap
                                    ttsManager.speak(currentSpoken.warningsAndSideEffects)
                                } else {
                                    // Capture & identify with Warnings target
                                    triggerCaptureAndIdentify(SpeechTarget.WarningsAndSideEffects)
                                }
                            },
                            onLongPress = {
                                val currentSpoken = spokenResult
                                if (recognizedMatchOutcome != null && currentSpoken != null) {
                                    // In-place speech playback for Complete Profile Summary
                                    gestureFeedbackText = statusLongPress
                                    ttsManager.speak(currentSpoken.fullSpeech)
                                } else {
                                    // Capture & identify with Full Profile target
                                    triggerCaptureAndIdentify(SpeechTarget.FullProfile)
                                }
                            },
                            onMovementCancelled = {
                                // Tremor/panning movement filter > 20px
                                gestureFeedbackText = statusCancelled
                            }
                        )
                ) {
                    // Continuous Live Camera Preview
                    CameraPreviewView(
                        captureManager = captureManager,
                        modifier = Modifier.fillMaxSize(),
                        onReady = {
                            cameraState = CameraState.Ready
                        },
                        onError = { error ->
                            cameraState = CameraState.Error(
                                message = error.localizedMessage ?: "Failed to open camera hardware"
                            )
                        }
                    )

                    // Framing Reticle
                    val hasIdentifiedCard = recognizedMatchOutcome != null
                    CameraFramingOverlay(
                        modifier = Modifier.fillMaxSize(),
                        bracketColor = if (hasIdentifiedCard) Color(0xFF81C784) else Color(0xFF145DA0),
                        scrimColor = if (hasIdentifiedCard) Color(0x33000000) else Color(0x66000000)
                    )
                }

                // Top Bar: Back / Reset Navigation, Live Status Pill, and Scan Again action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (recognizedMatchOutcome != null) {
                                resetToScanningState()
                            } else {
                                ttsManager.stop()
                                onBackClick()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black.copy(alpha = 0.65f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = if (recognizedMatchOutcome != null) "← ${stringResource(R.string.reset_scan)}" else "← ${stringResource(id = R.string.back)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Live Status Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.65f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isReadingLabel -> Color.Yellow
                                            recognizedMatchOutcome != null -> StatusSuccess
                                            state is CameraState.Ready -> StatusSuccess
                                            else -> Color.Gray
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    isReadingLabel -> stringResource(id = R.string.reading_label)
                                    recognizedMatchOutcome != null -> stringResource(R.string.medicine_identified)
                                    state is CameraState.Initializing -> "Starting camera…"
                                    state is CameraState.Ready -> "Ready to scan"
                                    else -> "Camera"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (recognizedMatchOutcome != null) {
                        Button(
                            onClick = resetToScanningState,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Black.copy(alpha = 0.65f),
                                contentColor = Color(0xFF90CAF9)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.scan_again),
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF90CAF9)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.scan_again),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // In-Place Voice Feedback & Gestures Pill Banner
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 96.dp, start = 16.dp, end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isSpeaking = (ttsState as? TtsState.Ready)?.isSpeaking == true
                    if (isSpeaking || gestureFeedbackText != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.80f),
                            border = BorderStroke(
                                1.dp,
                                if (isSpeaking) Color(0xFF81C784) else Color(0xFF64B5F6).copy(alpha = 0.8f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = gestureFeedbackText ?: if (isSpeaking) "Speaking medicine details…" else "",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (gestureFeedbackText?.contains("cancelled") == true) {
                                        Color(0xFFFF8A80)
                                    } else {
                                        Color(0xFF81C784)
                                    }
                                )
                                if (isSpeaking) {
                                    Button(
                                        onClick = { ttsManager.stop() },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                                    ) {
                                        Text("Stop Voice", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                // AR Overlay or Scanning Controls Overlay
                val matchOutcome = recognizedMatchOutcome
                val ocrResult = recognizedOcrResult

                if (matchOutcome != null && ocrResult != null) {
                    // Instant 3D AR Card Overlay on Live Camera View
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 130.dp, bottom = 24.dp, start = 16.dp, end = 16.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Live3DArMedicineCard(
                                matchOutcome = matchOutcome,
                                ocrResult = ocrResult,
                                onReadInformationClick = {
                                    ttsManager.stop()
                                    if (onReadInformationClick != null) {
                                        onReadInformationClick(ocrResult, matchOutcome)
                                    } else {
                                        onOcrResult?.invoke(ocrResult, null)
                                    }
                                },
                                onScanAgainClick = resetToScanningState
                            )
                        }
                    }
                } else {
                    // Idle Scanning Controls (Gesture hints and manual capture button)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp, start = 20.dp, end = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Gesture Hint Pill over Live Camera
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.75f)
                        ) {
                            Text(
                                text = stringResource(R.string.tap_to_scan_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE2E8F0),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        Button(
                            onClick = { triggerCaptureAndIdentify(SpeechTarget.BasicNameAndDosage) },
                            enabled = state is CameraState.Ready && !isReadingLabel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryBlue,
                                disabledContainerColor = PrimaryBlue.copy(alpha = 0.5f)
                            )
                        ) {
                            if (isReadingLabel) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(id = R.string.reading_label),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                            } else {
                                Text(
                                    text = stringResource(id = R.string.capture_button),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3D AR Card Overlay anchored directly on the live camera screen.
 * Displays identified medicine details, verified status badges, and interactive controls:
 * - Dedicated 'READ INFORMATION' button (the ONLY trigger to full-screen structured text).
 * - 'Scan Again' reset control.
 */
@Composable
private fun Live3DArMedicineCard(
    matchOutcome: MatchOutcome,
    ocrResult: OcrResult,
    onReadInformationClick: () -> Unit,
    onScanAgainClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xEE0B132B) // High-contrast translucent slate glassmorphism
        ),
        border = BorderStroke(
            1.5.dp,
            when (matchOutcome) {
                is MatchOutcome.ConfidentMatch -> Color(0xFF81C784)
                is MatchOutcome.AmbiguousMatch -> Color(0xFFFFB74D)
                else -> Color(0xFF64748B)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (matchOutcome) {
                is MatchOutcome.ConfidentMatch -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF81C784),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.match_confirmed_title).uppercase(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF81C784)
                        )
                    }

                    val brand = matchOutcome.record.brandName ?: matchOutcome.record.genericName ?: "Unknown"
                    Text(
                        text = brand,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    val strength = matchOutcome.record.strength.orEmpty()
                    val form = matchOutcome.record.dosageForm.orEmpty()
                    if (strength.isNotBlank() || form.isNotBlank()) {
                        Text(
                            text = "$strength $form".trim(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF90CAF9)
                        )
                    }

                    HorizontalDivider(color = Color(0xFF334155))

                    // Key active ingredient
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Active:", fontSize = 14.sp, color = Color(0xFF94A3B8))
                        Text(
                            matchOutcome.record.genericName ?: "Unspecified",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }

                    // Package attributes (Batch / Expiry)
                    val batch = matchOutcome.extractedBatch ?: TextNormaliser.extractBatchCandidate(ocrResult.rawText)
                    val expiry = matchOutcome.extractedExpiry ?: TextNormaliser.extractExpiryCandidate(ocrResult.rawText)
                    if (batch != null || expiry != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Package:", fontSize = 14.sp, color = Color(0xFF94A3B8))
                            Text(
                                "Batch: ${batch ?: "N/A"} • Exp: ${expiry ?: "N/A"}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }

                is MatchOutcome.AmbiguousMatch -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.match_uncertain_title).uppercase(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB74D)
                        )
                    }

                    Text(
                        text = "Identity Uncertain (${matchOutcome.candidates.size} Candidates)",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "Camera detected multiple possible catalogue matches. Will not guess without verified dosage.",
                        fontSize = 14.sp,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 20.sp
                    )
                }

                is MatchOutcome.NoMatch -> {
                    Text(
                        text = "No Reference Match",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Package could not be matched with local catalogue (${matchOutcome.explanation}).",
                        fontSize = 14.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }

                is MatchOutcome.InsufficientText -> {
                    Text(
                        text = "Unreadable Text",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "OCR could not extract clear text (${matchOutcome.explanation}). Ensure steady 15–25 cm framing under good lighting.",
                        fontSize = 14.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }

            // In-place touch gesture guide
            Text(
                text = "👆 Tap: Name • ✌️ 2x: Warnings • ⏱️ Hold: Full Summary",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Dedicated 'READ INFORMATION' Button (the ONLY trigger to full-screen structured text)
            Button(
                onClick = onReadInformationClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .semantics {
                        contentDescription = "Open full-screen structured text page to review all medicine details"
                    },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0284C7),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.read_information),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Reset / 'Scan Again' Control
            OutlinedButton(
                onClick = onScanAgainClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .semantics {
                        contentDescription = "Clear current AR card and return camera to scanning state"
                    },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.8f))
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.scan_again),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
