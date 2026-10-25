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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.armod.identifier.R
import com.armod.identifier.ar.ArAvailabilityChecker
import com.armod.identifier.ar.ArAvailabilityStatus
import com.armod.identifier.ar.ArCameraSurfaceView
import com.armod.identifier.ar.ArSessionManager
import com.armod.identifier.ar.ArSessionState
import com.armod.identifier.ar.ArTrackingState
import com.armod.identifier.matching.MatchOutcome
import com.armod.identifier.ocr.OcrResult
import com.armod.identifier.tts.SpeechContentGenerator
import com.armod.identifier.tts.TextToSpeechManager
import com.armod.identifier.tts.TtsState
import com.armod.identifier.ui.gesture.SpeechTarget
import com.armod.identifier.ui.gesture.accessibleTouchGestures
import com.armod.identifier.util.TextNormaliser

/**
 * Screen S05 / AR Overlay — Conditional Augmented Reality medicine overlay with live camera feed.
 *
 * Implements:
 * - Checks ARCore hardware compatibility at runtime.
 * - If supported: embeds [ArCameraSurfaceView] rendering the live hardware camera via OpenGL ES 2.0.
 * - Displays live ARCore tracking state from real camera frames.
 * - Overlays readable medicine information and package attributes above the live camera view.
 * - If unsupported or failed (e.g. Samsung Galaxy F13): seamlessly displays the 2D camera fallback
 *   with a prominent, one-tap transition to the accessible 2D result screen.
 * - Preserves deterministic lifecycle management (safe resume, pause, and close).
 * - Guarantees high-contrast WCAG AAA typography and minimum 48dp touch targets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArOverlayScreen(
    matchOutcome: MatchOutcome,
    ocrResult: OcrResult,
    onViewAccessibleResultClick: () -> Unit,
    onScanAnotherClick: () -> Unit,
    onBackClick: () -> Unit,
    initialSpeechTarget: SpeechTarget? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val isDark = isSystemInDarkTheme()

    var arSessionState by remember { mutableStateOf<ArSessionState>(ArSessionState.Uninitialized) }
    var surfaceViewRef by remember { mutableStateOf<ArCameraSurfaceView?>(null) }

    val statusSingleTap = stringResource(R.string.gesture_status_single_tap)
    val statusDoubleTap = stringResource(R.string.gesture_status_double_tap)
    val statusLongPress = stringResource(R.string.gesture_status_long_press)
    val statusCancelled = stringResource(R.string.gesture_status_cancelled)
    val readInfoDesc = stringResource(R.string.read_information_desc)
    val scanAgainDesc = stringResource(R.string.scan_again_desc)

    var ttsState by remember { mutableStateOf<TtsState>(TtsState.Initializing) }
    val ttsManager = remember {
        TextToSpeechManager(context) { newState ->
            ttsState = newState
        }
    }

    val spokenResult = remember(matchOutcome, ocrResult) {
        SpeechContentGenerator.generateSpeech(matchOutcome, ocrResult)
    }

    var gestureFeedbackText by remember { mutableStateOf<String?>(null) }

    // Auto-announce initial speech target once upon TTS engine readiness if provided
    var hasAnnouncedInitially by remember { mutableStateOf(false) }
    LaunchedEffect(ttsState) {
        if (ttsState is TtsState.Ready && !hasAnnouncedInitially && initialSpeechTarget != null) {
            hasAnnouncedInitially = true
            when (initialSpeechTarget) {
                SpeechTarget.BasicNameAndDosage -> {
                    gestureFeedbackText = statusSingleTap
                    ttsManager.speak(spokenResult.basicNameAndDosage)
                }
                SpeechTarget.WarningsAndSideEffects -> {
                    gestureFeedbackText = statusDoubleTap
                    ttsManager.speak(spokenResult.warningsAndSideEffects)
                }
                SpeechTarget.FullProfile -> {
                    gestureFeedbackText = statusLongPress
                    ttsManager.speak(spokenResult.fullSpeech)
                }
            }
        }
    }

    val arSessionManager = remember {
        ArSessionManager(context) { newState ->
            arSessionState = newState
        }
    }

    // Lifecycle: Create and resume AR session on entry, release on dispose
    LaunchedEffect(Unit) {
        val availability = ArAvailabilityChecker.checkAvailability(context)
        if (availability is ArAvailabilityStatus.SupportedAndInstalled) {
            val success = arSessionManager.createSession(activity)
            if (success) {
                arSessionManager.resume()
            }
        } else if (availability is ArAvailabilityStatus.UnsupportedDevice) {
            arSessionState = ArSessionState.Unsupported(availability.reason)
        } else {
            arSessionState = ArSessionState.Unsupported(
                "ARCore is not installed or unavailable on this device. 2D fallback active."
            )
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (arSessionState is ArSessionState.Active) {
                        surfaceViewRef?.onResume()
                        arSessionManager.resume()
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    surfaceViewRef?.onPause()
                    arSessionManager.pause()
                    ttsManager.stop()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            surfaceViewRef?.onPause()
            arSessionManager.destroy()
            ttsManager.stop()
            ttsManager.release()
        }
    }

    BackHandler {
        ttsManager.stop()
        onBackClick()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Live ARCore OpenGL Camera Background Feed
        if (arSessionState !is ArSessionState.Unsupported && arSessionState !is ArSessionState.Error) {
            AndroidView(
                factory = { ctx ->
                    ArCameraSurfaceView(ctx, arSessionManager).also {
                        surfaceViewRef = it
                        it.onResume()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Scrim and Compose UI Overlay
        Scaffold(
            containerColor = if (arSessionState is ArSessionState.Unsupported || arSessionState is ArSessionState.Error) {
                if (isDark) Color(0xFF101720) else Color(0xFF1E293B)
            } else {
                Color.Transparent
            },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.ar_overlay_title),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            color = Color.White,
                            modifier = Modifier.semantics { heading() }
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.semantics {
                                contentDescription = "Navigate back from AR overlay"
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.65f)
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .accessibleTouchGestures(
                        enabled = true,
                        onSingleTap = {
                            gestureFeedbackText = statusSingleTap
                            ttsManager.speak(spokenResult.basicNameAndDosage)
                        },
                        onDoubleTap = {
                            gestureFeedbackText = statusDoubleTap
                            ttsManager.speak(spokenResult.warningsAndSideEffects)
                        },
                        onLongPress = {
                            gestureFeedbackText = statusLongPress
                            ttsManager.speak(spokenResult.fullSpeech)
                        },
                        onMovementCancelled = {
                            gestureFeedbackText = statusCancelled
                        }
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. AR Tracking Status Indicator Pill (live updated from AR frame tracking)
                    ArTrackingStatusPill(
                        sessionState = arSessionState,
                        isDark = true
                    )

                    // 1b. Accessible Touch & Voice Controls Banner
                    val isSpeaking = (ttsState as? TtsState.Ready)?.isSpeaking == true
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.75f)
                        ),
                        border = BorderStroke(1.dp, if (isSpeaking) Color(0xFF81C784) else Color(0xFF64B5F6).copy(alpha = 0.8f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "👆 Tap: Name • ✌️ 2x: Warnings • ⏱️ Hold: Full",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFE2E8F0)
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
                            gestureFeedbackText?.let { feedback ->
                                Text(
                                    text = feedback,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (feedback.contains("cancelled")) Color(0xFFFF8A80) else Color(0xFF81C784)
                                )
                            }
                        }
                    }

                    // 2. Camera-View Medicine Information Card or 2D Fallback Card
                    when (arSessionState) {
                        is ArSessionState.Active -> {
                            ArSpatialMedicineCard(
                                matchOutcome = matchOutcome,
                                ocrResult = ocrResult,
                                isDark = true
                            )
                        }

                        is ArSessionState.Unsupported,
                        is ArSessionState.Error -> {
                            val reason = (arSessionState as? ArSessionState.Unsupported)?.reason
                                ?: (arSessionState as? ArSessionState.Error)?.message
                                ?: stringResource(R.string.ar_fallback_explanation)
                            Ar2DFallbackCard(
                                explanation = reason,
                                onViewAccessibleResult = onViewAccessibleResultClick,
                                isDark = isDark
                            )
                        }

                        is ArSessionState.Initializing,
                        ArSessionState.Uninitialized -> {
                            ArLoadingCard(isDark = true)
                        }
                    }

                    // 3. Live Camera Targeting Guidance
                    if (arSessionState is ArSessionState.Active) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Black.copy(alpha = 0.60f)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF64748B).copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF60A5FA),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Live camera tracking active. Hold device 15–25 cm over package.",
                                    fontSize = 13.sp,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }

                    // 4. Academic Prototype Disclaimer Notice
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.65f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Academic Prototype: Live camera overlay for research demonstration only. Not a clinically validated medical tool. Always check physical packaging.",
                                fontSize = 12.sp,
                                color = Color(0xFFCBD5E1),
                                lineHeight = 17.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 5. Primary and Secondary Action Controls: Dedicated 'READ INFORMATION' button and 'Scan Again' reset
                    Button(
                        onClick = onViewAccessibleResultClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .semantics {
                                contentDescription = readInfoDesc
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

                    OutlinedButton(
                        onClick = onScanAnotherClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .semantics {
                                contentDescription = scanAgainDesc
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
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Status pill showing live tracking state or fallback status.
 */
@Composable
private fun ArTrackingStatusPill(
    sessionState: ArSessionState,
    isDark: Boolean
) {
    val (statusText, statusBg, statusFg) = when (sessionState) {
        is ArSessionState.Active -> when (sessionState.trackingState) {
            is ArTrackingState.Tracking -> Triple(
                stringResource(R.string.ar_status_tracking),
                if (isDark) Color(0xFF81C784) else Color(0xFF1B5E20),
                if (isDark) Color(0xFF003300) else Color.White
            )
            is ArTrackingState.SearchingPlanes -> Triple(
                stringResource(R.string.ar_status_searching),
                if (isDark) Color(0xFF90CAF9) else Color(0xFF1565C0),
                if (isDark) Color(0xFF002F54) else Color.White
            )
            is ArTrackingState.Limited -> Triple(
                sessionState.trackingState.reason,
                if (isDark) Color(0xFFFFB74D) else Color(0xFF8A4B00),
                if (isDark) Color(0xFF3E1F00) else Color.White
            )
            is ArTrackingState.Lost -> Triple(
                "Tracking lost — Hold over package",
                if (isDark) Color(0xFFFF8A80) else Color(0xFFC62828),
                if (isDark) Color(0xFF4C0000) else Color.White
            )
        }
        is ArSessionState.Unsupported -> Triple(
            stringResource(R.string.ar_status_unsupported),
            Color(0xFF475569),
            Color.White
        )
        is ArSessionState.Error -> Triple(
            stringResource(R.string.ar_status_error),
            if (isDark) Color(0xFFFF8A80) else Color(0xFFC62828),
            if (isDark) Color(0xFF4C0000) else Color.White
        )
        is ArSessionState.Initializing -> Triple(
            "Initializing ARCore session…",
            Color(0xFF334155),
            Color.White
        )
        is ArSessionState.Uninitialized -> Triple(
            "Starting AR session…",
            Color(0xFF334155),
            Color.White
        )
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(statusBg)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = statusText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = statusFg
        )
    }
}

/**
 * Floating AR Spatial Medicine Card displaying verified identification attributes.
 */
@Composable
private fun ArSpatialMedicineCard(
    matchOutcome: MatchOutcome,
    ocrResult: OcrResult,
    isDark: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xDD0F172A)
        ),
        border = BorderStroke(
            1.5.dp,
            when (matchOutcome) {
                is MatchOutcome.ConfidentMatch -> if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                is MatchOutcome.AmbiguousMatch -> if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100)
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
                        val accentGreen = if (isDark) Color(0xFF81C784) else Color(0xFF4CAF50)
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = accentGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.match_confirmed_title).uppercase(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentGreen
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
                            color = if (isDark) Color(0xFF90CAF9) else Color(0xFF80D4FF)
                        )
                    }

                    HorizontalDivider(color = Color(0xFF475569))

                    // Key attributes
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
                        val accentAmber = if (isDark) Color(0xFFFFB74D) else Color(0xFFFFA726)
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = accentAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(R.string.match_uncertain_title).uppercase(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentAmber
                        )
                    }

                    Text(
                        text = "Identity Uncertain (${matchOutcome.candidates.size} Candidates)",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "Camera detected multiple possible matches. For safety, the app will not guess. Switch to 2D view to review all candidates.",
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
                        text = "OCR could not extract clear text (${matchOutcome.explanation}). Ensure proper lighting.",
                        fontSize = 14.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }
    }
}

/**
 * 2D Fallback Card rendered when ARCore is unsupported or unavailable.
 */
@Composable
private fun Ar2DFallbackCard(
    explanation: String,
    onViewAccessibleResult: () -> Unit,
    isDark: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFF263238)
        ),
        border = BorderStroke(1.5.dp, Color(0xFF64748B))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF90CAF9) else Color(0xFF64B5F6),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = stringResource(R.string.ar_fallback_active_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Text(
                text = explanation,
                fontSize = 15.sp,
                color = Color(0xFFCBD5E1),
                lineHeight = 22.sp
            )

            Text(
                text = "All medicine identification, OCR details, voice guidance, and safety disclaimers remain fully accessible via the 2D result screen.",
                fontSize = 14.sp,
                color = Color(0xFF94A3B8),
                lineHeight = 20.sp
            )
        }
    }
}

/**
 * Loading state card while ARCore initializes.
 */
@Composable
private fun ArLoadingCard(isDark: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFF263238)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Initializing AR Environment…",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = "Checking device ARCore readiness and motion sensors.",
                fontSize = 14.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}
