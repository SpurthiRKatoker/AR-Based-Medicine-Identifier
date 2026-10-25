# TASKS.md — Implementation Checklist

**Project:** SLAM-Based AR Medicine Identifier with Voice-Guided Accessibility Mode  
**Status at template creation:** Not started  
**Owner of project-state updates:** Anti-Gravity only  
**Rule:** Do not mark a task complete until the phase's verification checks pass and the evidence is recorded.

## How to use this file

- Work through phases in order. Do not start a later phase while the current phase is blocked or unverified.
- Mark a checkbox `[x]` only after doing the work and testing it.
- For each completed phase, add a short evidence note: commands run, tests performed, device/emulator used, and results.
- If a task cannot be completed, leave it unchecked and record the blocker. Never claim a feature works without evidence.
- At the end of every phase, Anti-Gravity must update `MEMORY.md`, `DECISIONS.md`, and this `TASKS.md`.
- Do not invent progress. This template starts with all implementation tasks unchecked.

## Phase 0 — Read project instructions and establish baseline

- [x] Read all available project documents, including the PRD, TRD, App Flow, UI/UX Design Brief, Backend Schema, `MEMORY.md`, `DECISIONS.md`, and `IMPLEMENTATION_PLAN.md`.
- [x] Inspect the existing repository and identify whether it is empty, partially scaffolded, or already contains code.
- [x] Confirm the target device is Samsung Galaxy F13 and identify the available Android/Gradle/JDK toolchain.
- [x] Record actual repository state and toolchain findings in `MEMORY.md`.
- [x] List conflicts, missing information, or decisions requiring user input before implementation.
- [x] Do not create application features in this phase.

**Phase verification**
- [x] All available project documents have been reviewed.
- [x] Repository/toolchain status is evidenced rather than assumed.
- [x] Any blocking ambiguity is reported to the user.

**Evidence / notes:**
- Document review completed across all 9 specification markdown files and the technical context document.
- Repository inspected via PowerShell: Clean slate (10 documentation/PDF files present, no existing code, no Gradle files, not a git repository).
- Toolchain verified: Android Studio OpenJDK 21.0.10 at `C:\Program Files\Android\Android Studio\jbr\bin\java.exe`, Android SDK at `C:\Users\skzah\AppData\Local\Android\Sdk` (platforms: `android-36.1`, build-tools: `36.1.0` and `37.0.0`, adb: `platform-tools\adb.exe`).
- Connectivity checked: `adb devices` shows 0 connected devices, emulator has 0 configured AVDs.
- Blockers & conflicts noted: `JAVA_HOME` pointing to non-existent `C:\Java\bin` (will override to JBR 21), `ANDROID_HOME` will be exported, PRD Section 8 barcode mention overridden by D-009 / PRD Section 15.1 / user prompt (barcode scanning strictly out of scope).

## Phase 1 — Project setup and build foundation

- [x] Create or validate the native Android project using Kotlin and Jetpack Compose.
- [x] Configure the Android SDK, Gradle, JDK, minimum SDK baseline (provisionally API 26), and app identifier.
- [x] Create a clear package/folder structure for UI, navigation, camera, OCR, matching, local data, speech, and optional AR.
- [x] Add only the dependencies required by the approved architecture.
- [x] Configure version control and add an appropriate `.gitignore`.
- [x] Confirm no backend, authentication service, cloud API, or remote database is introduced.
- [ ] Build and launch the empty app on an emulator or the Samsung Galaxy F13 if available (Build passed; launch NOT RUN / BLOCKED due to no connected physical device and BIOS virtualization disabled).

**Phase verification**
- [x] Debug build succeeds.
- [ ] App launches without crashing (NOT RUN / BLOCKED: no physical device attached; local emulator blocked by disabled BIOS virtualization).
- [x] Package structure and dependency choices are documented.
- [x] Any device-specific compatibility limitation is recorded.

**Evidence / notes:**
- Git initialized in workspace; standard Android `.gitignore` configured (ignoring `.gradle/`, `app/build/`, `local.properties`).
- Native Android project scaffolded with Kotlin 2.0.21, Compose Compiler Plugin, AGP 8.7.3, and Gradle 8.11.1 Wrapper.
- Application ID: `com.armod.identifier`, `compileSdk = 35`, `minSdk = 26`, `targetSdk = 35`.
- Toolchain: Android Studio bundled JBR (OpenJDK 21.0.10) and Android SDK at `C:\Users\skzah\AppData\Local\Android\Sdk`. User environment variables `JAVA_HOME` and `ANDROID_HOME` configured.
- Clean package structure established under `com.armod.identifier`: `data/model/`, `data/local/`, `domain/`, `camera/`, `ocr/`, `matching/`, `speech/`, `ar/`, `ui/navigation/`, `ui/components/`, `ui/theme/`, `util/`.
- Verified build and unit tests:
  - `.\gradlew.bat test`: BUILD SUCCESSFUL (45 tasks executed). `BaselineUnitTest` passed.
  - `.\gradlew.bat assembleDebug`: BUILD SUCCESSFUL (35 tasks executed). Output artifact verified at `app/build/outputs/apk/debug/app-debug.apk` (9,472,683 bytes).
- Launch status: NOT RUN / BLOCKED because Samsung Galaxy F13 is not attached via USB debugging, and local emulator is blocked by CPU firmware virtualization being disabled in BIOS.

## Phase 2 — Local medicine data model and bundled dataset

- [x] Define the local medicine record model based on `Backend Schema` and the approved PRD/TRD.
- [x] Define required and optional fields, including stable ID, display name, package/brand identifiers where supported, searchable text, and source/provenance metadata.
- [x] Add a small curated JSON dataset for the prototype (target approximately 20–30 clearly labelled package records, subject to verified data availability).
- [x] Validate JSON parsing and required fields at app startup or through tests.
- [x] Add matching/search functions over the bundled data.
- [x] Handle unknown, missing, duplicate, or malformed records safely.
- [x] Check data source attribution and licence/terms before including records.
- [x] Do not add unsupported medical claims, personalized dosing advice, or fabricated medicine details.
- [x] Do not introduce Room/SQLite unless a concrete approved need (such as optional local scan history) is established.

**Phase verification**
- [x] Dataset parses successfully.
- [x] Automated tests cover valid, missing, duplicate, and malformed records.
- [x] At least one known sample record can be retrieved locally while offline.
- [x] Data provenance is recorded for every included record where available.

**Evidence / notes:**
- `MedicineRecord` model annotated with `kotlinx.serialization` annotations (`@Serializable`, `@SerialName`), mapping exact schema properties from Backend Schema Section 4.
- `app/src/main/assets/medicines.json` created with 25 curated, real-world medicine records sourced from NLEM 2022, NHS UK, DailyMed/FDA, and WHO EML.
- Full provenance and attribution documented in `docs/dataset-sources.md`. No fabricated facts or personal dosing advice included.
- `JsonMedicineParser` implemented with strict validation: detects malformed JSON, missing required fields (`id`, `sourceTitle`, `datasetVersion`), duplicate IDs, and empty datasets, while safely ignoring unexpected optional fields.
- `JsonMedicineLocalDataSource` implemented offering cached in-memory access and fast lookup methods (`getMedicineById`, `searchByName`, `findByExactNameAndStrength`).
- Verified inside APK: `assets/medicines.json` confirmed packaged inside `app-debug.apk` (25,414 bytes).
- Automated tests in `MedicineDataValidationTest`: All 9 test cases passed in `./gradlew test`. `./gradlew assembleDebug` passed cleanly.

## Phase 3 — Permissions and camera foundation (no authentication)

- [x] Confirm and document that user authentication is out of scope; do not build signup, login, sessions, or protected routes.
- [x] Implement camera permission request and denial handling.
- [x] Implement camera preview/capture using an Android-compatible camera approach (CameraX 1.4.1).
- [x] Provide clear guidance when permission is denied or the camera is unavailable.
- [x] Keep image processing on-device and avoid uploading frames or OCR text.
- [x] Add a way to retry or return safely from camera errors.

**Phase verification**
- [x] Permission granted: camera preview/capture works (verified live on connected Redmi Note 11).
- [x] Permission denied: app remains usable and explains how to proceed ("Try again" / "Open App Settings").
- [x] Camera unavailable/error: no crash; a useful recovery path is shown.
- [x] Network inspection/code review confirms no image upload path was added (`android.permission.INTERNET` absent).

**Evidence / notes:**
- Verified connected physical device: Redmi Note 11 (`79145af9`, Model 2201117TI, Android 13 / API 33).
- Added `android.permission.CAMERA` to `AndroidManifest.xml`. Confirmed zero internet/network permissions exist in manifest.
- Integrated CameraX 1.4.1 (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`) with `concurrent-futures-ktx:1.2.0`.
- Implemented `CameraCaptureManager` managing lifecycle-bound preview and on-device photo capture, immediately closing `ImageProxy` after metadata extraction to prevent memory leaks.
- Implemented `CameraFramingOverlay` with high-contrast corner brackets (Primary Blue `#145DA0`) and subtle scrim.
- Implemented screens: Welcome (S01), Permission (S02), Scan Guidance (S03), and LiveScan (S04) with Compose state navigation and `BackHandler`.
- Verified on physical hardware (Redmi Note 11):
  1. First launch and runtime permission request handled.
  2. Granted permission transitioned immediately to Scan Guidance, then live camera stream opened.
  3. Image captured successfully at 2448 × 3264 px resolution (0° rotation) and shown in capture confirmation card.
  4. Permission denial handled without crash, correctly rendering Permission Denied screen with "Try again" and "Back" buttons.
  5. Recovery guidance verified on device.
- Automated tests: Total 13 tests passed in `./gradlew.bat test` (3 tests in `CameraStateTest`, 9 in `MedicineDataValidationTest`, 1 in `BaselineUnitTest`).
- Build verification: `.\gradlew.bat assembleDebug` produced verified APK (11,856,070 bytes) and installed onto physical hardware via ADB.

## Phase 4 — On-device OCR pipeline

- [x] Integrate ML Kit Text Recognition v2 for on-device text recognition.
- [x] Process a captured image and return recognized text plus useful text-block/line structure where available.
- [x] Add progress/loading state and a way to cancel or retry.
- [x] Handle empty, blurry, low-light, rotated, or unreadable captures with honest feedback.
- [x] Keep OCR output transient unless the user explicitly chooses an approved local-history feature.
- [x] Add unit tests for text normalization and field extraction using representative sample OCR strings.

**Phase verification**
- [x] OCR works on representative labelled medicine-package images (verified live on Redmi Note 11 with Calcium + D3 and Dr. Reddy's Omeprazole 20mg).
- [x] Empty/unreadable input produces a clear recovery message (verified via S06 empty state card and automated tests).
- [x] OCR runs on-device without a cloud dependency (ML Kit Latin script model, zero network calls, no INTERNET permission).
- [x] Text extraction tests pass (all 7 unit tests in `OcrPipelineUnitTest` passed).

**Evidence / notes:**
- Pinned `com.google.mlkit:text-recognition:16.0.1` in `gradle/libs.versions.toml` and `app/build.gradle.kts`. Confirmed `android.permission.INTERNET` remains absent from manifest.
- Implemented `OcrProcessor` interface and `MlKitOcrProcessor` with `InputImage.fromMediaImage` and guaranteed `imageProxy.close()` on background `Dispatchers.Default`.
- Structured output model in `OcrResult`: blocks (`OcrBlock`), lines (`OcrLine`), bounding boxes (`OcrBoundingBox`), dimensions, and line count.
- Added candidate pattern regexes in `TextNormaliser` for printed expiry dates (`EXP`, `EXPIRY`, `USE BY`) and batch codes (`BATCH NO`, `LOT`, `B. NO`).
- Implemented S06 `ReviewOcrScreen` with scrollable raw text, detected line breakdown, extracted candidate chips, empty/unreadable card, "Scan another package", and "Back" navigation.
- Added `reading_label` loading state in `LiveScanScreen` with `CircularProgressIndicator`.
- Automated tests: All 7 test cases in `OcrPipelineUnitTest` passed in `.\gradlew.bat test`. Total 20 tests pass across test suite (1 Baseline, 3 Camera, 9 Medicine, 7 OCR).
- Build verification: `.\gradlew.bat assembleDebug` succeeds in 16s (APK size: 55,285,051 bytes including bundled ML Kit Latin OCR model).
- Physical device verification: Installed and verified on Redmi Note 11 (Android 13 / API 33). Tested against real medicine packages:
  - Calcium + Vitamin D3 package: 33 lines, 19 blocks, Batch code T535 detected.
  - Dr. Reddy's Omeprazole 20mg package: 45 lines, 22 blocks recognized.
  - Device active screen verified in recovery session via `screencap`.

## Phase 5 — Field extraction, local matching, and confidence gate

- [x] Extract only supported fields from OCR text; preserve an unknown/unreadable state when a field cannot be read.
- [x] Normalize text for matching without discarding meaningful distinctions.
- [x] Match OCR results against the bundled local dataset.
- [x] Implement a conservative confidence/ambiguity gate.
- [x] Show a single result only when the evidence meets the defined threshold; otherwise show possible matches or ask the user to retake the image.
- [x] Ensure low-confidence matches are never presented as certain.
- [x] Do not infer expiry dates or other fields that OCR cannot support.

**Phase verification**
- [x] Tests cover confident match, ambiguous match, no match, and unreadable fields.
- [x] Low-confidence cases cannot be presented as confirmed identity.
- [x] Matching works with network disabled.

**Evidence / notes:**
- Created `com.armod.identifier.matching.MatchOutcome` with typed outcomes: `ConfidentMatch`, `AmbiguousMatch`, `NoMatch`, and `InsufficientText`.
- Implemented `com.armod.identifier.matching.MedicineMatcher`:
  - Token-level matching against brand names, generic active ingredients, dosage forms, and manufacturers.
  - Regex extraction for numerical strength and units (`mg`, `mcg`, `g`, `ml`, `iu`, `%`), including OCR glued token resilience (`20.mg`, `P20mg`).
  - Conflicting strength hard disqualifier: candidate strength mismatch prevents false identification.
  - Multi-strength catalogue ambiguity gate: if a matched medicine name has multiple catalogued strength formulations (e.g., Paracetamol 500mg vs 650mg, Atorvastatin 10mg vs 20mg) and OCR strength is absent or unverified, match is downgraded to `AmbiguousMatch`.
  - Expiry and batch values extracted only via `TextNormaliser` package regexes; strictly excluded from medicine identity matching.
- Screen S06 `ReviewOcrScreen` updated with:
  - Confident match banner with green accents, verified badges, and expandable monograph accordion (directions, warnings, side effects).
  - Ambiguous match warning card explaining uncertainty and listing candidates with individual strengths and forms.
  - No match card explaining uncatalogued package or insufficient evidence.
  - Insufficient text retry card when OCR text is empty or blank.
  - Preserved raw OCR text expander and detected lines for developer/auditor inspection.
- Unit tests: Added 15 unit tests in `MedicineMatcherTest` covering exact match, glued OCR noise, conflicting strengths, multi-strength ambiguity, uncatalogued medicines, date/batch exclusion, and real 25-record catalogue fixtures. Total 35/35 unit tests pass in `./gradlew.bat test`.
- Build: `./gradlew.bat assembleDebug` succeeded in 19s (APK: 56,066,979 bytes).
- Hardware verification: Installed debug APK on connected Redmi Note 11 (Android 13 / API 33, ADB ID `79145af9`). Confirmed `MainActivity` launch and reactive matching state transitions.

## Phase 6 — Accessible results and voice guidance

- [x] Build the medicine result screen using the approved UI/UX brief.
- [x] Integrate Android TextToSpeech for read-aloud results and key instructions.
- [x] Add visible controls to speak, stop speech, and repeat speech.
- [x] Ensure important information is available as text as well as speech.
- [x] Support large readable text, adequate contrast, clear touch targets, and screen-reader semantics.
- [x] Clearly distinguish confirmed readable fields from unknown/unreadable fields.
- [x] Include a concise safety message that the app is an academic prototype and not a substitute for pharmacist/clinician advice.

**Phase verification**
- [x] Text and speech agree and do not invent missing values.
- [x] Speech can be started/stopped/repeated and handles unavailable TTS gracefully.
- [x] Core flow is usable without relying on color or speech alone.
- [x] TalkBack/Android accessibility checks are performed where available.

**Evidence / notes:**
- Created `com.armod.identifier.tts.SpeechContentGenerator`: pure JVM component synthesizing clear English readouts for `ConfidentMatch`, `AmbiguousMatch`, `NoMatch`, and `InsufficientText`. Features phonetic dosage expansion (`20mg` -> `20 milligrams`), batch/expiry attribute readouts, and mandatory academic safety disclaimers.
- Created `com.armod.identifier.tts.TextToSpeechManager`: lifecycle-safe native `android.speech.tts.TextToSpeech` manager with MainLooper state synchronization, `UtteranceProgressListener` tracking, Speak/Stop/Repeat operations, and deterministic resource release on dispose.
- Built Screen S07 `MedicineResultScreen`:
  - High-contrast typography scale (26sp title, 20sp subtitle, 15sp body) and >= 48-56dp touch targets.
  - Dedicated `VoiceGuidancePanel` with Speak, Stop, Repeat buttons and dynamic status indicator ("Speaking…", "Ready to speak", "Voice unavailable").
  - Dedicated cards for Confident matches (monograph directions, warnings, side effects), Ambiguous matches (candidate cards and uncertainty explanation), No Match, and Insufficient Text.
  - Highly visible Academic Prototype Safety Notice banner.
- Integrated into `MainActivity`: LiveScan auto-navigates to S07 `MedicineResultScreen`; S06 `ReviewOcrScreen` and S07 `MedicineResultScreen` cross-link cleanly with back and inspection buttons.
- Automated tests: Added 6 unit tests in `SpeechContentGeneratorTest` covering all matching outcomes, phonetic expansions, and disclaimer checks. Total 41/41 unit tests pass in `./gradlew.bat test`.
- Build verification: `./gradlew.bat assembleDebug` succeeded in 27s (APK: 56,068,844 bytes).
- Physical hardware verification: Deployed to connected Redmi Note 11 (Android 13 / API 33, ADB ID `79145af9`). Confirmed app launch, foreground execution, and zero logcat crashes.
- Phase 6 UI Bug Fix (Voice controls clipping & contrast):
  - Root cause: Previous 3-button horizontal row using OutlinedButton resulted in button width < 100dp on narrow mobile screens (392dp device width), clipping labels and truncating text. Stop/Repeat buttons suffered low contrast.
  - Fix: Refactored `VoiceGuidancePanel` into a responsive 2-row layout:
    - Row 1: Full-width primary `Speak` Button (52dp height, filled container).
    - Row 2: Equal-width `Stop` Button (48dp height, solid Crimson Red `#EF5350` / `#C62828`) and `Repeat` Button (48dp height, solid Teal `#80CBC4` / `#00796B`).
    - Dark theme palette refined in `Theme.kt` and `Color.kt` for WCAG AAA contrast >= 7:1 across all controls, status badges, and card text in both light and dark themes.
    - Verified on physical Redmi Note 11 (`screen_result_v2_top.png` and `screen_result_v2_ready.png`). Zero clipping, full label visibility, high contrast, smooth scrolling, and complete navigation bar clearance.
  - Automated tests: 41/41 unit tests pass (`.\gradlew.bat test`). Debug APK assembled (`.\gradlew.bat assembleDebug`).

## Phase 7 — AR overlay and 2D fallback

- [x] Check ARCore support and compatibility on the target handset/build environment before implementation.
- [x] If feasible within the one-week prototype, implement a simple AR overlay for supported extracted fields.
- [x] Keep the overlay concise and anchored in a stable, understandable way.
- [x] If ARCore is unsupported, unstable, or threatens the deadline, provide the approved accessible 2D result/camera fallback instead.
- [x] Do not block the main identification flow on AR availability.

**Phase verification**
- [x] Either the AR overlay works on a supported target or the 2D fallback is complete and tested.
- [x] Unsupported AR does not crash or prevent identification.
- [x] Accessibility and text/speech results remain available.

**Evidence / notes:**
- Device & Compatibility Audit:
  - Audited target devices before adding ARCore dependencies:
    - Redmi Note 11 (Current test device, Snapdragon 680): Officially certified on Google ARCore Supported Devices list; `com.google.ar.core` v1.56.262080393 preinstalled and enabled (`installed=true`); hardware camera and gyro sensors operational.
    - Samsung Galaxy F13 (Original target, Exynos 850): Budget handset, NOT on Google ARCore Supported Devices list; lacks certified sensor calibration and cannot run ARCore without errors. Confirmed D-008: AR must be conditional (`com.google.ar.core` optional, `android.hardware.camera.ar` required="false"), and 2D fallback is strictly mandatory so non-AR devices never crash or block.
  - Toolchain compatibility: ARCore 1.47.0 (`com.google.ar:core:1.47.0`) integrates cleanly with AGP 8.7.3, Kotlin 2.0.21, and compileSdk 35.
  - Zero-network compliance: ARCore AAR manifest merged `INTERNET` and `ACCESS_NETWORK_STATE`; these were explicitly blocked via `tools:node="remove"` in `AndroidManifest.xml`. Merged manifest verified via build tools: zero network permissions exist in the compiled APK.
- Architecture & Implementation:
  - Created `ArAvailabilityChecker` and typed `ArAvailabilityStatus` sealed interface (`SupportedAndInstalled`, `SupportedNeedsInstall`, `UnsupportedDevice`, `Unknown`) with graceful timeout and error handling.
  - Created `ArSessionManager` with safe lifecycle hooks (`createSession`, `resume`, `pause`, `close`), continuous autofocus configuration for close-up medicine package scanning, and frame-level tracking updates (`SearchingPlanes`, `Tracking`, `Limited`, `Lost`).
  - Created Screen S05 `ArOverlayScreen`:
    - Dynamic tracking status pill ("Detecting package surface…", "Package tracked", "Tracking limited", "Tracking lost").
    - Spatial floating medicine card showing verified fields: brand name, strength, dosage form, active ingredient, and physical package batch/expiry attributes.
    - Explicit uncertainty visibility: ambiguous matches display candidate variants and warning pills.
    - Prominent, high-contrast primary button: "Switch to 2D Result" (direct transition to S07).
    - Prominent secondary button: "Scan another package" (resets state and returns to live scan).
    - Unsupported / failure fallback card: if ARCore is not available, displays an explanatory card and one-tap switch to the 2D accessible result screen.
    - Camera contention avoided: ARCore and CameraX run on separate screens; neither holds the camera device when the other is active.
  - Screen S07 `MedicineResultScreen` updated with "View AR Camera Overlay" button providing seamless bidirectional navigation between 2D and AR views.
  - Navigation wired in `Screen.ArOverlay` and `MainActivity.kt`.
- Automated tests: Added 6 unit tests in `ArAvailabilityCheckerTest` covering AR availability mappings and session state defaults. All 47 unit tests pass in `.\gradlew.bat test`.
- Build verification: `.\gradlew.bat assembleDebug` succeeded cleanly (debug APK: 56,361,256 bytes).
- Phase 7 Acceptance Review & Hardware Camera Correction:
  - Audit revealed initial screen initialized ARCore session but lacked an OpenGL ES rendering pipeline to draw the camera hardware texture, leaving the background dark and tracking static.
  - Corrected by creating `ArCameraSurfaceView` and `ArCameraRenderer` (OpenGL ES 2.0 quad with external OES texture and `frame.transformCoordinates2d`), driving an active `session.update()` render loop on every frame.
  - Configured `setZOrderMediaOverlay(true)` and replaced root `Surface` with `Box` in `MainActivity.kt` to prevent Compose offscreen compositing layers from occluding the native camera SurfaceView.
  - Implemented dynamic tracking state extraction in `ArSessionManager.updateFromFrame(frame)`: extracts `TrackingState.TRACKING` ("AR Tracking Active") and reasons (`INSUFFICIENT_LIGHT` -> "Low lighting: improve illumination", `EXCESSIVE_MOTION`, etc.) dispatched safely via main-looper Handler.
  - Verified on physical Redmi Note 11 (Android 13 / API 33, ADB ID `79145af9`):
    - Physical screenshot `screen_ar_live_overlay2.png` captures the real desk, keyboard, and physical environment behind the semi-translucent Compose overlay with high-contrast text and green "AR Tracking Active" status pill.
    - Verified live tracking failure recovery: when phone was facing dark desk, tracking pill dynamically showed amber warning "Low lighting: improve illumination" (`screen_ar_live_camera.png`); upon lifting and pointing into room, tracking dynamically updated to "AR Tracking Active".
    - Verified bidirectional navigation with S07 2D Result screen ("Switch to 2D Result" / "View AR Camera Overlay").
    - Verified zero network permissions (`android.permission.INTERNET` and `ACCESS_NETWORK_STATE` strictly absent from merged manifest).

## Phase 8 — UI polish, integration, and edge cases

- [x] Apply the approved visual design consistently.
- [x] Add loading, empty, permission-denied, no-match, ambiguous-match, and error states.
- [x] Verify navigation and back behavior across the complete flow.
- [x] Check small-screen layout, text scaling, and landscape/rotation behavior as practical.
- [x] Remove dead code, debug-only UI, and unused dependencies.
- [x] Confirm the app remains offline-first.

**Phase verification**
- [x] All defined screens and transitions have been exercised.
- [x] No known blocking crash remains.
- [x] Layout and accessibility checks are recorded.

**Evidence / notes:**
- Complete end-to-end integration across all screens:
  - S01 Welcome -> S02 Permission/Readiness -> S03 Guidance -> S04 Live Camera Scan -> S06 OCR Review / S07 Medicine Result -> S05 AR Overlay / 2D Fallback -> Rescan.
- Lifecycle hardening implemented:
  - `PermissionScreen`: Attached `LifecycleEventObserver` listening for `ON_RESUME`. If user navigates to Android System Settings and grants camera permission, returning to the app immediately detects the grant and advances to `ScanGuidance` without requiring process restart.
  - `MedicineResultScreen`: Attached `LifecycleEventObserver` listening for `ON_PAUSE`. Halts active TTS speech (`ttsManager.stop()`) immediately when the app is backgrounded, minimized, or screen locked, preventing runaway audio leaks.
  - `ArOverlayScreen`: Attached `LifecycleEventObserver` listening for `ON_PAUSE`/`ON_RESUME`/`ON_DESTROY`. Halts `surfaceViewRef?.onPause()` and `arSessionManager.pause()` when paused; resumes only when active; releases native ARCore resources on disposal.
  - S07 AR Button: Dynamically checks `ArAvailabilityChecker.isSupported(context)` and appends `(2D Fallback)` hint if the device lacks ARCore support (e.g. Samsung Galaxy F13), providing clear feedback before tapping.
- Enhanced `TextNormaliser`:
  - `EXPIRY_REGEX` updated to support `EXP.DATE`, `USE BEFORE`, and varying spacing/punctuation.
  - `BATCH_REGEX` updated to support hyphens in batch codes (e.g. `LOT-9982`, `XF-201`).
- Automated regression test suite:
  - Implemented `app/src/test/java/com/armod/identifier/edgecases/Phase8EdgeCaseTest.kt` (17 test cases) covering:
    - Blank, whitespace-only, and noise/symbol inputs yielding `MatchOutcome.InsufficientText`.
    - Input below minimum 3-alphanumeric character threshold yielding `MatchOutcome.InsufficientText`.
    - Multi-strength generic active ingredient (e.g., Paracetamol IP) without verified strength yielding `MatchOutcome.AmbiguousMatch`, preventing arbitrary strength guessing.
    - Conflicting strength (e.g., OCR says 650mg against 500mg catalogue record) disqualifying candidate from high confidence match.
    - Uncatalogued medicine yielding `MatchOutcome.NoMatch` while preserving extracted batch and expiry.
    - Batch/expiry alone without medicine text yielding `MatchOutcome.NoMatch` without fabricating medicine identity.
    - Various expiry and batch formats safely parsed or returning null without crashing.
    - TTS content generation verifying brand, generic, phonetic strength, dosage form, batch/expiry, academic disclaimer presence, and absence of literal "null".
    - TTS graceful handling of missing optional fields (batch/expiry).
    - ARCore unsupported status, tracking failure reason mappings, and error state transitions.
  - All 64 unit tests pass in `.\gradlew.bat test`.
- Offline compliance verification:
  - Inspected merged manifest at `app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml`.
  - Confirmed `android.permission.INTERNET` and `android.permission.ACCESS_NETWORK_STATE` are strictly absent. Zero network traffic possible.
- Physical device verification on target device Samsung Galaxy F13 (`SM-E135F` / `m13dd`, Android 14 / API 34, ADB ID `RZ8WA00694L`):
  - Debug APK built and installed cleanly via ADB (`Performing Streamed Install -> Success`).
  - Screen S01 Welcome: Verified title, Academic Prototype Notice, and "Start scanning" button (`samsung_f13_p8_launch.png`).
  - Screen S03 Guidance: Verified framing instructions, "Begin scan", and "Back" buttons (`samsung_f13_p8_screen2.png`).
  - Screen S04 CameraX Live Scan: Verified hardware camera preview, blue framing reticle, "Ready to scan" badge, and capture button (`samsung_f13_p8_camera.png`).
  - Screen S07 Accessible Medicine Result:
    - Scanned real physical medicine package placed under camera: "Omez Omeprazole Capsules IP 20 mg" (`samsung_f13_p8_back_camera.png`).
    - Verified on-device OCR and matching engine: Identified "CONFIRMED CATALOGUE MATCH: Omez 20 mg Capsule" with active ingredient "Omeprazole", dosage form "Capsule", manufacturer "Dr. Reddy's Laboratories Ltd", purpose, directions, warnings, precautions, and side effects (`samsung_f13_p8_omez_result.png`).
    - Physical package batch/expiry attributes honestly reported as "Not detected on label" without fabrication (`samsung_f13_p8_scrolled.png`).
    - Voice guidance panel: Verified "Speak", "Stop", and "Repeat" buttons render with full labels, high contrast, and zero clipping (`samsung_f13_p8_omez_result.png`).
    - TTS execution: Verified connection to Samsung's native on-device TTS engine (`com.samsung.SMT`) via logcat (`TextToSpeech: Connected to TTS engine`).
  - AR 2D Fallback:
    - Tapped "View AR Camera Overlay (2D Fallback)" button (`samsung_f13_p8_buttons.png`).
    - Screen S05 verified: Gracefully detected unsupported ARCore hardware on Exynos 850 and displayed "2D Camera Fallback Active" card with one-tap "Switch to 2D Result" button (`samsung_f13_p8_ar_fallback.png`).
    - Tapped "Switch to 2D Result": Returned immediately to 2D monograph without crash or session leak (`samsung_f13_p8_back_to_2d.png`).
  - Back navigation and re-scan: Tapped back arrow in top bar; returned to live camera feed ready for next scan (`samsung_f13_p8_back_camera.png`).

## Feature Extension — Accessible Touch Gesture Control Layer with Tremor & Panning Filtering

- [x] Expand `SpeechContentGenerator` and `SpokenResult` to generate modular spoken text for `basicNameAndDosage` (single tap) and `warningsAndSideEffects` (double tap).
- [x] Implement pure JVM `AccessibleGestureStateMachine` with 20px movement threshold, 300ms double-tap window, and 500ms long-press hold duration.
- [x] Implement custom Compose pointer input modifier `Modifier.accessibleTouchGestures(...)`.
- [x] Create accessible, high-contrast `AccessibleGesturePad` component with tactile haptic feedback and dynamic gesture/cancellation status pill.
- [x] Wire `AccessibleGesturePad` into S07 `MedicineResultScreen` within `VoiceGuidancePanel`, preserving redundant Stop/Repeat buttons.
- [x] Enable touch gestures on S04 `LiveScanScreen` with "Capture & Play" flow: Single tap captures & plays Name/Dosage, double tap captures & plays Warnings/Side Effects, long press captures & plays Complete Monograph, and camera panning/tremors cancel the action.
- [x] Enable touch gestures on S05 `ArOverlayScreen` with on-device TextToSpeech playback over live AR camera feed and camera panning cancellation.
- [x] Route `SpeechTarget` across `MainActivity` navigation.
- [x] Pin JBR 21 via `org.gradle.java.home` in `gradle.properties`.
- [x] Add automated unit test suite `AccessibleGestureStateMachineTest` and expand `SpeechContentGeneratorTest`.

**Feature verification:**
- [x] `./gradlew.bat testDebugUnitTest`: BUILD SUCCESSFUL in 8s (72 unit tests passed across 9 suites; 0 failures).
- [x] `./gradlew.bat assembleDebug`: BUILD SUCCESSFUL in 8s (APK verified at `app/build/outputs/apk/debug/app-debug.apk`, 56,563,743 bytes).
- [x] Movement filtering verified: Movements $> 20$px (accidental drags, camera panning, or tremors) cancel gesture actions.

## Feature Extension — Unified Single-Screen Camera Workflow with Instant 3D AR Overlay

- [x] Implement single-screen gesture handling: all touch gestures (Single Tap, Double Tap, Long Press) operate directly on the camera screen without unwanted page transitions.
- [x] Single Tap: Captures current frame, identifies medicine on-device, speaks Basic Name and Dosage, and immediately spawns the 3D AR Card Overlay right on the live camera view.
- [x] Double Tap: In-place audio playback for Warnings and Side Effects without screen transition.
- [x] Long Press: In-place audio playback for Complete Profile Summary without screen transition.
- [x] Movement Filter: Ignores touch movements $> 20$px to prevent accidental triggers during camera panning or tremors.
- [x] Instant AR Card Overlay: Spawns directly on the main camera screen anchored to the package/surface without forcing user away from live camera view.
- [x] Dedicated 'READ INFORMATION' Button: Prominent button on camera overlay screen as the ONLY trigger opening full-screen structured text page (`Screen.Result`).
- [x] Reset State: Prominent 'Scan Again' / Reset control on camera screen clears current AR card, resets tracking visuals, and returns camera view cleanly back to scanning state.
- [x] Adjust UI controllers (`LiveScanScreen.kt`, `ArOverlayScreen.kt`), event listeners (`accessibleTouchGestures`), and screen navigation states (`MainActivity.kt`).
- [x] Create comprehensive automated test suite `UnifiedCameraWorkflowTest.kt` verifying all 5 behavioral rules.

**Feature verification:**
- [x] `.\gradlew.bat test`: BUILD SUCCESSFUL in 7s (79 unit tests passed across 10 test suites; 0 failures).
- [x] `.\gradlew.bat assembleDebug`: BUILD SUCCESSFUL in 12s (APK verified at `app/build/outputs/apk/debug/app-debug.apk`).
- [x] All 5 exact behavioral rules verified in code and automated unit tests.
- [x] Update Screen S07 `MedicineResultScreen` to act purely as a structured text review hub:
  - Removed `AccessibleGesturePad` completely; touch gestures remain exclusively on camera screen.
  - Added simple audio playback controls (Speak, Stop, Repeat buttons) with dynamic status indicator.
  - Structured text sections: Brand & Generic Name, Dosage, Expiry Date & Batch, Clinical Usage (Purpose & Directions), Warnings, Side Effects, and Academic Disclaimer.
  - Added in-place `RawOcrTextSection` showing extracted raw text preview and detailed breakdown button.
  - Preserved 'Scan Another Package' and 'View AR Camera Overlay' action buttons.

## Phase 9 — End-to-end testing and prototype handoff

- [ ] Run unit tests and any available instrumentation/UI tests.
- [ ] Perform a manual end-to-end test: launch → camera permission → capture → OCR → local match → text result → speech.
- [ ] Test denied permission, no text, poor image, unknown medicine, ambiguous match, unreadable expiry, and TTS unavailable.
- [ ] Test with network disabled.
- [ ] Build a debug APK and confirm the output artifact exists.
- [ ] Document build/run instructions, known limitations, and the exact tested device/emulator.
- [ ] Ensure no production-readiness or clinical-validation claims are made.
- [ ] Update `MEMORY.md`, `DECISIONS.md`, and `TASKS.md` with final verified state.

**Phase verification**
- [ ] Required end-to-end flow passes on the available target.
- [ ] Build and test results are recorded.
- [ ] Remaining limitations are explicit.
- [ ] Prototype handoff instructions are reproducible.

**Evidence / notes:** _Anti-Gravity fills in after execution._

## Current blocker / next action

**Next phase to execute:** Phase 9 — End-to-end testing and prototype handoff (STOPPED — waiting for explicit user approval before beginning Phase 9).  
**Current implementation status:** Phase 8, Accessible Touch Gesture Layer, and Unified Single-Screen Camera Workflow complete. All 79 unit tests pass. Debug APK built successfully.  
**Do not pre-check any implementation task.**
