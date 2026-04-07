-- ============================================
-- RJSF Pre-Award Review Database Schema
-- Composite Form Architecture (Approach B)
-- ============================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Form configurations (JSON Schema + UI Schema)
CREATE TABLE IF NOT EXISTS form_configurations (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  form_key VARCHAR(100) UNIQUE NOT NULL,
  title VARCHAR(255) NOT NULL,
  description TEXT,
  json_schema JSONB NOT NULL,
  ui_schema JSONB DEFAULT '{}'::jsonb,
  default_data JSONB DEFAULT '{}'::jsonb,
  version INTEGER DEFAULT 1,
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Award records
CREATE TABLE IF NOT EXISTS awards (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  log_number VARCHAR(50) UNIQUE NOT NULL,
  award_number VARCHAR(50),
  award_mechanism TEXT,
  funding_opportunity TEXT,
  principal_investigator VARCHAR(255),
  performing_organization TEXT,
  contracting_organization TEXT,
  period_of_performance VARCHAR(100),
  award_amount DECIMAL(15, 2),
  program_office TEXT,
  program VARCHAR(255),
  science_officer VARCHAR(255),
  gor_cor VARCHAR(255),
  pi_budget DECIMAL(15, 2),
  final_recommended_budget DECIMAL(15, 2),
  program_manager VARCHAR(255),
  contract_grants_specialist VARCHAR(255),
  branch_chief VARCHAR(255),
  prime_award_type VARCHAR(50) DEFAULT 'extramural',
  status VARCHAR(50) DEFAULT 'under_negotiation',
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Form submissions — ONE composite row per award
CREATE TABLE IF NOT EXISTS form_submissions (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  award_id UUID NOT NULL REFERENCES awards(id) ON DELETE CASCADE,
  form_config_id UUID NOT NULL REFERENCES form_configurations(id),
  form_key VARCHAR(100) NOT NULL,
  form_data JSONB NOT NULL DEFAULT '{}'::jsonb,
  status VARCHAR(50) NOT NULL DEFAULT 'not_started',
  section_status JSONB DEFAULT '{}'::jsonb,
  submitted_at TIMESTAMP WITH TIME ZONE,
  completion_date TIMESTAMP WITH TIME ZONE,
  is_locked BOOLEAN DEFAULT false,
  schema_version_id UUID,
  schema_version INTEGER,
  notes TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  UNIQUE(award_id, form_key)
);

-- Project personnel associated with awards
CREATE TABLE IF NOT EXISTS project_personnel (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  award_id UUID NOT NULL REFERENCES awards(id) ON DELETE CASCADE,
  organization VARCHAR(255) NOT NULL,
  country VARCHAR(100) DEFAULT 'USA',
  project_role VARCHAR(100) NOT NULL,
  name VARCHAR(255) NOT NULL,
  is_subcontract BOOLEAN DEFAULT false,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Linked files for Final Recommendation sections
CREATE TABLE IF NOT EXISTS award_linked_files (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  award_id UUID NOT NULL REFERENCES awards(id) ON DELETE CASCADE,
  section VARCHAR(100) NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  description TEXT,
  last_updated TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Form schema versions
CREATE TABLE IF NOT EXISTS form_schema_versions (
  id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  form_id       UUID NOT NULL REFERENCES form_configurations(id),
  version       INTEGER NOT NULL,
  json_schema   JSONB NOT NULL,
  ui_schema     JSONB DEFAULT '{}'::jsonb,
  default_data  JSONB DEFAULT '{}'::jsonb,
  change_notes  TEXT,
  is_current    BOOLEAN DEFAULT false,
  created_at    TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE (form_id, version)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_one_current_version
  ON form_schema_versions (form_id) WHERE is_current = true;

-- Schema migrations
CREATE TABLE IF NOT EXISTS schema_migrations (
  id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  form_id          UUID NOT NULL REFERENCES form_configurations(id),
  from_version     INTEGER NOT NULL,
  to_version       INTEGER NOT NULL,
  migration_script JSONB NOT NULL DEFAULT '[]'::jsonb,
  created_at       TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE (form_id, from_version, to_version)
);

-- Application users
CREATE TABLE IF NOT EXISTS app_users (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  username VARCHAR(100) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(255),
  role VARCHAR(100) NOT NULL,
  organization VARCHAR(255) NOT NULL,
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Transformer template history
CREATE TABLE IF NOT EXISTS transformer_template_history (
  id              BIGSERIAL PRIMARY KEY,
  form_id         TEXT        NOT NULL,
  version         INTEGER     NOT NULL,
  template_json   JSONB       NOT NULL,
  loaded_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (form_id, version)
);

-- Indexes
CREATE INDEX idx_form_submissions_award ON form_submissions(award_id);
CREATE INDEX idx_form_submissions_form_key ON form_submissions(form_key);
CREATE INDEX idx_form_submissions_status ON form_submissions(status);
CREATE INDEX idx_awards_log_number ON awards(log_number);
CREATE INDEX idx_form_configurations_form_key ON form_configurations(form_key);
CREATE INDEX idx_award_linked_files_award ON award_linked_files(award_id);
CREATE INDEX idx_award_linked_files_section ON award_linked_files(section);
CREATE INDEX idx_app_users_username ON app_users(username);
CREATE INDEX IF NOT EXISTS idx_tth_form_id ON transformer_template_history(form_id);
CREATE INDEX IF NOT EXISTS idx_tth_form_version ON transformer_template_history(form_id, version);

-- ============================================
-- Composite form configuration
-- All section properties in one flat namespace
-- ============================================

INSERT INTO form_configurations (form_key, title, description, json_schema, ui_schema, default_data) VALUES
('pre_award_composite', 'Pre-Award Composite Review', 'Composite form containing all pre-award review sections with shared formData', '{
  "title": "Pre-Award Composite Review",
  "type": "object",
  "properties": {

    "pi_budget":                    { "type": "number", "title": "PI Budget" },
    "final_recommended_budget":     { "type": "number", "title": "Final Recommended Budget" },
    "funding_source":               { "type": "string", "title": "Funding Source", "enum": ["congressional", "core_program", "supplemental", "other"] },
    "negotiation_status":           { "type": "string", "title": "Negotiation Status", "enum": ["not_started", "initial_contact", "under_negotiation", "terms_agreed", "pending_signatures"], "default": "not_started" },
    "program_manager":              { "type": "string", "title": "Program Manager" },
    "co_principal_investigator":    { "type": "string", "title": "Co-Principal Investigator" },
    "contract_grants_specialist":   { "type": "string", "title": "Contract/Grants Specialist" },
    "branch_chief":                 { "type": "string", "title": "Branch Chief" },
    "prime_award_type":             { "type": "string", "title": "Prime Award (Intra/Extra)", "enum": ["extramural", "intramural", "extramural_intramural", "intramural_extramural"], "default": "extramural" },
    "pi_notification_date":         { "type": "string", "title": "PI Notification Date", "format": "date" },
    "personnel": {
      "type": "array",
      "title": "Project Personnel",
      "items": {
        "type": "object",
        "required": ["name", "organization", "project_role"],
        "properties": {
          "name":             { "type": "string", "title": "Name" },
          "organization":     { "type": "string", "title": "Organization" },
          "country":          { "type": "string", "title": "Country", "default": "USA" },
          "project_role":     { "type": "string", "title": "Project Role" },
          "percent_effort":   { "type": "number", "title": "% Effort", "minimum": 0, "maximum": 100 },
          "is_subcontract":   { "type": "boolean", "title": "Subcontract", "default": false }
        }
      }
    },
    "overview_notes":               { "type": "string", "title": "Overview Notes" },

    "safety_q1":          { "type": "string", "title": "1. Programmatic Record of Environmental Compliance (REC) available?", "enum": ["yes", "no"] },
    "programmatic_rec":   { "type": "string", "title": "Selected Programmatic REC" },
    "safety_q2":          { "type": "string", "title": "2. Involves Army-provided infectious agents?", "enum": ["yes", "no"] },
    "safety_q3":          { "type": "string", "title": "3. Involves Biological Select Agents or Toxins (BSAT)?", "enum": ["yes", "no"] },
    "safety_q4":          { "type": "string", "title": "4. Involves specific chemical agents?", "enum": ["yes", "no"] },
    "safety_q5":          { "type": "string", "title": "5. Involves pesticides outside of established lab?", "enum": ["yes", "no"] },
    "safety_q6":          { "type": "string", "title": "6. Potential likelihood of significant negative effects on public health, safety, or environment?", "enum": ["yes", "no"] },
    "safety_q7":          { "type": "string", "title": "7. Does the project involve ionizing radiation or radioactive materials?", "enum": ["yes", "no"] },
    "safety_q8":          { "type": "string", "title": "8. Has a NEPA compliance determination been completed?", "enum": ["yes", "no", "not_applicable"] },
    "safety_notes":       { "type": "string", "title": "Note" },

    "animal_q1":          { "type": "string", "title": "1. Animals used?", "enum": ["yes", "no"] },
    "animal_species":     { "type": "array", "title": "Selected Animal Species", "items": { "type": "string", "enum": ["Rodents (mouse/rat/hamster)", "Non-Human Primates (NHP)", "Pigs", "Rabbits", "Dogs", "Service animals", "Cats", "Marine mammals", "Ferrets", "Birds", "Fish/Aquatic", "Other"] }, "uniqueItems": true },
    "animal_q2":          { "type": "string", "title": "2. Will any DOD-funded animal studies be performed at a site outside the US?", "enum": ["yes", "no"] },
    "animal_q3":          { "type": "string", "title": "3. Will any DOD-funded animal studies be performed at a site in a foreign country of concern (FCOC)?", "enum": ["yes", "no", "unknown"] },
    "animal_q4":          { "type": "string", "title": "4. Has a valid IACUC protocol approval been obtained or submitted?", "enum": ["approved", "submitted", "not_yet_submitted", "not_applicable"] },
    "iacuc_protocol_number": { "type": "string", "title": "IACUC Protocol Number (if available)" },
    "animal_q5":          { "type": "string", "title": "5. Does this project involve endangered or protected species?", "enum": ["yes", "no"] },
    "animal_start_date":  { "type": "string", "title": "6. Estimated Animal Research Start (if known)" },
    "animal_notes":       { "type": "string", "title": "Notes" },

    "no_review_default_no": { "type": "boolean", "title": "No - All responses below will be defaulted to NO" },
    "human_s1_q1":        { "type": "string", "title": "1. Commercially available human cell lines, including cadaveric and HEK293 cells", "enum": ["yes", "no"] },
    "human_s1_q2":        { "type": "string", "title": "2. Commercially available human organoids", "enum": ["yes", "no"] },
    "human_s1_q3":        { "type": "string", "title": "3. Commercially available POOLED human products", "enum": ["yes", "no"] },
    "human_s1_q4":        { "type": "string", "title": "4. Established, existing patient-derived xenograft (PDX) models", "enum": ["yes", "no"] },
    "human_s1_q5":        { "type": "string", "title": "5. Commercial services", "enum": ["yes", "no"] },
    "human_s1_notes":     { "type": "string", "title": "Note" },

    "has_default_no":     { "type": "boolean", "title": "No - All responses below will be defaulted to NO" },
    "human_has_q1":       { "type": "string", "title": "1. Collecting human specimens prospectively for research purposes", "enum": ["yes", "no"] },
    "human_has_q2":       { "type": "string", "title": "2. Human cell lines that cannot be purchased from a vendor", "enum": ["yes", "no"] },
    "human_has_q3":       { "type": "string", "title": "3. Commercially available human anatomical substances (non-pooled)", "enum": ["yes", "no"] },
    "human_has_q4":       { "type": "string", "title": "4. Creation of new patient-derived xenograft (PDX) models from human tissue samples", "enum": ["yes", "no"] },
    "human_has_q5":       { "type": "string", "title": "5. Using human specimens obtained through clinical trials, research studies, collaborations, biobanks, or clinical departments", "enum": ["yes", "no"] },
    "human_has_q6":       { "type": "string", "title": "6. Cadavers or post-mortem human specimens", "enum": ["yes", "no"] },
    "human_has_q7":       { "type": "string", "title": "7. Unique or regulated sample types, e.g., human embryonic stem cell lines", "enum": ["yes", "no"] },
    "human_has_notes":    { "type": "string", "title": "Note" },

    "human_ds_q1":        { "type": "string", "title": "1. Does the project involve secondary use of human data?", "enum": ["yes", "no"] },
    "human_ds_notes":     { "type": "string", "title": "Note" },

    "human_hs_q1":        { "type": "string", "title": "1. Interaction/intervention with human subjects?", "enum": ["yes", "no"] },
    "human_hs_q2":        { "type": "string", "title": "2. A Clinical trial?", "enum": ["yes", "no"] },
    "ct_fda_q1":          { "type": "string", "title": "a. Is it FDA regulated?", "enum": ["yes", "no"] },
    "ct_nonus_q1":        { "type": "string", "title": "b. Will any clinical trial sites be located outside the US?", "enum": ["yes", "no"] },
    "human_hs_notes":     { "type": "string", "title": "Note" },

    "human_ost_q1":       { "type": "string", "title": "1. Situations requiring additional Human Research Regulatory Agency/DOD review?", "enum": ["yes", "no"] },
    "human_ost_notes":    { "type": "string", "title": "Note" },

    "estimated_start_date": { "type": "string", "title": "1. Estimated Human Research Start Date (if known)" },

    "acq_personnel_qualifications":      { "type": "string", "title": "i. Are the type/qualifications of proposed personnel appropriate?", "enum": ["yes", "no"] },
    "acq_personnel_effort":              { "type": "string", "title": "ii. Is the level of effort of proposed personnel appropriate?", "enum": ["yes", "no"] },
    "acq_personnel_salary_cap":          { "type": "string", "title": "iii. Are proposed salaries within the applicable salary cap?", "enum": ["yes", "no", "not_applicable"] },
    "acq_personnel_fringe_rate":         { "type": "string", "title": "iv. Are the proposed fringe benefit rates reasonable?", "enum": ["yes", "no"] },
    "acq_personnel_notes":               { "type": "string", "title": "Note" },

    "acq_equip_included":                { "type": "string", "title": "i. Are any equipment costs included in the proposed budget?", "enum": ["yes", "no"] },
    "acq_equip_necessary":               { "type": "string", "title": "ii. Is the equipment necessary to conduct the project?", "enum": ["yes", "no"] },
    "acq_equip_cost_appropriate":        { "type": "string", "title": "iii. In general, does the cost appear to be appropriate?", "enum": ["yes", "no"] },
    "acq_equip_notes":                   { "type": "string", "title": "Note" },

    "acq_travel_included":               { "type": "string", "title": "i. Are funds for travel included in the proposed budget?", "enum": ["yes", "no"] },
    "acq_travel_appropriate":            { "type": "string", "title": "ii. Are the number and type(s) of trip(s) appropriate for the project?", "enum": ["yes", "no"] },
    "acq_travel_notes":                  { "type": "string", "title": "Note" },

    "acq_materials_included":            { "type": "string", "title": "i. Are funds for materials, supplies and consumables included?", "enum": ["yes", "no"] },
    "acq_materials_appropriate":         { "type": "string", "title": "ii. Are the types and quantities of proposed items appropriate?", "enum": ["yes", "no"] },
    "acq_materials_cost_appropriate":    { "type": "string", "title": "iii. Do the costs appear to be appropriate?", "enum": ["yes", "no"] },
    "acq_materials_notes":               { "type": "string", "title": "Note" },

    "acq_consultant_included":           { "type": "string", "title": "i. Are funds for consultant(s)/collaborator(s) included?", "enum": ["yes", "no"] },
    "acq_consultant_necessary":          { "type": "string", "title": "ii. Is the proposed consultant(s)/collaborator(s) necessary?", "enum": ["yes", "no"] },
    "acq_consultant_duties_described":   { "type": "string", "title": "iii. Are the duties sufficiently described?", "enum": ["yes", "no"] },
    "acq_consultant_costs_appropriate":  { "type": "string", "title": "iv. Do the costs/fees appear to be appropriate?", "enum": ["yes", "no"] },
    "acq_consultant_notes":              { "type": "string", "title": "Note" },

    "acq_third_party_included":          { "type": "string", "title": "i. Are funds for a 3rd party included in the proposed budget?", "enum": ["yes", "no"] },
    "acq_third_party_value_added":       { "type": "string", "title": "ii. Is the 3rd party providing value added to the project?", "enum": ["yes", "no"] },
    "acq_third_party_work_described":    { "type": "string", "title": "iii. Is the work to be performed sufficiently described?", "enum": ["yes", "no"] },
    "acq_third_party_budget_concerns":   { "type": "string", "title": "iv. Are there any concerns with the associated budget?", "enum": ["yes", "no"] },
    "acq_third_party_notes":             { "type": "string", "title": "Note" },

    "acq_other_direct_included":         { "type": "string", "title": "i. Are funds for Other Direct Costs included?", "enum": ["yes", "no"] },
    "acq_other_direct_justified":        { "type": "string", "title": "ii. Are the costs necessary and/or fully justified?", "enum": ["yes", "no"] },
    "acq_other_direct_breakdown":        { "type": "string", "title": "iii. Is the breakdown sufficient?", "enum": ["yes", "no"] },
    "acq_other_direct_notes":            { "type": "string", "title": "Note" },

    "acq_additional_has_concerns":       { "type": "string", "title": "Do you have any other budget-related concerns?", "enum": ["yes", "no"] },
    "acq_additional_notes":              { "type": "string", "title": "Note" },

    "acq_peer_review_score":             { "type": "number", "title": "Overall Review Score (if applicable)", "minimum": 1, "maximum": 10 },
    "acq_peer_review_outcome":           { "type": "string", "title": "Review Recommendation", "enum": ["fund", "fund_with_modifications", "defer", "do_not_fund"] },
    "acq_peer_comments":                 { "type": "string", "title": "Note (Required)" },

    "acq_sow_comments":                  { "type": "string", "title": "Note (Required)" },

    "acq_cps_received":                  { "type": "string", "title": "a. Has an updated and certified CPS document been received?", "enum": ["yes", "no"] },
    "acq_cps_foreign_influence":         { "type": "string", "title": "b. Has foreign influence screening been completed?", "enum": ["yes", "no", "in_progress"] },
    "acq_cps_overlap_identified":        { "type": "string", "title": "c. Has any scientific, budgetary, or commitment overlap been identified?", "enum": ["yes", "no"] },
    "acq_cps_comments":                  { "type": "string", "title": "Note (Required)" },

    "acq_ier_applicable":                { "type": "string", "title": "a. Is the IER requirement applicable for this project?", "enum": ["yes", "no", "unclear"] },
    "acq_ier_comment":                   { "type": "string", "title": "Note (required if Unclear)" },
    "acq_ier_plan_included":             { "type": "string", "title": "b. Was a Planned IER Report Included with the proposal?", "enum": ["yes", "no"] },
    "acq_ier_plan_notes":                { "type": "string", "title": "Note" },

    "acq_dmp_received":                  { "type": "string", "title": "a. Has an acceptable Data Management Plan been received?", "enum": ["yes", "no"] },
    "acq_dmp_repository_identified":     { "type": "string", "title": "b. Has a designated data repository been identified?", "enum": ["yes", "no", "not_applicable"] },
    "acq_dmp_sharing_timeline":          { "type": "string", "title": "c. Is the proposed data sharing timeline consistent with DOD policy?", "enum": ["yes", "no"] },
    "acq_dmp_notes":                     { "type": "string", "title": "Note" },

    "acq_special_requirements":          { "type": "array", "title": "Selected Requirements", "items": { "type": "string", "enum": ["Cooperative Agreement", "Option Years", "ClinicalTrials.gov submission", "FDA Regulatory Requirement", "Payment Restrictions for Human Use Approval", "Participation in IPR Meetings", "Participation in Milestone Meetings", "Federal TBI Registry submission (FITBIR)", "National Database for Autism Research", "National Institute of Mental Health Data Archive", "Additional Key Personnel", "Minimum Effort for the PI", "Annual Inclusion Enrollment Reporting", "Annual Quad Charts", "Award Expiration Transition Plan", "Quarterly Reports", "Quarterly Quad Chart", "Semi Annual Reports", "Semi Annual Quad Chart", "Annual Reports", "Final Report"] }, "uniqueItems": true },
    "acq_special_notes":                 { "type": "string", "title": "Note (optional)" },

    "scientific_overlap":      { "type": "string", "title": "Was scientific overlap identified during negotiations?", "enum": ["yes", "no"] },
    "foreign_involvement":     { "type": "string", "title": "Was this project reported to RISG for foreign involvement?", "enum": ["yes", "no"] },
    "risg_approval":           { "type": "string", "title": "Does this project have RISG approval to proceed?", "enum": ["yes", "no"] },
    "so_recommendation":       { "type": "string", "title": "SO Recommendation", "enum": ["approval", "disapproval"] },
    "so_comments":             { "type": "string", "title": "SO Comments" },
    "gor_recommendation":      { "type": "string", "title": "GOR/COR Recommendation", "enum": ["approval", "disapproval"] },
    "gor_comments":            { "type": "string", "title": "GOR/COR Comments" }
  }
}'::jsonb, '{}'::jsonb, '{}'::jsonb);

-- ============================================
-- Page Layout configuration (drives version selector)
-- ============================================
INSERT INTO form_configurations (form_key, title, description, json_schema, ui_schema, default_data) VALUES
('page_layout', 'Page Layout', 'Controls page-level labels and section visibility per version', '{
  "title": "Page Layout",
  "type": "object",
  "properties": {
    "overview_header": { "type": "string", "title": "Overview Header" },
    "review_header": { "type": "string", "title": "Review Header" }
  }
}'::jsonb, '{}'::jsonb, '{"overview_header": "Pre-Award Overview", "review_header": "Pre-Award / Negotiations Review"}'::jsonb);

-- ============================================
-- Seed demo award
-- ============================================
INSERT INTO awards (
  log_number, award_number, award_mechanism, principal_investigator,
  performing_organization, contracting_organization, period_of_performance,
  award_amount, program_office, program, science_officer, gor_cor,
  pi_budget, final_recommended_budget, program_manager,
  contract_grants_specialist, branch_chief, prime_award_type
) VALUES (
  'TE020005', '12369',
  'Investigator-Initiated Research with Optional Nested Post-doctoral Traineeship(s)',
  'Bill Jones',
  'Activ Surgical Robotics (Omniboros)',
  '10493279 CANADA INC',
  '9/1/2015 to 8/31/2018',
  500001.00,
  'Congressionally Directed Medical Research Programs',
  'Test Data Program',
  'Pending Assignment',
  'Naba Bora',
  1448199.00, 1448199.00, 'Naba Bora',
  'Pending Assignment', 'Pending Assignment', 'extramural'
);

-- ============================================
-- Seed demo personnel
INSERT INTO project_personnel (award_id, organization, country, project_role, name, is_subcontract)
SELECT a.id, 'Johns Hopkins University', 'USA', 'PI/PD', 'John Smith', false
FROM awards a WHERE a.log_number = 'TE020005';

INSERT INTO project_personnel (award_id, organization, country, project_role, name, is_subcontract)
SELECT a.id, 'Walter Reed Medical', 'USA', 'Co-Investigator', 'Walter White', false
FROM awards a WHERE a.log_number = 'TE020005';

-- Seed ONE composite form submission per award
-- ============================================
INSERT INTO form_submissions (award_id, form_config_id, form_key, status, section_status, form_data)
SELECT a.id, fc.id, 'pre_award_composite', 'in_progress',
  '{
    "overview": "in_progress",
    "safety_review": "not_started",
    "animal_review": "not_started",
    "human_no_regulatory": "not_started",
    "human_anatomical": "not_started",
    "human_data_secondary": "not_started",
    "human_subjects": "not_started",
    "human_special_topics": "not_started",
    "human_estimated_start": "not_started",
    "acq_br_personnel": "not_started",
    "acq_br_equipment": "not_started",
    "acq_br_travel": "not_started",
    "acq_br_materials": "not_started",
    "acq_br_consultant": "not_started",
    "acq_br_third_party": "not_started",
    "acq_br_other_direct": "not_started",
    "acq_br_additional": "not_started",
    "acq_peer_review": "not_started",
    "acq_sow_concerns": "not_started",
    "acq_cps": "not_started",
    "acq_ier": "not_started",
    "acq_data_management": "not_started",
    "acq_special_requirements": "not_started",
    "final_recommendation": "not_started"
  }'::jsonb,
  jsonb_build_object(
    'pi_budget', a.pi_budget,
    'final_recommended_budget', a.final_recommended_budget,
    'program_manager', a.program_manager,
    'contract_grants_specialist', a.contract_grants_specialist,
    'branch_chief', a.branch_chief,
    'prime_award_type', a.prime_award_type,
    'personnel', '[
      {"name": "John Smith", "organization": "Johns Hopkins University", "country": "USA", "project_role": "PI/PD", "is_subcontract": false},
      {"name": "Walter White", "organization": "Walter Reed Medical", "country": "USA", "project_role": "Co-Investigator", "is_subcontract": false}
    ]'::jsonb
  )
FROM awards a
CROSS JOIN form_configurations fc
WHERE a.log_number = 'TE020005' AND fc.form_key = 'pre_award_composite';

-- ============================================
-- Schema versions
-- ============================================

-- Composite form v1 (current)
INSERT INTO form_schema_versions (form_id, version, json_schema, ui_schema, default_data, is_current, change_notes)
SELECT id, 1, json_schema, ui_schema, COALESCE(default_data, '{}'::jsonb), true,
       'Initial composite version — all sections merged into flat namespace'
FROM form_configurations WHERE form_key = 'pre_award_composite';

-- Page layout v1
INSERT INTO form_schema_versions (form_id, version, json_schema, ui_schema, default_data, is_current, change_notes)
SELECT id, 1, json_schema, ui_schema,
  '{"overview_header": "Pre-Award Overview", "review_header": "Pre-Award / Negotiations Review"}'::jsonb,
  false, 'Initial layout — original headers'
FROM form_configurations WHERE form_key = 'page_layout'
ON CONFLICT (form_id, version) DO UPDATE SET is_current = false;

-- Page layout v2 (current)
INSERT INTO form_schema_versions (form_id, version, json_schema, ui_schema, default_data, is_current, change_notes)
SELECT id, 2, json_schema, ui_schema,
  '{"overview_header": "Award Overview & Personnel", "review_header": "Pre-Award Compliance & Negotiations Review"}'::jsonb,
  true, 'Updated headers for clarity'
FROM form_configurations WHERE form_key = 'page_layout'
ON CONFLICT (form_id, version) DO NOTHING;

-- Pin submission to composite v1
UPDATE form_submissions fs
SET schema_version_id = fsv.id, schema_version = 1
FROM form_schema_versions fsv
WHERE fs.form_config_id = fsv.form_id AND fsv.version = 1
  AND fs.schema_version_id IS NULL;

-- ============================================
-- Seed test user: jphipps / test / SO / CDMRP
-- ============================================
INSERT INTO app_users (username, password_hash, display_name, role, organization)
VALUES ('jphipps', '$2b$12$t4sdX/EhG8ck9ZvE8j8r5ucDrt1IKfioRq7d5jObZNuGjevGqKTAe', 'Joshua Phipps', 'SO', 'CDMRP')
ON CONFLICT (username) DO NOTHING;
