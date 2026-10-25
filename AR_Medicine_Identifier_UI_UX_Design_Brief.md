# Document 04 — UI/UX Design Brief: Visual & Interaction Design Guide

**Project:** SLAM-Based AR Medicine Identifier with Voice-Guided Accessibility Mode  
**Document version:** 1.0  
**Project type:** Academic Android prototype / proof of concept  
**Target device:** Samsung Galaxy F13  
**Design priority:** Accessibility, clarity, trust, and fast recovery over visual effects

## 1. Design Purpose

The interface should help someone identify and understand readable information on a medicine package without requiring them to read small print or precisely operate a complex camera UI. The visual experience should be calm, uncluttered, and trustworthy. It should support speech and screen readers as first-class ways to use the app, not as optional add-ons.

The app should feel like a focused accessibility utility, not a social app, shopping app, or clinical dashboard. It should communicate uncertainty honestly and never make an uncertain medicine match look definitive.

## 2. Overall Aesthetic Direction

**Direction:** Minimal, calm, modern, high-contrast, accessibility-first Android utility.

Design principles:
- **Clarity before decoration:** Keep the camera view and result information easy to understand.
- **One primary action per screen:** Make the next step obvious.
- **Low cognitive load:** Use short labels, plain language, and limited choices.
- **Trust without false certainty:** Use explicit text states such as “Match needs confirmation” and “Unable to read expiry.”
- **Consistent feedback:** Loading, success, uncertainty, and failure must have both clear text and an appropriate visual treatment.
- **Accessible in motion:** The user may be holding a phone and medicine package at the same time, so controls should be large and spaced apart.
- **AR is secondary:** Any AR panel should reinforce the result, while the same content remains available in normal UI text and the accessibility tree.

Avoid flashy gradients, excessive animation, tiny icon-only buttons, dense data tables on phone screens, and decorative 3D effects that compete with the camera view.

## 3. Color Palette

Use a restrained, high-contrast palette. These are design tokens for the prototype and should be checked against actual text/background combinations during implementation.

| Token | Hex value | Usage |
|---|---|---|
| Primary | `#145DA0` | Main buttons, selected controls, key navigation accents |
| Primary dark | `#0B3C68` | Pressed states and dark-surface accents |
| Secondary | `#00796B` | Secondary actions and neutral positive accents |
| Background light | `#F7F9FC` | Main light-mode background |
| Surface light | `#FFFFFF` | Cards, result panels, dialogs |
| Text primary | `#17212B` | Main text on light surfaces |
| Text secondary | `#44515E` | Supporting text on light surfaces |
| Border | `#CBD5E1` | Dividers, outlines, input boundaries |
| Dark background | `#101820` | Main dark-mode background |
| Dark surface | `#18232D` | Cards and panels in dark mode |
| Dark text | `#F3F6F9` | Main text on dark surfaces |
| Dark text secondary | `#C4CFD8` | Supporting text on dark surfaces |
| Error | `#B3261E` | Error text and error-state icon |
| Warning | `#8A4B00` | Expiry caution and warning state |
| Success | `#176B45` | Positive status, paired with text/icon |
| Focus outline | `#005FCC` | Visible keyboard/switch-access focus indicator |

### Status colour rules

- Never communicate expiry status by colour alone. Always show a text label and, where useful, a distinct icon.
- Use **Expired**, **Expires soon**, **Caution**, **OK**, and **Unable to read expiry** as explicit statuses.
- Ensure status text meets contrast requirements against its surface.
- Avoid using green as a substitute for proof that a medicine is safe or appropriate to take. “OK” means only that the printed expiry date was parsed and is not within the configured warning window; it is not a medical safety judgement.
- Verify colour contrast in both light and dark modes. The table gives starting tokens, not a substitute for contrast testing.

## 4. Typography

- **Preferred font:** Android system sans-serif / Roboto. Do not add a custom font dependency for the prototype.
- **Headings:** Medium or semibold weight; short and descriptive.
- **Body text:** Regular weight, plain language, generous line spacing.
- **Medicine name:** Visually prominent and allowed to wrap onto multiple lines.
- **Strength and expiry:** Separate rows with explicit labels; do not rely on position or colour alone.
- **Supporting content:** Smaller than the main result but still comfortably readable.
- **Speech captions:** Start at 24 sp-equivalent where practical; allow Android font scaling and multi-line wrapping.
- **General body text:** Start around 18 sp for primary instructions and 16 sp for secondary information; test on the actual phone.
- **Touch target labels:** Prefer text plus icon instead of icon-only controls.

Suggested type scale:

| Role | Starting size | Weight |
|---|---:|---|
| Main screen title | 28 sp | Semibold |
| Result medicine name | 26 sp | Bold / semibold |
| Section heading | 20 sp | Semibold |
| Primary instruction | 18 sp | Medium |
| Body / label value | 16–18 sp | Regular |
| Caption / status | 16–24 sp depending on importance | Medium |
| Small metadata | 14 sp minimum where unavoidable | Regular |

Do not hard-code text to a fixed pixel size. Use Android `sp` units and support system font scaling. Avoid truncating medicine names, strength values, expiry dates, or warning text.

## 5. Component Style

- **Corner radius:** 12–16 dp for cards and major panels; 10–12 dp for buttons and fields.
- **Buttons:** Large, clearly labelled, full-width primary action when appropriate. Minimum 48 dp high; prefer 56 dp for the main scanning actions.
- **Cards:** Flat or very subtle elevation. Use cards to group identity, expiry, and label details without creating a dense dashboard.
- **Borders:** Use thin, visible outlines where they help distinguish controls and surfaces.
- **Shadows:** Minimal. Avoid heavy drop shadows that make the interface visually noisy.
- **Icons:** Use familiar Material-style icons with text labels for important actions.
- **Motion:** Short, purposeful transitions only. Avoid pulsing, flashing, or unnecessary movement that could distract or be uncomfortable.
- **Feedback:** Use text and icons for success, warnings, and errors; haptics may supplement feedback if implemented.
- **Dialogs:** Reserve for brief, interruptive decisions. Use a full screen for candidate confirmation when comparing medicine identities.
- **Camera overlays:** Keep the viewfinder mostly unobstructed. Put guidance near the edge of the screen and ensure it is also spoken when speech is available.

## 6. Dark Mode / Light Mode

**Preference:** Support system light/dark theme if practical, with both themes readable and tested. If implementing both would jeopardise the one-week deadline, finish one coherent light theme first and keep colour tokens centralised so dark mode can be added consistently.

- Respect Android's system theme where feasible.
- Do not invert colours blindly; choose appropriate surface, text, border, and status colours for each theme.
- Ensure AR panels remain readable over bright, dark, reflective, or visually busy camera backgrounds. Use an opaque or sufficiently solid panel surface behind text.
- Test expiry and error status in both themes if both are implemented.

## 7. Inspiration References

These are broad interaction references, not designs to copy exactly:
- **Google Lookout:** Accessibility-oriented camera guidance and spoken feedback as a useful conceptual reference.
- **Google ML Kit sample apps:** Simple camera/OCR interaction and technical clarity.
- **Material Design 3:** Android components, accessibility patterns, dynamic type, and system theme behaviour.

The supplied project reference does not prescribe specific inspiration apps; these are design suggestions for the prototype. The app should maintain its own medicine-focused identity and should not imply an affiliation with these products.

## 8. Key UI Patterns

### 8.1 Welcome / safety screen
- App name and one-sentence purpose.
- Short, readable academic-prototype disclaimer.
- One dominant **Start scanning** button.
- No login, signup, dashboard, or onboarding carousel.

### 8.2 Scan screen
- Camera preview takes most of the screen.
- Minimal framing guide; avoid complex reticles or tiny controls.
- Short status message: **Move closer**, **Hold steady**, **Reduce glare**, **Reading label**, or **Ready to scan**.
- A large manual capture/scan action for the prototype.
- Help and stop/back actions remain accessible.
- If automatic point-and-hold triggering is implemented later, do not remove a predictable manual path without testing.

### 8.3 Recognition/loading state
- Show a short progress indicator and the text **Reading label…**.
- Prevent repeated capture submissions while the same recognition job is running.
- Provide a cancel/rescan action if it can safely cancel the pending operation.

### 8.4 Medicine result
Display in a stable, predictable order:
1. Medicine name and match status.
2. Strength and dosage form, if known.
3. Printed expiry date and expiry status, if readable.
4. Other sourced label information, if present.
5. Raw OCR text or a **Review what was read** action if implemented.
6. Main actions: **Listen / Repeat**, **More details**, **Scan another**; **Save scan** only if history exists.

Do not show blank sections as empty cards. Hide unavailable optional fields or label them as unavailable.

### 8.5 Candidate confirmation
- Explain why confirmation is needed, such as missing strength or two similar names.
- List candidates with name, strength, and form in clear text.
- Do not preselect a candidate in a way that pressures the user to accept it.
- Provide **This matches the package**, **Choose another**, and **Rescan** actions.
- Keep a route to **None of these** / **Not my medicine**.

### 8.6 Unknown / error state
- Explain the issue without blame.
- State what the app knows and does not know.
- Give one main recovery action, normally **Rescan**.
- Add a short specific tip only when useful, such as reducing glare or moving closer.
- Never replace an unknown state with a guessed result just to make the UI feel complete.

### 8.7 Details / saved history
- Use grouped sections, not a dense table.
- Mark saved results as previous scans and show the scan time.
- Ask before destructive bulk deletion where feasible.
- If history is not implemented in the prototype, remove the related navigation and buttons entirely rather than showing non-functional controls.

## 9. Interaction and Feedback Rules

- Every visible button must perform the action its label promises.
- Keep one primary action per screen and make secondary actions visually quieter.
- Use clear verbs: **Start scanning**, **Capture**, **Rescan**, **Repeat**, **More details**, **Scan another**.
- Do not use vague actions such as **OK** when the action can be described precisely.
- Show immediate feedback when capture starts and when recognition finishes.
- Do not automatically dismiss results.
- Stop or queue speech predictably when the user taps repeat, scans again, navigates away, or the app backgrounds.
- Do not speak the same guidance continuously; rate-limit repeated prompts.
- Ask for confirmation before accepting an ambiguous medicine candidate.
- Make retry possible after recoverable failures.
- Preserve the user's place in the flow when changing between AR presentation and 2D fallback.

## 10. Accessibility Requirements

Accessibility is a core product requirement, not a finishing step.

### Text and contrast
- Target WCAG-style contrast of at least **7:1 for primary text** where practical, particularly for low-vision use; verify actual colour pairs.
- Do not use colour alone to communicate status.
- Support Android system font scaling and display scaling.
- Avoid clipped or truncated medicine names, strengths, dates, and warnings.
- Provide large-print captions synchronised with TTS where feasible.

### Touch and layout
- Minimum touch target: **48 × 48 dp**; prefer **56 × 56 dp** for primary actions.
- Maintain generous spacing between actions to reduce accidental taps.
- Do not make swipe gestures the only way to access an action.
- Avoid time-limited screens and automatically disappearing results.
- Keep primary actions in predictable positions.

### TalkBack and assistive technology
- Use meaningful accessibility labels and roles for all controls.
- Keep AR-rendered text mirrored in normal Compose UI so TalkBack can access it.
- Announce state changes such as **Reading label**, **Match needs confirmation**, and **Unable to read expiry** without flooding the screen reader.
- Ensure candidate lists and result fields are read in a logical order.
- Do not communicate essential information through haptics or sound alone.

### Speech and haptics
- Spoken guidance should be short and actionable.
- Offer repeat and stop controls.
- If TTS is unavailable, preserve the complete visual flow and explain the limitation.
- Haptic signals, if implemented, supplement speech and text rather than replacing them.
- Avoid speech overlap between guidance, loading messages, and result summaries.

## 11. Mobile Layout Requirements

- Design for a portrait phone layout first, especially the Samsung Galaxy F13.
- Use responsive Compose layouts that respect status/navigation bars and system insets.
- Allow vertical scrolling for long medicine details.
- Keep the primary action reachable without requiring precise one-handed tapping.
- Ensure camera controls remain visible over the preview and do not cover the medicine label unnecessarily.
- Support larger font settings without overlapping buttons or clipping result text.
- Test on the actual target device and, if available, one emulator or second Android device.
- The prototype does not require tablet, desktop, web, or iOS layouts.

## 12. AR Overlay Design

AR is an enhancement to the accessible result, not the only place where information exists.

- Use a solid, high-contrast panel behind overlay text.
- Keep the overlay compact: medicine name, strength, expiry status, and a clear action to open the full result.
- Avoid placing text directly over complex or reflective packaging.
- Keep the panel stable when tracking is reliable.
- If tracking is lost, remove or visibly invalidate the stale spatial panel and preserve the result in 2D UI.
- Do not imply that the overlay is attached to the correct physical package if the pose or object association is stale.
- Keep text and actions available through Compose and TalkBack even when the AR renderer is active.
- Do not spend the one-week deadline on elaborate 3D animation or depth effects.

## 13. Example Screen Copy

Use short, plain-language copy. These examples may be adapted, but the safety meaning must remain.

| Situation | Suggested copy |
|---|---|
| Welcome | “Read medicine labels with your camera and hear the information aloud.” |
| Disclaimer | “This is an academic prototype. It may misread packaging. Verify medicine details with a pharmacist or doctor.” |
| Ready | “Point the camera at the printed label.” |
| Move closer | “Move a little closer so the text is easier to read.” |
| Glare | “There is glare on the package. Tilt it slightly.” |
| Hold steady | “Hold the phone steady.” |
| Loading | “Reading label…” |
| High-confidence match | “This label reads as [name], [strength]. Please compare it with the package.” |
| Confirmation | “I found similar matches. Please check the name and strength on the package.” |
| Unknown | “I couldn’t identify this medicine from the prototype dataset.” |
| Expiry unreadable | “I couldn’t read the expiry date reliably.” |
| Expired | “The printed expiry date appears to have passed. Ask a pharmacist about what to do; do not rely on this prototype alone.” |
| TTS unavailable | “Speech is unavailable right now. The result is shown on screen.” |
| AR unavailable | “AR view is unavailable. You can still read the result below.” |

Avoid copy such as “This medicine is safe,” “Take this medicine,” or “Your correct dose is…”.

## 14. Design Tokens and Implementation Guidance

Centralise values instead of scattering raw colours and dimensions across composables.

Suggested token groups:
- `ColorTokens`: primary, background, surface, text, border, error, warning, success, focus.
- `TypeTokens`: title, medicine name, section heading, body, caption.
- `ShapeTokens`: card radius, button radius, dialog radius.
- `SpacingTokens`: small, medium, large, screen padding, inter-control gap.
- `TouchTargetTokens`: standard and primary action minimum sizes.

Use Material 3 theme infrastructure where available. Keep the first implementation small and consistent; do not build a custom design system library for this prototype.

## 15. Design QA Checklist

- [ ] Welcome screen explains the prototype and has one clear primary action.
- [ ] All primary actions have labels and touch targets of at least 48 dp.
- [ ] Result text is readable at default and enlarged system font sizes.
- [ ] Primary text/background combinations meet the intended contrast target.
- [ ] Expiry statuses use text and icons, not colour alone.
- [ ] Unknown and ambiguous results are visually distinct from verified matches.
- [ ] No screen auto-dismisses important results.
- [ ] TTS does not overlap itself and can be repeated/stopped.
- [ ] TalkBack can reach all important result fields and actions.
- [ ] AR overlay text is duplicated in accessible Compose UI.
- [ ] Tracking loss switches to a 2D result instead of hiding information.
- [ ] Buttons for deferred features are removed or visibly disabled with an explanation.
- [ ] The layout works in portrait on the Samsung Galaxy F13.
- [ ] Light/dark mode is coherent if both modes are implemented.
- [ ] No visual treatment implies clinical validation or certainty beyond the recognition evidence.

## 16. Source Basis and Design Assumptions

This brief follows the supplied `AR_Medicine_Identifier_Deep_Technical_Context.pdf`, especially its accessibility-first interaction model, spoken guidance and result output, large-print captions, TalkBack accessibility mirror, high-contrast UI, generous touch targets, non-auto-dismissing results, and AR panel fallback requirements.

The source recommends starting values including approximately 24 sp-equivalent captions, contrast of at least 7:1, touch targets of at least 48 dp (56 dp preferred), and a calm, accessible interaction flow. The colour palette, typography scale, component radii, and inspiration references in this brief are proposed design choices for consistency; they are not specified by the source document and should be validated during implementation.
