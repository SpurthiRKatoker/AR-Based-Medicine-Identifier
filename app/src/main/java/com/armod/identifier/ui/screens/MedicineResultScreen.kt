package com.armod.identifier.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.armod.identifier.R
import com.armod.identifier.ar.ArAvailabilityChecker
import com.armod.identifier.ar.ArAvailabilityStatus
import com.armod.identifier.data.model.MedicineRecord
import com.armod.identifier.matching.MatchCandidate
import com.armod.identifier.matching.MatchOutcome
import com.armod.identifier.ocr.OcrResult
import com.armod.identifier.tts.SpeechContentGenerator
import com.armod.identifier.tts.SpokenResult
import com.armod.identifier.tts.TextToSpeechManager
import com.armod.identifier.tts.TtsState
import com.armod.identifier.ui.gesture.SpeechTarget
import com.armod.identifier.util.TextNormaliser

/**
 * Screen S07 / Read Information Screen — Structured Text Review and Control Hub.
 *
 * Designed as a clean, focused, accessible text review hub:
 * - Touch gestures are removed entirely from this screen (they remain exclusively on the live camera screen).
 * - Structured Medicine Information: Brand & Generic Name, Dosage, Expiry Date & Batch,
 *   Clinical Usage (Purpose & Directions), Warnings, Side Effects, and Academic Disclaimer.
 * - Simple Voice Guidance Controls: Speak, Stop, and Repeat audio buttons without gesture pads.
 * - Inspect Raw OCR Text: Expandable in-place raw OCR text section and detailed review button.
 * - Action Buttons: 'Scan Another Package' (resets to camera) and 'View AR Camera Overlay' (returns to live AR view).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicineResultScreen(
    matchOutcome: MatchOutcome,
    ocrResult: OcrResult,
    onScanAnotherClick: () -> Unit,
    onInspectOcrClick: () -> Unit,
    onBackClick: () -> Unit,
    onViewArClick: (() -> Unit)? = null,
    initialSpeechTarget: SpeechTarget? = null
) {
    val context = LocalContext.current
    var ttsState by remember { mutableStateOf<TtsState>(TtsState.Initializing) }

    val ttsManager = remember {
        TextToSpeechManager(context) { newState ->
            ttsState = newState
        }
    }

    val spokenResult = remember(matchOutcome, ocrResult) {
        SpeechContentGenerator.generateSpeech(matchOutcome, ocrResult)
    }

    // Auto-announce initial summary or requested target once when TTS engine is ready
    var hasAnnouncedInitially by remember { mutableStateOf(false) }
    LaunchedEffect(ttsState) {
        if (ttsState is TtsState.Ready && !hasAnnouncedInitially) {
            hasAnnouncedInitially = true
            when (initialSpeechTarget) {
                SpeechTarget.BasicNameAndDosage -> ttsManager.speak(spokenResult.basicNameAndDosage)
                SpeechTarget.WarningsAndSideEffects -> ttsManager.speak(spokenResult.warningsAndSideEffects)
                SpeechTarget.FullProfile -> ttsManager.speak(spokenResult.fullSpeech)
                null -> ttsManager.speak(spokenResult.summary)
            }
        }
    }

    // Deterministically halt speech on pause (app minimize/screen lock) and release TTS engine on dispose
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
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Medicine Information",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            modifier = Modifier.semantics { heading() }
                        )
                        Text(
                            text = "Structured Text Review",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            ttsManager.stop()
                            onBackClick()
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "Navigate back to live camera scanner"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Simple Voice Guidance Audio Controls (No gesture pad)
            VoiceGuidancePanel(
                ttsState = ttsState,
                spokenResult = spokenResult,
                onSpeak = { ttsManager.speak(spokenResult.fullSpeech) },
                onStop = { ttsManager.stop() },
                onRepeat = { ttsManager.repeat() }
            )

            // 2. Structured Medicine Information Display based on MatchOutcome
            when (matchOutcome) {
                is MatchOutcome.ConfidentMatch -> {
                    ConfidentResultCard(
                        record = matchOutcome.record,
                        ocrResult = ocrResult,
                        extractedBatch = matchOutcome.extractedBatch,
                        extractedExpiry = matchOutcome.extractedExpiry
                    )
                }

                is MatchOutcome.AmbiguousMatch -> {
                    AmbiguousResultCard(
                        candidates = matchOutcome.candidates,
                        explanation = matchOutcome.explanation,
                        ocrResult = ocrResult,
                        extractedBatch = matchOutcome.extractedBatch,
                        extractedExpiry = matchOutcome.extractedExpiry
                    )
                }

                is MatchOutcome.NoMatch -> {
                    NoMatchResultCard(
                        explanation = matchOutcome.explanation
                    )
                }

                is MatchOutcome.InsufficientText -> {
                    InsufficientTextResultCard(
                        explanation = matchOutcome.explanation
                    )
                }
            }

            // 3. Mandatory Academic Safety Notice
            AcademicSafetyDisclaimerCard()

            // 4. Inspect Raw OCR Text Section (In-place view & detailed breakdown button)
            RawOcrTextSection(
                ocrResult = ocrResult,
                onInspectDetailedClick = {
                    ttsManager.stop()
                    onInspectOcrClick()
                }
            )

            // 5. Action Buttons (Scan Another Package & View AR Camera Overlay)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Primary Action: Scan Another Package (Resets state & returns to scanner)
                Button(
                    onClick = {
                        ttsManager.stop()
                        onScanAnotherClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .semantics {
                            contentDescription = "Scan another medicine package"
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.scan_another),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Secondary Action: View AR Camera Overlay (Returns to live camera with active AR card)
                if (onViewArClick != null) {
                    val isDark = isSystemInDarkTheme()
                    val isArSupported = remember {
                        ArAvailabilityChecker.checkAvailability(context) is ArAvailabilityStatus.SupportedAndInstalled
                    }
                    Button(
                        onClick = {
                            ttsManager.stop()
                            onViewArClick()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .semantics {
                                contentDescription = if (isArSupported) {
                                    "Switch back to live camera view with active AR card overlay"
                                } else {
                                    "Switch back to live camera view with active 2D fallback overlay"
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF80CBC4) else Color(0xFF00796B),
                            contentColor = if (isDark) Color(0xFF003731) else Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArSupported) {
                                stringResource(R.string.ar_view_ar_overlay)
                            } else {
                                "${stringResource(R.string.ar_view_ar_overlay)} (2D Fallback)"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Simple, clean Voice Guidance toolbar with Speak, Stop, and Repeat playback controls.
 * Replaces any gesture pad with direct, accessible, high-contrast buttons.
 */
@Composable
fun VoiceGuidancePanel(
    ttsState: TtsState,
    spokenResult: SpokenResult,
    onSpeak: () -> Unit,
    onStop: () -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                heading()
                contentDescription = "Voice guidance audio controls"
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Icon, Title, and State Indicator Pill
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
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = stringResource(R.string.voice_guidance_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Live status badge
                val isSpeaking = (ttsState as? TtsState.Ready)?.isSpeaking == true
                val speakingBg = if (isDark) Color(0xFF81C784) else Color(0xFF1B5E20)
                val speakingFg = if (isDark) Color(0xFF003300) else Color.White
                val errorBg = if (isDark) Color(0xFFFF8A80) else Color(0xFFC62828)
                val errorFg = if (isDark) Color(0xFF4C0000) else Color.White
                val warnBg = if (isDark) Color(0xFFFFB74D) else Color(0xFF8A4B00)
                val warnFg = if (isDark) Color(0xFF3E1F00) else Color.White

                val (statusText, statusBg, statusFg) = when (ttsState) {
                    is TtsState.Initializing -> Triple("Initializing voice…", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                    is TtsState.Ready -> if (isSpeaking) {
                        Triple(stringResource(R.string.tts_status_speaking), speakingBg, speakingFg)
                    } else {
                        Triple(stringResource(R.string.tts_status_idle), MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
                    }
                    is TtsState.Error -> Triple("Voice error", errorBg, errorFg)
                    is TtsState.LanguageUnavailable -> Triple("Voice unavailable", warnBg, warnFg)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusFg
                    )
                }
            }

            // Summary description caption
            Text(
                text = spokenResult.summary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            HorizontalDivider()

            // Audio Playback Controls
            // Row 1: Full-Width Primary Speak Button
            Button(
                onClick = onSpeak,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .semantics {
                        contentDescription = "Speak full medicine information readout aloud"
                    },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                enabled = ttsState is TtsState.Ready
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Speak Full Monograph",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Row 2: Equal-Width Secondary Controls (Stop & Repeat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Stop Button: Solid Crimson Red
                val stopBg = if (isDark) Color(0xFFEF5350) else Color(0xFFC62828)
                val stopFg = if (isDark) Color(0xFF380000) else Color.White
                Button(
                    onClick = onStop,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .semantics {
                            contentDescription = "Stop voice guidance speech"
                        },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = stopBg,
                        contentColor = stopFg,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    enabled = ttsState is TtsState.Ready
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.tts_stop),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Repeat Button: Solid Teal / Secondary
                val repeatBg = if (isDark) Color(0xFF80CBC4) else MaterialTheme.colorScheme.secondary
                val repeatFg = if (isDark) Color(0xFF003731) else MaterialTheme.colorScheme.onSecondary
                Button(
                    onClick = onRepeat,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .semantics {
                            contentDescription = "Repeat speech readout from beginning"
                        },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = repeatBg,
                        contentColor = repeatFg,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    enabled = ttsState is TtsState.Ready
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.tts_repeat),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Error notice if TTS language or engine failed
            if (ttsState is TtsState.LanguageUnavailable) {
                Text(
                    text = ttsState.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            } else if (ttsState is TtsState.Error) {
                Text(
                    text = ttsState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * High-confidence verified medicine result card displaying structured clinical monograph details:
 * - Brand & Generic Name
 * - Dosage (Strength & Dosage Form)
 * - Expiry Date & Batch Number (Physical Package Attributes)
 * - Clinical Usage (Purpose & Directions)
 * - Warnings & Precautions
 * - Side Effects
 */
@Composable
fun ConfidentResultCard(
    record: MedicineRecord,
    ocrResult: OcrResult,
    extractedBatch: String?,
    extractedExpiry: String?
) {
    val isDark = isSystemInDarkTheme()
    val confirmedColor = if (isDark) Color(0xFF81C784) else Color(0xFF1B5E20)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.5.dp, confirmedColor)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Verified Status Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = confirmedColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.match_confirmed_title).uppercase(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = confirmedColor
                )
            }

            // 1. Brand & Generic Name Section
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val brandDisplay = record.brandName ?: record.genericName ?: "Unknown medicine"
                Text(
                    text = brandDisplay,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 32.sp
                )

                if (!record.genericName.isNullOrBlank() && record.genericName != brandDisplay) {
                    Text(
                        text = "Active Ingredient: ${record.genericName}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 2. Dosage Section (Strength & Form)
            val strengthDisplay = record.strength.orEmpty()
            val formDisplay = record.dosageForm.orEmpty()
            if (strengthDisplay.isNotBlank() || formDisplay.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Dosage:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "$strengthDisplay $formDisplay".trim(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            HorizontalDivider()

            // Identity Detail Breakdown Rows
            IdentityDetailRow(label = "Generic Name", value = record.genericName ?: "Not specified")
            IdentityDetailRow(label = "Verified Strength", value = record.strength ?: "Not specified")
            IdentityDetailRow(label = "Dosage Form", value = record.dosageForm ?: "Not specified")
            IdentityDetailRow(label = "Manufacturer", value = record.manufacturer ?: "Not specified")

            HorizontalDivider()

            // 3. Expiry Date & Batch Number (Physical Package Attributes from OCR)
            Text(
                text = stringResource(R.string.package_details_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            val batch = extractedBatch
                ?: TextNormaliser.extractBatchCandidate(ocrResult.rawText)
                ?: stringResource(R.string.not_detected_on_label)
            val expiry = extractedExpiry
                ?: TextNormaliser.extractExpiryCandidate(ocrResult.rawText)
                ?: stringResource(R.string.not_detected_on_label)

            IdentityDetailRow(label = stringResource(R.string.expiry_date_label), value = expiry)
            IdentityDetailRow(label = stringResource(R.string.batch_number_label), value = batch)

            HorizontalDivider()

            // 4. Clinical Usage (Purpose & Directions)
            if (!record.labelPurpose.isNullOrBlank()) {
                Text(
                    text = "Clinical Purpose / Indication",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = record.labelPurpose,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }

            if (!record.labelDirections.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.directions_title),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = record.labelDirections,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }

            HorizontalDivider()

            // 5. Warnings & Precautions
            if (record.warnings.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.warnings_title),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    text = record.warnings.joinToString(". "),
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }

            // 6. Side Effects
            if (record.commonSideEffects.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.side_effects_title),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = record.commonSideEffects.joinToString(", "),
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

/**
 * Ambiguous match card showing uncertainty explanation and available candidates.
 */
@Composable
fun AmbiguousResultCard(
    candidates: List<MatchCandidate>,
    explanation: String,
    ocrResult: OcrResult,
    extractedBatch: String?,
    extractedExpiry: String?
) {
    val isDark = isSystemInDarkTheme()
    val warningColor = if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.5.dp, warningColor)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = warningColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.match_uncertain_title).uppercase(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = warningColor
                )
            }

            Text(
                text = "Identity Uncertain — Multiple Matches",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "The camera detected text consistent with more than one catalogue record ($explanation). " +
                    "For your safety, the app will not guess. Please check your physical packaging directly.",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )

            HorizontalDivider()

            Text(
                text = "Possible Medicine Candidates:",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            candidates.forEachIndexed { index, candidate ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val brand = candidate.record.brandName ?: candidate.record.genericName ?: "Unknown"
                        val generic = candidate.record.genericName ?: "Not specified"
                        Text(
                            text = "${index + 1}. $brand ($generic)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Strength: ${candidate.record.strength ?: "Unspecified"} • Form: ${candidate.record.dosageForm ?: "Unspecified"}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Manufacturer: ${candidate.record.manufacturer ?: "Not specified"}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            val batch = extractedBatch ?: TextNormaliser.extractBatchCandidate(ocrResult.rawText)
            val expiry = extractedExpiry ?: TextNormaliser.extractExpiryCandidate(ocrResult.rawText)
            if (batch != null || expiry != null) {
                HorizontalDivider()
                Text(
                    text = stringResource(R.string.package_details_title),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (expiry != null) IdentityDetailRow(label = stringResource(R.string.expiry_date_label), value = expiry)
                if (batch != null) IdentityDetailRow(label = stringResource(R.string.batch_number_label), value = batch)
            }
        }
    }
}

/**
 * Result card when no medicine in the local catalogue matches the package.
 */
@Composable
fun NoMatchResultCard(explanation: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.match_none_title).uppercase(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Text(
                text = "Medicine Not Found in Local Catalogue",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "This packaging could not be matched against any curated reference records in the offline dataset.\n\nReason: $explanation",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )

            Text(
                text = "Tips: Ensure the medicine brand name and strength are clearly in view, avoid bright reflections on foil packaging, and hold steady.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Result card when OCR text was unreadable or blank.
 */
@Composable
fun InsufficientTextResultCard(explanation: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.match_insufficient_title).uppercase(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text(
                text = "Unreadable Label Text",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "The on-device OCR engine could not detect clear printed characters on the captured image ($explanation).",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )

            Text(
                text = "Please ensure the label is well lit, held 15–25 cm away from the camera, and not blurred by hand motion.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Inspect Raw OCR Text Section with in-place expandable view and detailed inspector action.
 */
@Composable
fun RawOcrTextSection(
    ocrResult: OcrResult,
    onInspectDetailedClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Inspect Raw OCR Text",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "${ocrResult.lines.size} lines",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "Raw character output detected on packaging by on-device ML Kit OCR.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Toggle Expand Button
            OutlinedButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isExpanded) "Hide Raw Text Preview" else "Show Raw Text Preview",
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (ocrResult.rawText.isNotBlank()) ocrResult.rawText else "No readable text detected.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Detailed inspection button linking to ReviewOcrScreen
            Button(
                onClick = onInspectDetailedClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = stringResource(R.string.inspect_raw_ocr),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Mandatory Academic Prototype Safety Disclaimer Banner.
 */
@Composable
fun AcademicSafetyDisclaimerCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = stringResource(R.string.academic_disclaimer_full),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun IdentityDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.58f)
        )
    }
}
