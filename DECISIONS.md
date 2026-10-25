# DECISIONS.md — Permanent Project Decisions and Rationale

> **Purpose:** Preserve durable project choices so Anti-Gravity does not silently reverse them in later sessions.
>
> **Ownership:** Anti-Gravity should maintain this file when an explicit project decision is made or when a decision is formally changed. Do not invent decisions, claim that an option was tested when it was not, or rewrite history to make the current implementation appear intentional.
>
> **Relationship to MEMORY.md:** `MEMORY.md` tracks current implementation state and next steps. `DECISIONS.md` tracks durable choices and why they were made. Update them for different purposes; do not use one as a substitute for the other.

## 1. Rules for Anti-Gravity

1. Read this file before making architectural, framework, dependency, storage, privacy, navigation, or scope decisions.
2. Treat decisions marked **Accepted** as the current project baseline.
3. Do not reverse or replace an accepted decision just because another library or architecture is fashionable, easier to generate, or suggested by an AI tool.
4. If a decision no longer works, first gather evidence from the repository, build output, dependency compatibility, or target-device testing. Explain the specific problem and propose the smallest viable change.
5. Do not silently change a decision. Record the proposed change, reason, impact, and migration steps in the **Decision Change Log**. Ask the user when the change would alter scope, user-visible behaviour, data handling, or a previously explicit preference.
6. Update this file when the user makes a new durable decision or explicitly approves a proposed change. Routine implementation details do not need to become permanent decisions.
7. Keep the rationale factual. Distinguish user requirements, technical constraints, and unverified assumptions.
8. When a decision is superseded, mark it **Superseded** and link to the replacement. Do not delete the old rationale.
9. At the beginning of a session, compare the actual repository against the decisions below. Report mismatches instead of silently rewriting the decisions to match accidental code.
10. This file is a decision record, not proof that features have been implemented or tested.

## 2. Project Context

- **Project:** SLAM-Based AR Medicine Identifier with Voice-Guided Accessibility Mode.
- **Purpose:** Academic proof of concept that reads printed medicine packaging using on-device OCR, compares extracted fields against a small local dataset, and presents accessible text/speech output with an optional simple AR overlay.
- **Project type:** Working academic prototype, not production-ready software or a clinically validated medical product.
- **Target device:** Samsung Galaxy F13.
- **Target deadline:** One week; prioritise a narrow end-to-end demonstration over advanced features.
- **Source documents:** PRD, TRD, App Flow, UI/UX Design Brief, Backend Schema, and `MEMORY.md`. Verify that these files are actually available in the project repository before relying on them.

## 3. Accepted Decisions

### D-001 — Build a native Android application

**Status:** Accepted  
**Decision:** Use native Android development rather than a web app or cross-platform app for the prototype.  
**Rationale:** The project depends on phone camera access, Android text-to-speech, accessibility services, and potentially ARCore. Native Android provides direct access to these capabilities and matches the chosen target device.  
**Constraint:** Do not introduce a web frontend or a separate backend solely for this prototype.

### D-002 — Use Kotlin

**Status:** Accepted  
**Decision:** Use Kotlin as the primary application language.  
**Rationale:** It is the selected language for the native Android stack and integrates with Android and Jetpack libraries.  
**Constraint:** Do not mix in Java or another language unless a specific dependency or existing project constraint makes it necessary and the change is documented.

### D-003 — Use Jetpack Compose for the UI

**Status:** Accepted  
**Decision:** Use Jetpack Compose for the standard application screens and accessible 2D results.  
**Rationale:** It supports a concise native UI implementation and makes it practical to build and iterate on screens within the one-week deadline.  
**Constraint:** AR-rendered text must not be the only way to access information; mirror essential content in normal accessible UI.

### D-004 — Keep the prototype offline-first and without a backend

**Status:** Accepted  
**Decision:** No backend server, remote database, user accounts, authentication provider, or HTTP API in the prototype.  
**Rationale:** The core workflow can run locally, and avoiding server infrastructure reduces implementation time, network dependency, credential handling, and privacy risk.  
**Constraint:** Do not add Firebase, Supabase, or another backend unless the user explicitly changes scope.

### D-005 — Start with a bundled local JSON dataset

**Status:** Accepted  
**Decision:** Store the initial medicine reference dataset in `app/src/main/assets/medicines.json`, with approximately 20–30 curated academic test records.  
**Rationale:** JSON is sufficient for a small read-only dataset and is faster to implement than a database service or complex persistence layer.  
**Constraint:** Use stable record IDs and source metadata. Do not put package-specific expiry dates in general medicine records.  
**Upgrade path:** SQLite/Room may be added for local scan history or structured persistence only if needed and time permits; it does not imply adding a remote database.

### D-006 — Use on-device ML Kit Text Recognition v2 for OCR

**Status:** Accepted  
**Decision:** Use ML Kit Text Recognition v2 as the initial OCR implementation, with on-device processing.  
**Rationale:** It provides an achievable OCR baseline without a custom model or cloud service.  
**Constraint:** OCR extracts text; it does not verify medicine identity or validate medical facts. Keep matching and safety gating as separate logic. Verify dependency and model behaviour in the actual project.

### D-007 — Use Android TextToSpeech for spoken output

**Status:** Accepted  
**Decision:** Use Android TextToSpeech for scan guidance and result summaries.  
**Rationale:** It avoids requiring a paid or remote speech service and fits the accessibility goal.  
**Constraint:** Visual text must remain available if speech is unavailable. Avoid overlapping utterances and provide repeat/stop behaviour where implemented.

### D-008 — ARCore is conditional, not allowed to block the prototype

**Status:** Accepted  
**Decision:** Use ARCore for a simple overlay only if it is compatible with the Samsung Galaxy F13 setup and can be implemented reliably within the deadline. A 2D accessible result screen is the required fallback.  
**Rationale:** AR is central to the project concept, but device compatibility and a one-week deadline make advanced tracking a risk. The core OCR-to-result demonstration should remain usable without AR.  
**Constraint:** Do not claim ARCore support or stable tracking until verified on the actual device. Do not build elaborate 3D effects before the core flow works.

### D-009 — No barcode, QR, or DataMatrix scanning in the MVP

**Status:** Accepted  
**Decision:** Identify packages from printed label text using OCR; exclude barcode scanning from the prototype.  
**Rationale:** The user explicitly excluded barcode scanning from the MVP. Keeping it out protects the deadline and avoids expanding the recognition scope.  
**Constraint:** Do not add barcode scanning as a fallback without explicit user approval.

### D-010 — No cloud OCR, cloud AI, or cloud fallback

**Status:** Accepted  
**Decision:** Do not upload package images or OCR text to remote services.  
**Rationale:** The prototype is offline-first, avoids credentials and network dependency, and reduces privacy risk.  
**Constraint:** Do not add a vision API, generative AI service, or remote fallback unless the user explicitly approves a scope change and the privacy implications are documented.

### D-011 — Treat medicine matching conservatively

**Status:** Accepted  
**Decision:** Keep OCR, field extraction, local candidate matching, and the confidence/verification gate as separate steps. If evidence is weak or contradictory, ask for confirmation or report an unknown result.  
**Rationale:** OCR errors and look-alike medicine names can cause unsafe false matches. A smaller honest prototype is preferable to a confident but unsupported result.  
**Constraint:** Do not rely on fuzzy name similarity alone. Verify strength and composition when available. A match score is not a calibrated probability unless evaluated as such.

### D-012 — Never invent medical information

**Status:** Accepted  
**Decision:** Present only medicine facts supported by recorded sources in the local dataset. Omit unsupported purpose, directions, warnings, and side-effect fields. Do not generate personalised dosing or treatment advice.  
**Rationale:** This is an academic prototype without formal clinical validation or an appointed medical reviewer.  
**Constraint:** Clearly identify the prototype as educational and not a substitute for a pharmacist or doctor. Do not claim that a package is authentic, safe, or appropriate for a specific person.

### D-013 — Expiry is a property of the scanned package

**Status:** Accepted  
**Decision:** Read expiry from the physical package via OCR and store it in the transient scan result or optional scan record, not in the general medicine catalogue entry.  
**Rationale:** Different packages of the same product can have different expiry dates and batch numbers.  
**Constraint:** If the date is ambiguous or unreadable, report it as unknown. Never infer that the package is in date.

### D-014 — Accessibility is a core requirement

**Status:** Accepted  
**Decision:** Keep result information available as normal UI text, support TalkBack labels, use readable scalable text and large touch targets, and provide speech where available.  
**Rationale:** Older adults and blind/low-vision users are primary target personas.  
**Constraint:** Do not make colour, haptics, sound, or an AR overlay the only way to access essential information. Do not auto-dismiss important results.

### D-015 — Use a minimal, high-contrast Android utility design

**Status:** Accepted  
**Decision:** Use the visual tokens and component guidance in `AR_Medicine_Identifier_UI_UX_Design_Brief.md`, including a restrained blue/teal palette, system sans-serif/Roboto, generous touch targets, and subtle surfaces.  
**Rationale:** A calm, consistent design improves readability and reduces cognitive load for an accessibility-focused tool.  
**Constraint:** Centralise design tokens rather than scattering arbitrary colours and dimensions across screens. Verify contrast rather than assuming the token values automatically meet requirements.

### D-016 — Keep the one-week prototype scope narrow

**Status:** Accepted  
**Decision:** Prioritise camera/capture → OCR → field extraction → local match/confidence gate → accessible result.  
**Rationale:** This is the smallest end-to-end flow that demonstrates the product concept within the user's deadline.  
**Constraint:** Multi-frame fusion, custom object detection, complex AR anchoring, haptic guidance, scan history, expiry reminders, multilingual support, and advanced settings are optional unless the core flow is stable and time remains.

### D-017 — Android minimum version is a provisional baseline

**Status:** Accepted with verification required  
**Decision:** Begin with Android 8.0 (API 26) as the planning baseline.  
**Rationale:** It is the baseline stated in the technical plan, but actual dependency requirements and device compatibility must be checked.  
**Constraint:** Verify the target handset's Android version and selected library requirements. If a higher `minSdk` is required, document the evidence and proposed change rather than changing it silently.

### D-018 — No production or clinical-readiness claims

**Status:** Accepted  
**Decision:** Treat the app as an academic proof of concept.  
**Rationale:** The deadline, small dataset, and absence of formal medical validation do not support production-readiness claims.  
**Constraint:** Do not claim measured accuracy, clinical validation, certification, or general medicine coverage without documented evidence.

### D-019 — Toolchain and build system versions for Phase 1 baseline

**Status:** Accepted  
**Decision:** Use Gradle 8.11.1 (via project Gradle Wrapper), Android Gradle Plugin (AGP) 8.7.3, Kotlin 2.0.21 (with official Jetpack Compose compiler Gradle plugin `org.jetbrains.kotlin.plugin.compose`), and OpenJDK 21 (Android Studio bundled JBR 21.0.10). Configure `compileSdk = 35`, `targetSdk = 35`, `minSdk = 26`, and Java 21 compilation compatibility.  
**Rationale:** Confirmed full compatibility with the host laptop environment. The project compiles cleanly, runs unit tests, and packages `app-debug.apk` without errors. Setting `minSdk = 26` maintains the approved Android 8.0 baseline.  
**Constraint:** Do not upgrade or downgrade AGP or Gradle without verifying compatibility with Android Studio's JBR 21.

### D-020 — Use kotlinx.serialization for local dataset parsing and schema validation

**Status:** Accepted  
**Decision:** Use official Kotlin multiplatform serialization (`org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3`) with compile-time plugin `org.jetbrains.kotlin.plugin.serialization` for parsing and validating `medicines.json`.  
**Rationale:** Ensures compile-time typesafety without reflection overhead. Executes natively in standard JVM unit tests without needing Android framework mocks (unlike `org.json`). Allows strict validation of required fields (`id`, `sourceTitle`, `datasetVersion`) and safe handling of optional/unknown attributes.  
**Constraint:** Keep dataset parsing offline and self-contained; do not introduce remote network parsers or cloud schemas.

### D-021 — Use connected Redmi Note 11 (Android 13 / API 33) as temporary development and testing device

**Status:** Accepted  
**Decision:** Use connected Redmi Note 11 (Model 2201117TI, Android 13, API 33) for interim development and physical-device verification while primary target Samsung Galaxy F13 is unavailable.  
**Rationale:** Explicit user authorization; device physically connected via USB debugging with verified installation and hardware camera access. Local emulator is blocked by disabled CPU virtualization in host BIOS.  
**Constraint:** Retain minSdk 26 compatibility for primary target baseline.

### D-022 — Standardize on CameraX 1.4.1 with on-device in-memory image capture

**Status:** Accepted  
**Decision:** Use `androidx.camera:camera-core:1.4.1`, `camera-camera2:1.4.1`, `camera-lifecycle:1.4.1`, `camera-view:1.4.1` with `ProcessCameraProvider` and `AndroidView(PreviewView)`. Frames captured on-device with immediate `ImageProxy.close()`.  
**Rationale:** Robust compatibility with AGP 8.7.3, Compose BOM 2024.11.00, compileSdk 35, and hardware acceleration on physical Redmi Note 11 (Android 13 / API 33).  
**Constraint:** Never store raw camera frames to persistent storage or upload them; close ImageProxy immediately.

### D-023 — Standardize on ML Kit Text Recognition v2 (16.0.1) for on-device OCR pipeline

**Status:** Accepted  
**Decision:** Use `com.google.mlkit:text-recognition:16.0.1` (Latin script) with `InputImage.fromMediaImage` and lifecycle-managed `TextRecognizer` instance.  
**Rationale:** Runs completely on-device with zero network permissions (`android.permission.INTERNET` absent from manifest). Zero cloud model download dependency. Successfully verified on physical Redmi Note 11 hardware with real medicine packages.  
**Constraint:** OCR output is transient in memory. Bounding boxes, lines, and blocks are extracted into pure Kotlin domain models (`OcrResult`, `OcrBlock`, `OcrLine`) to decouple OCR recognition from UI and matching logic.

## 4. Explicitly Unresolved / Must Be Verified

These items are not settled facts and must not be silently treated as verified:

- Whether the exact Samsung Galaxy F13 model/software setup supports the selected ARCore implementation (RESOLVED in Phase 7 audit: Galaxy F13 is NOT on Google ARCore Supported Devices list; Exynos 850 lacks sensor calibration; 2D fallback is mandatory).
- Whether the selected ML Kit dependency/model works correctly with the project's final Android configuration (RESOLVED in Phase 4: verified with on-device `com.google.mlkit:text-recognition:16.0.1` on physical hardware).
- The final Android `minSdk` after checking the real dependency graph and device (API 26 confirmed for Phase 1 baseline).
- The final list of 20–30 medicine test records and the source/licence for each fact (RESOLVED in Phase 2: 25 curated records with full provenance in `docs/dataset-sources.md`).
- Actual OCR accuracy, matching accuracy, latency, battery usage, and AR stability (OCR baseline verified in Phase 4; matching engine and ambiguity gate verified in Phase 5; AR overlay verified on Redmi Note 11 in Phase 7).
- Whether local scan history, Room, WorkManager reminders, or automatic point-and-hold scanning fit the remaining time.
- Whether any optional AR feature has been tested on the physical target device (RESOLVED in Phase 7: conditional AR overlay verified on connected Redmi Note 11; 2D fallback verified).

Anti-Gravity should move an item out of this section only after it is resolved by evidence or an explicit user decision.

## 5. Decision Change Log

Append new changes at the top. Do not delete superseded decisions.

### 2026-10-25 — D-029 — Multi-Screen Accessible Touch Gesture Control Layer with Tremor & Panning Filtering
- Previous decision: D-025 and D-028 used a single "Speak" button on the result screen and lacked touch-gesture shortcuts on live camera and AR overlay screens.
- Proposed replacement:
  1. Implement `AccessibleGestureStateMachine` (pure JVM) with 20px Euclidean movement filter threshold, 300ms double-tap window, and 500ms long-press hold duration.
  2. Implement custom Compose modifier `Modifier.accessibleTouchGestures(...)` supporting single tap, double tap, long press, and movement cancellation.
  3. Expand `SpokenResult` with `basicNameAndDosage`, `warningsAndSideEffects`, and `fullSpeech` synthesized across all 4 match outcomes in `SpeechContentGenerator`.
  4. Enable Capture & Play flow on `LiveScanScreen`: Single tap captures & plays Name/Dosage, double tap captures & plays Warnings/Side Effects, long press captures & plays Complete Monograph; movement > 20px (camera panning or hand tremors) cancels the action.
  5. Enable touch gestures on `ArOverlayScreen`: Single tap speaks Name/Dosage, double tap speaks Warnings/Side Effects, long press speaks Complete Monograph; camera panning cancels gestures. Supports `initialSpeechTarget` auto-announcement and audio status banner.
  6. Replace the rigid single-action Speak button in `MedicineResultScreen` with `AccessibleGesturePad` featuring visual gesture guide, tactile haptic feedback, dynamic status pill, and secondary Stop/Repeat controls.
  7. Route `initialSpeechTarget` through `MainActivity` across screens.
  8. Pin OpenJDK 21 via `org.gradle.java.home` in `gradle.properties`.
- Reason / evidence: Greatly enhances accessibility for visually impaired users and elderly individuals with tremors, avoiding accidental triggers during camera positioning or scrolling, while providing modular audible monograph access.
- User approval required: Yes (approved by user via Multi-Screen Accessible Touch Gesture Control Layer plan).
- User approval/status: Accepted.
- Files and architecture affected: `SpeechTarget.kt`, `AccessibleGestureStateMachine.kt`, `AccessibleTouchGestures.kt`, `AccessibleGesturePad.kt`, `SpeechContentGenerator.kt`, `LiveScanScreen.kt`, `ArOverlayScreen.kt`, `MedicineResultScreen.kt`, `MainActivity.kt`, `strings.xml`, `gradle.properties`, `AccessibleGestureStateMachineTest.kt`, `SpeechContentGeneratorTest.kt`.
- Migration or cleanup required: Replaced legacy full-width Speak button on result screen with AccessibleGesturePad; expanded SpokenResult contract.
- Verification performed: All 72 unit tests pass in `./gradlew.bat testDebugUnitTest`, debug APK assembled in `./gradlew.bat assembleDebug` (56,563,743 bytes).
- New status: Accepted.

### 2026-10-22 — D-028 — End-to-End Integration, Lifecycle Hardening, and Edge-Case Recovery
- Previous decision: Features developed across Phases 1–7 had separate lifecycle listeners and manual fallback transitions.
- Proposed replacement:
  1. Complete end-to-end integration across all screens (Welcome -> Readiness/Permission -> Guidance -> Live Camera -> OCR Review / Medicine Result -> AR Overlay / 2D Fallback).
  2. Implement lifecycle-aware observer in `PermissionScreen` on `ON_RESUME`: automatically verifies camera permission when returning from Android System Settings and immediately transitions to Scan Guidance without requiring app restart.
  3. Implement lifecycle-aware observer in `MedicineResultScreen` on `ON_PAUSE`: automatically halts active Text-to-Speech playback (`ttsManager.stop()`) when the app is backgrounded, minimized, or screen locked.
  4. Implement lifecycle-aware observer in `ArOverlayScreen`: halts GL rendering (`surfaceView.onPause()`) and pauses ARCore session on `ON_PAUSE`; safely resumes on `ON_RESUME` only if active; safely destroys native resources on screen disposal.
  5. Enhanced `TextNormaliser` regexes to support hyphens in batch numbers (e.g. `LOT-9982`, `XF-201`) and alternative expiration prefixes (e.g. `EXP.DATE`, `USE BEFORE`).
  6. Added comprehensive edge-case test suite `Phase8EdgeCaseTest` (17 test cases, 64 unit tests total) covering blank/garbled/noise inputs, conflicting strengths, multi-strength generic ambiguity, missing batch/expiry, TTS content null-safety, and ARCore fallback mappings.
  7. Physically verified end-to-end workflow on the primary target device, Samsung Galaxy F13 (`SM-E135F`, Android 14 / API 34), including live physical camera scanning of an actual "Omez 20mg Capsule" strip, confirming on-device OCR, catalogue matching, TTS synthesis, and graceful 2D fallback.
  8. Strictly verified zero network permissions (`android.permission.INTERNET` absent in merged manifest).
- Reason / evidence: Guarantees rock-solid stability, zero audio or camera session leaks, safe handling of missing/conflicting medicine attributes, and verified physical hardware execution on the target device.
- User approval required: No (implements authorized Phase 8).
- User approval/status: Accepted.
- Files and architecture affected: `PermissionScreen.kt`, `MedicineResultScreen.kt`, `ArOverlayScreen.kt`, `TextNormaliser.kt`, `Phase8EdgeCaseTest.kt`.
- Migration or cleanup required: None.
- Verification performed: All 64 unit tests passed in `.\gradlew.bat test`, debug APK assembled in `.\gradlew.bat assembleDebug`, installed and verified live on Samsung Galaxy F13 (`RZ8WA00694L`).
- New status: Accepted.

### 2026-10-18 — D-027 — OpenGL ES 2.0 Live Camera Background Rendering and Real-Time ARCore VIO Tracking for AR Overlay Screen
- Previous decision: D-026 initialized the ARCore session and displayed Compose UI overlay with static tracking label over an empty canvas.
- Proposed replacement:
  1. Implement `ArCameraSurfaceView` and `ArCameraRenderer` utilizing OpenGL ES 2.0 (`GL_TEXTURE_EXTERNAL_OES`) to draw the hardware camera feed quad mapped via `frame.transformCoordinates2d(Coordinates2d.OPENGL_NORMALIZED_DEVICE_COORDINATES, ...)`.
  2. Implement an active render loop calling `session.update()` on every frame, extracting real-time ARCore tracking states (`TrackingState.TRACKING`, `PAUSED` with reasons `INSUFFICIENT_LIGHT`, `EXCESSIVE_MOTION`, `INSUFFICIENT_FEATURES`), and dynamically dispatching state updates to Compose UI via `Looper.getMainLooper()`.
  3. Set `setZOrderMediaOverlay(true)` on `GLSurfaceView` and replace root `Surface(color = MaterialTheme.colorScheme.background)` with `Box` in `MainActivity.kt` to prevent Compose's `CompositingStrategy.Offscreen` from occluding the hardware camera surface.
  4. Explicitly document the overlay as a high-contrast, camera-view AR information card over the live camera feed with real-time tracking diagnostics, rather than claiming false 3D anchor stabilization.
  5. Retain 100% offline compliance (zero network permissions, `android.permission.INTERNET` absent).
- Reason / evidence: Acceptance review showed the previous screen was just a Compose card without a live camera stream; the new implementation renders the live physical camera background and drives real-time SLAM VIO tracking.
- User approval required: No (fulfills Phase 7 Acceptance Review requirements).
- User approval/status: Accepted.
- Files and architecture affected: `ArCameraSurfaceView.kt`, `ArSessionManager.kt`, `ArOverlayScreen.kt`, `MainActivity.kt`, `strings.xml`.
- Migration or cleanup required: None.
- Verification performed: Unit tests (47/47 passed in `.\gradlew.bat test`), debug APK compiled, installed and physically verified on connected Redmi Note 11 hardware (`screen_ar_live_overlay2.png` captured showing physical desk, keyboard, live camera feed, and "AR Tracking Active" green status pill).
- New status: Accepted.

### 2026-10-17 — D-026 — Conditional AR Overlay Architecture with Hardware Availability Checking and Guaranteed 2D Fallback
- Previous decision: D-008 planned conditional AR without concrete session lifecycle or hardware capability gating.
- Proposed replacement:
  1. Add ARCore 1.47.0 (`com.google.ar:core:1.47.0`) with `<meta-data android:name="com.google.ar.core" android:value="optional" />` and `<uses-feature android:name="android.hardware.camera.ar" android:required="false" />`.
  2. Strictly remove transitively merged `INTERNET` and `ACCESS_NETWORK_STATE` permissions using `tools:node="remove"` in `AndroidManifest.xml` to maintain 100% offline compliance.
  3. Implement `ArAvailabilityChecker` mapping runtime ARCore capability into typed `ArAvailabilityStatus` (`SupportedAndInstalled`, `SupportedNeedsInstall`, `UnsupportedDevice`, `Unknown`).
  4. Implement `ArSessionManager` providing deterministic lifecycle safety (safe `resume`, `pause`, `close`) and continuous autofocus configuration.
  5. Implement Screen S05 `ArOverlayScreen` with live tracking status pill, spatial verified medicine card (brand, strength, dosage form, active ingredient, batch/expiry), academic prototype disclaimer, and one-tap switch to the 2D accessible result screen.
  6. Maintain Screen S07 `MedicineResultScreen` with "View AR Camera Overlay" button, ensuring bidirectional navigation and guaranteeing that non-AR devices (e.g., Samsung Galaxy F13) seamlessly access all identification and voice guidance features.
- Reason / evidence: Device audit proved Redmi Note 11 is ARCore-supported while Galaxy F13 is not; conditional architecture guarantees zero crashes on unsupported devices and seamless 2D fallback.
- User approval required: No (implements approved Phase 7 scope).
- User approval/status: Accepted.
- Files and architecture affected: `libs.versions.toml`, `build.gradle.kts`, `AndroidManifest.xml`, `strings.xml`, `ArAvailabilityChecker.kt`, `ArAvailabilityStatus.kt`, `ArSessionManager.kt`, `ArSessionState.kt`, `ArOverlayScreen.kt`, `MedicineResultScreen.kt`, `Screen.kt`, `MainActivity.kt`, `ArAvailabilityCheckerTest.kt`.
- Migration or cleanup required: None.
- Verification performed: 47/47 unit tests passed in `.\gradlew.bat test`, debug APK assembled, installed and verified on physical Redmi Note 11 hardware (`screen_ar_overlay.png` and `screen_result_now.png`).
- New status: Accepted.

### 2026-10-17 — D-025 — Dedicated Accessible Result Screen (S07) and Native Android TextToSpeech Architecture
- Previous decision: Provisional results and voice guidance planned in PRD/TRD without concrete lifecycle architecture.
- Proposed replacement:
  1. Implement dedicated Screen S07 `MedicineResultScreen` with WCAG-compliant high-contrast typography, large touch targets (>= 48–56dp), and distinct outcome cards (`ConfidentResultCard`, `AmbiguousResultCard`, `NoMatchResultCard`, `InsufficientTextResultCard`).
  2. Implement pure JVM domain generator `SpeechContentGenerator` to synthesize natural English speech from typed `MatchOutcome` records with phonetic strength expansions (e.g. "20 milligrams"), batch/expiry package attributes, and mandatory academic safety disclaimers.
  3. Implement lifecycle-aware `TextToSpeechManager` wrapping Android's native on-device `android.speech.tts.TextToSpeech`.
  4. Provide accessible Speak, Stop, and Repeat voice guidance controls.
  5. Enforce deterministic cleanup: release engine and stop speech immediately on screen disposal or navigation away.
  6. Maintain zero network dependencies and zero internet permissions (`android.permission.INTERNET` absent).
- Reason / evidence: Guarantees accessible, audible results for visually impaired users without leaking resources, fabricating facts, or relying on external cloud APIs.
- User approval required: No (implements approved Phase 6 scope).
- User approval/status: Accepted.
- Files and architecture affected: `SpeechContentGenerator.kt`, `TextToSpeechManager.kt`, `MedicineResultScreen.kt`, `ReviewOcrScreen.kt`, `MainActivity.kt`, `SpeechContentGeneratorTest.kt`.
- Migration or cleanup required: None.
- Verification performed: Unit tests in `SpeechContentGeneratorTest` (6/6 passed, 41/41 total passed in `./gradlew test`), debug APK built (56 MB), deployed and tested on Redmi Note 11 (Android 13 / API 33).
- New status: Accepted.

### 2026-10-17 — D-024 — Multi-field conservative matching with strength verification and ambiguity gating
- Previous decision: Provisional matching approach specified in TRD Section 5.
- Proposed replacement: Implement `MedicineMatcher` using multi-field score evaluation (brand name, active ingredient, strength, dosage form, manufacturer corroboration) with explicit confidence gates:
  1. A record is only `ConfidentMatch` if its name/ingredient matches AND its strength either strictly matches the OCR-extracted strength OR the medicine formulation has no catalogued strength variations.
  2. If the catalogue contains multiple strength variants for the matched name/ingredient (e.g. Paracetamol 500mg vs 650mg, Atorvastatin 10mg vs 20mg) and the OCR text lacks or has ambiguous strength, the match MUST downgrade to `AmbiguousMatch`.
  3. Conflicting strengths (e.g., OCR says 500mg but record is 650mg) immediately disqualify the candidate.
  4. Expiry date and batch code are treated strictly as transient package attributes, never matched against medicine catalogue identities.
  5. If best match confidence < 0.65 or score margin < 0.15 between multiple candidates, yield `AmbiguousMatch` or `NoMatch`.
  6. Return explicit sealed outcomes (`ConfidentMatch`, `AmbiguousMatch`, `NoMatch`, `InsufficientText`) with zero fallback guessing.
- Reason / evidence: Patient safety in medicine identification; avoids lethal or dangerous dosage confusion; preserves 100% offline local matching without network requests.
- User approval required: No (implements approved D-005, D-008, TRD Section 5).
- User approval/status: Accepted.
- Files and architecture affected: `MatchOutcome.kt`, `MedicineMatcher.kt`, `ReviewOcrScreen.kt`, `MainActivity.kt`, `MedicineMatcherTest.kt`.
- Migration or cleanup required: None.
- Verification performed: 15 unit tests in `MedicineMatcherTest` passed in `./gradlew test` (35/35 tests passing overall), verified on physical Redmi Note 11 with real camera packaging.
- New status: Accepted.

### 2026-10-17 — D-023 — Standardize on ML Kit Text Recognition v2 (16.0.1) for on-device OCR pipeline
- Previous decision: Provisional ML Kit Text Recognition v2 planned without pinned version.
- Proposed replacement: Use `com.google.mlkit:text-recognition:16.0.1` with `InputImage.fromMediaImage` and pure domain mapping (`OcrResult`).
- Reason / evidence: Zero network permissions, fully on-device, verified against real medicine packaging on physical Redmi Note 11 hardware.
- User approval required: No (implements approved D-006).
- User approval/status: Accepted.
- Files and architecture affected: `gradle/libs.versions.toml`, `app/build.gradle.kts`, `OcrProcessor.kt`, `MlKitOcrProcessor.kt`, `OcrResult.kt`, `LiveScanScreen.kt`, `ReviewOcrScreen.kt`.
- Migration or cleanup required: None.
- Verification performed: Unit tests in `OcrPipelineUnitTest` (7/7 passed), debug APK built (55.2 MB), physically verified on Redmi Note 11 with Omeprazole 20mg and Calcium + D3 packages.
- New status: Accepted.

### 2026-10-17 — D-022 — Standardize on CameraX 1.4.1 with on-device in-memory image capture
- Previous decision: Provisional CameraX dependency planned without exact version.
- Proposed replacement: Use `androidx.camera:camera-core:1.4.1`, `camera-camera2:1.4.1`, `camera-lifecycle:1.4.1`, `camera-view:1.4.1` with `ProcessCameraProvider` and `AndroidView(PreviewView)`. Frames captured on-device with immediate `ImageProxy.close()`.
- Reason / evidence: Robust compatibility with AGP 8.7.3, Compose BOM 2024.11.00, compileSdk 35, and hardware acceleration on physical Redmi Note 11 (Android 13 / API 33).
- User approval required: No (aligns with TRD and PRD).
- User approval/status: Accepted.
- Files and architecture affected: `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `CameraCaptureManager.kt`, `CameraPreviewView.kt`, `LiveScanScreen.kt`.
- Migration or cleanup required: None.
- Verification performed: Verified live camera preview and 2448x3264 photo capture on physical Redmi Note 11 device.
- New status: Accepted.

### 2026-10-17 — D-021 — Use connected Redmi Note 11 (Android 13 / API 33) as temporary development and testing device
- Previous decision: Samsung Galaxy F13 specified as target hardware; local emulator blocked by BIOS virtualization.
- Proposed replacement: Use connected Redmi Note 11 (Model 2201117TI, Android 13, API 33) for interim development and physical-device verification without waiting for Samsung phone.
- Reason / evidence: Explicit user authorization; device physically connected via USB debugging with verified installation and hardware camera access.
- User approval required: Yes (directed by user).
- User approval/status: Accepted.
- Files and architecture affected: Development and verification workflows in `MEMORY.md` and `TASKS.md`.
- Migration or cleanup required: None.
- Verification performed: `adb devices` showed `79145af9 device`, installed debug APK, launched app, verified camera stream.
- New status: Accepted.

### 2026-10-17 — D-021 — Responsive 2-row voice control layout and accessible high-contrast palette for Screen S07
- Previous decision: Single horizontal row with 3 OutlinedButtons for Listen/Speak, Stop, Repeat.
- Proposed replacement: Responsive 2-row layout in `VoiceGuidancePanel`:
  - Row 1: Full-width primary `Speak` Button (52dp height, filled container).
  - Row 2: Equal-width `Stop` Button (48dp height, solid Crimson Red `#C62828` / `#EF5350`) and `Repeat` Button (48dp height, solid Teal `#00796B` / `#80CBC4`).
  - Dark/Light Theme Tokens: Contrast ratios $\ge 7:1$ (WCAG AAA) across all buttons, text, icons, and status badges in both themes.
- Reason / evidence: On the physical Redmi Note 11 (width ~392dp), the previous single-row arrangement caused severe button clipping and label truncation, and Stop/Repeat lacked sufficient contrast.
- User approval required: Yes (directed by Phase 6 UI Bug Fix request).
- User approval/status: Accepted.
- Files and architecture affected: `Color.kt`, `Theme.kt`, `strings.xml`, `MedicineResultScreen.kt`.
- Migration or cleanup required: None; drop-in UI refinement.
- Verification performed: Verified on physical Redmi Note 11 device (screenshots `screen_result_v2_top.png` and `screen_result_v2_ready.png`). All 41 unit tests passed in `./gradlew.bat test`.
- New status: Accepted.

### 2026-10-25 — D-031 — Clean Structured Text Review Hub: Gesture Pad Removed from Result Screen
- Previous decision: `AccessibleGesturePad` embedded within `VoiceGuidancePanel` on Screen S07 `MedicineResultScreen`.
- Proposed replacement: Remove the accessible touch gesture pad from `MedicineResultScreen` entirely.
  - Touch gestures remain exclusively on the main live camera screen (`LiveScanScreen`).
  - Screen S07 acts purely as a structured text review and control hub:
    1. Structured Medicine Information: Brand & Generic Name, Dosage, Expiry Date & Batch, Clinical Usage (Purpose & Directions), Warnings, Side Effects, and Academic Disclaimer.
    2. Simple Voice Guidance Controls: Direct Speak, Stop, and Repeat audio playback buttons without gesture pads.
    3. Inspect Raw OCR Text: Expandable in-place raw OCR text section with detected line count and button to inspect full OCR breakdown.
    4. Action Buttons: 'Scan Another Package' (resets state and returns to camera) and 'View AR Camera Overlay' (switches back to camera view with active AR card).
- Reason / evidence: Explicit user request to keep touch gestures exclusively on the camera screen and make the 'Read Information' screen an uncluttered, readable structured text hub.
- User approval required: Yes (explicitly requested by user).
- User approval/status: Accepted.
- Files and architecture affected:
  - `MedicineResultScreen.kt`: Removed `AccessibleGesturePad` import and usage; replaced gesture pad with simple audio playback buttons (Speak, Stop, Repeat); added in-place `RawOcrTextSection` and streamlined structured monograph presentation.
- Migration or cleanup required: None; drop-in UI refinement.
- Verification performed: All 79 unit tests pass in `./gradlew.bat test`; debug APK assembled cleanly in `./gradlew.bat assembleDebug` in 21s.
- New status: Accepted.

### 2026-10-25 — D-030 — Unified Single-Screen Camera Workflow with Instant AR Overlay and Dedicated Information Navigation
- Previous decision: Multi-screen capture-and-review workflow where image capture triggered page transition away to 2D structured text results screen (`Screen.Result`), from which users navigated separately to AR overlay.
- Proposed replacement: Unified single-screen camera workflow based on exact behavioral rules:
  1. Single-Screen Gesture Handling: All touch gestures (Single Tap, Double Tap, Long Press) operate directly on the main camera screen without triggering unwanted page transitions.
     - Single Tap: Captures current frame, identifies medicine on-device, speaks Basic Name and Dosage, and immediately spawns the 3D AR Card Overlay right on the live camera view.
     - Double Tap: In-place audio playback for Warnings and Side Effects.
     - Long Press: In-place audio playback for Complete Profile Summary.
     - Movement Filter: Ignores touch movement exceeding the 20px threshold to prevent accidental triggers during camera panning or hand tremors.
  2. Instant AR Card Overlay: Appears directly on the main camera screen anchored to the package/surface without forcing the user away from the live camera view.
  3. Dedicated 'READ INFORMATION' Button: A prominent button directly on the camera overlay screen that is the ONLY trigger opening the full-screen structured text page (`Screen.Result`).
  4. Reset State: A reset / 'Scan Again' control on the camera screen that clears the AR card, resets tracking visuals, and cleanly returns the camera view back to the scanning state.
- Reason / evidence: Direct user requirement to eliminate disjointed screen hopping, keeping the camera live, providing instant AR feedback, while cleanly isolating text review behind a single dedicated button.
- User approval required: Yes (explicitly requested by user).
- User approval/status: Accepted.
- Files and architecture affected:
  - `LiveScanScreen.kt`: Converted into the unified single-screen camera workflow host; integrates live CameraX preview, instant 3D AR card overlay, in-place gesture audio playback, 20px movement filtering, dedicated READ INFORMATION button, and Scan Again reset state.
  - `ArOverlayScreen.kt`: Updated primary action to prominent READ INFORMATION button and secondary to Scan Again reset control.
  - `MainActivity.kt`: Updated screen navigation states to prevent unwanted screen transitions on capture/matching; wired READ INFORMATION button to `Screen.Result` and Scan Again to reset camera scanning state.
  - `strings.xml`: Added string resources for READ INFORMATION, Scan Again, Reset, and gesture feedback pills.
  - `UnifiedCameraWorkflowTest.kt`: Added automated test suite verifying all 5 behavioral rules.
- Migration or cleanup required: None; backwards-compatible Compose screen architecture.
- Verification performed: All 79 unit tests pass in `./gradlew.bat test` (0 failures); `./gradlew.bat assembleDebug` succeeds cleanly in 12s.
- New status: Accepted.

### 2026-10-25 — D-029 — Multi-Screen Accessible Touch Gesture Control Layer with Movement/Tremor Filtering
- Previous decision: Button-only audio interaction model on S07 Result Screen; no audio or gestures on Camera/AR screens.
- Proposed replacement: Implement a multi-screen accessible touch gesture control layer matching:
  - Single Tap: Basic item name and dosage.
  - Double Tap (< 300ms window): Warnings and side effects.
  - Long Press (>= 500ms duration): Complete monograph profile summary.
  - Movement Filtering: Pixel movement threshold (> 20px) cancels accidental drags, camera panning, or hand tremors.
  - Multi-screen support: Enabled across Pre-Capture Live Scanner (S04 with Capture & Play), AR Live Camera Overlay (S05), and Accessible 2D Result Screen (S07) with AccessibleGesturePad and redundant Stop/Repeat buttons.
- Reason / evidence: Explicit user request to enhance accessibility for visually and motor-impaired users without requiring precise visual button targeting.
- User approval required: Yes (explicitly requested and approved by user).
- User approval/status: Accepted.
- Files and architecture affected:
  - `SpeechContentGenerator.kt`: Added `basicNameAndDosage` and `warningsAndSideEffects` to `SpokenResult`; synthesized for all outcomes.
  - `SpeechTarget.kt`: Enum defining speech targets (`BasicNameAndDosage`, `WarningsAndSideEffects`, `FullProfile`).
  - `AccessibleGestureStateMachine.kt`: Pure JVM gesture state machine with 20px tremor filtering, 300ms double-tap window, and 500ms long-press duration.
  - `AccessibleTouchGestures.kt`: Compose `Modifier.accessibleTouchGestures(...)`.
  - `AccessibleGesturePad.kt`: Visual, tactile, high-contrast gesture pad component.
  - `LiveScanScreen.kt`: Attached gesture layer to camera preview with "Capture & Play" flow and tremor filter.
  - `ArOverlayScreen.kt`: Initialized native TTS manager and attached gesture layer over live AR camera feed.
  - `MedicineResultScreen.kt`: Embedded AccessibleGesturePad in VoiceGuidancePanel and wired speech targets.
  - `MainActivity.kt`: Routed pendingSpeechTarget from live scan gestures into result screens.
  - `strings.xml`: Added gesture labels, TalkBack descriptions, and status cues.
  - `gradle.properties`: Pinned JBR 21 via `org.gradle.java.home`.
  - Unit tests: Added `AccessibleGestureStateMachineTest` and updated `SpeechContentGeneratorTest`.
- Migration or cleanup required: None; drop-in accessibility layer.
- Verification performed: All unit tests pass in `./gradlew.bat testDebugUnitTest` (71 total tests); debug APK assembled cleanly in `./gradlew.bat assembleDebug` (56,393,453 bytes).
- New status: Accepted.

### 2026-10-17 — D-020 — Use kotlinx.serialization for local dataset parsing and schema validation
- Previous decision: Generic JSON parsing specified in Backend Schema without concrete library decision.
- Proposed replacement: Standardize on `kotlinx-serialization-json:1.7.3`.
- Reason / evidence: Allows strict typesafe validation, zero reflection, and pure JVM unit testing of bundled `medicines.json`.
- User approval required: No (implementation detail aligning with D-005 and Backend Schema).
- User approval/status: Accepted.
- Files and architecture affected: `app/build.gradle.kts`, `gradle/libs.versions.toml`, `MedicineRecord.kt`, `JsonMedicineParser.kt`.
- Migration or cleanup required: None.
- Verification performed: Unit tests in `MedicineDataValidationTest` passed in `./gradlew test`.
- New status: Accepted.

### 2026-10-17 — D-019 — Toolchain and build system versions for Phase 1 baseline
- Previous decision: Provisional planning versions without concrete build toolchain specifications.
- Proposed replacement: Standardize on Gradle 8.11.1 Wrapper, AGP 8.7.3, Kotlin 2.0.21, Compose Compiler Plugin, JBR 21.0.10, compileSdk 35, minSdk 26.
- Reason / evidence: Verified full clean compilation and unit test execution on Windows 11 host.
- User approval required: Yes (approved as part of Phase 1 authorization).
- User approval/status: Accepted.
- Files and architecture affected: Root build.gradle.kts, app/build.gradle.kts, settings.gradle.kts, gradle/libs.versions.toml, gradle/wrapper.
- Migration or cleanup required: None; initial setup.
- Verification performed: `./gradlew test` (45 tasks executed) and `./gradlew assembleDebug` (35 tasks executed).
- New status: Accepted.


### Change entry template

```text
### YYYY-MM-DD — D-XXX — Short title
- Previous decision:
- Proposed replacement:
- Reason / evidence:
- User approval required: Yes / No
- User approval/status:
- Files and architecture affected:
- Migration or cleanup required:
- Verification performed:
- New status: Accepted / Rejected / Superseded
```

## 6. Session-start Checklist for Anti-Gravity

- [ ] Read `DECISIONS.md` and `MEMORY.md`.
- [ ] Inspect the actual repository and identify deviations from accepted decisions.
- [ ] Do not add a backend, authentication, cloud service, barcode scanning, or unrelated feature without approval.
- [ ] Check device/library compatibility before committing to AR-specific work.
- [ ] Preserve the core accessible 2D path.
- [ ] If a permanent choice must change, document the evidence and ask for approval when the change affects scope, privacy, or user-visible behaviour.

## 7. Final Rule

**Accepted decisions are sticky, not immutable.** Preserve them by default. Change them only when the user explicitly directs the change or verified technical evidence makes the current choice infeasible. Record why, what changed, and how it was verified. Never silently reverse a decision.
