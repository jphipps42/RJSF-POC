# RJSF Pre-Award Review System - Test Case Documentation

**Date:** 2026-04-13
**Test Suite Location:** `Testing/`
**Total Test Cases:** 60
**Last Run Result:** All 60 passing

---

## How to Run

| Command | Scope |
|---|---|
| `cd Testing && mvn test` | Unit tests only (33 tests, ~1s) |
| `cd Testing && mvn verify` | Unit + Integration + E2E (60 tests, ~2 min) |
| `cd Testing && mvn verify -De2e.headless=false` | Same, with visible Chrome browser |

**Prerequisites for Integration & E2E tests:**
- PostgreSQL running at `localhost:5432` (via `docker start rjsf_postgres`)
- Spring Boot service running at `localhost:3001`
- React dev server running at `localhost:5173` (E2E only)

---

## 1. Unit Tests (33 tests)

Unit tests use Mockito for isolation — no database or server required.

### 1.1 FormSubmissionService (`unit/service/FormSubmissionServiceTest.java`)

Tests the core form submission service that manages draft saves, section submissions, resets, and transformer sync.

| # | Test Name | What It Tests |
|---|---|---|
| 1 | saves draft and updates section status to in_progress | Calling `saveDraft()` with a sectionId sets that section's status to `in_progress`, pins the schema version, and persists the form data |
| 2 | throws EntityNotFoundException for missing submission | `saveDraft()` with an unknown UUID throws a 404-equivalent exception |
| 3 | throws IllegalStateException when section is locked | `saveDraft()` on a section with status `submitted` rejects the save with a 403-equivalent exception |
| 4 | throws IllegalStateException when entire submission is locked | `saveDraft()` with no sectionId on a locked submission rejects the save |
| 5 | calls transformer sync after save | After `saveDraft()`, the `SubmissionWriteService.writeSection()` is called with the correct template formId (`pre-award-overview` for the overview section) |
| 6 | transformer failure does not prevent JSONB save | If `writeSection()` throws a RuntimeException, the JSONB save still completes successfully (fail-safe behavior) |
| 7 | submits a single section and marks it as submitted | `submit()` with a sectionId sets that section to `submitted` while leaving overall status as `in_progress` |
| 8 | locks submission when all sections are submitted | `submit()` on the last remaining unsubmitted section sets overall status to `submitted`, `is_locked` to true, and stamps `submittedAt` / `completionDate` |
| 9 | resets a single section to in_progress for editing | `reset()` with a sectionId changes that section from `submitted` to `in_progress` and unlocks the submission |
| 10 | resets entire submission - unlocks all submitted sections | `reset()` with no sectionId changes all sections to `in_progress`, clears `submittedAt`, and unlocks |

### 1.2 PathResolver (`unit/transformer/PathResolverTest.java`)

Tests the dot-notation path resolver used by the transformer to extract/set values in nested Map structures.

| # | Test Name | What It Tests |
|---|---|---|
| 11 | get() retrieves top-level value | `get("pi_budget", data)` returns the value at the top level |
| 12 | get() retrieves nested value via dot notation | `get("personal.firstName", data)` traverses nested maps |
| 13 | get() returns empty for missing path | `get("nonexistent", data)` returns `Optional.empty()` |
| 14 | get() returns empty for missing nested path | `get("personal.email", data)` returns empty when the nested key doesn't exist |
| 15 | set() creates top-level value | `set("pi_budget", 200000, data)` adds a top-level key |
| 16 | set() creates nested value with auto-vivification | `set("personal.firstName", "Alex", data)` creates intermediate maps as needed |
| 17 | set() overwrites existing value | `set("pi_budget", 999, data)` replaces an existing value |

### 1.3 TypeCoercionService (`unit/transformer/TypeCoercionServiceTest.java`)

Tests the type conversion service that coerces Java values to SQL-compatible types for the transformer pipeline.

| # | Test Name | What It Tests |
|---|---|---|
| 18 | coerces string to TEXT | Number `42` converts to String `"42"` |
| 19 | coerces string to INTEGER | String `"42"` converts to Integer `42` |
| 20 | coerces number to INTEGER | Double `42.7` truncates to Integer `42` |
| 21 | coerces string to NUMERIC (BigDecimal) | String `"145000.50"` converts to `BigDecimal` |
| 22 | coerces string to BOOLEAN | `"true"` / `"false"` strings convert to Boolean |
| 23 | coerces number to BOOLEAN | `1` → true, `0` → false |
| 24 | coerces ISO string to DATE | `"2025-05-01"` converts to `LocalDate` |
| 25 | coerces Map to JSONB as PGobject | A Map is serialized to a `PGobject` with type `jsonb` for PostgreSQL compatibility |
| 26 | coerces List to JSONB as PGobject | A List is serialized to a `PGobject` with type `jsonb` |
| 27 | coerces string to UUID | UUID string is parsed and validated via `UUID.fromString()` |
| 28 | returns null for null input | Null value passes through as null regardless of target type |

### 1.4 TransformerTemplate (`unit/transformer/TransformerTemplateTest.java`)

Tests loading and querying of transformer template JSON files that define the relational table mappings.

| # | Test Name | What It Tests |
|---|---|---|
| 29 | loads overview template from classpath | `pre-award-overview.transformer.json` deserializes correctly with formId, tableName, version, and 12+ fields |
| 30 | fieldsBySection() returns all fields when sectionId is null | Passing `null` returns the complete field list (used for full-form writes) |
| 31 | fieldsBySection() filters by tag | Passing `"human_no_regulatory"` returns only fields tagged with that section, and no overlap with `"human_anatomical"` fields |
| 32 | fieldsBySection() returns empty for unknown section | Passing `"nonexistent_section"` returns an empty list |
| 33 | all 6 templates have correct table names and formIds | Validates all 6 template files (`pre-award-overview`, `pre-award-safety`, `pre-award-animal`, `pre-award-human`, `pre-award-acquisition`, `pre-award-final`) load with correct formId, tableName, non-empty fields, and required audit/schema config |

---

## 2. Integration Tests (14 tests)

Integration tests run against the live PostgreSQL database via Spring `MockMvc`. They test the full request-response cycle through the Spring Boot controllers, services, repositories, and database.

### 2.1 Award API (`integration/AwardApiIntegrationTest.java`)

| # | Test Name | What It Tests |
|---|---|---|
| 34 | GET /api/awards returns seeded award list | The awards endpoint returns a list containing the seeded `TE020005` award |
| 35 | GET /api/awards/by-log/{logNumber} returns award with submissions and personnel | Loading award `TE020005` returns the full `AwardDetailDto` with 1 composite submission and 2+ personnel records |
| 36 | GET /api/awards/by-log/{logNumber} returns 404 for unknown log | Requesting a non-existent log number returns HTTP 404 |
| 37 | POST /api/auth/login succeeds with seeded user jphipps/test | Login with username `jphipps` and password `test` returns HTTP 200 with user details (role: SO, display_name: Joshua Phipps) |
| 38 | POST /api/auth/login fails with wrong password | Login with incorrect password returns HTTP 401 |

### 2.2 Form Submission API (`integration/FormSubmissionApiIntegrationTest.java`)

| # | Test Name | What It Tests |
|---|---|---|
| 39 | Fetch seeded submission ID via award lookup | Retrieves the composite submission UUID from the seeded award data |
| 40 | PUT /save?section=overview saves draft and sets section status | Saving overview data returns HTTP 200 with status `in_progress`, section_status `in_progress`, and the saved form_data values echoed back |
| 41 | PUT /save?section=safety_review saves safety data (resets if locked) | Resets safety section first if previously submitted, then saves safety data successfully |
| 42 | GET /form-submissions/{id} returns submission with schema | Fetching a submission by ID returns the full DTO including `json_schema` for rendering |
| 43 | GET /form-submissions/{id} returns 404 for unknown ID | Requesting a non-existent submission UUID returns HTTP 404 |

### 2.3 Transformer Relational Sync (`integration/TransformerSyncIntegrationTest.java`)

Tests that saving form sections via the API triggers the transformer to write typed columns into the per-section relational tables.

| # | Test Name | What It Tests |
|---|---|---|
| 44 | Setup: fetch seeded submission and award IDs | Precondition — retrieves the submission and award UUIDs needed by subsequent tests |
| 45 | Overview save writes to form_pre_award_overview table | After saving overview data via the API, a direct JDBC query confirms `pi_budget` and `program_manager` are written to the `form_pre_award_overview` relational table keyed by `award_id` |
| 46 | Safety save writes to form_pre_award_safety table | After saving safety data, JDBC confirms `safety_q1`, `safety_q2`, and `safety_notes` are in the `form_pre_award_safety` table |
| 47 | Subsequent saves update the same row (upsert) | Saving overview a second time updates the existing row (count stays at 1) rather than inserting a duplicate |

---

## 3. End-to-End Tests (13 tests)

E2E tests use Selenium WebDriver with Chrome to test the full user flow through the React UI, Spring Boot API, and PostgreSQL database. Each test logs in as the seeded user `jphipps` / `test`.

### 3.1 Review Page Navigation (`e2e/ReviewPageE2ETest.java`)

| # | Test Name | What It Tests |
|---|---|---|
| 48 | Login and page loads with award summary | Navigates to login page, enters credentials, submits, verifies redirect to review page showing "Pre-Award" header and "Joshua Phipps" username |
| 49 | Overview section is expanded by default and shows PI Budget | Verifies the overview accordion is pre-expanded on page load with "PI Budget" and "Program Manager" fields visible |
| 50 | Safety section shows correct question text from POC | Expands Section A and verifies the exact question text matches the POC specification: "Programmatic Record of Environmental Compliance", "Army-provided infectious agents", "Biological Select Agents or Toxins" |
| 51 | Animal section shows correct question text | Expands Section B and verifies "Animals used?" question text |
| 52 | Human Review group header is visible and expandable | Expands Section C group header and verifies subsection titles appear (Regulatory Review, Anatomical, Secondary Use, etc.) |
| 53 | Acquisition group header is visible and expandable | Expands Section D group header and verifies subsection titles appear (Budget Review, Peer, Statement of Work, Data Management) |
| 54 | Final Recommendation section shows correct question text | Expands Final Recommendation and verifies "scientific overlap" and "RISG" text is present |
| 55 | Logged-in user display name appears in header | Verifies "Joshua Phipps" appears in the global header after login |

### 3.2 Form Save and Submit (`e2e/FormSaveE2ETest.java`)

| # | Test Name | What It Tests |
|---|---|---|
| 56 | Save Draft button appears in Safety section | Resets safety section via API, expands it, and verifies at least one visible "Save Draft" button exists |
| 57 | Saving section data persists to backend | Saves safety data via the REST API directly, then loads the UI and verifies the section shows "In Progress" status |
| 58 | Submit button shows confirmation dialog | Clicks the "Submit to Safety Office" button and verifies a confirmation dialog appears, then cancels |
| 59 | Data persists across page reload | Saves a radio selection via API reset + UI interaction, reloads the page, re-expands the section, and verifies a radio button is still checked |
| 60 | Unauthenticated access redirects to login page | Navigates directly to `/review/TE020005` without logging in and verifies redirect to `/login` |

---

## Test Architecture

```
Testing/
├── pom.xml                              # Maven config with Selenium + Spring Boot Test dependencies
└── src/test/
    ├── java/com/egs/rjsf/
    │   ├── unit/
    │   │   ├── service/
    │   │   │   └── FormSubmissionServiceTest.java    (10 tests)
    │   │   └── transformer/
    │   │       ├── PathResolverTest.java              (7 tests)
    │   │       ├── TypeCoercionServiceTest.java       (11 tests)
    │   │       └── TransformerTemplateTest.java        (5 tests)
    │   ├── integration/
    │   │   ├── AwardApiIntegrationTest.java           (5 tests)
    │   │   ├── FormSubmissionApiIntegrationTest.java   (5 tests)
    │   │   └── TransformerSyncIntegrationTest.java     (4 tests)
    │   └── e2e/
    │       ├── BaseE2ETest.java                        (shared login/accordion helpers)
    │       ├── ReviewPageE2ETest.java                  (8 tests)
    │       └── FormSaveE2ETest.java                    (5 tests)
    └── resources/
        └── application-test.yml                        (test configuration)
```

### Test Isolation Strategy

| Tier | Database | Server | Isolation |
|---|---|---|---|
| **Unit** | None (mocked) | None | Full isolation via Mockito |
| **Integration** | Live PostgreSQL (localhost:5432) | Embedded Spring MockMvc | Tests reset sections before modifying to handle prior state |
| **E2E** | Live PostgreSQL | Live Spring Boot + Vite | Tests reset sections via REST API before interacting via browser |

### Key Design Decisions

- **No Testcontainers** — Integration tests use the existing Docker PostgreSQL container rather than spinning up ephemeral containers, since Docker Desktop on macOS has compatibility issues with Testcontainers
- **Idempotent tests** — Integration and E2E tests reset sections via the `/reset` API before testing saves, making them safe to run repeatedly
- **Headless Chrome** — E2E tests default to headless mode; pass `-De2e.headless=false` to see the browser
- **Accordion handling** — E2E tests use `aria-expanded` attribute detection to avoid toggling already-expanded sections
- **Credentials** — All tests use the seeded user `jphipps` / `test` (role: SO, org: CDMRP) from `init.sql`
