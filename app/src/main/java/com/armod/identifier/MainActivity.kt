package com.armod.identifier

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.armod.identifier.data.local.JsonMedicineLocalDataSource
import com.armod.identifier.data.model.MedicineRecord
import com.armod.identifier.matching.MatchOutcome
import com.armod.identifier.matching.MedicineMatcher
import com.armod.identifier.ocr.MlKitOcrProcessor
import com.armod.identifier.ocr.OcrResult
import com.armod.identifier.ui.gesture.SpeechTarget
import com.armod.identifier.ui.navigation.Screen
import com.armod.identifier.ui.screens.ArOverlayScreen
import com.armod.identifier.ui.screens.LiveScanScreen
import com.armod.identifier.ui.screens.MedicineResultScreen
import com.armod.identifier.ui.screens.PermissionScreen
import com.armod.identifier.ui.screens.ReviewOcrScreen
import com.armod.identifier.ui.screens.ScanGuidanceScreen
import com.armod.identifier.ui.screens.WelcomeScreen
import com.armod.identifier.ui.theme.ARMedicineIdentifierTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialRoute = intent?.getStringExtra("initial_screen")
        setContent {
            ARMedicineIdentifierTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(initialRoute = initialRoute)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(initialRoute: String? = null) {
    val context = LocalContext.current
    val ocrProcessor = remember { MlKitOcrProcessor() }
    val medicineDataSource = remember { JsonMedicineLocalDataSource(context) }
    val startingScreen = when (initialRoute) {
        "ar_overlay" -> Screen.ArOverlay
        "result" -> Screen.Result
        "review_ocr" -> Screen.ReviewOcr
        else -> Screen.Welcome
    }
    var currentScreen by remember { mutableStateOf<Screen>(startingScreen) }
    var pendingSpeechTarget by remember { mutableStateOf<SpeechTarget?>(null) }
    var currentMatchOutcome by remember { mutableStateOf<MatchOutcome?>(null) }
    var currentOcrResult by remember {
        mutableStateOf<OcrResult?>(
            if (initialRoute == "result" || initialRoute == "ar_overlay") {
                OcrResult(
                    rawText = "Omez 20mg Capsules Dr. Reddy's B.No: T535 EXP: 12/2027",
                    lines = listOf(
                        com.armod.identifier.ocr.OcrLine("Omez 20mg Capsules"),
                        com.armod.identifier.ocr.OcrLine("Dr. Reddy's"),
                        com.armod.identifier.ocr.OcrLine("B.No: T535"),
                        com.armod.identifier.ocr.OcrLine("EXP: 12/2027")
                    )
                )
            } else null
        )
    }
    var medicineCatalogue by remember { mutableStateOf<List<MedicineRecord>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            medicineCatalogue = medicineDataSource.getAllMedicines()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to load medicine catalogue", e)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            ocrProcessor.close()
        }
    }

    when (currentScreen) {
        Screen.Welcome -> {
            WelcomeScreen(
                onStartScanningClick = {
                    val hasCameraPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasCameraPermission) {
                        currentScreen = Screen.ScanGuidance
                    } else {
                        currentScreen = Screen.Permission
                    }
                }
            )
        }

        Screen.Permission -> {
            BackHandler {
                currentScreen = Screen.Welcome
            }
            PermissionScreen(
                onPermissionGranted = {
                    currentScreen = Screen.ScanGuidance
                },
                onBackClick = {
                    currentScreen = Screen.Welcome
                }
            )
        }

        Screen.ScanGuidance -> {
            BackHandler {
                currentScreen = Screen.Welcome
            }
            ScanGuidanceScreen(
                onBeginScanClick = {
                    currentScreen = Screen.LiveScan
                },
                onBackClick = {
                    currentScreen = Screen.Welcome
                }
            )
        }

        Screen.LiveScan -> {
            BackHandler {
                currentScreen = Screen.ScanGuidance
            }
            LiveScanScreen(
                ocrProcessor = ocrProcessor,
                medicineCatalogue = medicineCatalogue,
                initialOcrResult = currentOcrResult,
                initialMatchOutcome = currentMatchOutcome,
                onReadInformationClick = { ocrResult, outcome ->
                    currentOcrResult = ocrResult
                    currentMatchOutcome = outcome
                    currentScreen = Screen.Result
                },
                onBackClick = {
                    currentScreen = Screen.ScanGuidance
                }
            )
        }

        Screen.Result -> {
            val result = currentOcrResult ?: OcrResult(rawText = "")
            val matchOutcome = currentMatchOutcome ?: remember(result, medicineCatalogue) {
                if (medicineCatalogue.isNotEmpty()) {
                    MedicineMatcher.match(result, medicineCatalogue)
                } else {
                    MatchOutcome.InsufficientText("Loading reference catalogue…")
                }
            }

            BackHandler {
                pendingSpeechTarget = null
                currentScreen = Screen.LiveScan
            }
            MedicineResultScreen(
                matchOutcome = matchOutcome,
                ocrResult = result,
                initialSpeechTarget = pendingSpeechTarget,
                onScanAnotherClick = {
                    currentOcrResult = null
                    currentMatchOutcome = null
                    pendingSpeechTarget = null
                    currentScreen = Screen.LiveScan
                },
                onInspectOcrClick = {
                    currentScreen = Screen.ReviewOcr
                },
                onViewArClick = {
                    currentScreen = Screen.LiveScan
                },
                onBackClick = {
                    pendingSpeechTarget = null
                    currentScreen = Screen.LiveScan
                }
            )
        }

        Screen.ArOverlay -> {
            val result = currentOcrResult ?: OcrResult(rawText = "")
            val matchOutcome = currentMatchOutcome ?: remember(result, medicineCatalogue) {
                if (medicineCatalogue.isNotEmpty()) {
                    MedicineMatcher.match(result, medicineCatalogue)
                } else {
                    MatchOutcome.InsufficientText("Loading reference catalogue…")
                }
            }

            BackHandler {
                currentScreen = Screen.LiveScan
            }
            ArOverlayScreen(
                matchOutcome = matchOutcome,
                ocrResult = result,
                initialSpeechTarget = pendingSpeechTarget,
                onViewAccessibleResultClick = {
                    currentScreen = Screen.Result
                },
                onScanAnotherClick = {
                    currentOcrResult = null
                    currentMatchOutcome = null
                    pendingSpeechTarget = null
                    currentScreen = Screen.LiveScan
                },
                onBackClick = {
                    currentScreen = Screen.LiveScan
                }
            )
        }

        Screen.ReviewOcr -> {
            val result = currentOcrResult ?: OcrResult(rawText = "")
            val matchOutcome = remember(result, medicineCatalogue) {
                if (medicineCatalogue.isNotEmpty()) {
                    MedicineMatcher.match(result, medicineCatalogue)
                } else {
                    MatchOutcome.InsufficientText("Loading reference catalogue…")
                }
            }

            BackHandler {
                currentScreen = Screen.Result
            }
            ReviewOcrScreen(
                ocrResult = result,
                matchOutcome = matchOutcome,
                onScanAnotherClick = {
                    currentOcrResult = null
                    currentScreen = Screen.LiveScan
                },
                onViewResultClick = {
                    currentScreen = Screen.Result
                },
                onBackClick = {
                    currentScreen = Screen.Result
                }
            )
        }

        else -> {
            WelcomeScreen(
                onStartScanningClick = {
                    currentScreen = Screen.Permission
                }
            )
        }
    }
}
