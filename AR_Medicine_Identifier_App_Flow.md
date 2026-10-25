# Document 03 — App Flow: Navigation & User Journey Map

**Project:** SLAM-Based AR Medicine Identifier with Voice-Guided Accessibility Mode  
**Document version:** 1.0  
**Project type:** Academic Android prototype  
**Target device:** Samsung Galaxy F13  
**Scope:** One-week proof of concept; no login, backend, barcode scanning, or cloud fallback

## 1. Purpose

This document maps the screens and transitions for the prototype so the app is implemented as one connected user journey rather than a collection of isolated screens.

The central flow is:

**Launch → Permissions / readiness → Scan guidance → Camera + OCR → Parse and match → Confidence gate → Result / confirmation / rescan → Optional save → Scan again or exit**

The app must never present an uncertain medicine match as a fact. When recognition fails, the user should receive a clear explanation and a next action.

## 2. Navigation Type

Use a simple **single-activity, Compose-based navigation flow**.

- No sidebar, account navigation, or bottom tab bar is needed for the prototype.
- The main navigation is a small set of screens with clear primary actions.
- During scanning, use a camera view with a minimal guidance overlay.
- Present medicine details in an accessible result screen. If ARCore works reliably, show a simple AR panel as a parallel presentation layer; keep the same information available in normal Compose UI.
- Use the standard Android back action where it is safe and predictable.
- Do not auto-dismiss the result screen or automatically begin another scan after a result.

## 3. Screen / Page List

| ID | Screen | Purpose | Main actions |
|---|---|---|---|
| S01 | Welcome / Safety Notice | Explain the academic prototype, its limits, and the need to verify medicine information | **Start scanning**, **Read disclaimer again** |
| S02 | Permission / Readiness | Request camera permission and check OCR/TTS readiness; AR readiness is conditional | **Allow camera**, **Continue without speech** if TTS is unavailable, **Retry** |
| S03 | Scan Guidance | Explain how to frame the package and what the app can read | **Begin scan**, **Hear instructions again**, **Back** |
| S04 | Live Scan / Camera | Show camera preview, framing guidance, and scan state | **Capture / Scan**, **Stop**, **Help** |
| S05 | Recognising | Show a short progress state while OCR, extraction, and matching run | **Cancel / Rescan** |
| S06 | Review OCR Text (conditional) | Optional screen for checking raw OCR text or correcting obvious recognition errors if implemented | **Confirm text**, **Retake photo**, **Back** |
| S07 | Candidate Confirmation | Ask the user to confirm when the match is medium confidence or multiple candidates are close | **Confirm candidate**, **Choose another**, **Rescan**, **Not my medicine** |
| S08 | Medicine Result | Show the matched identity, available sourced label facts, and expiry status | **Listen / Repeat**, **More details**, **Save scan** (optional), **Scan another**, **Back** |
| S09 | Unknown / Uncertain Result | Explain that the app could not identify the medicine reliably | **Rescan**, **Review OCR text** if implemented, **Return to camera** |
| S10 | Expiry Detail (optional section or dialog) | Explain the expiry date read from the current package and the status calculation | **Hear expiry again**, **Close** |
| S11 | Saved Scans (stretch) | Show locally saved scan metadata | **Open scan**, **Delete entry**, **Clear history** |
| S12 | Settings / Accessibility (minimal) | Provide a few local preferences if time allows | **Speech on/off**, **Speech rate**, **Repeat instructions**, **Clear history** if implemented |
| S13 | Error / Recovery State | Present actionable errors such as permission denied, OCR failure, missing voice, or AR tracking loss | **Retry**, **Open settings** where applicable, **Use 2D mode**, **Back** |

**MVP priority:** S01–S05 and S08–S09 are the core screens. S07 is required for uncertain matches. S06, S10, S11, and S12 may be simplified or deferred if needed to meet the one-week deadline.

## 4. Navigation Structure

### Main path

- **Welcome → Permission/Readiness → Scan Guidance → Live Scan**
- **Live Scan → Recognising → Medicine Result** when the match passes the high-confidence gate.
- **Live Scan → Recognising → Candidate Confirmation** when the result needs explicit confirmation.
- **Live Scan → Recognising → Unknown/Uncertain Result** when text is unreadable, no local record matches, or the confidence gate rejects the result.
- **Medicine Result → Scan Guidance / Live Scan** when the user chooses **Scan another**.
- **Medicine Result → Saved Scans** only if local history is implemented and the user chooses **Save scan** or opens history.
- Any scanning state may route to **Error/Recovery** when a permission, camera, OCR, TTS, or AR issue blocks that action.

### Navigation rules

1. Keep one active scan session at a time.
2. When a new scan starts, clear transient OCR text, candidate lists, and prior result overlays so data from the previous package cannot leak into the new result.
3. Back from a result returns to the prior safe screen; **Scan another** deliberately starts a new scan and clears the old scan state.
4. If the user cancels recognition, discard the pending result and return to Live Scan.
5. If AR tracking is unavailable or lost, retain the result in the 2D Compose screen; do not force the user back to the start.
6. A result is not saved unless the user explicitly chooses to save it.
7. Do not require an account or network connection at any point.

## 5. First Screen / Entry Points

### First launch
1. Show **Welcome / Safety Notice**.
2. User taps **Start scanning**.
3. Show camera permission request and readiness checks.
4. After permission is granted, show **Scan Guidance**.
5. User taps **Begin scan** to open Live Scan.

### Returning launch
- If camera permission is already granted, show the Welcome screen or a concise home screen with **Start scanning**. For the simplest implementation, always open Welcome and retain no previous camera session.
- Never start the camera or speech automatically before the user chooses to begin.

### Permission outcomes
- **Granted:** continue to Scan Guidance.
- **Denied once:** explain why camera access is needed and offer **Try again**.
- **Permanently denied:** show instructions to enable camera permission in Android Settings, plus **Back** or **Exit**.
- **TTS unavailable:** allow the visual flow to continue, clearly indicate that speech is unavailable, and keep all information visible as text.
- **ARCore unsupported / unavailable:** offer the 2D scan/result mode if camera/OCR can still run.

## 6. Core User Journey 1 — Identify a Supported Medicine

1. User opens the app and sees the Welcome / Safety Notice.
2. User taps **Start scanning**.
3. App checks camera permission and basic readiness.
4. User grants permission and sees Scan Guidance.
5. User taps **Begin scan**.
6. App opens the live camera view and displays concise framing instructions.
7. User points the phone at a medicine strip, carton, or bottle label and captures a usable frame (automatic dwell-triggering is optional; a manual capture button is acceptable for the prototype).
8. App shows Recognising while OCR and matching run asynchronously.
9. App extracts likely name, strength, and expiry tokens from OCR text.
10. App compares the extracted identity with the small local dataset.
11. If the candidate passes the configured confidence and verification checks, app opens Medicine Result.
12. Result screen shows the matched name, strength, the recognised expiry and its status if readable, and only those additional label facts supported by the local dataset.
13. App speaks a concise summary if TTS is available; the same content remains readable on screen.
14. User may tap **Listen / Repeat**, **More details**, **Save scan** if implemented, or **Scan another**.
15. If the user chooses **Scan another**, clear prior scan data and return to Live Scan or Scan Guidance.

## 7. Core User Journey 2 — Uncertain, Unknown, or Unreadable Medicine

1. User follows the normal launch and scan path.
2. OCR returns no stable medicine name, the medicine is absent from the dataset, the strength conflicts, or multiple candidates are too similar.
3. App does not automatically announce a candidate as certain.
4. If there are plausible candidates with adequate evidence, open Candidate Confirmation and present the candidates with their readable identity fields.
5. User may confirm a candidate only after comparing it with the printed package, choose another candidate, or choose **Rescan**.
6. If the user cannot confidently verify a candidate, route to Unknown / Uncertain Result.
7. The screen says what failed where possible (for example, “I couldn't read the strength” or “This medicine isn't in the prototype dataset”) and gives a targeted next action.
8. User taps **Rescan** to return to Live Scan.
9. After repeated failure, keep the unknown state available; do not loop indefinitely or pressure the user into choosing a candidate.
10. If expiry is unreadable, the app shows **Unable to read expiry** separately from medicine identity. It must not infer that the package is in date.

## 8. Core User Journey 3 — Expiry Status and Accessible Output

1. User scans a package with a legible printed expiry date.
2. OCR output is parsed by the expiry parser independently of the medicine-name matcher.
3. If the date is valid and unambiguous, app calculates the status using the documented month/year handling rule.
4. Result screen displays the expiry date and a text status such as **Expired**, **Expires soon**, **Caution**, or **OK**. Do not rely on colour alone.
5. App speaks the expiry date and status in the summary if TTS is available.
6. User taps **Listen / Repeat** to hear it again, or uses the screen reader to access the same information.
7. If the date is ambiguous or cannot be parsed, show and speak (when available) that the expiry could not be read reliably.

## 9. Button and Action Behaviour

| Current screen | Action | Expected behaviour / destination |
|---|---|---|
| Welcome | Start scanning | Request/check camera permission, then go to Scan Guidance or Permission/Readiness error |
| Welcome | Read disclaimer again | Speak or display the safety notice; remain on Welcome |
| Permission/Readiness | Allow camera | Invoke Android permission request; continue only after permission is granted |
| Permission/Readiness | Retry | Recheck permission/readiness; remain with actionable error if still blocked |
| Scan Guidance | Begin scan | Open Live Scan and initialise capture |
| Scan Guidance | Hear instructions again | Speak the short framing instructions; remain on screen |
| Live Scan | Capture / Scan | Submit a usable image to OCR and navigate to Recognising |
| Live Scan | Help | Show or speak framing tips; return to Live Scan |
| Live Scan | Stop / Back | Stop camera resources safely and return to Scan Guidance |
| Recognising | Cancel / Rescan | Cancel pending work if possible, clear pending state, return to Live Scan |
| Recognising | Success | Route by confidence gate to Result, Confirmation, or Unknown |
| Candidate Confirmation | Confirm candidate | Recheck required fields; show result marked as user-confirmed, not algorithmically certain |
| Candidate Confirmation | Choose another | Update selected candidate and re-run checks |
| Candidate Confirmation | Rescan | Clear candidates and return to Live Scan |
| Candidate Confirmation | Not my medicine | Route to Unknown / Uncertain Result |
| Medicine Result | Listen / Repeat | Speak the result summary without overlapping existing speech |
| Medicine Result | More details | Expand or navigate to the available sourced label details; no new medical claims |
| Medicine Result | Save scan | Save metadata locally only if history is implemented; otherwise hide/disable this action |
| Medicine Result | Scan another | Stop speech, clear current scan state, and return to Live Scan |
| Medicine Result | Back | Return to the prior safe screen; do not silently begin another scan |
| Unknown / Uncertain | Rescan | Clear pending OCR/candidate data and return to Live Scan |
| Unknown / Uncertain | Review OCR text | Open optional Review OCR Text screen if implemented; otherwise remain on unknown state |
| Expiry Detail | Close | Return to Medicine Result |
| Saved Scans | Open scan | Show the saved record, clearly labelled as a previous scan |
| Saved Scans | Delete entry | Ask for confirmation if practical, delete local metadata, update list |
| Settings | Speech on/off or rate | Apply preference and keep it local |
| Error / Recovery | Retry | Reinitialise the failed component and retry once; retain actionable error if it fails again |
| Error / Recovery | Use 2D mode | Keep accessible results available without AR tracking |
| Any screen | Exit / system Back | Stop TTS and release camera/AR resources when leaving the relevant flow |

## 10. Empty, Loading, and Error States

### Empty states
- **No medicines in local dataset:** Show a setup/development warning; do not claim that all medicines are unsupported because the dataset is empty.
- **No saved scans:** Show “No saved scans yet” and a **Start scanning** action. Hide this screen if history is not implemented.
- **No additional details:** Show “Additional label information is not available in this prototype dataset.”
- **No OCR text:** Show “No readable text found. Improve lighting, reduce glare, and try again.”
- **No match:** Show “Medicine not found in the prototype dataset.”

### Loading states
- **Initialising camera/AR:** Show “Starting camera…” with a retry action if it takes too long.
- **Recognising:** Show “Reading label…” and avoid duplicate scan submissions.
- **Loading dataset:** For a small bundled JSON file, loading should be brief; show a basic loading state if needed.
- **Starting TTS:** If speech is not ready, preserve visual access and show a non-blocking message.

### Error states
- **Camera permission denied:** Explain that camera access is required to scan; offer retry or Android Settings guidance.
- **Camera unavailable / interrupted:** Stop capture safely, show a retry action, and do not crash.
- **OCR failure:** Show a retry/rescan action and preserve a diagnostic message for testing.
- **Unreadable name or strength:** Ask the user to recapture or review OCR; do not make a confident match.
- **Ambiguous expiry:** Show “Unable to read expiry” and do not infer a status.
- **Medicine not in dataset:** Explain the dataset limitation and offer rescan.
- **TTS unavailable:** Keep all information visible, allow the user to continue, and offer retry if useful.
- **AR tracking lost:** Switch to 2D presentation; do not hide the result or block the user.
- **Local dataset malformed/missing:** Show a clear development error and log a diagnostic; do not crash at startup if a fallback screen can be shown.
- **Unexpected exception:** Show a generic recovery message and a retry/rescan action. Do not expose stack traces to end users.

## 11. Modal, Overlay, and Speech Behaviour

- **Safety notice:** Can be a normal first screen rather than a modal, so screen readers can read it in sequence.
- **Camera permission:** Use the Android system permission dialog.
- **Candidate confirmation:** Prefer a dedicated accessible screen rather than a crowded modal, especially for low-vision users.
- **AR overlay:** Secondary to the standard UI; every result must also be available as normal Compose text and through the accessibility tree.
- **Help tips:** A small Compose dialog or expandable section is acceptable. It must have a clear close action.
- **Expiry detail:** Use an expandable section or simple dialog, not a required navigation detour.
- **Speech:** New result speech should stop or queue behind current speech predictably. The **Stop** or repeat action must not create overlapping TTS utterances.
- **System interruption:** Pause or stop speech and camera processing appropriately when the app is backgrounded or a permission/system screen opens.

## 12. Redirect / Routing Logic

There is no signup/login flow because the prototype has no accounts.

- **App launch → Welcome.**
- **Start scanning → Permission/Readiness → Scan Guidance** when ready.
- **Begin scan → Live Scan.**
- **Capture → Recognising.**
- **High-confidence, verified match → Medicine Result.**
- **Medium-confidence or close candidates → Candidate Confirmation.**
- **Low confidence, no match, or unusable OCR → Unknown / Uncertain Result.**
- **AR unsupported/lost → Keep current screen and use 2D presentation.**
- **Scan another → Clear current state → Live Scan.**
- **Save scan → Local history only, if implemented.**
- **Camera denied permanently → Permission instructions; no camera loop.**
- **TTS unavailable → Continue with visual text.**
- **Exit app → Stop TTS and release camera/AR resources.**

## 13. State Model for Implementation

Keep the scanning flow explicit. A minimal state model could use:

```kotlin
enum class ScanUiState {
    Welcome,
    CheckingReadiness,
    ScanGuidance,
    LiveScan,
    Recognising,
    ReviewOcr,
    ConfirmCandidate,
    Result,
    Unknown,
    Error
}
```

The enum is a conceptual starting point; the implementation may use a sealed class or navigation library. Do not use a screen state alone to represent the confidence outcome. Keep the recognition result separately typed, for example:

- `HighConfidence(candidate, parsedFields)`
- `NeedsConfirmation(candidates, parsedFields, reason)`
- `Unmatched(parsedFields, reason)`
- `Unreadable(reason)`

The UI should render the appropriate destination from the recognition outcome, not from a raw similarity score alone.

## 14. One-Week Build Priorities

### Must implement first
1. Welcome / safety notice.
2. Camera permission and readiness.
3. Live camera capture.
4. OCR and visible raw text.
5. Name/strength/expiry parsing.
6. Small local dataset and cautious matching.
7. Result, confirmation, and unknown states.
8. Accessible on-screen text and TTS.
9. Rescan/retry and safe cleanup.

### Implement if time permits
- Simple ARCore overlay and tracking-loss fallback.
- OCR review/edit screen.
- Speech-rate settings.
- Local scan history.
- Expiry reminders.
- Automatic point-and-hold trigger, haptic guidance, and multi-frame fusion.

Do not let optional features delay the core end-to-end scan flow.

## 15. Prototype Safety Notice

Display a concise notice such as:

> “This is an academic prototype. It may misread medicine packaging and its dataset is limited. Verify all medicine details with the package and a pharmacist or doctor. Do not use this app to decide whether to start, stop, or change a medicine or dose.”

The notice is part of the product flow, not a substitute for conservative recognition behaviour.

## 16. Source Basis

This flow follows the supplied `AR_Medicine_Identifier_Deep_Technical_Context.pdf`, especially its end-to-end workflow, accessibility-first guidance, confidence-gated recognition, AR tracking recovery, local history, expiry handling, and prototype cut-line guidance. Screen names and navigation transitions are implementation-level design decisions derived from that workflow. Features marked optional are narrowed for the user's one-week academic prototype and are not claims that they already exist in the code.
