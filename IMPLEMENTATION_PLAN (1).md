# IMPLEMENTATION_PLAN.md — Step-by-Step Build Sequence

**Project:** SLAM-Based AR Medicine Identifier with Voice-Guided Accessibility Mode  
**Delivery target:** One-week academic proof-of-concept prototype  
**Current state:** Not started  
**Execution rule:** One phase at a time. Implement, test, report, update project records, and wait for the phase gate before proceeding.

## Purpose and non-negotiable execution rules

This roadmap prevents the coding agent from skipping foundations or building features on unverified assumptions.

Anti-Gravity must follow these rules:

1. Read the project documents and inspect the actual repository before editing files.
2. Work on exactly one phase at a time, in numerical order.
3. At the beginning of a phase, state its goal, planned file changes, dependencies, and acceptance criteria.
4. Implement only the current phase. Do not start the next phase while the current phase is failing or unverified.
5. At the end of the phase, run relevant tests/builds and perform the manual checks listed for that phase.
6. Report the files changed, work completed, commands run, test results, failures, limitations, and evidence. Separate verified facts from assumptions.
7. If verification fails, fix the current phase and retest. Do not mark it complete or move forward.
8. If blocked by missing information or unavailable hardware/tooling, explain the blocker and ask the user only when a decision is genuinely required. Do not silently substitute architecture.
9. At the end of every phase, update `MEMORY.md`, `DECISIONS.md`, and `TASKS.md`. Only Anti-Gravity updates these project-state documents during implementation. Do not claim a test passed unless it was actually run.
10. After reporting the phase and updating those files, **stop and wait for the user's approval to continue**. Do not automatically start the next phase in the same response/session unless the user explicitly authorizes sequential execution without approval gates.
11. Preserve the project constraints in `DECISIONS.md`. If a change is necessary, explain why and record the decision and rationale before implementing it.
12. This is an academic prototype, not a clinically validated medical product. Never invent medicine facts, claim clinical validation, or present uncertain OCR matching as certain.

## Project constraints that govern every phase

- Native Android application: Kotlin + Jetpack Compose.
- Target device: Samsung Galaxy F13.
- Minimum SDK baseline: Android 8.0 / API 26 provisionally, subject to build and dependency compatibility checks.
- On-device OCR: ML Kit Text Recognition v2.
- Speech: Android TextToSpeech.
- Offline-first: bundled local JSON dataset; no backend, auth, server, cloud API, or remote database.
- Dataset target: approximately 20–30 clearly labelled package records if suitable records with traceable provenance are available.
- Barcode scanning is excluded from the MVP.
- ARCore overlay is optional and conditional on compatibility/time; an accessible 2D fallback is mandatory.
- Unreadable or missing values must remain unknown. No personalized dosing advice or unsupported medical claims.
- One-week deadline: prioritize a working, tested end-to-end flow over production-grade extras.
- Do not add Room/SQLite unless an approved, concrete need is identified.

## Phase 1 — Project setup and repository foundation

**Goal:** Establish a reproducible Android build and a clean, understandable structure before implementing features.

**Tasks**
- Inspect the repository, existing files, Android SDK, Gradle, JDK, and available emulator/device.
- Read all supplied project documents and record any contradictions or blockers.
- Create/validate the Kotlin + Jetpack Compose project.
- Configure Gradle, app ID, SDK levels, dependencies, and `.gitignore`.
- Establish a package structure such as:
  - `data/model/`
  - `data/local/`
  - `domain/`
  - `camera/`
  - `ocr/`
  - `matching/`
  - `speech/`
  - `ar/`
  - `ui/navigation/`
  - `ui/screens/`
  - `ui/components/`
  - `util/`
- Keep the structure proportional to the prototype; do not create empty abstractions without a use case.

**Required verification**
- Build the debug variant.
- Launch the starter app on an emulator or the target phone if available.
- Record actual SDK/JDK/Gradle versions and any compatibility warnings.

**Done criteria**
- Project builds and launches.
- Dependencies and folder structure are documented.
- No cloud/backend/auth dependencies have been introduced.

**Phase output to user**
- Summary of setup, exact files changed, build/launch evidence, environment versions, unresolved blockers.

**Project record updates**
- Update `MEMORY.md`, `DECISIONS.md`, and `TASKS.md`; mark Phase 1 complete only if verification passes.

## Phase 2 — Local data schema and bundled dataset

**Goal:** Make the local data contract stable before building matching or results UI.

**Tasks**
- Define the medicine/package record model using `Backend Schema`, PRD, and TRD as the authority.
- Include a stable local ID, display name, supported searchable identifiers/text, and source/provenance metadata.
- Keep fields nullable/unknown where the source data or OCR may not support them.
- Create a small bundled JSON dataset, aiming for 20–30 records only if reliable, traceable records can be sourced within the project scope.
- Validate the JSON schema and parsing behavior.
- Add repository-local search/matching functions only as needed by the next phase.
- Document each record's source and verify applicable licence/terms. Do not manufacture data to hit a record count.

**Required verification**
- Unit tests for parsing valid JSON, missing required fields, malformed JSON, duplicate IDs, and unknown optional fields.
- Confirm the dataset can be read with network disabled.
- Confirm source/provenance metadata is available for included records.

**Done criteria**
- Data model and JSON format are stable and tested.
- Invalid records fail safely with a useful diagnostic.
- No unsupported medical facts are added.

**Phase output to user**
- Schema summary, dataset size and provenance status, files changed, test command/results, any records intentionally omitted.

**Project record updates**
- Update `MEMORY.md`, `DECISIONS.md`, and `TASKS.md`. Stop for approval before Phase 3.

## Phase 3 — Camera permissions and capture foundation (authentication is not applicable)

**Goal:** Provide a reliable, privacy-preserving way to capture a package image.

**Important:** The generic web-app concept of authentication does not apply to this offline single-user Android prototype. Do not implement signup, login, logout, sessions, accounts, or protected routes.

**Tasks**
- Add camera permission declarations and runtime permission handling.
- Implement camera preview and image capture using a suitable Android camera library/API.
- Handle permission denied, permanent denial, unavailable camera, and capture failure.
- Keep frames and image processing on-device. Do not add upload/network code.
- Add a simple camera screen consistent with the App Flow and UI/UX brief.

**Required verification**
- Permission granted → camera opens and capture works.
- Permission denied → app explains the limitation and does not crash.
- Camera/capture error → recoverable error state.
- Review code/dependencies to ensure no remote upload or auth flow exists.

**Done criteria**
- Camera can capture a usable image and return to the app flow.
- Permission and error paths are tested.
- No authentication service is present.

**Phase output to user**
- Permission/camera behavior, device/emulator used, files changed, tests/manual checks, blockers.

**Project record updates**
- Update `MEMORY.md`, `DECISIONS.md`, and `TASKS.md`. Stop for approval before Phase 4.

## Phase 4 — On-device OCR

**Goal:** Convert captured package images into text without a cloud dependency.

**Tasks**
- Integrate ML Kit Text Recognition v2 in the supported on-device configuration.
- Connect camera capture to OCR input.
- Preserve useful line/block structure for downstream extraction.
- Add loading, retry, cancel/back, no-text, and processing-error states.
- Keep OCR results transient unless a separately approved local-history feature is added.
- Add representative OCR fixtures for field-extraction tests.

**Required verification**
- Build succeeds with the OCR dependency.
- Run OCR against representative labelled medicine-package images.
- Test empty/poor-quality input and error paths.
- Verify operation with network disabled.

**Done criteria**
- The app returns OCR text from a usable image and handles failures honestly.
- OCR does not require a cloud service.
- Basic OCR processing and normalization tests pass.

**Phase output to user**
- OCR implementation summary, representative test cases/results, limitations, files changed.

**Project record updates**
- Update `MEMORY.md`, `DECISIONS.md`, and `TASKS.md`. Stop for approval before Phase 5.

## Phase 5 — Field extraction, local matching, and uncertainty handling

**Goal:** Match recognized text to local records without overstating confidence.

**Tasks**
- Normalize OCR text while preserving important distinctions such as strength/variant tokens when available.
- Extract only fields supported by the OCR and local data model.
- Match extracted text to the bundled dataset.
- Define and implement a conservative match/confidence gate; document the approach and thresholds.
- Return explicit states: confident match, ambiguous candidates, no match, and insufficient/unreadable text.
- Never fill missing expiry or medicine fields by guessing.
- Keep match logic independent from UI where practical so it can be tested.

**Required verification**
- Unit tests for known match, ambiguous match, no match, OCR noise, and missing fields.
- Confirm uncertain matches cannot appear as confirmed identity.
- Confirm local matching works offline.

**Done criteria**
- Matching results are deterministic for test fixtures.
- Low-confidence and unknown cases are visible and safe.
- No personalized dosing or unsupported medical claims are generated.

**Phase output to user**
- Matching rules, confidence behavior, test results, sample cases, files changed.

**Project record updates**
- Update `MEMORY.md`, `DECISIONS.md`, and `TASKS.md`. Stop for approval before Phase 6.

## Phase 6 — Results UI and voice-guided accessibility

**Goal:** Make the identification result understandable through both text and speech.

**Tasks**
- Implement result, no-match, and ambiguous-match screens using the UI/UX brief.
- Integrate Android TextToSpeech.
- Provide speak, stop, and repeat controls; handle missing/unavailable speech engine gracefully.
- Make essential information visible in text, not audio-only.
- Apply readable typography, contrast, touch target sizing, and accessibility labels/semantics.
- Clearly label unreadable/unknown fields.
- Include a short academic-prototype safety disclaimer.

**Required verification**
- Manually test speak/stop/repeat and the unavailable-TTS path where feasible.
- Confirm speech output matches displayed values and does not verbalize guesses as facts.
- Check TalkBack/semantics and text scaling where tooling/device permits.

**Done criteria**
- Results are usable without speech, and speech is a functional additional access path.
- Unknown/uncertain fields remain explicit.
- Core screen states have been checked.

**Phase output to user**
- Screens/components added, TTS behavior, accessibility checks and limitations, test results.

**Project record updates**
- Update `MEMORY.md`, `DECISIONS.md`, and `TASKS.md`. Stop for approval before Phase 7.

## Phase 7 — AR overlay or accessible 2D fallback

**Goal:** Add a minimal AR presentation only if it is compatible and does not jeopardize the core flow.

**Tasks**
- Verify ARCore support and library/device compatibility before committing to the overlay.
- If feasible, display a simple overlay with only supported extracted fields.
- Keep a 2D accessible result view available at all times.
- If ARCore support, stability, or schedule is inadequate, finish the 2D fallback and document why AR was deferred.
- Do not let AR failures block OCR, matching, text results, or speech.

**Required verification**
- Test the overlay on supported hardware if available.
- Test fallback behavior when AR is unavailable.
- Verify the primary flow remains usable without AR.

**Done criteria**
- Either the minimal AR overlay is verified on supported hardware or the 2D fallback is verified.
- The app handles unavailable AR without crashing.

**Phase output to user**
- Compatibility evidence, chosen AR/fallback path, tests, limitations, files changed.

**Project record updates**
- Update `MEMORY.md`, `DECISIONS.md`, and `TASKS.md`. Stop for approval before Phase 8.

## Phase 8 — Integration, UI polish, and edge cases

**Goal:** Make the end-to-end prototype coherent and recoverable.

**Tasks**
- Integrate camera → OCR → extraction/matching → result → speech.
- Polish navigation, loading states, empty states, errors, retry paths, and back behavior.
- Check text scaling, contrast, touch targets, and small-screen layout.
- Remove unused dependencies and debug-only behavior.
- Verify offline-first behavior and check that no accidental network dependency has appeared.
- Fix issues discovered during integration; avoid adding nonessential scope.

**Required verification**
- Exercise every main route and error state manually.
- Run all unit tests and the debug build.
- Test network-disabled operation.
- Record device/emulator and Android version used.

**Done criteria**
- Main user journey works end-to-end on available test hardware.
- Known blocking crashes are fixed.
- Remaining limitations are documented rather than hidden.

**Phase output to user**
- Integration summary, routes tested, build/test output, remaining issues.

**Project record updates**
- Update `MEMORY.md`, `DECISIONS.md`, and `TASKS.md`. Stop for approval before Phase 9.

## Phase 9 — Final testing and academic prototype handoff

**Goal:** Verify the prototype, provide reproducible build instructions, and clearly state its limitations.

**Tasks**
- Run unit tests and available UI/instrumentation tests.
- Run manual end-to-end scenarios:
  - successful readable package
  - permission denied
  - blurry/empty image
  - unknown medicine
  - ambiguous candidates
  - unreadable or missing expiry field
  - speech unavailable
  - AR unavailable / 2D fallback
  - network disabled
- Build the final debug APK and verify the artifact exists.
- Document setup, build, run, test commands, dataset provenance, and known limitations.
- Confirm README/project instructions reflect actual implementation.
- Confirm there are no clinical-validation claims or unsupported medical advice.
- Update the project records with final verified state.

**Done criteria**
- Debug build succeeds and the APK artifact is present.
- Core end-to-end flow passes on the available target.
- Test outcomes and untested areas are explicit.
- A new developer can follow the documented setup/build instructions.
- All three project-state files are current: `MEMORY.md`, `DECISIONS.md`, `TASKS.md`.

## Phase completion report template

Anti-Gravity must provide this report at the end of every phase:

1. **Phase and goal**
2. **Status:** PASS / BLOCKED / FAIL (never use PASS without evidence)
3. **Work completed**
4. **Files created or changed**
5. **Commands executed**
6. **Tests/checks run and exact results**
7. **Manual/device verification**
8. **Known limitations and unresolved issues**
9. **`MEMORY.md` update summary**
10. **`DECISIONS.md` update summary**
11. **`TASKS.md` update summary**
12. **Next phase proposed**
13. **Approval request:** stop and wait before proceeding

## Overall done criteria

The prototype is complete when:
- The Android debug build installs and launches on the available target.
- A user can capture a medicine package image, run on-device OCR, attempt a local match, and receive a text result plus optional spoken guidance.
- Unknown and uncertain results are clearly handled.
- The core flow works offline.
- The 2D fallback works regardless of ARCore availability.
- Relevant tests and manual checks have been run and recorded.
- Dataset provenance and limitations are documented.
- `MEMORY.md`, `DECISIONS.md`, and `TASKS.md` reflect only verified project state.
- No production-readiness or clinical-validation claim is made.
