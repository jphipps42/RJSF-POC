# Technical Specification: RJSF Pre-Award Review Application

**Version:** 1.0
**Date:** March 2026
**Project:** RJSF-POC (React JSON Schema Form - Proof of Concept)

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [System Architecture](#2-system-architecture)
3. [Deployment Architecture](#3-deployment-architecture)
4. [Database Design](#4-database-design)
5. [API Specification](#5-api-specification)
6. [Frontend Architecture](#6-frontend-architecture)
7. [RJSF Form Engine](#7-rjsf-form-engine)
8. [Form Submission Lifecycle](#8-form-submission-lifecycle)
9. [Review Section Schemas](#9-review-section-schemas)
10. [Technology Stack](#10-technology-stack)
11. [Configuration & Environment](#11-configuration--environment)
12. [Future Considerations](#12-future-considerations)

---

## 1. Executive Summary

The RJSF Pre-Award Review Application is a full-stack web application for managing multi-stage regulatory compliance reviews for government research awards. It replaces a static HTML/JavaScript prototype with a database-backed React application using the React JSON Schema Form (RJSF) framework.

**Key Capabilities:**
- Dynamic form generation from JSON Schema definitions stored in PostgreSQL
- Four review sections: Safety, Animal Research, Human Research, and Acquisition/Contracting
- Form draft saving and submission with section locking
- Award metadata management with project personnel tracking
- Configurable Prime Award type that conditionally shows "Create Record" actions in the personnel grid
- Administrative reset functionality for submitted sections
- Final Recommendation workflow for SO and GOR/COR approvals

**Primary Users:** Government grants/contracts specialists, science officers, and GOR/COR personnel conducting pre-award negotiation reviews.

---

## 2. System Architecture

The application follows a three-tier architecture: React SPA frontend, Express.js REST API, and PostgreSQL database.

![System Architecture](diagrams/system-architecture.png)

### Architecture Layers

| Layer | Technology | Port | Purpose |
|-------|-----------|------|---------|
| Frontend | React 19 + RJSF + Material-UI | 5173 | Single-page application with dynamic form rendering |
| API Server | Express.js 5 | 3001 | RESTful CRUD operations for all entities |
| Database | PostgreSQL 16 | 5432 | Persistent storage for schemas, form data, and awards |

### Communication

- **Frontend to API:** HTTP REST calls via Axios, JSON request/response bodies
- **API to Database:** PostgreSQL wire protocol via `pg` connection pool
- **Cross-Origin:** CORS middleware enabled on the API server

---

## 3. Deployment Architecture

The development environment uses Docker Compose for PostgreSQL and Node.js processes for the API server and Vite dev server.

![Deployment Architecture](diagrams/deployment-architecture.png)

### Services

| Service | Image/Runtime | Container | Persistence |
|---------|---------------|-----------|-------------|
| PostgreSQL | `postgres:16-alpine` | `rjsf_postgres` | Named volume `pgdata` |
| API Server | Node.js | Host process | Stateless (database-backed) |
| Frontend | Vite Dev Server | Host process | N/A |

### Startup Sequence

```bash
# 1. Start PostgreSQL (auto-runs init.sql on first launch)
docker-compose up -d

# 2. Start API server
cd server && npm run dev

# 3. Start React dev server
cd client && npm run dev
```

---

## 4. Database Design

### Entity-Relationship Diagram

![Entity Relationship Diagram](diagrams/entity-relationship.png)

### Table Definitions

#### `users`
Stores registered user accounts. Currently scaffolded for future authentication integration.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK, DEFAULT uuid_generate_v4() | Unique identifier |
| email | VARCHAR(255) | UNIQUE, NOT NULL | User email address |
| password_hash | VARCHAR(255) | NOT NULL | Bcrypt hashed password |
| created_at | TIMESTAMPTZ | DEFAULT NOW() | Account creation time |
| updated_at | TIMESTAMPTZ | DEFAULT NOW() | Last modification time |

#### `awards`
Parent entity for all review submissions. Represents a single research award under negotiation.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Unique identifier |
| log_number | VARCHAR(50) | UNIQUE, NOT NULL | Primary award reference (e.g., TE020005) |
| award_number | VARCHAR(50) | | Numeric award identifier |
| award_mechanism | TEXT | | Type of award mechanism |
| funding_opportunity | TEXT | | Associated funding opportunity |
| principal_investigator | VARCHAR(255) | | PI name |
| performing_organization | TEXT | | Research performing org |
| contracting_organization | TEXT | | Contracting org |
| period_of_performance | VARCHAR(100) | | Start/end dates |
| award_amount | DECIMAL(15,2) | | Total award value |
| program_office | TEXT | | Sponsoring program office |
| program | VARCHAR(255) | | Program name |
| science_officer | VARCHAR(255) | | Assigned science officer |
| gor_cor | VARCHAR(255) | | Government Officer Representative |
| pi_budget | DECIMAL(15,2) | | PI requested budget |
| final_recommended_budget | DECIMAL(15,2) | | Final negotiated budget |
| program_manager | VARCHAR(255) | | Assigned program manager |
| contract_grants_specialist | VARCHAR(255) | | Assigned specialist |
| branch_chief | VARCHAR(255) | | Branch chief |
| prime_award_type | VARCHAR(50) | DEFAULT 'extramural' | extramural, intramural, extramural_intramural, intramural_extramural |
| status | VARCHAR(50) | DEFAULT 'under_negotiation' | Award lifecycle status |
| created_by | UUID | FK -> users(id) | Creator reference |

#### `form_configurations`
Stores JSON Schema and UI Schema definitions for each review section. Schema-driven form generation is the core of the RJSF pattern.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Unique identifier |
| form_key | VARCHAR(100) | UNIQUE, NOT NULL | Lookup key (e.g., 'safety_review') |
| title | VARCHAR(255) | NOT NULL | Display title |
| description | TEXT | | Form description |
| json_schema | JSONB | NOT NULL | JSON Schema draft-07 definition |
| ui_schema | JSONB | DEFAULT '{}' | RJSF UI customization directives |
| default_data | JSONB | DEFAULT '{}' | Pre-populated form values |
| version | INTEGER | DEFAULT 1 | Schema version tracker |
| is_active | BOOLEAN | DEFAULT true | Soft delete flag |

#### `form_submissions`
Stores actual form response data for each review section per award. Each award gets one submission per active form configuration.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Unique identifier |
| award_id | UUID | FK -> awards(id) CASCADE, NOT NULL | Parent award |
| form_config_id | UUID | FK -> form_configurations(id), NOT NULL | Schema reference |
| form_key | VARCHAR(100) | NOT NULL | Denormalized form identifier |
| form_data | JSONB | DEFAULT '{}' | User-entered form responses |
| status | VARCHAR(50) | NOT NULL, DEFAULT 'not_started' | not_started, in_progress, submitted |
| submitted_by | UUID | FK -> users(id) | Who submitted |
| submitted_at | TIMESTAMPTZ | | Submission timestamp |
| completion_date | TIMESTAMPTZ | | Completion timestamp |
| is_locked | BOOLEAN | DEFAULT false | Prevents editing after submission |
| notes | TEXT | | Additional notes |
| | | UNIQUE(award_id, form_key) | One submission per form per award |

#### `project_personnel`
Stores personnel associated with an award.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK | Unique identifier |
| award_id | UUID | FK -> awards(id) CASCADE, NOT NULL | Parent award |
| organization | VARCHAR(255) | NOT NULL | Personnel's organization |
| country | VARCHAR(100) | DEFAULT 'USA' | Country |
| project_role | VARCHAR(100) | NOT NULL | Role (PI/PD, Co-Investigator, etc.) |
| name | VARCHAR(255) | NOT NULL | Personnel name |
| is_subcontract | BOOLEAN | DEFAULT false | Subcontract flag |

### Indexes

| Index | Table | Column(s) | Purpose |
|-------|-------|-----------|---------|
| idx_form_submissions_award | form_submissions | award_id | Fast lookup by award |
| idx_form_submissions_form_key | form_submissions | form_key | Fast lookup by form type |
| idx_form_submissions_status | form_submissions | status | Status filtering |
| idx_awards_log_number | awards | log_number | Log number lookups |
| idx_form_configurations_form_key | form_configurations | form_key | Config lookups |

---

## 5. API Specification

![API Endpoints](diagrams/api-endpoints.png)

### Base URL: `http://localhost:3001/api`

### Awards Endpoints

| Method | Path | Description | Request Body | Response |
|--------|------|-------------|-------------|----------|
| GET | `/awards` | List all awards | - | Award[] |
| GET | `/awards/:id` | Get award with submissions and personnel | - | Award + submissions[] + personnel[] |
| GET | `/awards/by-log/:logNumber` | Get award by log number (with nested data) | - | Award + submissions[] + personnel[] |
| POST | `/awards` | Create award (auto-creates submissions) | Award fields | Award |
| PUT | `/awards/:id` | Update award fields | Partial award fields | Award |

**GET Response Shape (by-log or by-id):**
```json
{
  "id": "uuid",
  "log_number": "TE020005",
  "principal_investigator": "Bill Jones",
  "prime_award_type": "extramural",
  "submissions": [
    {
      "id": "uuid",
      "form_key": "safety_review",
      "form_data": {},
      "status": "not_started",
      "is_locked": false,
      "form_title": "A. Safety Requirements Review",
      "json_schema": { ... },
      "ui_schema": { ... }
    }
  ],
  "personnel": [
    {
      "id": "uuid",
      "name": "John Smith",
      "organization": "Johns Hopkins University",
      "project_role": "PI/PD",
      "is_subcontract": false
    }
  ]
}
```

### Form Submissions Endpoints

| Method | Path | Description | Request Body | Response |
|--------|------|-------------|-------------|----------|
| GET | `/form-submissions?award_id=X` | List submissions for an award | - | Submission[] (with joined config) |
| GET | `/form-submissions/:id` | Get single submission | - | Submission (with joined config) |
| GET | `/form-submissions/by-award/:awardId/:formKey` | Get by award + form key | - | Submission |
| PUT | `/form-submissions/:id/save` | Save draft (checks lock) | `{ form_data: {} }` | Updated Submission |
| PUT | `/form-submissions/:id/submit` | Submit and lock section | `{ form_data: {} }` | Locked Submission |
| PUT | `/form-submissions/:id/reset` | Reset to defaults | - | Reset Submission |

**Lock Enforcement:** Save and Submit return HTTP 403 if `is_locked = true`.

### Form Configurations Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/form-configurations` | List all active configurations |
| GET | `/form-configurations/:formKey` | Get single configuration by key |
| POST | `/form-configurations` | Create new configuration |
| PUT | `/form-configurations/:id` | Update configuration (increments version) |
| DELETE | `/form-configurations/:id` | Soft delete (sets is_active = false) |

### Personnel Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/personnel?award_id=X` | List personnel (optionally by award) |
| POST | `/personnel` | Add personnel record |
| DELETE | `/personnel/:id` | Delete personnel record |

### Health Check

| Method | Path | Response |
|--------|------|----------|
| GET | `/health` | `{ "status": "ok", "timestamp": "ISO8601" }` |

---

## 6. Frontend Architecture

### Component Hierarchy

![Component Hierarchy](diagrams/component-hierarchy.png)

### Component Inventory

| Component | File | Purpose |
|-----------|------|---------|
| App | `App.jsx` | Theme provider, router configuration |
| ReviewPage | `pages/ReviewPage.jsx` | Main page: data fetching, layout orchestration |
| GlobalHeader | `components/GlobalHeader.jsx` | EGS branded banner (40px, #1E4489) |
| TopNav | `components/TopNav.jsx` | 10-item horizontal navigation bar |
| AwardSummary | `components/AwardSummary.jsx` | 3-column award metadata display |
| OverviewPanel | `components/OverviewPanel.jsx` | Budget, contacts, dates, personnel table |
| ReviewSection | `components/ReviewSection.jsx` | RJSF-powered accordion form section |
| StatusBadge | `components/StatusBadge.jsx` | Color-coded status pill chip |
| FinalRecommendation | `components/FinalRecommendation.jsx` | SO/GOR recommendation workflow |
| NotesPanel | `components/NotesPanel.jsx` | Textarea-based note input |
| RightPanel | `components/RightPanel.jsx` | Collapsible document management sidebar |
| ResetModal | `components/ResetModal.jsx` | Admin section reset dialog |

### Page Layout

The ReviewPage implements the POC's two-panel layout:

```
+--------------------------------------------------+
|  GlobalHeader (EGS Banner)                        |
+--------------------------------------------------+
|  TopNav (Summary | Details | ... | PreAward Review | ...)  |
+--------------------------------------------------+
|  Context Bar (Breadcrumb)        | Return to Search |
+--------------------------------------------------+
|  Status Bar (Under Negotiation)  | Quick Search     |
+--------------------------------------------------+
|  AwardSummary (3-column: Log#, PI, Program)       |
+--------------------------------------------------+
|  Left Panel (scrollable)    |  Right Panel         |
|  +------------------------+ |  (collapsible)       |
|  | OverviewPanel          | |  Document &           |
|  | - Budget info          | |  File Management      |
|  | - Personnel table      | |                      |
|  +------------------------+ |                      |
|  | ReviewSection: Safety  | |                      |
|  | ReviewSection: Animal  | |                      |
|  | ReviewSection: Human   | |                      |
|  | ReviewSection: Acquis. | |                      |
|  +------------------------+ |                      |
|  | FinalRecommendation    | |                      |
|  | NotesPanel (SO/GOR)    | |                      |
|  | NotesPanel (ChangeLog) | |                      |
|  +------------------------+ |                      |
|  [Reset Checklist Button]   |                      |
+-----------------------------+----------------------+
```

### State Management

The ReviewPage manages application state locally with React hooks:

| State | Type | Source | Purpose |
|-------|------|--------|---------|
| award | Object | API: getAwardByLog | Award metadata |
| submissions | Array | API response (nested) | Form submission data with schemas |
| personnel | Array | API response (nested) | Project personnel records |
| loading | Boolean | Local | Data fetch loading indicator |
| error | String | Local | Error message display |
| rightPanelCollapsed | Boolean | Local | Right sidebar toggle |
| resetModalOpen | Boolean | Local | Reset dialog visibility |

### Prime Award Dropdown Behavior

The Overview Panel contains a Prime Award type dropdown that conditionally affects the personnel data grid:

| Dropdown Value | Personnel Grid Action Column |
|----------------|------------------------------|
| Extramural Only | Edit button only |
| Intragovernmental Only | Edit button only |
| **Extramural w/Intragovernmental Component** | **Edit + Create Record buttons** |
| Intragovernmental w/Extramural Component | Edit button only |

When the user selects "Extramural w/Intragovernmental Component", a "Create Record" button appears next to the "Edit" button for each personnel row, enabling creation of intragovernmental subaward records.

---

## 7. RJSF Form Engine

The application uses React JSON Schema Form (RJSF) v6.4.1 with the Material-UI theme to dynamically generate forms from database-stored JSON Schema definitions.

![RJSF Data Flow](diagrams/rjsf-data-flow.png)

### How It Works

1. **Schema Storage:** JSON Schema and UI Schema are stored as JSONB columns in the `form_configurations` table
2. **Schema Delivery:** When the client loads an award, the API joins `form_submissions` with `form_configurations` to deliver both the user's data and the schema in a single response
3. **Form Rendering:** The `ReviewSection` component passes the schema to `@rjsf/mui`'s `<Form>` component, which auto-generates Material-UI form widgets
4. **Validation:** AJV8 validates form data against the JSON Schema in real-time
5. **Persistence:** Form data is stored as a JSONB blob in `form_submissions.form_data`

### RJSF Component Configuration

```jsx
<Form
  schema={submission.json_schema}        // JSON Schema from DB
  uiSchema={submission.ui_schema}        // UI customization from DB
  formData={formData}                    // Controlled state
  validator={validator}                  // @rjsf/validator-ajv8
  onChange={(e) => setFormData(e.formData)}
  disabled={isLocked}                    // Lock after submission
  readonly={isLocked}
/>
```

### UI Schema Widget Mappings

| Widget | Used For | Example |
|--------|----------|---------|
| `radio` | Yes/No/Unknown questions | All safety, animal, human questions |
| `textarea` | Multi-line text input | Notes fields, comments |
| `checkboxes` | Multi-select arrays | Animal species, special requirements |
| `hidden` | Non-visible fields | Programmatic REC selection |

---

## 8. Form Submission Lifecycle

### State Diagram

![Form Status Lifecycle](diagrams/form-status-lifecycle.png)

### Status Transitions

| From | To | Trigger | API Endpoint | Side Effects |
|------|----|---------|-------------|-------------|
| not_started | in_progress | Save Draft | PUT /:id/save | Updates form_data |
| in_progress | in_progress | Save Draft | PUT /:id/save | Updates form_data |
| in_progress | submitted | Submit | PUT /:id/submit | Sets is_locked=true, records submitted_at |
| submitted | not_started | Admin Reset | PUT /:id/reset | Clears form_data, is_locked=false |

### Sequence Diagram

![Form Submission Flow](diagrams/form-submission-flow.png)

### Submit Button Labels by Section

| Form Key | Submit Button Text |
|----------|-------------------|
| safety_review | Submit to Safety Office |
| animal_review | Submit to Animal Research Regulatory Office |
| human_review | Submit to Human Research Regulatory Agency |
| acquisition_review | Submit to Finance for processing |

### Status Badge Colors

| Status | Background | Text Color | Hex |
|--------|-----------|------------|-----|
| Not Started | Blue-gray | Light | #719fcc |
| In Progress | Orange | Dark | #e29032 |
| Completed | Dark green | White | #0e1d13 |
| Submitted | Green | Light | #088835 |

---

## 9. Review Section Schemas

### A. Safety Requirements Review (`safety_review`)

6 yes/no radio questions covering environmental compliance, infectious agents, biological select agents, chemical agents, pesticides, and public health impact. Includes a notes textarea and a hidden field for programmatic REC document selection.

### B. Animal Research Review (`animal_review`)

5 questions: animal usage (yes/no), species selection (checkbox array with 9 options), international study sites, foreign countries of concern (yes/no/unknown), estimated start date (text), and notes.

### C. Human Research Review (`human_review`)

Nested object with 6 subsections:

| Subsection | Content | Questions |
|------------|---------|-----------|
| a. No Regulatory Review Required | Cell lines, organoids, pooled products, PDX models, commercial services | 5 yes/no + notes |
| b. Human Anatomical Substances | Specimen collection, non-vendor cell lines, non-pooled substances, PDX creation, specimen sources, cadavers, regulated samples | 7 yes/no + notes |
| c. Human Data - Secondary Use | Secondary use of human data | 1 yes/no + notes |
| d. Human Subjects | Interaction/intervention, clinical trials, FDA regulation, international sites | 4 yes/no + notes |
| e. Other/Special Topics | Emergency research, genomic data, fetal tissue, sensitive cadaver use | 1 yes/no + notes |
| f. Estimated Start | Estimated human research start date | 1 text field |

### D. Acquisition/Contracting Review (`acquisition_review`)

7 subsections with detailed budget review:

| Subsection | Content |
|------------|---------|
| 1. Budget Review | Personnel (2q), Equipment (3q), Travel (2q), Materials (3q), Consultant (4q), 3rd Party (4q), Other Direct (3q), Additional Concerns (1q) - all yes/no with notes |
| 2. Peer Review | Required comment textarea |
| 3. SOW Concerns | Required comment textarea |
| 4. CPS | CPS received (yes/no) + required comment |
| 5. IER | Applicability (yes/no/unclear) + plan included + notes |
| 6. Data Management Plan | DMP received (yes/no) + notes |
| 7. Special Requirements | 21-item checkbox list covering agreements, regulatory, program-specific, data submission, personnel, and deliverables |

---

## 10. Technology Stack

### Frontend

| Package | Version | Purpose |
|---------|---------|---------|
| react | 19.2.4 | UI library |
| react-dom | 19.2.4 | DOM rendering |
| react-router-dom | 7.13.2 | Client-side routing |
| @rjsf/core | 6.4.1 | JSON Schema form engine |
| @rjsf/mui | 6.4.1 | Material-UI theme for RJSF |
| @rjsf/utils | 6.4.1 | RJSF utilities |
| @rjsf/validator-ajv8 | 6.4.1 | AJV8 schema validation |
| @mui/material | 7.3.9 | Component library |
| @mui/icons-material | 7.3.9 | Icon set |
| @emotion/react | 11.14.0 | CSS-in-JS styling |
| @emotion/styled | 11.14.1 | Styled components |
| axios | 1.13.6 | HTTP client |
| vite | 8.0.2 | Build tool and dev server |

### Backend

| Package | Version | Purpose |
|---------|---------|---------|
| express | 5.2.1 | Web framework |
| pg | 8.20.0 | PostgreSQL driver |
| cors | 2.8.6 | CORS middleware |
| dotenv | 17.3.1 | Environment variables |
| jsonwebtoken | 9.0.3 | JWT support (infrastructure) |
| bcryptjs | 3.0.3 | Password hashing (infrastructure) |
| uuid | 13.0.0 | UUID generation |

### Infrastructure

| Technology | Version | Purpose |
|-----------|---------|---------|
| PostgreSQL | 16 (Alpine) | Primary database |
| Docker Compose | v2 | Container orchestration |
| Node.js | 18+ | Server runtime |

---

## 11. Configuration & Environment

### Environment Variables (`server/.env`)

| Variable | Default | Description |
|----------|---------|-------------|
| DATABASE_URL | postgresql://rjsf_user:rjsf_pass@localhost:5432/rjsf_forms | PostgreSQL connection string |
| JWT_SECRET | dev-secret-change-in-production | JWT signing secret (infrastructure) |
| PORT | 3001 | API server port |

### Docker Compose Configuration

| Setting | Value |
|---------|-------|
| PostgreSQL Image | postgres:16-alpine |
| Container Name | rjsf_postgres |
| DB User | rjsf_user |
| DB Password | rjsf_pass |
| DB Name | rjsf_forms |
| Host Port | 5432 |
| Data Volume | pgdata (named) |
| Init Script | server/db/init.sql (auto-executed on first start) |

### Seeded Demo Data

The init.sql script seeds:
- 4 form configurations (safety, animal, human, acquisition review schemas)
- 1 demo award (Log Number: TE020005, PI: Bill Jones)
- 4 form submissions (one per configuration, status: not_started)
- 2 project personnel (John Smith - PI/PD, Walter White - Co-Investigator)

---

## 12. Future Considerations

### Planned Enhancements

| Feature | Current State | Enhancement |
|---------|--------------|-------------|
| Authentication | JWT middleware scaffolded, login UI removed | Integrate with enterprise SSO/OAuth |
| File Management | Right panel UI scaffolded | Backend file upload/download with S3 or local storage |
| Final Recommendation | UI complete, no persistence | API endpoints for SO/GOR recommendation save |
| Notes Panels | UI only | Persistence endpoints for SO/GOR notes and change log |
| Personnel Management | Display + placeholder buttons | Full CRUD modals for add/edit/delete |
| Role-Based Access | None | Admin vs reviewer vs read-only roles |
| Audit Trail | submitted_at/submitted_by tracked | Full change history logging |
| Form Versioning | Version field exists | Migration strategy for schema changes |
| Conditional Logic | Basic (equipment enable/disable in POC) | RJSF dependencies and conditional rendering |
| Notification | None | Email alerts on submission events |

### Extensibility Points

1. **New Form Sections:** Add a row to `form_configurations` - submissions auto-created for existing awards
2. **Custom RJSF Widgets:** Register custom field/widget components for specialized inputs
3. **Schema Evolution:** Use the `version` field to track and migrate schema changes
4. **Multi-Award Support:** Router already parameterized by log number; add award list/search page
