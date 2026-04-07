# Pre-Award Overview Form — Implementation Record

**Date:** 2026-04-07
**Status:** Implemented

---

## 1. Objective

Convert the Pre-Award Overview panel from a hardcoded UI component that reads/writes directly to the `awards` table into a proper RJSF-driven form backed by the form submission system, with project personnel embedded as an array field rendered as a data grid.

## 2. Starting State

The `OverviewPanel` component read award metadata fields (`pi_budget`, `final_recommended_budget`, `program_manager`, `contract_grants_specialist`, `branch_chief`, `prime_award_type`) directly from the `awards` table via `AwardController`. Personnel were managed as a separate CRUD table against the `project_personnel` table with inline edit/delete and an add modal. No JSON Schema, no `form_configurations` entry, and no `form_submissions` record existed for the overview data.

## 3. What Was Implemented

### 3.1 Backend: New Form Configuration

**Files created/modified:**

- `service/src/main/resources/db/init.sql` — Added `pre_award_overview` form configuration with JSON Schema including a `personnel` array field, UI Schema with field ordering, and seed data populated from existing `awards` columns + `project_personnel` table via `jsonb_agg()`
- `service/src/main/resources/db/migrations/004_add_pre_award_overview_form.sql` — Migration script for existing databases: creates the form configuration, backfills `form_submissions` with data from `awards` + `project_personnel`, creates schema version 1
- `service/src/main/resources/templates/pre-award-overview.transformer.json` — Transformer template mapping all overview fields to typed relational columns, including `personnel` as a JSONB column with `jsonStringify`/`jsonParse` transforms

**JSON Schema fields:**
| Field | Type | Notes |
|-------|------|-------|
| `pi_budget` | number | Currency amount |
| `final_recommended_budget` | number | Currency amount |
| `program_manager` | string | Personnel name |
| `contract_grants_specialist` | string | Personnel name |
| `branch_chief` | string | Personnel name |
| `pi_notification_date` | string (date) | ISO date format |
| `prime_award_type` | string (enum) | extramural, intramural, extramural_intramural, intramural_extramural |
| `personnel` | array of objects | Embedded personnel with name, organization, country, project_role, is_subcontract |
| `notes` | string | Free-text notes |

### 3.2 Frontend: OverviewPanel Refactored

**Files created:**
- `client/src/components/PersonnelDataGrid.jsx` — Standalone data grid component for the personnel array

**Files modified:**
- `client/src/components/OverviewPanel.jsx` — Complete rewrite from direct award access to form submission system
- `client/src/pages/ReviewPage.jsx` — Updated to find and pass `pre_award_overview` submission; removed direct award update logic

### 3.3 Implementation Iterations

The implementation went through several iterations to resolve state management challenges:

**Iteration 1: RJSF Form with Custom `ui:field`**
- Registered `PersonnelDataGrid` as a custom RJSF field via `fields={{ personnelGrid: PersonnelDataGrid }}` and `uiSchema: { personnel: { "ui:field": "personnelGrid" } }`
- **Problem:** RJSF custom field `onChange` has a complex signature `(value, path, errorSchema, id)` that didn't propagate personnel changes correctly. Data disappeared on add/edit.

**Iteration 2: Dual Update Path**
- Added `formContext.onPersonnelChange` callback alongside RJSF `onChange` to auto-save personnel changes
- **Problem:** Two competing `setFormData` calls with stale closures overwrote each other, causing data loss

**Iteration 3: Separate Rendering (Final)**
- Removed personnel from the RJSF `<Form>` entirely by deleting it from the schema passed to the Form
- Rendered `PersonnelDataGrid` as a standalone component outside the Form, managing personnel as a separate piece of `formData` state
- Personnel changes call `handlePersonnelChange` which updates state + auto-saves to DB immediately
- Scalar field changes go through the RJSF Form's `onChange` which preserves the personnel array via `formDataRef`

**Iteration 4: Remove RJSF Form Entirely**
- Replaced the RJSF `<Form>` component with manual inline `FieldRow` components (label left, input right)
- Eliminated all RJSF rendering issues — direct MUI `TextField` and `Select` with explicit state management
- Compact layout: labels 220px wide, inputs capped at 340px, `maxLength: 100`
- Notes rendered below personnel as a multiline textarea

### 3.4 Personnel Data Grid Features

The `PersonnelDataGrid` component replicates the original OverviewPanel's personnel table:

- **Table columns:** Organization, Country, Project Role, Name, Subcontract (checkbox), Action
- **Inline editing:** Click edit icon → row switches to text fields → check/X to save/cancel
- **Add Personnel modal:** Dialog with Name, Organization, Country, Project Role, Subcontract fields; required field validation
- **Delete confirmation modal:** Shows personnel name and role before deleting
- **Prime Award logic:** When `prime_award_type` is `extramural_intramural` or `intramural_extramural`, a "Create Record" button appears in each row's action column (matching the original POC behavior)
- **Auto-save:** Add, edit, and delete operations immediately persist to the database via `saveFormDraft`
- **Locked state:** Action column hidden when form is submitted/locked

### 3.5 Prime Award Conditional Logic

The `prime_award_type` dropdown drives visibility of the "Create Record" button in the personnel grid:

```
formContext={{ primeAwardType: formData?.prime_award_type }}
```

The `PersonnelDataGrid` reads `formContext.primeAwardType` and computes:
```javascript
const showCreateRecord = primeAwardType === 'extramural_intramural' 
                      || primeAwardType === 'intramural_extramural';
```

This is reactive — changing the dropdown instantly shows/hides the button with no save needed.

### 3.6 Field Ordering

The `ui:order` in the JSON Schema and the manual rendering order both follow:
1. PI Budget
2. Final Recommended Budget
3. Program Manager
4. Contract/Grants Specialist
5. Branch Chief
6. PI Notification Date
7. Prime Award (Intra/Extra) — positioned immediately before personnel
8. Project Personnel — data grid
9. Overview Notes — below personnel

### 3.7 Final Recommendation Role-Based Access

The `FinalRecommendation` component was updated to use `AuthContext` for role-based conditional editing:

- **SO Recommendation section:** Editable only when `user.role === 'SO'`; read-only with "Read Only" chip badge and gray background for other roles; Save/Send buttons hidden for non-SO users
- **GOR/COR Recommendation section:** Editable only when `user.role === 'GOR'` or `user.role === 'COR'`; same read-only treatment for other roles
- Both sections are always **visible** to all roles

## 4. Architecture Decision: Why Not RJSF for Personnel?

The personnel array was initially implemented as a custom RJSF field (`ui:field`), then as part of the RJSF Form, and finally rendered completely outside it. The reasons:

1. **RJSF's default array widget** renders each item as an accordion with individual field inputs — not the tabular data grid layout required
2. **Custom `ui:field` components** in RJSF v6 have a complex `onChange` contract that doesn't work reliably for array mutations from modal dialogs
3. **State management conflicts** between RJSF's internal form state and React state caused data loss during re-renders
4. **Direct rendering** with MUI `Table` + explicit `onChange` callbacks is simpler, more predictable, and matches the original UI exactly

The scalar fields were also moved out of RJSF to achieve the compact inline label-left/input-right layout without fighting RJSF's default MUI field templates.

## 5. Data Flow

```
User edits scalar field
  → handleFieldChange(field, value)
  → setFormData({...prev, [field]: value})
  → formDataRef.current updated
  → User clicks "Save Draft"
  → saveFormDraft(submissionId, formDataRef.current)
  → API persists to form_submissions.form_data JSONB

User adds/edits/deletes personnel
  → PersonnelDataGrid calls onChange(newPersonnelArray)
  → handlePersonnelChange(newPersonnel)
  → setFormData({...prev, personnel: newPersonnel})
  → formDataRef.current updated
  → Auto-save: saveFormDraft(submissionId, formDataRef.current)
  → API persists immediately (no manual Save Draft needed)

Page load
  → ReviewPage fetches award via getAwardByLog()
  → Extracts pre_award_overview submission from response
  → Passes as overviewSubmission prop to OverviewPanel
  → useVersionedFormData resolves schema + formData
  → Component renders with data from form_submissions.form_data
```

## 6. Files Changed (Complete List)

### Backend
| File | Change |
|------|--------|
| `service/src/main/resources/db/init.sql` | Added `pre_award_overview` form configuration with personnel array schema; seed data with `jsonb_agg()` |
| `service/src/main/resources/db/migrations/004_add_pre_award_overview_form.sql` | Migration script for existing databases |
| `service/src/main/resources/templates/pre-award-overview.transformer.json` | Transformer template with personnel as JSONB field |

### Frontend
| File | Change |
|------|--------|
| `client/src/components/PersonnelDataGrid.jsx` | **New** — Data grid with inline edit, add modal, delete confirmation, prime award conditional logic |
| `client/src/components/OverviewPanel.jsx` | **Rewritten** — Manual inline fields, personnel grid, notes below personnel, auto-save on personnel changes |
| `client/src/components/FinalRecommendation.jsx` | Added role-based conditional editing for SO and GOR/COR recommendation sections |
| `client/src/pages/ReviewPage.jsx` | Extract `pre_award_overview` submission; pass to OverviewPanel; removed `handlePrimeAwardChange` and `personnel` state |

## 7. Verification Steps

1. `docker stop rjsf_postgres && docker rm rjsf_postgres` — remove old container
2. Start fresh PostgreSQL with updated init.sql
3. `cd service && mvn spring-boot:run` — start backend
4. `cd client && npm run dev` — start frontend
5. Navigate to `http://localhost:5173/review/TE020005`
6. Overview panel shows inline fields with PI Budget=1448199, Program Manager="Naba Bora"
7. Personnel grid shows John Smith (PI/PD) and Walter White (Co-Investigator)
8. Add a person → grid updates + "Personnel saved" snackbar appears
9. Change Prime Award to "Extramural w/Intragovernmental Component" → "Create Record" buttons appear in personnel rows
10. Edit a scalar field → click Save Draft → "Overview saved" snackbar
11. Refresh page → all data persists
12. Log in as SO → SO Recommendation is editable, GOR/COR is read-only
13. Log in as GOR → GOR/COR Recommendation is editable, SO is read-only
