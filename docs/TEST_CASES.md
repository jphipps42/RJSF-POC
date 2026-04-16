# RJSF Pre-Award Review System - Test Case Documentation

**Date:** 2026-04-16
**Test Suite Location:** `Testing/`
**Total Test Cases:** 84
**Last Run Result:** All 84 passing (70 unit + 14 integration)

---

## How to Run

| Command | Scope |
|---|---|
| `cd Testing && mvn test` | Unit tests only (70 tests, ~2s) |
| `cd Testing && mvn verify` | Unit + Integration + E2E (84+ tests, ~2 min) |
| `cd Testing && mvn verify -De2e.headless=false` | Same, with visible Chrome browser |
| `cd Testing && mvn failsafe:integration-test -Dit.test=SyncModeToggleIntegrationTest` | Sync toggle tests only |

**Prerequisites for Integration & E2E tests:**
- PostgreSQL running at `localhost:5432` (via `docker start rjsf_postgres`)
- Spring Boot service running at `localhost:3001`
- React dev server running at `localhost:5173` (E2E only)

---

## 1. Unit Tests (70 tests)

Unit tests use Mockito for isolation — no database or server required.

### 1.1 FormSubmissionService (`unit/service/FormSubmissionServiceTest.java`)

Tests the core form submission service that manages draft saves, section submissions, resets, and relational sync strategy delegation.

| # | Test Name | What It Tests |
|---|---|---|
| 1 | saves draft and updates section status to in_progress | Calling `saveDraft()` with a sectionId sets that section's status to `in_progress`, pins the schema version, and persists the form data |
| 2 | throws EntityNotFoundException for missing submission | `saveDraft()` with an unknown UUID throws a 404-equivalent exception |
| 3 | throws IllegalStateException when section is locked | `saveDraft()` on a section with status `submitted` rejects the save with a 403-equivalent exception |
| 4 | throws IllegalStateException when entire submission is locked | `saveDraft()` with no sectionId on a locked submission rejects the save |
| 5 | calls relational sync strategy after save | After `saveDraft()`, the active strategy's `writeSection()` is called with the correct template formId (`pre-award-overview` for the overview section) |
| 6 | relational sync failure does not prevent JSONB save | If `writeSection()` throws a RuntimeException, the JSONB save still completes successfully (fail-safe behavior) |
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
| 23 | coerces number to BOOLEAN | `1` -> true, `0` -> false |
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
| 33 | all 6 templates have correct table names and formIds | Validates all 6 template files load with correct formId, tableName, non-empty fields, and required audit/schema config |

### 1.5 FormDataExtractor (`unit/sync/FormDataExtractorTest.java`) -- NEW

Tests the utility that safely extracts typed values from `Map<String, Object>` form data for the POJO sync strategy.

| # | Test Name | What It Tests |
|---|---|---|
| 34 | getString() extracts string value | Returns the string value for an existing key |
| 35 | getString() returns null for missing key | Returns null when the key does not exist |
| 36 | getString() converts non-string to string | Integer `42` converts to `"42"` |
| 37 | getString() strips whitespace | `"  padded  "` becomes `"padded"` |
| 38 | getBigDecimal() extracts from integer | Integer `750000` becomes `BigDecimal("750000")` |
| 39 | getBigDecimal() extracts from double | Double `3.14` becomes `BigDecimal("3.14")` |
| 40 | getBigDecimal() extracts from string | String `"999.99"` parses to BigDecimal |
| 41 | getBigDecimal() returns null for missing key | Returns null when key is absent |
| 42 | getBigDecimal() returns null for non-numeric string | `"not_a_number"` returns null instead of throwing |
| 43 | getBoolean() extracts true boolean | Java `true` returns `true` |
| 44 | getBoolean() extracts false boolean | Java `false` returns `false` |
| 45 | getBoolean() parses string 'true' | String `"true"` returns `true` |
| 46 | getBoolean() treats non-zero number as true | Integer `1` returns `true` |
| 47 | getBoolean() treats zero as false | Integer `0` returns `false` |
| 48 | getBoolean() returns null for missing key | Returns null when key is absent |
| 49 | getLocalDate() parses ISO date string | `"2025-03-15"` becomes `LocalDate(2025, 3, 15)` |
| 50 | getLocalDate() returns null for invalid date | `"not-a-date"` returns null |
| 51 | getLocalDate() returns null for missing key | Returns null when key is absent |
| 52 | getJsonb() returns map as-is | Map value is returned by identity |
| 53 | getJsonb() returns list as-is | List value is returned by identity |
| 54 | getJsonb() returns null for missing key | Returns null when key is absent |

### 1.6 RelationalSyncStrategyManager (`unit/sync/RelationalSyncStrategyManagerTest.java`) -- NEW

Tests the runtime strategy manager that controls switching between MAPPER and POJO sync modes.

| # | Test Name | What It Tests |
|---|---|---|
| 55 | defaults to MAPPER mode | On construction, active mode is `MAPPER` and `getActiveStrategy()` returns the mapper |
| 56 | switches to POJO mode | `setActiveMode("POJO")` changes active strategy to the POJO implementation |
| 57 | switches back to MAPPER from POJO | Toggling back to `MAPPER` restores the original strategy |
| 58 | accepts lowercase mode name | `setActiveMode("pojo")` is accepted and normalized to `"POJO"` |
| 59 | throws for unknown mode | `setActiveMode("UNKNOWN")` throws `IllegalArgumentException` |
| 60 | setting same mode is a no-op | Setting `MAPPER` when already in `MAPPER` mode does not error |

### 1.7 PojoSyncStrategy (`unit/sync/PojoSyncStrategyTest.java`) -- NEW

Tests the JPA-based sync strategy that writes form data via entity classes and Spring Data repositories.

| # | Test Name | What It Tests |
|---|---|---|
| 61 | getName() returns POJO | Strategy self-identifies as `"POJO"` |
| 62 | Overview: creates new entity when none exists for award | First save creates a new `FormPreAwardOverview` with correct field values, schema version, and submitted_by |
| 63 | Overview: updates existing entity | Subsequent save finds and updates the existing entity without creating a duplicate |
| 64 | Safety: maps all safety fields | All 10 safety fields are correctly mapped from form data to entity setters |
| 65 | Human: only sets no_regulatory fields when sectionId is human_no_regulatory | Saving `human_no_regulatory` sets boolean and question fields without touching other subsections' data |
| 66 | Human: only sets anatomical fields when sectionId is human_anatomical | Saving `human_anatomical` sets its fields while preserving pre-existing `human_no_regulatory` data |
| 67 | Acquisition: only sets personnel fields when sectionId is acq_br_personnel | Personnel subsection fields are set; other subsection fields remain null |
| 68 | Acquisition: handles peer review score as BigDecimal | Double `8.5` is correctly converted to `BigDecimal("8.5")` for the NUMERIC column |
| 69 | Final Recommendation: maps all final recommendation fields | All 7 final recommendation fields are correctly mapped |
| 70 | unknown formId logs warning and does not throw | Passing `"unknown-form"` does not throw an exception |

---

## 2. Integration Tests (14 + 14 = 28 tests)

Integration tests run against the live PostgreSQL database via Spring `MockMvc`. They test the full request-response cycle through the Spring Boot controllers, services, repositories, and database.

### 2.1 Award API (`integration/AwardApiIntegrationTest.java`)

| # | Test Name | What It Tests |
|---|---|---|
| 71 | GET /api/awards returns seeded award list | The awards endpoint returns a list containing the seeded `TE020005` award |
| 72 | GET /api/awards/by-log/{logNumber} returns award with submissions and personnel | Loading award `TE020005` returns the full `AwardDetailDto` with 1 composite submission and 2+ personnel records |
| 73 | GET /api/awards/by-log/{logNumber} returns 404 for unknown log | Requesting a non-existent log number returns HTTP 404 |
| 74 | POST /api/auth/login succeeds with seeded user jphipps/test | Login with username `jphipps` and password `test` returns HTTP 200 with user details |
| 75 | POST /api/auth/login fails with wrong password | Login with incorrect password returns HTTP 401 |

### 2.2 Form Submission API (`integration/FormSubmissionApiIntegrationTest.java`)

| # | Test Name | What It Tests |
|---|---|---|
| 76 | Fetch seeded submission ID via award lookup | Retrieves the composite submission UUID from the seeded award data |
| 77 | PUT /save?section=overview saves draft and sets section status | Saving overview data returns HTTP 200 with status `in_progress` and the saved form_data values |
| 78 | PUT /save?section=safety_review saves safety data | Resets safety section first if previously submitted, then saves safety data successfully |
| 79 | GET /form-submissions/{id} returns submission with schema | Fetching a submission by ID returns the full DTO including `json_schema` |
| 80 | GET /form-submissions/{id} returns 404 for unknown ID | Requesting a non-existent submission UUID returns HTTP 404 |

### 2.3 Transformer Relational Sync (`integration/TransformerSyncIntegrationTest.java`)

| # | Test Name | What It Tests |
|---|---|---|
| 81 | Setup: fetch seeded submission and award IDs | Precondition -- retrieves the submission and award UUIDs |
| 82 | Overview save writes to form_pre_award_overview table | JDBC confirms `pi_budget` and `program_manager` in the relational table |
| 83 | Safety save writes to form_pre_award_safety table | JDBC confirms `safety_q1`, `safety_q2`, and `safety_notes` |
| 84 | Subsequent saves update the same row (upsert) | Row count stays at 1 after multiple saves |

### 2.4 Sync Mode Toggle -- End-to-End (`integration/SyncModeToggleIntegrationTest.java`) -- NEW

Tests the full MAPPER/POJO toggle flow: REST API mode switching, both write paths, section isolation, and mid-session toggling.

| # | Test Name | What It Tests |
|---|---|---|
| 85 | Setup: fetch seeded submission and award IDs | Retrieves IDs and ensures MAPPER mode |
| 86 | GET /api/sync-mode returns current mode | Endpoint returns `{"mode": "MAPPER"}` |
| 87 | PUT /api/sync-mode switches to POJO | Mode changes to POJO and persists across subsequent GET |
| 88 | PUT /api/sync-mode switches back to MAPPER | Mode reverts to MAPPER successfully |
| 89 | PUT /api/sync-mode rejects unknown mode | Invalid mode returns 4xx error |
| 90 | MAPPER: overview save writes to relational table | Transformer pipeline writes `pi_budget=111111` and `program_manager="Mapper PM"` via SqlExecutor |
| 91 | MAPPER: safety save writes to relational table | Transformer pipeline writes `safety_q1="yes"` and `safety_notes="Mapper note"` |
| 92 | POJO: overview save writes to relational table | JPA entity writes `pi_budget=222222` and `program_manager="POJO PM"` via Hibernate |
| 93 | POJO: safety save writes to relational table | JPA entity writes `safety_q1="no"`, `safety_q3="yes"`, `safety_notes="POJO note"` |
| 94 | POJO: human subsection save does not clobber other subsections | Saving `human_anatomical` after `human_no_regulatory` preserves both subsections' data in `form_pre_award_human` |
| 95 | POJO: final recommendation save writes to relational table | JPA entity writes `scientific_overlap`, `so_recommendation`, and `gor_comments` |
| 96 | Toggle: MAPPER writes, then POJO updates the same row | MAPPER inserts row, POJO updates it; row count stays 1, values reflect POJO write |
| 97 | Toggle: POJO writes, then MAPPER updates the same row | POJO inserts, MAPPER overwrites; values reflect MAPPER write |
| 98 | Cleanup: restore MAPPER mode | Ensures MAPPER mode is active after test suite completes |

---

## 3. End-to-End Tests (13 tests)

E2E tests use Selenium WebDriver with Chrome to test the full user flow through the React UI, Spring Boot API, and PostgreSQL database. Each test logs in as the seeded user `jphipps` / `test`.

### 3.1 Review Page Navigation (`e2e/ReviewPageE2ETest.java`)

| # | Test Name | What It Tests |
|---|---|---|
| 99 | Login and page loads with award summary | Navigates to login page, enters credentials, submits, verifies redirect to review page |
| 100 | Overview section is expanded by default and shows PI Budget | Verifies the overview accordion is pre-expanded with budget fields visible |
| 101 | Safety section shows correct question text from POC | Expands Section A and verifies exact question text from specification |
| 102 | Animal section shows correct question text | Expands Section B and verifies "Animals used?" text |
| 103 | Human Review group header is visible and expandable | Expands Section C group header and verifies subsection titles |
| 104 | Acquisition group header is visible and expandable | Expands Section D group header and verifies subsection titles |
| 105 | Final Recommendation section shows correct question text | Expands Final Recommendation and verifies question text |
| 106 | Logged-in user display name appears in header | Verifies "Joshua Phipps" in the global header |

### 3.2 Form Save and Submit (`e2e/FormSaveE2ETest.java`)

| # | Test Name | What It Tests |
|---|---|---|
| 107 | Save Draft button appears in Safety section | Resets safety section, expands it, verifies "Save Draft" button |
| 108 | Saving section data persists to backend | Saves via REST API, verifies "In Progress" status in UI |
| 109 | Submit button shows confirmation dialog | Clicks submit, verifies confirmation dialog, then cancels |
| 110 | Data persists across page reload | Saves, reloads, verifies radio button still checked |
| 111 | Unauthenticated access redirects to login page | Direct URL access without login redirects to `/login` |

---

## Test Architecture

```
Testing/
├── pom.xml                              # Maven config with Selenium + Spring Boot Test dependencies
└── src/test/
    ├── java/com/egs/rjsf/
    │   ├── unit/
    │   │   ├── service/
    │   │   │   └── FormSubmissionServiceTest.java      (11 tests)
    │   │   ├── sync/
    │   │   │   ├── FormDataExtractorTest.java          (21 tests) -- NEW
    │   │   │   ├── RelationalSyncStrategyManagerTest.java (6 tests) -- NEW
    │   │   │   └── PojoSyncStrategyTest.java           (10 tests) -- NEW
    │   │   └── transformer/
    │   │       ├── PathResolverTest.java                (7 tests)
    │   │       ├── TypeCoercionServiceTest.java         (11 tests)
    │   │       └── TransformerTemplateTest.java          (5 tests)
    │   ├── integration/
    │   │   ├── AwardApiIntegrationTest.java             (5 tests)
    │   │   ├── FormSubmissionApiIntegrationTest.java     (5 tests)
    │   │   ├── TransformerSyncIntegrationTest.java       (4 tests)
    │   │   └── SyncModeToggleIntegrationTest.java       (14 tests) -- NEW
    │   └── e2e/
    │       ├── BaseE2ETest.java                          (shared login/accordion helpers)
    │       ├── ReviewPageE2ETest.java                    (8 tests)
    │       └── FormSaveE2ETest.java                      (5 tests)
    └── resources/
        └── application-test.yml                          (test configuration)
```

### Test Isolation Strategy

| Tier | Database | Server | Isolation |
|---|---|---|---|
| **Unit** | None (mocked) | None | Full isolation via Mockito |
| **Integration** | Live PostgreSQL (localhost:5432) | Embedded Spring MockMvc | Tests reset sections before modifying to handle prior state |
| **E2E** | Live PostgreSQL | Live Spring Boot + Vite | Tests reset sections via REST API before interacting via browser |

### Key Design Decisions

- **No Testcontainers** -- Integration tests use the existing Docker PostgreSQL container rather than spinning up ephemeral containers
- **Idempotent tests** -- Integration and E2E tests reset sections via the `/reset` API before testing saves, making them safe to run repeatedly
- **Headless Chrome** -- E2E tests default to headless mode; pass `-De2e.headless=false` to see the browser
- **Strategy Pattern Testing** -- Unit tests mock both strategies independently; integration tests verify end-to-end through the real database with both MAPPER and POJO code paths
- **Section Isolation Verification** -- Integration tests confirm that saving one subsection (e.g., `human_anatomical`) does not overwrite another subsection's data (e.g., `human_no_regulatory`) in multi-section tables
