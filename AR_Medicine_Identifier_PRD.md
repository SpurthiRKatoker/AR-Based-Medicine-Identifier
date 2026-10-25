# Document 01 — Product Requirements Document (PRD)

**Project:** SLAM-Based AR Medicine Identifier with Voice-Guided Accessibility Mode  
**Document status:** Updated draft v1.1  
**Basis:** AR_Medicine_Identifier_Deep_Technical_Context.pdf (Version 1.0, October 2026)  
**Product platform for academic prototype:** Native Android; primary test device: Samsung Galaxy F13  
**Prototype stack direction:** Kotlin, Jetpack Compose, ML Kit OCR, a small local JSON/SQLite dataset, Android TextToSpeech; ARCore and OpenCV only where feasible within the one-week deadline

> **Product safety boundary:** This app reads medicine packaging and presents verified, general label-level information. It is not a diagnostic tool and must not recommend starting, stopping, skipping, or changing a medicine or dose. When identification is uncertain, it must say so and ask the user to rescan or confirm.

## 1. App Name

**AR Medicine Identifier**

## 2. Tagline

**Point your camera. Hear what the medicine label says.**

## 3. Problem

Medicine packaging often uses small, low-contrast text and may be difficult to read on reflective foil strips, bottles, and cartons. People may misread a medicine's name or strength, overlook an expiry date, forget label instructions, or struggle to use screen-based scanning interfaces—especially older adults and people with low vision or blindness.

A single-frame OCR result or name-only fuzzy match can also confuse look-alike or sound-alike medicines. In a medication context, a confident but incorrect identification is a serious safety risk.

The product addresses these problems by combining on-device text recognition, multi-frame refinement, matching against a curated and traceable medicine knowledge base, markerless AR positioning, and eyes-free accessibility guidance. It must fail safely rather than silently guess.

## 4. Target Users

### Primary persona — Older adult managing multiple medicines
An older adult who needs to identify medicine strips or bottles, check whether a printed expiry date has passed, and hear key label information without reading small print. They benefit from a short, forgiving flow, clear speech, large text, and no time-limited result screens.

### Primary persona — Blind or low-vision user
A person who cannot reliably aim using the screen or read conventional small-print interfaces. They need spoken guidance about framing, distance, glare, and steadiness, automatic scan triggering, haptic feedback, speech with synchronised captions, and compatibility with the platform screen reader.

### Secondary persona — Caregiver or family member
A person helping someone manage several medicines who wants to identify packaging quickly, review label-level facts, save a scan, and receive expiry reminders.

### Secondary persona — General user
A user who wants help reading an unfamiliar packaged medicine label and can use the same recognition flow with visual AR panels and optional spoken output.

## 5. Core Value Proposition

AR Medicine Identifier combines **medicine-specific, safety-gated label reading** with **markerless AR and accessibility-first interaction**. Instead of relying on a single image, a pre-registered image target, or a name-only match, the MVP refines text across usable frames, checks name/strength/composition against a curated database, and uses confidence thresholds to decide whether to present, ask for confirmation, or request a rescan.

Its key differentiators are:
- **Eyes-free first:** scan guidance through speech and haptics, with automatic point-and-hold triggering.
- **Safety-gated identification:** multi-frame fusion, cross-field verification, and explicit uncertainty handling.
- **AR that reflects reality:** panels follow a hand-held object or remain world-locked when the object is stationary; stale overlays are not presented as attached.
- **Offline-first privacy:** core recognition, database lookup, and speech work on-device by default.
- **Traceable information:** medical content comes from sourced, versioned, reviewed knowledge-base records, not text generated freely by an AI model.

## 6. Product Goals

1. Help users identify supported packaged medicines from printed label text.
2. Read key identity and expiry information aloud and display it in accessible text.
3. Present general label-level purpose, label directions, key warnings, and common side effects from verified database records.
4. Guide users to a readable camera view without requiring precise taps or sighted aiming.
5. Keep the core scan-and-read flow functional without a network connection.
6. Avoid confidently presenting an incorrect medicine identity.
7. Demonstrate a technically credible SLAM-based AR pipeline with stable, correctly positioned overlays.
8. Support local scan history and expiry reminders.

## 7. Core Features — Must Have (MVP)

### 7.1 Live camera and supported packaging
- Start an ARCore session and configure continuous autofocus where supported.
- Accept medicine strips/blister packs, cartons, and bottle labels as intended inputs.
- Keep the background in view when possible to help SLAM tracking.
- Provide a simpler 2D HUD fallback if AR tracking is unavailable or remains lost.

### 7.2 Image-quality gate and scan guidance
- Assess candidate image regions for sharpness, exposure, glare, and motion.
- Send only quality-acceptable frames to OCR.
- Provide specific, rate-limited guidance such as “Hold steady,” “Tilt the strip slightly—there is glare,” or “Move a little farther back.”
- Use speech and haptics as redundant guidance channels.
- Avoid vibration cues when shaking/motion makes them counterproductive.

### 7.3 On-device OCR and multi-frame fusion
- Read visible text using on-device OCR.
- Track text lines across usable frames and combine repeated readings.
- Treat a text line as stable when equivalent readings (edit distance ≤ 1) appear in at least 3 of the last 5 usable frames, as an initial design rule.
- Use character-level voting to reduce common OCR confusions such as O/0, I/1, S/5, and rn/m.
- Drop stale frames rather than allowing the perception pipeline to block the AR render loop.

### 7.4 Structured label-field extraction
Extract, where readable:
- Brand/product name
- Strength and units
- Composition/generic ingredient(s)
- Dosage form
- Expiry date
- Batch number
- Manufacturing and manufacturer/marketer details where available

Keep the raw OCR reading available for transparency. Never confuse manufacturing date (MFG) with expiry (EXP).

### 7.5 Medicine matching and verification
- Match against a curated, offline, closed-world knowledge base.
- Retrieve candidates using brand and composition keys, then rank using name similarity, strength, composition, form, and manufacturer signals.
- Require strength verification when available and cross-check the brand's recorded composition against the label reading.
- Treat a resolved barcode as an additional identification signal, but retain composition agreement checks.
- Force confirmation for close candidates and configured look-alike/sound-alike medicine pairs.
- Clearly report when a medicine is not in the database.

### 7.6 Confidence gate and fail-safe outcomes
Use the following as initial thresholds, to be tuned on validation data:
- **High confidence:** score ≥ 0.90, margin over runner-up ≥ 0.10, and strength verified. Present the result using wording such as “This reads as…”.
- **Medium confidence:** score ≥ 0.70 but below the high-confidence criteria, small margin, or unverified strength. Ask the user to confirm the candidate.
- **Low confidence:** score < 0.70 or no stable required fields. Give a targeted rescan cue; after repeated failure, offer manual/voice entry or an explicitly consented optional cloud fallback if implemented.

These thresholds are starting hypotheses, not validated performance claims. The app must not treat an uncertain match as fact.

### 7.7 Verified medicine information
- Present information in this order: identity → expiry verdict → how the label says to take it → key warnings → common side effects.
- Build user-facing medical text from verified database fields and templates only.
- Use plain language and clearly distinguish label strength from a personalised dose.
- Include a reminder to follow the prescriber or pharmacist.
- Offer a short summary first and more detail on request.

### 7.8 Expiry parsing and status
- Parse supported printed expiry formats, including month/year and explicit day/month/year forms.
- Repair common OCR digit confusions only inside date-like tokens.
- Treat a month-only expiry as the final day of that month unless a day is explicitly printed.
- Show/announce one of: Expired, Expires soon (≤ 30 days), Caution (≤ 90 days), or OK (> 90 days), using word + number + icon and not colour alone.
- If expiry cannot be read reliably, say so; never assume the medicine is in date.
- Announce an expired status prominently and advise the user to ask a pharmacist about disposal rather than use.

### 7.9 Accessibility-first result presentation
- Provide speech output with synchronised large-print captions.
- Use word-level caption highlighting where the TTS platform supports callbacks.
- Keep captions high-contrast (target contrast ≥ 7:1), at least 24 sp-equivalent initially, no more than two lines, and responsive to system font scaling.
- Mirror AR information into the normal accessibility tree; AR/3D-rendered text alone is not sufficient for TalkBack.
- Use touch targets of at least 48 dp (56 dp preferred) and generous spacing.
- Keep results visible until the user leaves or starts another scan; do not auto-dismiss them.

### 7.10 Point-and-hold automatic trigger
- Start recognition after a stable framing/dwell condition is met for approximately 1–1.5 seconds.
- Initial stability signals include low gyro motion, sufficient sharpness, and high text-token overlap between consecutive frames.
- Reset the dwell timer when the user moves.
- Keep a hardware-button trigger as a possible backup if supported and appropriate.

### 7.11 AR positioning and tracking recovery
- Use ARCore SLAM for camera pose, tracking state, planes/features, and anchors; do not treat SLAM as the medicine recogniser or as a hand-held object tracker.
- Lift OCR image coordinates into 3D using a suitable raycast when reliable or a size-prior depth estimate for close, hand-held objects.
- Use the camera pose and intrinsics from the frame's capture timestamp when late OCR results return.
- Support world-locked panels for stationary objects and object-following panels for hand-held objects.
- Smooth object-following transforms and avoid presenting stale overlays as if they were still attached.
- On tracking failure, explain the issue, suspend new OCR submissions where appropriate, and fall back to 2D presentation if tracking does not recover.

### 7.12 Local history and expiry reminders
- Save product ID, timestamp, confidence/gate outcome, parsed expiry, batch and raw text as appropriate.
- Keep thumbnails off by default.
- Store health-adjacent history encrypted at rest where feasible.
- Schedule configurable expiry reminders; proposed defaults are 30 days before, 7 days before, and on the expiry date.
- Restore reminder scheduling after device restart.
- Provide controls to delete saved history.

### 7.13 Offline-first operation and privacy
- Keep core OCR, matching, knowledge-base access, and TTS on-device by default.
- Do not send camera images or medicine history to a server by default.
- Request only permissions needed for the features the user enables.
- Any cloud fallback must be optional, require explicit consent for each use, send only a cropped label image, and verify returned text against the local database.

### 7.14 Onboarding and safety communication
- Explain aloud and on screen what the app does and does not do.
- Include a repeatable practice scan with a sample image.
- State that the app is informational only and not a substitute for a doctor or pharmacist.
- State that the app does not recommend changing a dose or treatment.
- Provide clear recovery guidance for denied camera permission, missing TTS voices, unknown medicines, and tracking failures.

## 8. Nice to Have (Should / Could / Future)

Prioritise these only after the core scan-and-read flow is working. Because the delivery window is one week, features are divided into the first-demo scope and stretch work. Keep implementation pragmatic and prototype-focused rather than production-grade.

### First-demo requirement
- **Barcode, QR, and DataMatrix scanning** as an additional identification channel. Include this in the first demo, as requested. It should identify a product only when the decoded code maps to a known record in the local dataset; otherwise report that the code is not in the prototype database.

### Should Have — Next priority
-
- Voice commands: “repeat,” “more detail,” “save,” “scan another,” “stop,” “slower,” and “faster.”
- Improved high-resolution still capture for OCR when the image is stable.
- Tall-man lettering for configured look-alike/sound-alike names.
- A custom object detector for strips, cartons, and bottle labels.
- A learned field extractor to complement rule-based parsing.
- A more advanced OCR model fine-tuned for reflective foil and additional scripts.
- A fuller evaluation suite comparing OCR, fusion, matching, and 2D/3D placement approaches.

### Could Have — Stretch / Future
- Opt-in cloud vision-language reading for difficult labels, with strict database verification.
- Multilingual OCR and spoken output for selected Indian languages.
- iOS port using ARKit/Vision.
- Hand/finger occlusion handling for AR panels.
- Curved bottle-label stitching across multiple viewpoints.
- Spatial audio anchored to the medicine object.
- Caregiver sharing with appropriate privacy protections.
- Loose-pill identification, prescription reading, interaction checking, refill integration, and head-worn AR are future projects, not MVP requirements.

## 9. User Stories

### Identification and reading
- As an older adult, I want the app to read a medicine's name and strength aloud so that I do not have to decipher small packaging text.
- As a user, I want the app to read the printed expiry date so that I can notice expired or soon-to-expire packaging.
- As a user, I want the app to explain the general purpose and label directions in plain language so that I can understand the information printed or associated with the medicine.
- As a user, I want to see the raw text the app read so that I or a caregiver can compare it with the package.

### Accuracy and safety
- As a user, I want the app to ask for confirmation when two medicine candidates are close so that it does not silently choose a look-alike medicine.
- As a user, I want the app to say when it is unsure or the medicine is unsupported so that I am not misled by a guessed result.
- As a user, I want the app to distinguish label strength from personal dosage advice so that the information is not mistaken for a prescription.
- As a user, I want an unreadable expiry date to be reported as unknown so that the app never implies that the medicine is in date without evidence.

### Accessibility
- As a blind or low-vision user, I want spoken and haptic aiming guidance so that I can position the package without relying on the screen.
- As a user with limited dexterity, I want scanning to trigger after I hold the package steady so that I do not need to tap a small control.
- As a user, I want captions synchronised with speech so that I can follow the spoken result when useful.
- As a screen-reader user, I want every result and action exposed through the standard accessibility tree so that the app remains usable with TalkBack.

### AR and interaction
- As a user, I want result panels to follow a hand-held medicine and remain stable when the medicine is stationary so that the overlay corresponds to the correct object.
- As a user, I want the app to switch to a simpler 2D view if AR tracking fails so that I can still access the recognition result.
- As a user, I want to say “repeat” or “tell me more” (if voice commands are enabled) so that I can control how much information is spoken.

### History and privacy
- As a caregiver, I want to save a scan locally so that I can review the medicine and expiry information later.
- As a user, I want expiry reminders so that I can be alerted before a saved medicine reaches its printed expiry date.
- As a privacy-conscious user, I want the core flow to work offline and keep images on my phone so that sensitive information is not uploaded by default.
- As a user, I want to delete my saved history so that I can remove medicine-related records from the device.

## 10. Out of Scope for the MVP

The MVP does **not**:
- Diagnose a condition or recommend treatment.
- Recommend starting, stopping, skipping, increasing, or decreasing a dose.
- Calculate a personalised dose or determine whether a medicine is appropriate for a particular person.
- Check drug interactions or contraindications across a user's medicines.
- Identify loose tablets or capsules by shape, colour, or imprint.
- Read handwritten prescriptions.
- Guarantee identification of every medicine or every packaging style.
- Depend on pre-registered image targets for every medicine.
- Require cloud connectivity for the core scan-and-read flow.
- Upload camera images or medicine history by default.
- Generate medical claims or patient-facing medical text freely with a generative model.
- Promise universal multilingual support.
- Require AR glasses or other head-worn hardware.
- Include caregiver cloud sharing or refill/prescription scheduling integration.
- Treat a recognition result as proof of authenticity or product safety.

## 11. Success Metrics and Acceptance Targets

All numerical targets below are **proposed starting targets from the technical reference**, not achieved results. Confirm them with representative testing.

| Area | Success metric | Initial target / acceptance direction |
|---|---|---|
| Speed | Time from stable framing to first result | Median ≤ 3 seconds on a mid-range phone |
| Identification | Top-1 accuracy on supported medicines in decent conditions | ≥ 90%, reported separately for strips, cartons, and bottles |
| Safety | False-confident rate (wrong result announced automatically) | ≤ 1%; prioritise this over raw accuracy |
| OCR | Character/word error rate and exact-match accuracy for name, strength, and expiry | Measure on a labelled test set; compare single-frame and multi-frame approaches |
| Extraction | Precision, recall, and F1 by field; expiry parse accuracy | Report per field on an annotated test set |
| AR stability | Overlay jitter while the object is still | ≤ 3 px RMS in a controlled tripod test |
| Rendering | AR/UI rendering frame rate while perception runs | ≥ 30 FPS; perception must not block rendering |
| Offline | Core scan, match, expiry, and speech flow without network | Must work offline on the supported device configuration |
| Privacy | Default network behaviour | No image leaves the device by default |
| Accessibility | Caption contrast, size, touch targets, screen-reader coverage | Contrast ≥ 7:1; captions start at ≥ 24 sp-equivalent; targets ≥ 48 dp |
| Reliability | End-to-end flow | Complete scan, present result, save, and reminder setup without crash on the test-device matrix |
| Accessibility usability | Task success, time on task, errors, user feedback | Measure with older/low-vision users where feasible and ethically approved; label simulated impairment clearly |
| Resources | Battery drain, peak temperature, memory use | Measure per device tier and per five minutes of scanning |

### Definition of a safe successful scan
A scan is successful only when the system either:
1. presents a candidate that passes the high-confidence gate and the verification checks; or
2. asks for explicit confirmation; or
3. honestly abstains and guides a rescan/manual fallback.

A plausible but wrong medicine announced as certain is a safety failure even if the UI appears to work.

## 12. High-Level User Workflow

1. **Open app:** The app explains readiness and starts the AR session.
2. **Aim:** The user holds one medicine package about 20–30 cm from the camera, adjusting as prompted.
3. **Guide:** The app measures framing, text size, glare, steadiness, and tracking; it provides short spoken/haptic cues.
4. **Stabilise:** When the view is sufficiently stable for roughly 1–1.5 seconds, the app triggers recognition automatically.
5. **Recognise:** The perception worker performs quality checks, OCR/barcode reading where available, and multi-frame fusion.
6. **Extract:** The app parses name, strength, composition, expiry, and other visible fields.
7. **Verify:** It retrieves candidates from the offline database and cross-checks name, strength, composition, form, and available manufacturer/barcode evidence.
8. **Gate:** High confidence presents a “reads as” result; medium confidence requests confirmation; low confidence prompts a targeted rescan.
9. **Present:** The app shows AR panels when tracking is valid and speaks the summary with synchronised captions and haptic confirmation.
10. **Interact:** The user can repeat the summary, request details, save the result, or scan another item.
11. **Save:** If requested, the app stores relevant scan metadata locally and schedules expiry reminders.
12. **Recover:** If tracking or speech fails, the app explains the problem and falls back to accessible 2D/text/haptic presentation where possible.

## 13. Product Constraints and Key Design Decisions

- **Project delivery scope:** This is a one-week academic prototype/MVP, not a production-ready medical application. Prioritise a demonstrable vertical slice, honest limitations, basic testing, and clear documentation over exhaustive coverage or production infrastructure.
- **Primary test device:** Samsung Galaxy F13. Verify ARCore support and the actual device/OS configuration early. If the device cannot run the required ARCore features, preserve the recognition and accessible 2D flow and use a compatible ARCore-supported device for AR validation if available.
- **Android version:** Target the minimum Android version supported by the selected ARCore, ML Kit, and TTS implementation and available on the Galaxy F13. Confirm the installed OS and SDK compatibility during project setup rather than assuming a version in advance.
- **MVP platform:** Native Android is the recommended shortest path to a working prototype. Unity + AR Foundation is an alternative if required by the course; native iOS is a later port option.
- **Suggested Android stack:** Kotlin, Jetpack Compose, ARCore, ML Kit Text Recognition and Barcode Scanning, OpenCV, SQLite/Room, Android TextToSpeech, WorkManager, and encrypted local storage.
- **Separate execution loops:** Rendering should run at 30–60 Hz; perception can run asynchronously at approximately 3–10 Hz depending on device; accessibility is event-driven. OCR must never block rendering.
- **Capture-time pose:** Store camera pose/intrinsics with each submitted frame and use that capture-time state when the OCR result returns.
- **AR anchors:** Use world-locked mode for a stationary object and object-following mode for a hand-held object. Do not assume a world anchor follows a moving strip.
- **Knowledge base:** For the one-week prototype, start with a small, clearly labelled demo dataset (for example, 20–50 medicine product records) and expand only if time allows. Use public, reusable sources where possible, such as India's National List of Essential Medicines for choosing generic ingredients, official package inserts/labels where accessible, and reputable open drug-label resources for general monograph fields. These sources do not automatically provide a complete Indian brand-to-composition database. Check each source's reuse terms, record source URLs and access dates, and do not present unverified community data as authoritative. If a field cannot be verified, omit it or mark it unavailable. The app must clearly state that its database is limited and for academic demonstration only.
- **Medical content governance:** Every monograph field should retain its source, source date, reviewer, and database version. Verify reuse licences for every data source.
- **Privacy:** Local processing and local encrypted history by default; optional cloud fallback only with explicit per-use consent.
- **Scope control:** Custom detection, learned extraction, multilingual output, cloud reading, and iOS should not delay the core MVP.

## 14. MVP Release Checklist

The academic prototype is ready for demonstration when the applicable core items below are true. Advanced AR and reminder items are stretch goals for the one-week build:

- [ ] Live camera and ARCore session start and recover from interruptions.
- [ ] Quality gating prevents obviously blurred/glared frames from being treated as reliable input.
- [ ] On-device OCR reads the supported test set and records field-level evaluation results.
- [ ] Multi-frame fusion is implemented and compared against single-frame OCR, if time permits; otherwise the limitation is documented.
- [ ] Name, strength, composition, and expiry extraction have automated tests.
- [ ] Matching verifies composition and strength and has a tested confidence/abstention gate.
- [ ] Low-confidence and look-alike cases never appear as certain results.
- [ ] Label information is sourced from the curated database only.
- [ ] Expiry parsing and month-end handling have unit tests.
- [ ] A simple AR overlay works if device support and time permit; otherwise a usable 2D fallback is demonstrated.
- [ ] Late OCR results use pose-at-capture-time compensation.
- [ ] Spoken guidance, result speech, captions, haptics, and screen-reader labels work together without overlapping speech.
- [ ] Scan history is local; thumbnails are off by default; delete-history control works.
- [ ] Expiry reminders survive device restart, if reminders are implemented within the available time.
- [ ] Offline end-to-end flow is tested.
- [ ] Safety disclaimer and unknown/unreadable outcomes are present.
- [ ] Test results include false-confident rate, latency, AR stability, accessibility, battery, and thermal measurements.

## 15. Implementation Decisions and Practical Data Plan

The following decisions reflect the project's academic scope and one-week prototype deadline. The goal is a demonstrable, safety-conscious proof of concept—not a production-ready medical application.

### 15.1 Confirmed project decisions

1. **Primary test device:** Samsung Galaxy F13. Confirm ARCore compatibility and the required camera/Android APIs on the actual device before committing to the full AR implementation. If the device cannot run the required ARCore configuration reliably, preserve the core OCR and accessible result flow and use the 2D fallback.
2. **Medicine database:** Begin with a small, manually curated academic test set rather than attempting to build a comprehensive medicine database. Start with approximately 20–30 common, clearly labelled packaged medicines that can be lawfully photographed or otherwise accessed for testing. Expand only if time permits.
3. **Review and authority:** This is an academic prototype and no formal medical authority or reviewer is currently available. Therefore, do not claim that the medicine content is clinically validated or professionally approved. Use traceable public references where permitted, record the source and access date for each entry, and clearly label the database as an educational prototype dataset. If a field cannot be supported by a trustworthy source, omit it rather than invent it.
4. **Barcode scanning:** Excluded from the MVP/prototype. Identification will use printed label text and the OCR pipeline.
5. **Minimum Android version:** Target **Android 8.0 (API 26) or higher as the initial planning baseline**, subject to compatibility checks for the selected ARCore release, libraries, and the Galaxy F13. This is a practical starting decision, not a guarantee that every feature or library works on every device. If the actual development environment or dependency requirements demand a higher minimum, document the change.
6. **Delivery deadline:** One week. Prioritise a narrow vertical slice that works from camera capture to OCR, a cautious match against a small local dataset, and accessible text/speech output. Do not attempt the full 12-week roadmap in this deadline.
7. **Cloud fallback:** Excluded from the MVP. Keep processing local and avoid sending images or medicine history to a server.

### 15.2 Medicine-list and information-source recommendation

For this academic prototype, **do not scrape commercial medicine portals or copy a dataset without checking its licence and terms**. Use a small, auditable test dataset.

Recommended approach:

- Start with approximately 20–30 medicine packages that are readily available to the team for controlled testing, such as common generic products with legible printed names, strengths, and expiry dates. The selection is for testing coverage, not a recommendation to use these medicines.
- Prefer generic ingredient names and label fields that can be checked against official public product labels or regulator/publisher information. Depending on the field and intended market, candidate starting points include official package inserts/leaflets, public regulator documents, and recognised essential-medicines lists for selecting generic ingredients.
- Use public drug-label resources such as DailyMed/openFDA only as supplementary references when appropriate; they are US-oriented and may not match Indian brand packaging or local label directions.
- Verify reuse terms and attribution requirements for each source before copying content into the app. Do not assume that a publicly viewable page or community dataset is automatically reusable.
- Store source title or URL, access date, relevant field(s), and a dataset version alongside each record. Keep the source notes in the project repository.
- For the prototype, prioritise identity fields (brand/name, strength, composition/form) and expiry parsing from the package in the camera view. Only display purpose, label directions, warnings, or side effects when the team has a traceable source for that exact information.
- If no trustworthy source or review is available for a medicine fact, show that the information is unavailable. Do not invent medical content or use generative AI to fill the gap.

**Important limitation:** A small, manually curated dataset is suitable for demonstrating the technical pipeline, but it is not a complete or clinically validated medicine reference. The prototype must communicate this limitation and must not be used to make medication decisions.

### 15.3 One-week prototype priority

The implementation order should be:

1. **Day 1 — Project setup and feasibility:** Check the Galaxy F13, Android Studio, camera permissions, and ARCore compatibility. Create the app shell and collect a small, consent-aware set of package images/videos.
2. **Day 2 — Camera and OCR baseline:** Display the live camera preview and run on-device OCR on captured frames. Save test transcripts for repeatable evaluation.
3. **Day 3 — Parsing and small local dataset:** Extract medicine name, strength, and expiry where possible. Build a small local JSON or SQLite dataset and add explicit unknown/unmatched behaviour.
4. **Day 4 — Matching and expiry:** Add conservative candidate matching, strength checks, expiry parsing, and the rule that uncertain matches must not be announced as certain.
5. **Day 5 — Accessible result flow:** Add large readable result text, Android text-to-speech, repeat/scan-again controls, and basic screen-reader labels.
6. **Day 6 — AR or fallback:** If ARCore works reliably on the test device, add a simple AR overlay. Otherwise, complete a reliable 2D overlay/HUD rather than risking the entire prototype on unstable spatial tracking.
7. **Day 7 — Test and demo:** Test known packages, unknown packages, unreadable expiry, poor lighting, and ambiguous names. Fix crashes and document limitations and measured results.

This is a prioritised plan, not a guarantee that every advanced feature can be completed in seven days. If time is short, preserve the core camera → OCR → cautious match → accessible result flow and treat stable SLAM anchoring, multi-frame fusion, haptic guidance, and reminders as stretch goals.

### 15.4 Prototype scope clarification

This project is an academic proof of concept. It is **not required to be production-ready**. The prototype should demonstrate the central idea and honestly document its limitations. It does not need to deliver a comprehensive medicine catalogue, clinical validation, cloud infrastructure, a custom trained detector, full multilingual support, a production-grade security review, or a polished cross-platform release.

## 16. Reference Basis and Assumptions

This PRD is based on the supplied **AR_Medicine_Identifier_Deep_Technical_Context.pdf**, especially:
- Pages 4–6: executive summary, user needs, functional requirements, and proposed non-functional targets.
- Pages 7–15: design critique, layered architecture, recognition pipeline, AR positioning, and failure handling.
- Pages 16–19: accessibility, end-to-end workflow, edge cases, history, and expiry reminders.
- Pages 21–26: knowledge-base strategy, recommended stack, module structure, roadmap, and MVP cut line.
- Pages 27–28: evaluation, safety, privacy, and risk register.
- Pages 32–33: data schemas, matching/expiry algorithms, and UI/accessibility starting specifications.

Where the source provides proposed targets or heuristic thresholds, this document labels them as initial targets rather than proven results. Project-owner decisions have been incorporated into Section 15. The medicine dataset, source licensing, and device-specific ARCore compatibility still require practical verification during the build; do not silently assume those are solved.
