package com.armod.identifier.ui.navigation

/**
 * Screen identifiers as defined in AR_Medicine_Identifier_App_Flow.md.
 */
sealed class Screen(val route: String) {
    data object Welcome : Screen("welcome")
    data object Permission : Screen("permission")
    data object ScanGuidance : Screen("scan_guidance")
    data object LiveScan : Screen("live_scan")
    data object Recognising : Screen("recognising")
    data object ReviewOcr : Screen("review_ocr")
    data object Result : Screen("result")
    data object ArOverlay : Screen("ar_overlay")
    data object Confirmation : Screen("confirmation")
    data object Unknown : Screen("unknown")
    data object Error : Screen("error")
}
