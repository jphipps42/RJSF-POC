-- ============================================
-- RJSF Pre-Award Review Database Schema
-- ============================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Form configurations (JSON Schema + UI Schema for each review section)
CREATE TABLE IF NOT EXISTS form_configurations (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  form_key VARCHAR(100) UNIQUE NOT NULL,        -- e.g. 'safety_review', 'animal_review'
  title VARCHAR(255) NOT NULL,
  description TEXT,
  json_schema JSONB NOT NULL,                    -- The JSON Schema definition
  ui_schema JSONB DEFAULT '{}'::jsonb,           -- The RJSF uiSchema
  default_data JSONB DEFAULT '{}'::jsonb,        -- Default form values
  version INTEGER DEFAULT 1,
  is_active BOOLEAN DEFAULT true,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Award records (the parent entity for all reviews)
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

-- Form submissions (completed form data for each review section per award)
CREATE TABLE IF NOT EXISTS form_submissions (
  id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  award_id UUID NOT NULL REFERENCES awards(id) ON DELETE CASCADE,
  form_config_id UUID NOT NULL REFERENCES form_configurations(id),
  form_key VARCHAR(100) NOT NULL,               -- denormalized for quick lookup
  form_data JSONB NOT NULL DEFAULT '{}'::jsonb,  -- The actual form response data
  status VARCHAR(50) NOT NULL DEFAULT 'not_started',  -- not_started, in_progress, completed, submitted
  submitted_at TIMESTAMP WITH TIME ZONE,
  completion_date TIMESTAMP WITH TIME ZONE,
  is_locked BOOLEAN DEFAULT false,
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
  section VARCHAR(100) NOT NULL,            -- e.g. 'final_budget', 'final_sow', 'overlap_mitigation'
  file_name VARCHAR(255) NOT NULL,
  description TEXT,
  last_updated TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_form_submissions_award ON form_submissions(award_id);
CREATE INDEX idx_form_submissions_form_key ON form_submissions(form_key);
CREATE INDEX idx_form_submissions_status ON form_submissions(status);
CREATE INDEX idx_awards_log_number ON awards(log_number);
CREATE INDEX idx_form_configurations_form_key ON form_configurations(form_key);
CREATE INDEX idx_award_linked_files_award ON award_linked_files(award_id);
CREATE INDEX idx_award_linked_files_section ON award_linked_files(section);

-- ============================================
-- Seed default form configurations
-- ============================================

INSERT INTO form_configurations (form_key, title, description, json_schema, ui_schema) VALUES
('safety_review', 'A. Safety Requirements Review', 'Safety checklist for pre-award review', '{
  "title": "Safety Requirements Review",
  "type": "object",
  "required": [],
  "properties": {
    "safety_q1": {
      "type": "string",
      "title": "1. Programmatic Record of Environmental Compliance (REC) available?",
      "enum": ["yes", "no"]
    },
    "programmatic_rec": {
      "type": "string",
      "title": "Selected Programmatic REC"
    },
    "safety_q2": {
      "type": "string",
      "title": "2. Involves Army-provided infectious agents?",
      "enum": ["yes", "no"]
    },
    "safety_q3": {
      "type": "string",
      "title": "3. Involves Biological Select Agents or Toxins (BSAT)?",
      "enum": ["yes", "no"]
    },
    "safety_q4": {
      "type": "string",
      "title": "4. Involves specific chemical agents?",
      "enum": ["yes", "no"]
    },
    "safety_q5": {
      "type": "string",
      "title": "5. Involves pesticides outside of established lab?",
      "enum": ["yes", "no"]
    },
    "safety_q6": {
      "type": "string",
      "title": "6. Potential likelihood of significant negative effects on public health, safety, or environment?",
      "enum": ["yes", "no"]
    },
    "notes": {
      "type": "string",
      "title": "Note"
    }
  }
}'::jsonb, '{
  "safety_q1": {"ui:widget": "radio"},
  "safety_q2": {"ui:widget": "radio"},
  "safety_q3": {"ui:widget": "radio"},
  "safety_q4": {"ui:widget": "radio"},
  "safety_q5": {"ui:widget": "radio"},
  "safety_q6": {"ui:widget": "radio"},
  "notes": {"ui:widget": "textarea", "ui:options": {"rows": 4}},
  "programmatic_rec": {"ui:widget": "hidden"}
}'::jsonb),

('animal_review', 'B. Animal Research Review Requirements', 'Animal research review checklist', '{
  "title": "Animal Research Review Requirements",
  "type": "object",
  "properties": {
    "animal_q1": {
      "type": "string",
      "title": "1. Animals used?",
      "enum": ["yes", "no"]
    },
    "animal_species": {
      "type": "array",
      "title": "Selected Animal Species",
      "items": {
        "type": "string",
        "enum": ["Rodents (mouse/rat/hamster)", "Non-Human Primates (NHP)", "Pigs", "Rabbits", "Dogs", "Service animals", "Cats", "Marine mammals", "Other"]
      },
      "uniqueItems": true
    },
    "animal_q2": {
      "type": "string",
      "title": "2. Will any DOD-funded animal studies be performed at a site outside the US?",
      "enum": ["yes", "no"]
    },
    "animal_q3": {
      "type": "string",
      "title": "3. Will any DOD-funded animal studies be performed at a site in a foreign country of concern (FCOC)?",
      "enum": ["yes", "no", "unknown"]
    },
    "animal_start_date": {
      "type": "string",
      "title": "5. Estimated Animal Research Start (if known)"
    },
    "notes": {
      "type": "string",
      "title": "Notes"
    }
  }
}'::jsonb, '{
  "animal_q1": {"ui:widget": "radio"},
  "animal_q2": {"ui:widget": "radio"},
  "animal_q3": {"ui:widget": "radio"},
  "animal_species": {"ui:widget": "checkboxes"},
  "notes": {"ui:widget": "textarea", "ui:options": {"rows": 4}}
}'::jsonb),

('human_review', 'C. Human Research Review Requirements', 'Human research review with subsections', '{
  "title": "Human Research Review Requirements",
  "type": "object",
  "properties": {
    "section_a": {
      "type": "object",
      "title": "a. Human Research that DOES NOT Require Regulatory Agency Review",
      "properties": {
        "no_review_default_no": {
          "type": "boolean",
          "title": "No - All responses below will be defaulted to NO"
        },
        "human_s1_q1": {
          "type": "string",
          "title": "1. Commercially available human cell lines, including cadaveric and HEK293 cells",
          "enum": ["yes", "no"]
        },
        "human_s1_q2": {
          "type": "string",
          "title": "2. Commercially available human organoids",
          "enum": ["yes", "no"]
        },
        "human_s1_q3": {
          "type": "string",
          "title": "3. Commercially available POOLED human products (e.g., serum, plasma, RBCs, urine, CSF, etc.)",
          "enum": ["yes", "no"]
        },
        "human_s1_q4": {
          "type": "string",
          "title": "4. Established, existing patient-derived xenograft (PDX) models",
          "enum": ["yes", "no"]
        },
        "human_s1_q5": {
          "type": "string",
          "title": "5. Commercial services",
          "enum": ["yes", "no"]
        },
        "notes": {
          "type": "string",
          "title": "Note"
        }
      }
    },
    "section_b": {
      "type": "object",
      "title": "b. Human Anatomical Substances",
      "properties": {
        "has_default_no": {
          "type": "boolean",
          "title": "No - All responses below will be defaulted to NO"
        },
        "human_has_q1": {
          "type": "string",
          "title": "1. Collecting human specimens prospectively for research purposes",
          "enum": ["yes", "no"]
        },
        "human_has_q2": {
          "type": "string",
          "title": "2. Human cell lines that cannot be purchased from a vendor",
          "enum": ["yes", "no"]
        },
        "human_has_q3": {
          "type": "string",
          "title": "3. Commercially available human anatomical substances (non-pooled)",
          "enum": ["yes", "no"]
        },
        "human_has_q4": {
          "type": "string",
          "title": "4. Creation of new patient-derived xenograft (PDX) models from human tissue samples",
          "enum": ["yes", "no"]
        },
        "human_has_q5": {
          "type": "string",
          "title": "5. Using human specimens obtained through clinical trials, research studies, collaborations, biobanks, or clinical departments",
          "enum": ["yes", "no"]
        },
        "human_has_q6": {
          "type": "string",
          "title": "6. Cadavers or post-mortem human specimens",
          "enum": ["yes", "no"]
        },
        "human_has_q7": {
          "type": "string",
          "title": "7. Unique or regulated sample types, e.g., human embryonic stem cell lines",
          "enum": ["yes", "no"]
        },
        "notes": {
          "type": "string",
          "title": "Note"
        }
      }
    },
    "section_c": {
      "type": "object",
      "title": "c. Human Data - Secondary Use",
      "properties": {
        "human_ds_q1": {
          "type": "string",
          "title": "1. Does the project involve secondary use of human data?",
          "enum": ["yes", "no"]
        },
        "notes": {
          "type": "string",
          "title": "Note"
        }
      }
    },
    "section_d": {
      "type": "object",
      "title": "d. Human Subjects",
      "properties": {
        "human_hs_q1": {
          "type": "string",
          "title": "1. Interaction/intervention with human subjects?",
          "enum": ["yes", "no"]
        },
        "human_hs_q2": {
          "type": "string",
          "title": "2. A Clinical trial?",
          "enum": ["yes", "no"]
        },
        "ct_fda_q1": {
          "type": "string",
          "title": "a. Is it FDA regulated?",
          "enum": ["yes", "no"]
        },
        "ct_nonus_q1": {
          "type": "string",
          "title": "b. Will any clinical trial sites be located outside the US?",
          "enum": ["yes", "no"]
        },
        "notes": {
          "type": "string",
          "title": "Note"
        }
      }
    },
    "section_e": {
      "type": "object",
      "title": "e. Other/Special Topics",
      "properties": {
        "human_ost_q1": {
          "type": "string",
          "title": "1. Situations requiring additional Human Research Regulatory Agency/DOD review?",
          "enum": ["yes", "no"]
        },
        "notes": {
          "type": "string",
          "title": "Note"
        }
      }
    },
    "section_f": {
      "type": "object",
      "title": "f. Estimated Start",
      "properties": {
        "estimated_start_date": {
          "type": "string",
          "title": "1. Estimated Human Research Start Date (if known)"
        }
      }
    }
  }
}'::jsonb, '{
  "section_a": {
    "human_s1_q1": {"ui:widget": "radio"},
    "human_s1_q2": {"ui:widget": "radio"},
    "human_s1_q3": {"ui:widget": "radio"},
    "human_s1_q4": {"ui:widget": "radio"},
    "human_s1_q5": {"ui:widget": "radio"},
    "notes": {"ui:widget": "textarea", "ui:options": {"rows": 3}}
  },
  "section_b": {
    "human_has_q1": {"ui:widget": "radio"},
    "human_has_q2": {"ui:widget": "radio"},
    "human_has_q3": {"ui:widget": "radio"},
    "human_has_q4": {"ui:widget": "radio"},
    "human_has_q5": {"ui:widget": "radio"},
    "human_has_q6": {"ui:widget": "radio"},
    "human_has_q7": {"ui:widget": "radio"},
    "notes": {"ui:widget": "textarea", "ui:options": {"rows": 3}}
  },
  "section_c": {
    "human_ds_q1": {"ui:widget": "radio"},
    "notes": {"ui:widget": "textarea", "ui:options": {"rows": 3}}
  },
  "section_d": {
    "human_hs_q1": {"ui:widget": "radio"},
    "human_hs_q2": {"ui:widget": "radio"},
    "ct_fda_q1": {"ui:widget": "radio"},
    "ct_nonus_q1": {"ui:widget": "radio"},
    "notes": {"ui:widget": "textarea", "ui:options": {"rows": 3}}
  },
  "section_e": {
    "human_ost_q1": {"ui:widget": "radio"},
    "notes": {"ui:widget": "textarea", "ui:options": {"rows": 3}}
  }
}'::jsonb),

('acq_br_personnel', 'a. Personnel', 'Budget review - Personnel', '{"title":"Personnel","type":"object","properties":{"qualifications":{"type":"string","title":"i. Are the type/qualifications of proposed personnel appropriate?","enum":["yes","no"]},"effort":{"type":"string","title":"ii. Is the level of effort of proposed personnel appropriate?","enum":["yes","no"]},"notes":{"type":"string","title":"Note"}}}'::jsonb, '{"qualifications":{"ui:widget":"radio"},"effort":{"ui:widget":"radio"},"notes":{"ui:widget":"textarea","ui:options":{"rows":3}}}'::jsonb),
('acq_br_equipment', 'b. Equipment', 'Budget review - Equipment', '{"title":"Equipment","type":"object","properties":{"included":{"type":"string","title":"i. Are any equipment costs included in the proposed budget?","enum":["yes","no"]},"necessary":{"type":"string","title":"ii. Is the equipment necessary to conduct the project?","enum":["yes","no"]},"cost_appropriate":{"type":"string","title":"iii. In general, does the cost appear to be appropriate?","enum":["yes","no"]},"notes":{"type":"string","title":"Note"}}}'::jsonb, '{"included":{"ui:widget":"radio"},"necessary":{"ui:widget":"radio"},"cost_appropriate":{"ui:widget":"radio"},"notes":{"ui:widget":"textarea","ui:options":{"rows":3}}}'::jsonb),
('acq_br_travel', 'c. Travel', 'Budget review - Travel', '{"title":"Travel","type":"object","properties":{"included":{"type":"string","title":"i. Are funds for travel included in the proposed budget?","enum":["yes","no"]},"appropriate":{"type":"string","title":"ii. Are the number and type(s) of trip(s), along with personnel traveling appropriate for the project?","enum":["yes","no"]},"notes":{"type":"string","title":"Note"}}}'::jsonb, '{"included":{"ui:widget":"radio"},"appropriate":{"ui:widget":"radio"},"notes":{"ui:widget":"textarea","ui:options":{"rows":3}}}'::jsonb),
('acq_br_materials', 'd. Materials, Supplies, Consumables', 'Budget review - Materials', '{"title":"Materials, Supplies, Consumables","type":"object","properties":{"included":{"type":"string","title":"i. Are funds for materials, supplies and consumables included in the proposed budget?","enum":["yes","no"]},"appropriate":{"type":"string","title":"ii. Are the types and quantities of proposed items appropriate/necessary to conduct the project?","enum":["yes","no"]},"cost_appropriate":{"type":"string","title":"iii. Do the costs appear to be appropriate?","enum":["yes","no"]},"notes":{"type":"string","title":"Note"}}}'::jsonb, '{"included":{"ui:widget":"radio"},"appropriate":{"ui:widget":"radio"},"cost_appropriate":{"ui:widget":"radio"},"notes":{"ui:widget":"textarea","ui:options":{"rows":3}}}'::jsonb),
('acq_br_consultant', 'e. Consultant/Collaborator', 'Budget review - Consultant', '{"title":"Consultant/Collaborator","type":"object","properties":{"included":{"type":"string","title":"i. Are funds for consultant(s)/collaborator(s) included in the proposed budget?","enum":["yes","no"]},"necessary":{"type":"string","title":"ii. Is the proposed consultant(s)/collaborator(s) necessary?","enum":["yes","no"]},"duties_described":{"type":"string","title":"iii. Are the duties sufficiently described?","enum":["yes","no"]},"costs_appropriate":{"type":"string","title":"iv. Do the costs/fees appear to be appropriate?","enum":["yes","no"]},"notes":{"type":"string","title":"Note"}}}'::jsonb, '{"included":{"ui:widget":"radio"},"necessary":{"ui:widget":"radio"},"duties_described":{"ui:widget":"radio"},"costs_appropriate":{"ui:widget":"radio"},"notes":{"ui:widget":"textarea","ui:options":{"rows":3}}}'::jsonb),
('acq_br_third_party', 'f. 3rd Party (Subawards/Consortium/Contractual)', 'Budget review - 3rd Party', '{"title":"3rd Party (Subawards/Consortium/Contractual)","type":"object","properties":{"included":{"type":"string","title":"i. Are funds for a 3rd party included in the proposed budget?","enum":["yes","no"]},"value_added":{"type":"string","title":"ii. Is the 3rd party providing value added to the project?","enum":["yes","no"]},"work_described":{"type":"string","title":"iii. Is the work to be performed by the site sufficiently described?","enum":["yes","no"]},"budget_concerns":{"type":"string","title":"iv. Are there any concerns with the associated budget?","enum":["yes","no"]},"notes":{"type":"string","title":"Note"}}}'::jsonb, '{"included":{"ui:widget":"radio"},"value_added":{"ui:widget":"radio"},"work_described":{"ui:widget":"radio"},"budget_concerns":{"ui:widget":"radio"},"notes":{"ui:widget":"textarea","ui:options":{"rows":3}}}'::jsonb),
('acq_br_other_direct', 'g. Other Direct Costs', 'Budget review - Other Direct Costs', '{"title":"Other Direct Costs","type":"object","properties":{"included":{"type":"string","title":"i. Are funds for Other Direct Costs included in the proposed budget?","enum":["yes","no"]},"justified":{"type":"string","title":"ii. Are the costs necessary and/or fully justified?","enum":["yes","no"]},"breakdown_sufficient":{"type":"string","title":"iii. Is the breakdown sufficient?","enum":["yes","no"]},"notes":{"type":"string","title":"Note"}}}'::jsonb, '{"included":{"ui:widget":"radio"},"justified":{"ui:widget":"radio"},"breakdown_sufficient":{"ui:widget":"radio"},"notes":{"ui:widget":"textarea","ui:options":{"rows":3}}}'::jsonb),
('acq_br_additional', 'h. Additional Budget Concerns', 'Budget review - Additional concerns', '{"title":"Additional Budget Concerns","type":"object","properties":{"has_concerns":{"type":"string","title":"Do you have any other budget-related concerns?","enum":["yes","no"]},"notes":{"type":"string","title":"Note"}}}'::jsonb, '{"has_concerns":{"ui:widget":"radio"},"notes":{"ui:widget":"textarea","ui:options":{"rows":3}}}'::jsonb),

('acq_peer_review', '2. Peer and Programmatic Review Recommendations', 'Peer and programmatic review recommendations', '{"title":"Peer and Programmatic Review Recommendations","type":"object","properties":{"comments":{"type":"string","title":"Note (Required)"}}}'::jsonb, '{"comments":{"ui:widget":"textarea","ui:options":{"rows":4}}}'::jsonb),

('acq_sow_concerns', '3. Statement of Work Related Concerns', 'Statement of work related concerns', '{"title":"Statement of Work Related Concerns","type":"object","properties":{"comments":{"type":"string","title":"Note (Required)"}}}'::jsonb, '{"comments":{"ui:widget":"textarea","ui:options":{"rows":4}}}'::jsonb),

('acq_cps', '4. Current and Pending Support (CPS)', 'Current and pending support documentation', '{"title":"Current and Pending Support (CPS)","type":"object","properties":{"cps_received":{"type":"string","title":"a. Has an updated and certified CPS document been received for all key personnel?","enum":["yes","no"]},"comments":{"type":"string","title":"Note (Required)"}}}'::jsonb, '{"cps_received":{"ui:widget":"radio"},"comments":{"ui:widget":"textarea","ui:options":{"rows":4}}}'::jsonb),

('acq_ier', '5. Inclusion Enrollment Report (IER)', 'Inclusion enrollment report review', '{"title":"Inclusion Enrollment Report (IER)","type":"object","properties":{"ier_applicable":{"type":"string","title":"a. Is the IER requirement applicable for this project?","enum":["yes","no","unclear"]},"ier_comment":{"type":"string","title":"Note (required if Unclear)"},"ier_plan_included":{"type":"string","title":"b. Was a Planned IER Report Included with the proposal?","enum":["yes","no"]},"ier_plan_notes":{"type":"string","title":"Note"}}}'::jsonb, '{"ier_applicable":{"ui:widget":"radio"},"ier_comment":{"ui:widget":"textarea","ui:options":{"rows":3}},"ier_plan_included":{"ui:widget":"radio"},"ier_plan_notes":{"ui:widget":"textarea","ui:options":{"rows":3}}}'::jsonb),

('acq_data_management', '6. Data Management Plan', 'Data management plan review', '{"title":"Data Management Plan","type":"object","properties":{"dmp_received":{"type":"string","title":"a. Has an acceptable Data Management Plan been received?","enum":["yes","no"]},"dmp_notes":{"type":"string","title":"Note"}}}'::jsonb, '{"dmp_received":{"ui:widget":"radio"},"dmp_notes":{"ui:widget":"textarea","ui:options":{"rows":3}}}'::jsonb),

('acq_special_requirements', '7. Special Requirements', 'Special requirements for acquisition', '{"title":"Special Requirements","type":"object","properties":{"selected_requirements":{"type":"array","title":"Selected Requirements","items":{"type":"string","enum":["Cooperative Agreement","Option Years","ClinicalTrials.gov submission","FDA Regulatory Requirement","Payment Restrictions for Human Use Approval","Participation in IPR Meetings","Participation in Milestone Meetings","Federal TBI Registry submission (FITBIR)","National Database for Autism Research","National Institute of Mental Health Data Archive","Additional Key Personnel","Minimum Effort for the PI","Annual Inclusion Enrollment Reporting","Annual Quad Charts","Award Expiration Transition Plan","Quarterly Reports","Quarterly Quad Chart","Semi Annual Reports","Semi Annual Quad Chart","Annual Reports","Final Report"]},"uniqueItems":true},"notes":{"type":"string","title":"Note (optional)"}}}'::jsonb, '{"selected_requirements":{"ui:widget":"checkboxes"},"notes":{"ui:widget":"textarea","ui:options":{"rows":3}}}'::jsonb);

-- Seed a demo award matching the POC
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

-- Create default form submissions for the demo award
INSERT INTO form_submissions (award_id, form_config_id, form_key, status)
SELECT a.id, fc.id, fc.form_key, 'not_started'
FROM awards a
CROSS JOIN form_configurations fc
WHERE a.log_number = 'TE020005';

-- Seed demo personnel
INSERT INTO project_personnel (award_id, organization, country, project_role, name, is_subcontract)
SELECT a.id, 'Johns Hopkins University', 'USA', 'PI/PD', 'John Smith', false
FROM awards a WHERE a.log_number = 'TE020005';

INSERT INTO project_personnel (award_id, organization, country, project_role, name, is_subcontract)
SELECT a.id, 'Walter Reed Medical', 'USA', 'Co-Investigator', 'Walter White', false
FROM awards a WHERE a.log_number = 'TE020005';
