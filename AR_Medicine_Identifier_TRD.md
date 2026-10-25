# Document 02 — Technical Requirements Document (TRD)

**Project:** SLAM-Based AR Medicine Identifier with Voice-Guided Accessibility Mode  
**Document version:** 1.0  
**Project type:** Academic proof of concept / working prototype  
**Target device:** Samsung Galaxy F13  
**Target delivery:** One week  
**Architecture:** Native Android, on-device processing, offline-first  
**Status:** Implementation baseline for Anti-Gravity

> **Scope rule:** Build a demonstrable prototype, not a production-ready medical application. Prioritise the camera → OCR → cautious local match → accessible result flow. ARCore overlays are required only if they work reliably on the target device within the deadline; a usable 2D overlay is the fallback. Do not add barcode scanning or cloud services to this prototype.

## 1. Technical Overview

The app uses the phone camera to capture a medicine package, recognises visible label text on-device, extracts fields such as medicine name, strength, and expiry date, and compares those fields against a small local academic dataset. It presents the result as accessible on-screen text and speech. If device support and time permit, it also projects a simple panel into the AR camera view.

The app is not a diagnostic or prescribing system. It must not recommend starting, stopping, or changing a medicine or dose. Uncertain matches must be shown as uncertain; unsupported medicine facts must be omitted rather than invented.

## 2. Technology Decisions

### 2.1 Frontend / Android UI

- **Language:** Kotlin.
- **UI framework:** Jetpack Compose.
- **Application type:** Native Android application.
- **Camera / AR view:** Android camera integration compatible with the selected ARCore setup. Prefer ARCore's camera feed when running an ARCore session; do not independently open a second camera session that conflicts with ARCore.
- **Result UI:** Compose panels and a conventional accessible 2D result screen. If ARCore is stable, show a simple projected overlay; otherwise use the 2D screen/HUD.
- **Minimum Android version:** Android 8.0 (API 26) is the initial planning baseline, subject to dependency and device compatibility checks. Raise it only if required by the chosen SDK/library.
- **Primary device:** Samsung Galaxy F13. Verify ARCore support, Android version, camera behaviour, and performance on the actual handset before depending on AR-specific functionality.

### 2.2 Backend

- **Backend framework/runtime:** None for the prototype.
- **Server/API:** None.
- **Reason:** The one-week academic build should avoid server setup, hosting, accounts, API secrets, network dependency, and unnecessary image transfer.
- All core recognition, matching, expiry parsing, and text-to-speech should work locally.

### 2.3 Database

- **Primary data store:** Small local JSON dataset for the fastest initial implementation.
- **Upgrade path:** SQLite, optionally accessed through Room, if relational queries or local scan history become necessary.
- **Medicine knowledge base:** Start with approximately 20–30 manually curated test records. This is a prototype dataset, not a complete or clinically validated medicine catalogue.
- **Suggested record fields:** `id`, `brandName`, `genericName`, `strength`, `dosageForm`, `manufacturer` (optional), `labelPurpose` (optional), `labelDirections` (optional), `warnings` (optional), `commonSideEffects` (optional), `sourceTitle`, `sourceUrl`, `sourceAccessDate`, `datasetVersion`.
- **Expiry date:** Parse from the live package image; do not treat a database's medicine record as the expiry date for a specific physical package.
- **Missing facts:** Store as null/absent and display “Not available in this prototype dataset.” Never fill missing medical fields with generated guesses.
- **History:** Optional stretch feature. If implemented, use Room/SQLite and let the user delete entries. Do not store camera images by default.

### 2.4 Authentication

- **Authentication provider:** None.
- **Accounts:** No user registration, login, password, OAuth, or remote profile.
- **Reason:** The prototype has no backend and stores its small dataset locally.

### 2.5 Hosting and Deployment

- **Cloud hosting:** None required.
- **Development:** Android Studio and Anti-Gravity on the development computer.
- **Distribution:** Install a debug APK on the Samsung Galaxy F13 using USB debugging or a locally transferred APK.
- **Release signing / Play Store:** Out of scope for the one-week prototype.
- **Network dependency:** The core flow must work in airplane mode after the app and its local dataset are installed.

## 3. Third-Party APIs and Services

| Library / service | Purpose | Prototype use / cost |
|---|---|---|
| Android SDK | Platform APIs, camera permissions, lifecycle, TTS | Required; no separate service fee |
| Kotlin | Application language | Required; no separate service fee |
| Jetpack Compose | UI and accessible result screens | Required; Android Jetpack library |
| ARCore SDK | SLAM tracking, camera pose, raycasts, optional AR overlay | Conditional on target-device compatibility; no cloud account required for core AR |
| ML Kit Text Recognition v2 | On-device OCR for printed medicine packaging | Primary OCR choice; use the on-device model/configuration and verify build/dependency requirements |
| Android TextToSpeech | Spoken scan guidance and medicine-label summary | Required for voice output; installed voice availability varies by device |
| AndroidX / Lifecycle | Lifecycle-aware components and coroutine integration where needed | Recommended standard Android dependencies |
| Kotlin Coroutines | Run OCR and parsing asynchronously without blocking rendering/UI | Recommended |
| Room / SQLite | Local persistence if history or structured queries are implemented | Optional; otherwise use local JSON |
| OpenCV Android SDK | Sharpness/glare metrics or image processing | Optional stretch; omit if setup threatens the deadline |
| WorkManager | Scheduled expiry reminders | Stretch feature; omit from the core vertical slice |
| Cloud vision/OCR API | None | Explicitly excluded |
| Barcode / QR / DataMatrix SDK | None | Explicitly excluded from MVP |

**Important:** ML Kit's on-device OCR recognises text; it does not verify medicine identity or validate medical facts. The local matching and uncertainty gate are separate application logic.

## 4. Key Libraries and Responsibilities

### Required or first-priority
- **Jetpack Compose:** Home, scanning state, result, confirmation, unknown-medicine, and error screens.
- **ML Kit Text Recognition v2:** Convert camera image frames or captured images into text blocks, lines, and bounding boxes.
- **Android TextToSpeech:** Speak short guidance and result summaries. Queue speech so guidance and results do not overlap; provide a repeat action.
- **Kotlin Coroutines:** Keep OCR, parsing, and matching off the main/UI thread.
- **AndroidX Lifecycle:** Start/stop camera or AR work with the app lifecycle and clean up resources.

### Conditional / stretch
- **ARCore:** Use if supported and stable on the Galaxy F13. Provide a simple overlay first; do not attempt complex 3D object tracking within the one-week deadline.
- **OpenCV:** Add only if there is enough time to implement and test a useful sharpness/glare gate.
- **Room + WorkManager:** Add only if the core scan flow is complete and time remains.

Avoid adding libraries merely because they appear in the reference architecture. Each dependency must serve a prototype requirement and compile successfully on the target setup.

## 5. Core Runtime Pipeline

The intended pipeline is:

1. **Capture:** Receive camera frames from the selected Android camera/ARCore configuration.
2. **Quality check:** Reject clearly unusable frames when a simple quality check is available; otherwise provide a manual capture/retry path.
3. **OCR:** Run on-device ML Kit text recognition on a captured or selected frame.
4. **Text normalisation:** Trim whitespace and normalise case and common separators while preserving the original OCR text for review.
5. **Field extraction:** Use cautious rules/regular expressions to identify likely medicine name, strength/unit, composition when present, and printed expiry date.
6. **Local matching:** Compare extracted identity fields with the small local dataset. Use name similarity as a candidate-generation aid, not as sole proof.
7. **Verification gate:** Check strength and composition when available. If multiple candidates are close, required fields are missing, or the match is not convincing, ask for confirmation or report that identification is uncertain.
8. **Expiry evaluation:** Parse the printed expiry date separately from medicine identity. If the date cannot be read or parsed reliably, report it as unknown.
9. **Presentation:** Show a readable Compose result and speak a concise summary. Use an AR overlay only when tracking is available; otherwise retain the 2D result.
10. **Recovery:** Allow rescan, manual correction/entry if implemented, and a clear unknown/unreadable state.

### Initial matching rules

- Prefer exact normalised name plus matching strength/composition.
- Use fuzzy similarity only to propose candidates.
- Never announce a fuzzy-only candidate as certain.
- If strength or composition contradicts the candidate, reject or ask for confirmation.
- If no local record matches, show “Medicine not found in the prototype dataset.”
- Do not equate a medicine name match with proof that a physical package is genuine or safe.

### Initial expiry rules

- Support common printed month/year and day/month/year forms that are explicitly recognised by tests.
- Do not silently convert an ambiguous number sequence into an expiry date.
- For month/year labels, treat expiry as the end of the stated month for the status calculation, and document this assumption.
- Use clear labels such as **Expired**, **Expires soon**, **Caution**, **OK**, or **Unable to read expiry**.
- Do not infer expiry from the medicine name or dataset.

## 6. Architecture and Folder Structure

Use a simple, feature-oriented Android project. Avoid building a multi-module enterprise architecture for a one-week prototype.

```text
ARMedicineIdentifier/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── README.md
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── assets/
│       │   │   └── medicines.json
│       │   ├── java/<package-path>/
│       │   │   ├── MainActivity.kt
│       │   │   ├── ui/
│       │   │   │   ├── HomeScreen.kt
│       │   │   │   ├── ScanScreen.kt
│       │   │   │   ├── ResultScreen.kt
│       │   │   │   ├── ConfirmationScreen.kt
│       │   │   │   └── components/
│       │   │   ├── camera/
│       │   │   │   └── CameraController.kt
│       │   │   ├── ocr/
│       │   │   │   └── TextRecognitionService.kt
│       │   │   ├── extraction/
│       │   │   │   ├── LabelFieldExtractor.kt
│       │   │   │   └── ExpiryDateParser.kt
│       │   │   ├── matching/
│       │   │   │   ├── MedicineMatcher.kt
│       │   │   │   └── ConfidenceGate.kt
│       │   │   ├── data/
│       │   │   │   ├── Medicine.kt
│       │   │   │   └── MedicineRepository.kt
│       │   │   ├── speech/
│       │   │   │   └── SpeechController.kt
│       │   │   ├── ar/
│       │   │   │   └── ArOverlayController.kt
│       │   │   └── util/
│       │   │       └── TextNormaliser.kt
│       │   └── res/
│       ├── test/
│       │   └── java/<package-path>/
│       └── androidTest/
└── docs/
    ├── dataset-sources.md
    └── test-results.md
```

### Folder rules
- `ui/` contains Compose screens and visual components only.
- `camera/` owns camera lifecycle and frame/image acquisition.
- `ocr/` wraps ML Kit and returns structured OCR output; it must not decide which medicine is correct.
- `extraction/` parses raw OCR into candidate fields and expiry tokens.
- `matching/` compares extracted fields with local records and applies the uncertainty gate.
- `data/` defines medicine models and loads `medicines.json`; do not mix medical facts into UI composables.
- `speech/` owns TTS lifecycle, queueing, repeat, and stop behaviour.
- `ar/` contains optional ARCore integration and must have a 2D fallback.
- `util/` contains small pure helper functions.
- `docs/` records dataset provenance, setup, known limitations, and measured tests.

The package path should be selected once when the project is created and used consistently. Use `PascalCase` for Kotlin class names, `camelCase` for functions/properties, `UPPER_SNAKE_CASE` for constants, and descriptive file names matching the primary class.

## 7. Data Model

A minimal local medicine record can use this shape:

```json
{
  "id": "sample-001",
  "brandName": "Example Brand",
  "genericName": "Example ingredient",
  "strength": "500 mg",
  "dosageForm": "tablet",
  "manufacturer": null,
  "labelPurpose": null,
  "labelDirections": null,
  "warnings": [],
  "commonSideEffects": [],
  "sourceTitle": "Source title",
  "sourceUrl": "https://example.invalid/source",
  "sourceAccessDate": "2026-10-15",
  "datasetVersion": "0.1"
}
```

The values above are placeholders showing the schema only. Do not include them as a real medicine entry. Replace `example.invalid` with a real source URL for every actual record, or omit unsupported fields.

Keep package-specific scan output separate from the knowledge-base record:

- `rawOcrText`: original OCR result.
- `parsedName`, `parsedStrength`, `parsedExpiry`: fields inferred from the current package.
- `candidateMedicineId`: local record selected for display, if any.
- `matchStatus`: `high_confidence`, `needs_confirmation`, `unmatched`, or `unreadable`.
- `matchReason`: short diagnostic explanation for logs/test output.
- `scanTimestamp`: time of scan.

Do not store raw images by default.

## 8. Environment Variables and Secrets

**Required environment variables: none.**

The prototype has no backend, no remote API, no authentication, and no external service credentials. Do not create fake API keys or add secrets to the repository.

Android configuration belongs in Gradle files and Android manifest declarations, not environment variables. Request camera permission only for scanning. Do not request location, contacts, microphone, or storage permissions unless a separately approved feature truly requires them.

If a future version adds a cloud service, document its purpose, consent flow, data sent, and secret management separately before implementing it. Cloud fallback is not part of this prototype.

## 9. Privacy, Safety, and Error Handling

- Core scanning must work offline.
- Camera images stay on-device; do not upload them.
- Do not add analytics or tracking SDKs.
- Explain that the prototype is educational and is not a substitute for a pharmacist or doctor.
- Do not recommend personalised dosing or treatment changes.
- Do not display unsupported medical information.
- On low confidence, ask the user to rescan or confirm rather than stating a guessed medicine as fact.
- If OCR cannot read expiry, explicitly show that the expiry is unknown.
- Handle denied camera permission, missing TTS voice, OCR errors, interrupted camera sessions, and AR tracking failure with readable messages and a recovery action.
- Ensure TTS does not repeatedly interrupt itself; offer repeat and stop controls.
- All key information must also be available in normal UI text for screen readers.

## 10. Performance and Technical Constraints

These are prototype targets, not guaranteed measurements:

- Keep OCR and matching off the main/UI thread.
- Show a loading state while recognition is running.
- Aim for a result within approximately 3 seconds after a usable capture on the target phone, and measure actual latency.
- Avoid processing every camera frame if this causes heat or UI lag; a captured-frame or throttled-frame design is acceptable for the prototype.
- Do not require network access for scanning or matching.
- Keep the local dataset small and versioned.
- The app must not crash when a medicine is missing from the dataset or expiry cannot be parsed.
- Use a 2D fallback if ARCore is unsupported, tracking is lost, or AR implementation exceeds the time budget.
- Do not claim an accuracy percentage until it has been measured on a documented test set.
- Do not block the camera/rendering loop while OCR runs.

## 11. Testing Requirements

At minimum, test the following manually on the Samsung Galaxy F13:

1. A supported package with clear text and a known local record.
2. A supported package with a strength that differs from another candidate.
3. A package absent from the local dataset.
4. A blurred or reflective package.
5. An expiry date in month/year format.
6. An expiry date in day/month/year format, if supported.
7. An unreadable or ambiguous expiry date.
8. Camera permission denied and subsequently granted.
9. TTS available and TTS unavailable or voice not installed.
10. AR tracking lost or unavailable, verifying that the 2D fallback still works.

Record the expected result, actual result, OCR text, match outcome, and notes in `docs/test-results.md`. Do not use a single successful demo scan as evidence of general accuracy.

## 12. One-Week Implementation Order

1. Create the Android project and confirm it builds on the target setup.
2. Implement the camera preview/capture and camera permission handling.
3. Integrate on-device ML Kit OCR and display raw recognised text.
4. Add field extraction and expiry parsing with unit tests.
5. Load the small local JSON dataset and implement conservative matching.
6. Add the confidence gate, unknown state, and explicit confirmation state.
7. Add accessible Compose result UI and TextToSpeech.
8. Attempt a simple ARCore overlay only after the 2D flow works and device support is confirmed.
9. Test edge cases, document limitations, and build a local debug APK.

If time becomes constrained, stop adding features and stabilise the core vertical slice. Do not spend the deadline on backend infrastructure, cloud AI, barcode support, custom model training, or advanced 3D effects.

## 13. Explicitly Out of Scope

- Backend server, cloud database, remote API, or user accounts.
- Barcode, QR, and DataMatrix scanning.
- Cloud OCR or vision-language model fallback.
- Production-grade clinical content review or certification.
- Comprehensive or guaranteed-correct medicine catalogue.
- Loose tablet/capsule identification.
- Drug-interaction checking or personalised dose recommendations.
- Custom-trained object detector or OCR model.
- iOS version or AR glasses support.
- Complex hand tracking, occlusion, cylindrical label stitching, or spatial audio.
- Production Play Store deployment, analytics, and remote monitoring.
- Expiry reminders and scan history unless the core flow is complete and time remains.

## 14. Acceptance Criteria for the Prototype

The prototype is acceptable for an academic demonstration when:

- The Android app installs and launches on the target device or a documented compatible test device.
- A user can capture a medicine label and see the on-device OCR result.
- The app can match a supported test label against the local dataset or report that it is unknown.
- A mismatch or uncertain result is not presented as a certain identification.
- Expiry parsing either returns a supported, plausible result or explicitly reports unknown.
- The result screen is readable and can speak a concise summary through Android TextToSpeech when a voice is available.
- The main flow works without a network connection.
- If AR is implemented, tracking failure does not block access to the result; otherwise the 2D fallback is demonstrated.
- The project includes setup instructions, dataset source notes, test results, and known limitations.

## 15. Source and Decision Notes

This TRD is based on the supplied `AR_Medicine_Identifier_Deep_Technical_Context.pdf`, particularly its recommended native Android stack, layered module structure, recognition and matching pipeline, offline knowledge-base strategy, accessibility approach, and prototype cut-line guidance. The reference recommends Kotlin + Jetpack Compose, ARCore, ML Kit Text Recognition v2, OpenCV where needed, a local SQLite knowledge base, Android TextToSpeech, and WorkManager for reminders.

The current TRD narrows that architecture for the user's one-week academic prototype: no backend, no authentication, no cloud fallback, no barcode scanning, a small local JSON dataset first, and AR only if feasible on the target phone. The Android minimum version is a planning baseline and must be confirmed against the actual device and selected dependencies.
