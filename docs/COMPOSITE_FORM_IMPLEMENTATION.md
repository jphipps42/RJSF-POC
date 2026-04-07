# Composite Form Implementation Plan (Approach B)

**Status:** Executed  
**Date:** April 7, 2026  
**Scope:** Full-stack refactor — Database, Spring Boot, React Frontend, Transformer Pipeline

---

## Context

The previous architecture used 18 independent `form_configurations`, each with its own `form_submissions` row per award. Each section had its own React component with independent `formData` state, independent RJSF `<Form>` instances, and independent save/submit operations. Per the **rjsf-nested-forms-technical-requirements.docx** (Approach B: Multiple Form Instances with Shared formData), the system was converted to use one unified `formData` object in a parent component, with child `<Form tagName="div">` instances that slice/merge from it. Each section saves independently but always persists the full formData.

This also included cleaning up legacy code, tables, and configurations from the previous per-section architecture.

---

## Phase 1: Database (init.sql Rewrite)

### 1.1 Removed Old Structures
- Removed all 18 individual `form_configurations` INSERT statements (safety_review, animal_review, human_*, acq_*, pre_award_overview)
- Removed `project_personnel` table and its seed data (personnel now embedded in formData)
- Removed the cross-join that created 18 `form_submissions` per award
- Removed all 18 `form_schema_versions` v1 seeds and the 7 v2 seeds
- Removed all `schema_migrations` entries for the old form keys

### 1.2 Added Composite Form Configuration
- Inserted ONE `form_configurations` row: `form_key = 'pre_award_composite'` with a merged JSON Schema containing ALL properties from all sections in a flat namespace
- Property collision resolution — all `notes` fields got section prefixes:
  - `notes` became `overview_notes`, `safety_notes`, `animal_notes`, `human_s1_notes`, `human_has_notes`, `human_ds_notes`, `human_hs_notes`, `human_ost_notes`, `acq_personnel_notes`, `acq_equip_notes`, `acq_travel_notes`, `acq_materials_notes`, `acq_consultant_notes`, `acq_third_party_notes`, `acq_other_direct_notes`, `acq_additional_notes`
  - Comments fields: `acq_peer_comments`, `acq_sow_comments`, `acq_cps_comments`, `acq_ier_comment`, `acq_ier_plan_notes`, `acq_dmp_notes`, `acq_special_notes`
  - Acquisition fields that collided (`included`, `appropriate`, `necessary`, `cost_appropriate`) were prefixed with subsection name (e.g., `acq_equip_included`, `acq_travel_appropriate`)
  - Already-namespaced fields stayed as-is: `safety_q1`-`q8`, `animal_q1`-`q5`, `human_s1_q1`-`q5`, `human_has_q1`-`q7`, etc.
  - New Final Recommendation fields added: `scientific_overlap`, `foreign_involvement`, `risg_approval`, `so_recommendation`, `so_comments`, `gor_recommendation`, `gor_comments`

### 1.3 Added section_status Column
```sql
-- Added to form_submissions table definition
section_status JSONB DEFAULT '{}'::jsonb
```
Tracks per-section status within the single composite submission: `{"overview": "in_progress", "safety_review": "not_started", ...}`. A section is "locked" when its status = `"submitted"`.

### 1.4 Seed Data
- Kept `page_layout` config and its v1/v2 versions unchanged
- Kept `awards` table and demo award unchanged
- Kept `award_linked_files` table unchanged
- Kept `app_users` table and test user unchanged
- Kept `transformer_template_history` table unchanged
- Created ONE `form_submissions` row per demo award with `form_key = 'pre_award_composite'`, pre-populated overview fields from award data + personnel array, `section_status` initialized with all 24 sections (`overview` as `"in_progress"`, all others as `"not_started"`)
- Created ONE `form_schema_versions` v1 row for the composite config

### Files Modified
- `service/src/main/resources/db/init.sql` — full rewrite

---

## Phase 2: Backend Java Changes

### 2.1 Entity Changes
- **`FormSubmission.java`**: Added `sectionStatus` field (`Map<String, Object>`, JSONB column `section_status`)

### 2.2 Service Changes
- **`FormSubmissionService.java`**:
  - `saveDraft(id, formData, sectionId)` — accepts optional section ID, updates `section_status[sectionId]` to `"in_progress"`, saves full formData
  - `submit(id, formData, sectionId)` — updates `section_status[sectionId]` to `"submitted"`, checks if ALL sections are submitted, if so sets overall `status = "submitted"` and `is_locked = true`
  - `reset(id, sectionId)` — if sectionId provided, resets only that section's status; if null, resets entire submission
- **`AwardService.java`**:
  - `create()` — creates ONE submission with `form_key = 'pre_award_composite'` instead of looping over all active configs
  - `buildDetailDto()` — removed `projectPersonnelRepository` usage and `personnel` from DTO (now lives in formData)

### 2.3 Controller Changes
- **`FormSubmissionController.java`**: Added optional `section` query parameter to save/submit/reset endpoints:
  - `PUT /api/form-submissions/{id}/save?section=safety_review`
  - `PUT /api/form-submissions/{id}/submit?section=safety_review`
  - `PUT /api/form-submissions/{id}/reset?section=safety_review`

### 2.4 DTO Changes
- **`SubmissionWithSchemaDto`**: Added `sectionStatus` field
- **`AwardDetailDto`**: Removed `personnel` field (`List<ProjectPersonnel>`)

### Files Modified
- `service/src/main/java/com/egs/rjsf/entity/FormSubmission.java`
- `service/src/main/java/com/egs/rjsf/service/FormSubmissionService.java`
- `service/src/main/java/com/egs/rjsf/service/AwardService.java`
- `service/src/main/java/com/egs/rjsf/controller/FormSubmissionController.java`
- `service/src/main/java/com/egs/rjsf/dto/SubmissionWithSchemaDto.java`
- `service/src/main/java/com/egs/rjsf/dto/AwardDetailDto.java`

### Files Deleted
- `service/src/main/java/com/egs/rjsf/entity/ProjectPersonnel.java`
- `service/src/main/java/com/egs/rjsf/repository/ProjectPersonnelRepository.java`
- `service/src/main/java/com/egs/rjsf/service/PersonnelService.java`
- `service/src/main/java/com/egs/rjsf/controller/PersonnelController.java`

---

## Phase 3: Frontend — Section Schemas & Manifest

### 3.1 Section Schema Files
Created 24 individual JSON Schema files in `client/src/schemas/sections/`, each defining ONLY that section's properties as a standalone `type: "object"` schema with namespaced property names and `x-schemaVersion`/`x-sectionKey` custom properties:

| File | Section |
|------|---------|
| `overview.schema.json` | PI budget, personnel array, program assignments |
| `safety-review.schema.json` | Safety questions q1-q8 |
| `animal-review.schema.json` | Animal research questions q1-q5 + species |
| `human-no-regulatory.schema.json` | Human research not requiring regulatory review |
| `human-anatomical.schema.json` | Human anatomical substances |
| `human-data-secondary.schema.json` | Secondary use of human data |
| `human-subjects.schema.json` | Human subjects interaction/intervention |
| `human-special-topics.schema.json` | Special topics |
| `human-estimated-start.schema.json` | Estimated start date |
| `acq-br-personnel.schema.json` | Budget review — personnel |
| `acq-br-equipment.schema.json` | Budget review — equipment |
| `acq-br-travel.schema.json` | Budget review — travel |
| `acq-br-materials.schema.json` | Budget review — materials |
| `acq-br-consultant.schema.json` | Budget review — consultant |
| `acq-br-third-party.schema.json` | Budget review — 3rd party |
| `acq-br-other-direct.schema.json` | Budget review — other direct costs |
| `acq-br-additional.schema.json` | Budget review — additional concerns |
| `acq-peer-review.schema.json` | Peer and programmatic review |
| `acq-sow-concerns.schema.json` | Statement of work concerns |
| `acq-cps.schema.json` | Current and pending support |
| `acq-ier.schema.json` | Inclusion enrollment report |
| `acq-data-management.schema.json` | Data management plan |
| `acq-special-requirements.schema.json` | Special requirements |
| `final-recommendation.schema.json` | Final recommendation (NEW — converted from manual MUI) |

### 3.2 UI Schema Files
Created 24 matching uiSchema files in `client/src/schemas/ui/` with widget mappings (radio, textarea, checkboxes, select, date, hidden).

### 3.3 Section Manifest
Created `client/src/schemas/section-manifest.json` defining the section hierarchy:
- Overview (standalone)
- A. Safety Review (standalone)
- B. Animal Review (standalone)
- C. Human Research Review (group header with 6 children)
- D. Acquisition/Contracting Review (group header with Budget Review sub-group of 8 + 6 standalone subsections)
- Final Recommendation (standalone)

---

## Phase 4: Frontend — New Components

### 4.1 CompositeForm.jsx (Core Orchestrator)
Implements Approach B from the requirements document:
- Holds single `formData` state shared across all sections
- Uses `formDataRef` to avoid stale closures in async save operations
- `sliceFor(schema, formData)` extracts only the properties each section's schema defines
- `mergeOnChange(sectionId, sectionData)` spreads section changes back into shared formData
- Auto-saves overview section on any personnel mutation (add, edit, delete) via JSON content comparison
- Per-section `dirtyFlags` and `saveStatus` tracking (idle | saving | saved | error)
- `handleSectionSave(sectionId)` validates via `ref.validateForm()`, then PUTs full formData with section param
- `handleSectionSubmit(sectionId)` validates then submits section
- Loads section manifest and renders section tree recursively
- Single-accordion expansion with smooth scroll-into-view

### 4.2 SectionPanel.jsx (Per-Section Wrapper)
Wraps each leaf section in an accordion with:
- RJSF `<Form tagName="div">` to prevent nested `<form>` elements (TI-02)
- `noHtml5Validate` (TI-03), `children={<></>}` to suppress default submit (TI-05)
- `ref` forwarded for programmatic `validateForm()` (TI-04)
- Section title + status badge + dirty/saved/error indicators
- Save Draft and Submit buttons with confirmation dialog
- Locked state display when section status = "submitted"

### 4.3 SectionGroup.jsx (Group Accordion)
Renders group headers (Human Research, Acquisition, Budget Review) as outer accordions containing child sections. Shows aggregate status badge computed from all leaf children.

### 4.4 LinkedFilesPanel.jsx (Extracted from FinalRecommendation)
The file-linking portion of the old FinalRecommendation component, extracted as a standalone component. Uses its own API/table (`award_linked_files`), rendered alongside the RJSF-driven Final Recommendation section.

---

## Phase 5: Frontend — Rewired Existing Files

### 5.1 ReviewPage.jsx — Major Simplification
- Removed imports of ReviewSection, HumanReviewSection, AcquisitionSection, FinalRecommendation, OverviewPanel
- Removed per-section submission filtering (overviewSubmission, standardSubmissions, humanSubmissions, acqSubmissions)
- Removed expandedSection/accordion management (moved into CompositeForm)
- Fetches award (now returns 1 composite submission)
- Renders single `<CompositeForm>` instead of 5+ specialized components
- Kept: layout version selector, right panel, reset modal, notes panels

### 5.2 api.js — Section-Aware Endpoints
- `saveFormDraft(id, formData, sectionId)` — appends `?section=` query param when provided
- `submitForm(id, formData, sectionId)` — appends section param
- `resetFormSubmission(id, sectionId)` — appends section param
- Removed: `getPersonnel`, `addPersonnel`, `updatePersonnel`, `deletePersonnel`

---

## Phase 6: Cleanup — Deleted Files

### Frontend
- `client/src/components/ReviewSection.jsx` — replaced by SectionPanel
- `client/src/components/HumanReviewSection.jsx` — replaced by manifest-driven rendering
- `client/src/components/AcquisitionSection.jsx` — replaced by manifest-driven rendering
- `client/src/components/FinalRecommendation.jsx` — replaced by RJSF schema + LinkedFilesPanel
- `client/src/components/OverviewPanel.jsx` — replaced by CompositeForm overview section

### Backend
- `service/src/main/java/com/egs/rjsf/entity/ProjectPersonnel.java`
- `service/src/main/java/com/egs/rjsf/repository/ProjectPersonnelRepository.java`
- `service/src/main/java/com/egs/rjsf/service/PersonnelService.java`
- `service/src/main/java/com/egs/rjsf/controller/PersonnelController.java`

### Transformer Templates
- Deleted `service/src/main/resources/templates/pre-award-overview.transformer.json` (subsumed by composite)

---

## Phase 7: Composite Transformer Template

Created `service/src/main/resources/templates/pre-award-composite.transformer.json`:
- `formId: "pre-award-composite"`
- `tableName: "form_pre_award_composite"`
- 107 field mappings across all sections
- All fields nullable (sections save independently, partial data is normal)
- `personnel` and `animal_species` and `acq_special_requirements` stored as JSONB
- Scalar fields: TEXT for yes/no/enum, NUMERIC for budgets/scores, DATE for dates, BOOLEAN for checkboxes
- Transforms: `toBigDecimal` for numerics, `toLocalDate`/`toIsoDateString` for dates, `jsonStringify`/`jsonParse` for arrays, `trimString` for text, `toBoolean` for checkboxes

---

## Post-Implementation Fix: Personnel Auto-Save

After initial implementation, personnel inline edits (clicking edit icon, changing fields, clicking check mark) were not persisting to the backend. Root cause: the auto-save logic only triggered when the personnel array length changed (add/delete), not when record content changed (edit).

**Fix:** Changed `mergeOnChange` to compare the full JSON content of the personnel array (`JSON.stringify`) rather than just the array length, so add, edit, and delete all trigger auto-save immediately.

---

## Architecture Summary

### Before (18 Independent Forms)
```
ReviewPage
  ├── OverviewPanel        → form_submissions[pre_award_overview]
  ├── ReviewSection        → form_submissions[safety_review]
  ├── ReviewSection        → form_submissions[animal_review]
  ├── HumanReviewSection   → form_submissions[human_*] (6 rows)
  ├── AcquisitionSection   → form_submissions[acq_*] (14 rows)
  └── FinalRecommendation  → manual MUI controls (no persistence)

18 form_configurations → 18 form_submissions per award
Each section: independent formData state, independent save/submit
```

### After (1 Composite Form)
```
ReviewPage
  └── CompositeForm (shared formData state)
        ├── SectionPanel[overview]              ─┐
        ├── SectionPanel[safety_review]          │
        ├── SectionPanel[animal_review]          │
        ├── SectionGroup[human_review]           │
        │     ├── SectionPanel[human_*] (×6)     ├── 1 form_submissions
        ├── SectionGroup[acquisition]            │   [pre_award_composite]
        │     ├── SectionGroup[budget_review]    │
        │     │     └── SectionPanel[acq_br_*]   │
        │     └── SectionPanel[acq_*] (×6)       │
        ├── SectionPanel[final_recommendation]   │
        └── LinkedFilesPanel                    ─┘

1 form_configuration → 1 form_submission per award
All sections: shared formData, per-section save via ?section= param
```

### Key Technical Decisions
| Decision | Rationale |
|----------|-----------|
| `tagName="div"` on all child Forms | HTML spec prohibits nested `<form>` elements |
| `formDataRef` for async saves | Avoids stale closure bugs with React useState |
| Flat namespace (no nesting) | Direct Transformer compatibility — one property per DB column |
| `section_status` JSONB column | Per-section status tracking within single submission row |
| Personnel auto-save on content change | Matches prior UX where add/edit/delete persisted immediately |
| Section manifest JSON on frontend | Drives hierarchy rendering without backend coupling |

---

## Verification Checklist

- [x] Database: `form_configurations` has 2 rows (`pre_award_composite` + `page_layout`)
- [x] Database: `form_submissions` has 1 row per award with `form_key = 'pre_award_composite'`
- [x] Database: `section_status` JSONB initialized with all 24 sections
- [x] Database: `project_personnel` table no longer exists
- [x] Database: Overview formData pre-populated (PI budget, program manager, 2 personnel)
- [x] API: `GET /api/awards/by-log/TE020005` returns 1 composite submission
- [x] API: Section-aware save/submit/reset endpoints accept `?section=` param
- [x] Spring Boot: Starts cleanly, Hibernate validates schema, all 3 transformer templates loaded
- [x] Frontend: 24 section schemas + 24 uiSchemas + manifest created
- [x] Frontend: CompositeForm renders all sections in correct accordion hierarchy
- [x] Frontend: Personnel auto-saves on add, edit, and delete
