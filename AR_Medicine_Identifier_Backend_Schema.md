# Document 05 — Backend Schema: Data Model & Storage Architecture

**Project:** SLAM-Based AR Medicine Identifier with Voice-Guided Accessibility Mode  
**Document version:** 1.0  
**Project type:** Academic Android prototype / proof of concept  
**Target device:** Samsung Galaxy F13  
**Backend:** None  
**Authentication:** None  
**Persistence plan:** Bundled local JSON first; optional local SQLite/Room only if history is implemented

> **Important scope decision:** This prototype has no server, user accounts, remote database, API routes, Row Level Security (RLS), or cloud file storage. Do not create Supabase/Firebase/backend infrastructure for this version. The schema below defines the local data structures the Android app needs and clearly separates required prototype data from optional persistence.

## 1. Purpose and Design Principles

This document defines the data structures used by the prototype so the implementation remains consistent across OCR, field extraction, local medicine matching, expiry evaluation, accessible result presentation, and optional scan history.

Design principles:

1. **Local-first:** the medicine dataset ships with the app and is read on-device.
2. **No authentication:** there are no user accounts or roles in this academic prototype.
3. **Traceable medicine facts:** every included medical/label fact should have source metadata. Unsupported facts remain absent.
4. **Separate product facts from package observations:** the knowledge-base record describes a medicine product; OCR-derived expiry and batch data describe the particular physical package scanned.
5. **Explicit uncertainty:** the scan result records whether it matched, needs confirmation, is unmatched, or is unreadable.
6. **Data minimisation:** do not save camera images by default. Scan history is optional and should store only what the prototype needs.
7. **No cloud transmission:** camera frames, OCR text, and history must not be uploaded by this version.

## 2. Storage Architecture

### Required for the prototype

**Bundled JSON asset:** `app/src/main/assets/medicines.json`

- Contains approximately 20–30 manually curated academic test records.
- Read-only at runtime.
- Versioned with a `datasetVersion`.
- Loaded by a repository class and used for local matching.
- Contains only facts supported by recorded sources.
- Does not contain package-specific expiry dates.

### Optional if time permits

**SQLite/Room local database:** used only if scan history or structured local persistence is implemented. A JSON dataset is sufficient for the initial medicine catalogue and avoids unnecessary database setup.

No remote database, backend server, API credentials, account table, cloud bucket, or authentication provider is required.

## 3. Entity Model

The prototype has two core logical entities:

1. **MedicineRecord** — a known product/medicine entry in the local reference dataset.
2. **ScanRecord** — the result of a particular camera scan. This is optional persistent storage; the app can also keep it in memory for the first demo.

The OCR text, extracted fields, candidate scores, and confidence-gate outcome may be represented as transient in-memory objects. Do not persist every intermediate frame.

## 4. Logical Table: `medicine_records`

**Physical storage in the MVP:** JSON objects in `medicines.json`.  
**If Room is introduced:** use a `medicine_records` table with the fields below.

| Column | Type | Required | Rules / purpose |
|---|---|---:|---|
| `id` | `TEXT` / Kotlin `String` | Yes | Stable primary key, e.g. `med-0001`; unique within dataset |
| `brand_name` | `TEXT` / nullable `String` | No | Brand printed on package, if present |
| `generic_name` | `TEXT` / nullable `String` | No | Generic ingredient name |
| `strength` | `TEXT` / nullable `String` | No | Printed product strength, including unit; preserve a display form |
| `dosage_form` | `TEXT` / nullable `String` | No | Tablet, capsule, syrup, etc., when supported |
| `manufacturer` | `TEXT` / nullable `String` | No | Manufacturer/marketer if the source supports it |
| `label_purpose` | `TEXT` / nullable `String` | No | General label-level purpose; include only with a traceable source |
| `label_directions` | `TEXT` / nullable `String` | No | Label directions, not personalised dosage advice |
| `warnings_json` | `TEXT` / JSON array | No | Sourced warnings; empty or absent if not supported |
| `common_side_effects_json` | `TEXT` / JSON array | No | Sourced common side effects; omit if not supported |
| `source_title` | `TEXT` | Yes for medical facts | Title/name of source supporting the record |
| `source_url` | `TEXT` / nullable `String` | Recommended | Source URL when available and permitted |
| `source_access_date` | `TEXT` / ISO date | Recommended | `YYYY-MM-DD` |
| `dataset_version` | `TEXT` | Yes | Dataset version, e.g. `0.1` |
| `notes` | `TEXT` / nullable `String` | No | Internal data-curation notes; do not present as medical advice |

### Constraints

- `id` must be unique and stable.
- Do not use a medicine's brand name as its primary key; names can be duplicated or changed.
- A missing field is `null`/absent, not a guessed value.
- Keep source metadata with each record. If sources differ by field, the JSON model may use a `sources` array with a field list per source.
- This table does not include expiry date or batch number as product-level columns because those belong to an individual package scan.

### Example JSON shape

The following is a schema illustration only; it is not a real medicine record and must not be shipped as a valid product entry:

```json
{
  "id": "sample-0001",
  "brandName": null,
  "genericName": null,
  "strength": null,
  "dosageForm": null,
  "manufacturer": null,
  "labelPurpose": null,
  "labelDirections": null,
  "warnings": [],
  "commonSideEffects": [],
  "sourceTitle": "PLACEHOLDER — replace before use",
  "sourceUrl": null,
  "sourceAccessDate": null,
  "datasetVersion": "0.1",
  "notes": "Schema example only; not a medicine entry"
}
```

The app must not treat this placeholder as a matchable record.

## 5. Optional Local Table: `scan_records`

Implement only if local scan history is completed after the core OCR-to-result flow. Otherwise keep scan state in memory and omit history screens/actions.

| Column | Type | Required | Rules / purpose |
|---|---|---:|---|
| `id` | `TEXT` / UUID string | Yes | Primary key generated on device |
| `medicine_record_id` | `TEXT` / nullable | No | Foreign key to `medicine_records.id` when a candidate was selected |
| `scanned_at` | `INTEGER` / epoch milliseconds | Yes | Scan timestamp |
| `raw_ocr_text` | `TEXT` / nullable | No | Full OCR text; sensitive-adjacent, avoid storing unless needed |
| `parsed_name` | `TEXT` / nullable | No | Name extracted from this package |
| `parsed_strength` | `TEXT` / nullable | No | Strength extracted from this package |
| `parsed_expiry_text` | `TEXT` / nullable | No | Raw expiry token read from the package |
| `parsed_expiry_date` | `TEXT` / ISO date or nullable | No | Parsed date; define month-only handling consistently |
| `batch_number` | `TEXT` / nullable | No | Only if read reliably and useful |
| `match_status` | `TEXT` | Yes | Enum: `high_confidence`, `needs_confirmation`, `unmatched`, `unreadable`, `user_confirmed` |
| `match_score` | `REAL` / nullable | No | Diagnostic score; not a medical truth probability unless calibrated |
| `match_reason` | `TEXT` / nullable | No | Brief explanation for testing, e.g. missing strength |
| `user_confirmed` | `INTEGER` / Boolean | Yes | Default false; true only after explicit user action |
| `dataset_version` | `TEXT` | Yes | Dataset version used for the match |
| `thumbnail_path` | `TEXT` / nullable | No | Omit by default; if implemented, use app-private storage and user deletion controls |

### Relationships

- `scan_records.medicine_record_id → medicine_records.id` is a nullable many-to-one relationship.
- A medicine record can be referenced by many scan records.
- A scan may have no medicine record when it is unreadable or unmatched.
- A scan's expiry date and batch number describe the scanned package, not every package of that product.

### Scan history constraints

- Do not persist camera images by default.
- Do not persist scan history unless the user chooses Save.
- If history is implemented, allow deletion of one entry and clearing all entries.
- Use app-private storage. Consider encrypted local storage only if feasible; do not falsely claim encryption if it has not been implemented and tested.
- Avoid storing the raw OCR text if it is not needed for the prototype.

## 6. Transient In-Memory Models

These do not require database tables in the MVP.

### `OcrObservation`

| Field | Type | Purpose |
|---|---|---|
| `rawText` | `String` | Text returned by OCR |
| `lines` | `List<OcrLine>` | Text lines and their bounding boxes |
| `capturedAtMillis` | `Long` | Frame capture time |
| `qualityStatus` | enum | `usable`, `blurred`, `glare`, `low_text`, `unknown` |

### `ParsedLabel`

| Field | Type | Purpose |
|---|---|---|
| `rawOcrText` | `String` | Original OCR output |
| `parsedName` | `String?` | Candidate medicine name |
| `parsedStrength` | `String?` | Candidate strength |
| `parsedComposition` | `String?` | Candidate ingredient/composition |
| `parsedDosageForm` | `String?` | Candidate dosage form |
| `rawExpiryText` | `String?` | Exact token read for expiry |
| `expiryDate` | `LocalDate?` | Parsed date if unambiguous |
| `parsedBatchNumber` | `String?` | Candidate batch/lot |
| `fieldWarnings` | `List<String>` | Parsing uncertainties |

### `MatchOutcome`

Use a typed result rather than returning only a medicine name:

- `HighConfidence(record, parsedLabel, reasons)`
- `NeedsConfirmation(candidates, parsedLabel, reasons)`
- `Unmatched(parsedLabel, reason)`
- `Unreadable(reason, parsedLabel?)`

The confidence score is a ranking signal unless calibrated against a labelled validation set. A high score alone must not override a strength/composition contradiction.

## 7. Keys, Indexes, and Constraints

For the initial JSON implementation:
- Validate unique `id` values when loading the dataset.
- Optionally build in-memory maps keyed by normalised brand and generic names.
- Do not add a database engine only to create indexes for 20–30 records.

If Room/SQLite is added:
- Primary key: `medicine_records.id`.
- Primary key: `scan_records.id`.
- Foreign key: `scan_records.medicine_record_id` references `medicine_records.id`, with `ON DELETE SET NULL` or an equivalent deliberate deletion policy.
- Index `medicine_records.brand_name` and `medicine_records.generic_name` if query performance or filtering requires them.
- Index `scan_records.scanned_at` for newest-first history.
- Consider a normalised name column for lookup rather than relying on case-sensitive display text.
- Enforce valid `match_status` values in application validation; SQLite `CHECK` constraints may be added if appropriate.

## 8. Authentication, Roles, and Access Control

- **Auth provider:** None.
- **User table:** None.
- **Roles:** None.
- **Guests:** Every person using the installed app is a local device user; there is no account distinction.
- **Access model:** The app reads its bundled medicine dataset and, if implemented, writes scan history to app-private local storage.
- **RLS:** Not applicable because there is no remote database.
- **API authorisation:** Not applicable because there are no API endpoints.

Do not add an admin role or a remote data-editing interface. Dataset changes are made by the developer by editing and rebuilding the local asset.

## 9. Sensitive Data and Storage Security

Potentially sensitive or health-adjacent fields include:
- Raw OCR text.
- Scanned medicine name, strength, expiry, and batch number.
- Scan timestamps and scan history.
- Optional captured thumbnails.

For the prototype:
- Keep these data on-device.
- Do not upload them to a server.
- Do not store images unless there is a clear test need and explicit user action.
- Keep files in app-private storage if saved.
- Provide deletion controls if history is implemented.
- Do not log full OCR text or medicine history in production-style logs; use synthetic test data for debugging where possible.
- Do not claim that data are encrypted at rest unless encryption is actually implemented and verified.
- The bundled reference dataset is read-only and does not contain user-specific records.

## 10. File / Media Storage

- **Medicine dataset:** `app/src/main/assets/medicines.json` (bundled, read-only).
- **Test fixtures:** store only synthetic or consented package images in the developer's test resources; do not bundle personal images unintentionally.
- **Runtime images:** in memory only by default.
- **Optional history thumbnails:** app-private internal storage, with explicit save and delete behaviour. This is a stretch feature and can be omitted.
- **No cloud bucket:** no Firebase Storage, Supabase Storage, S3, or similar service.

## 11. Events, Triggers, and Webhooks

There are no webhooks or server-side triggers.

Optional local events:
- `ScanStarted`
- `OcrCompleted`
- `MatchResolved`
- `ConfirmationAccepted`
- `ScanSaved`
- `ScanDeleted`
- `TtsUnavailable`
- `ArTrackingLost`

These may be represented as internal application events or diagnostic logs. Do not add analytics SDKs or transmit event data over the network.

If expiry reminders are implemented later, schedule them locally with WorkManager. They are not part of the core schema or required MVP.

## 12. API Endpoints

**None.** The prototype has no HTTP API, server routes, or remote service calls. Use local repository methods such as:

- `loadMedicineRecords()`
- `findCandidates(parsedLabel)`
- `evaluateMatch(parsedLabel, candidates)`
- `parseExpiry(rawExpiryText)`
- `saveScanRecord(record)` — optional
- `getRecentScans()` — optional
- `deleteScanRecord(id)` — optional
- `clearScanHistory()` — optional

These are application-level functions, not network endpoints.

## 13. Data Lifecycle

### Medicine dataset
1. Developer curates records and records source metadata.
2. Dataset is checked for unique IDs and placeholder entries.
3. JSON asset is bundled with the application.
4. Repository loads it on-device.
5. Matching reads from the in-memory dataset.
6. Updates require a code/data change and a new app build for this prototype.

### Scan
1. Camera image is processed in memory.
2. OCR output is parsed into a `ParsedLabel`.
3. Matching returns a typed `MatchOutcome`.
4. UI presents result, confirmation, or unknown state.
5. Transient data is discarded when the user starts a new scan or exits, unless the user explicitly saves a scan and history is implemented.

## 14. Schema Validation and Test Requirements

- [ ] Dataset parses successfully at app startup.
- [ ] Every real medicine record has a unique stable ID.
- [ ] Placeholder/sample schema records cannot be treated as real medicines.
- [ ] Medical facts have source metadata or are omitted.
- [ ] Missing optional fields do not crash the UI.
- [ ] Unmatched scans can be represented without a medicine foreign key.
- [ ] Expiry date and batch number are stored per scan, not per product record.
- [ ] Match outcomes distinguish high confidence, confirmation required, unmatched, and unreadable.
- [ ] Deleting a saved scan works if history is implemented.
- [ ] Camera images are not persisted by default.
- [ ] No HTTP requests are required for the core flow.
- [ ] No authentication or RLS code is added to this version.

## 15. Explicitly Out of Scope

- Remote PostgreSQL, Supabase, Firebase, or other cloud database.
- User accounts, authentication, user roles, and RLS.
- Backend API routes and webhooks.
- Cloud image/media storage.
- Cloud OCR or AI fallback.
- Multi-device sync and caregiver sharing.
- Server-managed medicine catalogue updates.
- Mandatory scan history or encrypted thumbnail storage.
- Production security certification.

## 16. Source Basis and Decisions

This document follows the supplied `AR_Medicine_Identifier_Deep_Technical_Context.pdf`, especially its offline curated knowledge base, suggested SQLite/Room model, scan-history considerations, data schemas, source traceability, and privacy principles.

The reference proposes a prebuilt SQLite knowledge base and encrypted local history as the broader architecture. For the user's one-week academic prototype, this document deliberately starts with a bundled JSON dataset and makes Room-based scan history optional. Authentication, RLS, APIs, webhooks, and cloud storage are not applicable to the chosen offline-only architecture.
