-- Form Field Metadata Table
-- Data dictionary mapping every column in each form data table back to
-- its source question, type, section, and template version.

CREATE TABLE IF NOT EXISTS form_field_metadata (
    id              BIGSERIAL PRIMARY KEY,
    table_name      TEXT        NOT NULL,   -- e.g. form_pre_award_safety
    column_name     TEXT        NOT NULL,   -- e.g. safety_q1
    question_name   TEXT        NOT NULL,   -- Human-readable question title from the schema
    question_type   TEXT        NOT NULL,   -- enum, text, number, boolean, date, jsonb
    section_id      TEXT        NOT NULL,   -- e.g. safety_review, overview
    section_title   TEXT        NOT NULL,   -- e.g. "Safety Review", "Pre-Award Overview"
    sql_type        TEXT        NOT NULL,   -- e.g. TEXT, NUMERIC, BOOLEAN, JSONB, DATE
    nullable        BOOLEAN     NOT NULL DEFAULT true,
    template_version INTEGER    NOT NULL,   -- Transformer template version this mapping belongs to
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(table_name, column_name, template_version)
);

-- ============================================================
-- form_pre_award_overview  (template: pre-award-overview v1)
-- ============================================================
INSERT INTO form_field_metadata (table_name, column_name, question_name, question_type, section_id, section_title, sql_type, nullable, template_version) VALUES
('form_pre_award_overview', 'pi_budget',                    'PI Budget',                                        'number',  'overview', 'Pre-Award Overview', 'NUMERIC', true,  1),
('form_pre_award_overview', 'final_recommended_budget',     'Final Recommended Budget',                         'number',  'overview', 'Pre-Award Overview', 'NUMERIC', true,  1),
('form_pre_award_overview', 'funding_source',               'Funding Source',                                   'text',    'overview', 'Pre-Award Overview', 'TEXT',    true,  1),
('form_pre_award_overview', 'negotiation_status',           'Negotiation Status',                               'text',    'overview', 'Pre-Award Overview', 'TEXT',    true,  1),
('form_pre_award_overview', 'prime_award_type',             'Prime Award (Intra/Extra)',                         'enum',    'overview', 'Pre-Award Overview', 'TEXT',    true,  1),
('form_pre_award_overview', 'program_manager',              'Program Manager',                                  'text',    'overview', 'Pre-Award Overview', 'TEXT',    true,  1),
('form_pre_award_overview', 'co_principal_investigator',    'Co-Principal Investigator',                        'text',    'overview', 'Pre-Award Overview', 'TEXT',    true,  1),
('form_pre_award_overview', 'contract_grants_specialist',   'Contract/Grants Specialist',                       'text',    'overview', 'Pre-Award Overview', 'TEXT',    true,  1),
('form_pre_award_overview', 'branch_chief',                 'Branch Chief',                                     'text',    'overview', 'Pre-Award Overview', 'TEXT',    true,  1),
('form_pre_award_overview', 'pi_notification_date',         'PI Notification Date',                             'date',    'overview', 'Pre-Award Overview', 'DATE',    true,  1),
('form_pre_award_overview', 'personnel',                    'Project Personnel',                                'jsonb',   'overview', 'Pre-Award Overview', 'JSONB',   true,  1),
('form_pre_award_overview', 'overview_notes',               'Overview Notes',                                   'text',    'overview', 'Pre-Award Overview', 'TEXT',    true,  1);

-- ============================================================
-- form_pre_award_safety  (template: pre-award-safety v1)
-- ============================================================
INSERT INTO form_field_metadata (table_name, column_name, question_name, question_type, section_id, section_title, sql_type, nullable, template_version) VALUES
('form_pre_award_safety', 'safety_q1',        '1. Programmatic Record of Environmental Compliance (REC) available?',                             'enum', 'safety_review', 'A. Safety Requirements Review', 'TEXT', true, 1),
('form_pre_award_safety', 'programmatic_rec',  'Selected Programmatic REC',                                                                      'text', 'safety_review', 'A. Safety Requirements Review', 'TEXT', true, 1),
('form_pre_award_safety', 'safety_q2',        '2. Involves Army-provided infectious agents?',                                                    'enum', 'safety_review', 'A. Safety Requirements Review', 'TEXT', true, 1),
('form_pre_award_safety', 'safety_q3',        '3. Involves Biological Select Agents or Toxins (BSAT)?',                                          'enum', 'safety_review', 'A. Safety Requirements Review', 'TEXT', true, 1),
('form_pre_award_safety', 'safety_q4',        '4. Involves specific chemical agents?',                                                           'enum', 'safety_review', 'A. Safety Requirements Review', 'TEXT', true, 1),
('form_pre_award_safety', 'safety_q5',        '5. Involves pesticides outside of established lab?',                                              'enum', 'safety_review', 'A. Safety Requirements Review', 'TEXT', true, 1),
('form_pre_award_safety', 'safety_q6',        '6. Potential likelihood of significant negative effects on public health, safety, or environment?','enum', 'safety_review', 'A. Safety Requirements Review', 'TEXT', true, 1),
('form_pre_award_safety', 'safety_q7',        '7. Does the project involve ionizing radiation or radioactive materials?',                        'enum', 'safety_review', 'A. Safety Requirements Review', 'TEXT', true, 1),
('form_pre_award_safety', 'safety_q8',        '8. Has a NEPA compliance determination been completed?',                                          'enum', 'safety_review', 'A. Safety Requirements Review', 'TEXT', true, 1),
('form_pre_award_safety', 'safety_notes',     'Note',                                                                                            'text', 'safety_review', 'A. Safety Requirements Review', 'TEXT', true, 1);

-- ============================================================
-- form_pre_award_animal  (template: pre-award-animal v1)
-- ============================================================
INSERT INTO form_field_metadata (table_name, column_name, question_name, question_type, section_id, section_title, sql_type, nullable, template_version) VALUES
('form_pre_award_animal', 'animal_q1',              '1. Animals used?',                                                                                   'enum', 'animal_review', 'B. Animal Research Review', 'TEXT',  true, 1),
('form_pre_award_animal', 'animal_species',         'Animal Species',                                                                                     'jsonb', 'animal_review', 'B. Animal Research Review', 'JSONB', true, 1),
('form_pre_award_animal', 'animal_q2',              '2. Will any DOD-funded animal studies be performed at a site outside the US?',                        'enum', 'animal_review', 'B. Animal Research Review', 'TEXT',  true, 1),
('form_pre_award_animal', 'animal_q3',              '3. Will any DOD-funded animal studies be performed at a site in a foreign country of concern (FCOC)?','enum', 'animal_review', 'B. Animal Research Review', 'TEXT',  true, 1),
('form_pre_award_animal', 'animal_q4',              '4. Has a valid IACUC protocol approval been obtained or submitted?',                                 'enum', 'animal_review', 'B. Animal Research Review', 'TEXT',  true, 1),
('form_pre_award_animal', 'iacuc_protocol_number',  'IACUC Protocol Number (if available)',                                                               'text', 'animal_review', 'B. Animal Research Review', 'TEXT',  true, 1),
('form_pre_award_animal', 'animal_q5',              '5. Does this project involve endangered or protected species?',                                      'enum', 'animal_review', 'B. Animal Research Review', 'TEXT',  true, 1),
('form_pre_award_animal', 'animal_start_date',      '6. Estimated Animal Research Start (if known)',                                                      'text', 'animal_review', 'B. Animal Research Review', 'TEXT',  true, 1),
('form_pre_award_animal', 'animal_notes',           'Notes',                                                                                              'text', 'animal_review', 'B. Animal Research Review', 'TEXT',  true, 1);

-- ============================================================
-- form_pre_award_human  (template: pre-award-human v1)
-- ============================================================

-- C.a No Regulatory Review Required
INSERT INTO form_field_metadata (table_name, column_name, question_name, question_type, section_id, section_title, sql_type, nullable, template_version) VALUES
('form_pre_award_human', 'no_review_default_no', 'No - All responses below will be defaulted to NO',                           'boolean', 'human_no_regulatory', 'C.a No Regulatory Review Required',   'BOOLEAN', true, 1),
('form_pre_award_human', 'human_s1_q1',          '1. Commercially available human cell lines',                                 'enum',    'human_no_regulatory', 'C.a No Regulatory Review Required',   'TEXT',    true, 1),
('form_pre_award_human', 'human_s1_q2',          '2. Commercially available human organoids',                                  'enum',    'human_no_regulatory', 'C.a No Regulatory Review Required',   'TEXT',    true, 1),
('form_pre_award_human', 'human_s1_q3',          '3. Commercially available POOLED human products',                            'enum',    'human_no_regulatory', 'C.a No Regulatory Review Required',   'TEXT',    true, 1),
('form_pre_award_human', 'human_s1_q4',          '4. Established, existing patient-derived xenograft (PDX) models',            'enum',    'human_no_regulatory', 'C.a No Regulatory Review Required',   'TEXT',    true, 1),
('form_pre_award_human', 'human_s1_q5',          '5. Commercial services',                                                    'enum',    'human_no_regulatory', 'C.a No Regulatory Review Required',   'TEXT',    true, 1),
('form_pre_award_human', 'human_s1_notes',       'Note',                                                                      'text',    'human_no_regulatory', 'C.a No Regulatory Review Required',   'TEXT',    true, 1),

-- C.b Human Anatomical Substances
('form_pre_award_human', 'has_default_no',       'No - All responses below will be defaulted to NO',                           'boolean', 'human_anatomical',    'C.b Human Anatomical Substances',     'BOOLEAN', true, 1),
('form_pre_award_human', 'human_has_q1',         '1. Collecting human specimens prospectively for research purposes',          'enum',    'human_anatomical',    'C.b Human Anatomical Substances',     'TEXT',    true, 1),
('form_pre_award_human', 'human_has_q2',         '2. Human cell lines that cannot be purchased from a vendor',                 'enum',    'human_anatomical',    'C.b Human Anatomical Substances',     'TEXT',    true, 1),
('form_pre_award_human', 'human_has_q3',         '3. Commercially available human anatomical substances (non-pooled)',         'enum',    'human_anatomical',    'C.b Human Anatomical Substances',     'TEXT',    true, 1),
('form_pre_award_human', 'human_has_q4',         '4. Creation of new patient-derived xenograft (PDX) models',                  'enum',    'human_anatomical',    'C.b Human Anatomical Substances',     'TEXT',    true, 1),
('form_pre_award_human', 'human_has_q5',         '5. Using human specimens obtained through clinical trials',                  'enum',    'human_anatomical',    'C.b Human Anatomical Substances',     'TEXT',    true, 1),
('form_pre_award_human', 'human_has_q6',         '6. Cadavers or post-mortem human specimens',                                'enum',    'human_anatomical',    'C.b Human Anatomical Substances',     'TEXT',    true, 1),
('form_pre_award_human', 'human_has_q7',         '7. Unique or regulated sample types',                                       'enum',    'human_anatomical',    'C.b Human Anatomical Substances',     'TEXT',    true, 1),
('form_pre_award_human', 'human_has_notes',      'Note',                                                                      'text',    'human_anatomical',    'C.b Human Anatomical Substances',     'TEXT',    true, 1),

-- C.c Human Data - Secondary Use
('form_pre_award_human', 'human_ds_q1',          '1. Does the project involve secondary use of human data?',                  'enum',    'human_data_secondary','C.c Human Data - Secondary Use',      'TEXT',    true, 1),
('form_pre_award_human', 'human_ds_notes',       'Note',                                                                      'text',    'human_data_secondary','C.c Human Data - Secondary Use',      'TEXT',    true, 1),

-- C.d Human Subjects
('form_pre_award_human', 'human_hs_q1',          '1. Interaction/intervention with human subjects?',                           'enum',    'human_subjects',      'C.d Human Subjects',                  'TEXT',    true, 1),
('form_pre_award_human', 'human_hs_q2',          '2. A Clinical trial?',                                                      'enum',    'human_subjects',      'C.d Human Subjects',                  'TEXT',    true, 1),
('form_pre_award_human', 'ct_fda_q1',            'a. Is it FDA regulated?',                                                   'enum',    'human_subjects',      'C.d Human Subjects',                  'TEXT',    true, 1),
('form_pre_award_human', 'ct_nonus_q1',          'b. Will any clinical trial sites be located outside the US?',                'enum',    'human_subjects',      'C.d Human Subjects',                  'TEXT',    true, 1),
('form_pre_award_human', 'human_hs_notes',       'Note',                                                                      'text',    'human_subjects',      'C.d Human Subjects',                  'TEXT',    true, 1),

-- C.e Other/Special Topics
('form_pre_award_human', 'human_ost_q1',         '1. Situations requiring additional Human Research Regulatory Agency/DOD review?', 'enum', 'human_special_topics', 'C.e Other/Special Topics',        'TEXT',    true, 1),
('form_pre_award_human', 'human_ost_notes',      'Note',                                                                      'text',    'human_special_topics','C.e Other/Special Topics',            'TEXT',    true, 1),

-- C.f Estimated Start
('form_pre_award_human', 'estimated_start_date', '1. Estimated Human Research Start Date (if known)',                          'text',    'human_estimated_start','C.f Estimated Human Research Start',  'TEXT',    true, 1);

-- ============================================================
-- form_pre_award_acquisition  (template: pre-award-acquisition v1)
-- ============================================================

-- D.1a Personnel
INSERT INTO form_field_metadata (table_name, column_name, question_name, question_type, section_id, section_title, sql_type, nullable, template_version) VALUES
('form_pre_award_acquisition', 'acq_personnel_qualifications', 'i. Are the type/qualifications of proposed personnel appropriate?',           'enum', 'acq_br_personnel',   'D.1a Budget Review - Personnel',                       'TEXT',    true, 1),
('form_pre_award_acquisition', 'acq_personnel_effort',         'ii. Is the level of effort of proposed personnel appropriate?',                'enum', 'acq_br_personnel',   'D.1a Budget Review - Personnel',                       'TEXT',    true, 1),
('form_pre_award_acquisition', 'acq_personnel_salary_cap',     'iii. Are proposed salaries within the applicable salary cap?',                 'enum', 'acq_br_personnel',   'D.1a Budget Review - Personnel',                       'TEXT',    true, 1),
('form_pre_award_acquisition', 'acq_personnel_fringe_rate',    'iv. Are the proposed fringe benefit rates reasonable?',                        'enum', 'acq_br_personnel',   'D.1a Budget Review - Personnel',                       'TEXT',    true, 1),
('form_pre_award_acquisition', 'acq_personnel_notes',          'Note',                                                                        'text', 'acq_br_personnel',   'D.1a Budget Review - Personnel',                       'TEXT',    true, 1),

-- D.1b Equipment
('form_pre_award_acquisition', 'acq_equip_included',           'i. Are any equipment costs included in the proposed budget?',                  'enum', 'acq_br_equipment',   'D.1b Budget Review - Equipment',                       'TEXT',    true, 1),
('form_pre_award_acquisition', 'acq_equip_necessary',          'ii. Is the equipment necessary to conduct the project?',                       'enum', 'acq_br_equipment',   'D.1b Budget Review - Equipment',                       'TEXT',    true, 1),
('form_pre_award_acquisition', 'acq_equip_cost_appropriate',   'iii. In general, does the cost appear to be appropriate?',                     'enum', 'acq_br_equipment',   'D.1b Budget Review - Equipment',                       'TEXT',    true, 1),
('form_pre_award_acquisition', 'acq_equip_notes',              'Note',                                                                        'text', 'acq_br_equipment',   'D.1b Budget Review - Equipment',                       'TEXT',    true, 1),

-- D.1c Travel
('form_pre_award_acquisition', 'acq_travel_included',          'i. Are funds for travel included in the proposed budget?',                     'enum', 'acq_br_travel',      'D.1c Budget Review - Travel',                          'TEXT',    true, 1),
('form_pre_award_acquisition', 'acq_travel_appropriate',       'ii. Are the number and type(s) of trip(s) appropriate?',                       'enum', 'acq_br_travel',      'D.1c Budget Review - Travel',                          'TEXT',    true, 1),
('form_pre_award_acquisition', 'acq_travel_notes',             'Note',                                                                        'text', 'acq_br_travel',      'D.1c Budget Review - Travel',                          'TEXT',    true, 1),

-- D.1d Materials
('form_pre_award_acquisition', 'acq_materials_included',       'i. Are funds for materials, supplies and consumables included?',               'enum', 'acq_br_materials',   'D.1d Budget Review - Materials/Supplies',               'TEXT',    true, 1),
('form_pre_award_acquisition', 'acq_materials_appropriate',    'ii. Are the types and quantities appropriate/necessary?',                      'enum', 'acq_br_materials',   'D.1d Budget Review - Materials/Supplies',               'TEXT',    true, 1),
('form_pre_award_acquisition', 'acq_materials_cost_appropriate','iii. Do the costs appear to be appropriate?',                                 'enum', 'acq_br_materials',   'D.1d Budget Review - Materials/Supplies',               'TEXT',    true, 1),
('form_pre_award_acquisition', 'acq_materials_notes',          'Note',                                                                        'text', 'acq_br_materials',   'D.1d Budget Review - Materials/Supplies',               'TEXT',    true, 1),

-- D.1e Consultant
('form_pre_award_acquisition', 'acq_consultant_included',      'i. Are funds for consultant(s)/collaborator(s) included?',                    'enum', 'acq_br_consultant',  'D.1e Budget Review - Consultant/Collaborator',          'TEXT',    true, 1),
('form_pre_award_acquisition', 'acq_consultant_necessary',     'ii. Is the proposed consultant(s)/collaborator(s) necessary?',                 'enum', 'acq_br_consultant',  'D.1e Budget Review - Consultant/Collaborator',          'TEXT',    true, 1),
('form_pre_award_acquisition', 'acq_consultant_duties_described','iii. Are the duties sufficiently described?',                                'enum', 'acq_br_consultant',  'D.1e Budget Review - Consultant/Collaborator',          'TEXT',    true, 1),
('form_pre_award_acquisition', 'acq_consultant_costs_appropriate','iv. Do the costs/fees appear to be appropriate?',                           'enum', 'acq_br_consultant',  'D.1e Budget Review - Consultant/Collaborator',          'TEXT',    true, 1),
('form_pre_award_acquisition', 'acq_consultant_notes',         'Note',                                                                        'text', 'acq_br_consultant',  'D.1e Budget Review - Consultant/Collaborator',          'TEXT',    true, 1),

-- D.1f Third Party
('form_pre_award_acquisition', 'acq_third_party_included',     'i. Are funds for a 3rd party included?',                                      'enum', 'acq_br_third_party', 'D.1f Budget Review - 3rd Party',                        'TEXT',    true, 1),
('form_pre_award_acquisition', 'acq_third_party_value_added',  'ii. Is the 3rd party providing value added?',                                 'enum', 'acq_br_third_party', 'D.1f Budget Review - 3rd Party',                        'TEXT',    true, 1),
('form_pre_award_acquisition', 'acq_third_party_work_described','iii. Is the work to be performed sufficiently described?',                    'enum', 'acq_br_third_party', 'D.1f Budget Review - 3rd Party',                        'TEXT',    true, 1),
('form_pre_award_acquisition', 'acq_third_party_budget_concerns','iv. Are there any concerns with the associated budget?',                     'enum', 'acq_br_third_party', 'D.1f Budget Review - 3rd Party',                        'TEXT',    true, 1),
('form_pre_award_acquisition', 'acq_third_party_notes',        'Note',                                                                        'text', 'acq_br_third_party', 'D.1f Budget Review - 3rd Party',                        'TEXT',    true, 1),

-- D.1g Other Direct Costs
('form_pre_award_acquisition', 'acq_other_direct_included',    'i. Are funds for Other Direct Costs included?',                               'enum', 'acq_br_other_direct','D.1g Budget Review - Other Direct Costs',               'TEXT',    true, 1),
('form_pre_award_acquisition', 'acq_other_direct_justified',   'ii. Are the costs necessary and/or fully justified?',                          'enum', 'acq_br_other_direct','D.1g Budget Review - Other Direct Costs',               'TEXT',    true, 1),
('form_pre_award_acquisition', 'acq_other_direct_breakdown',   'iii. Is the breakdown sufficient?',                                            'enum', 'acq_br_other_direct','D.1g Budget Review - Other Direct Costs',               'TEXT',    true, 1),
('form_pre_award_acquisition', 'acq_other_direct_notes',       'Note',                                                                        'text', 'acq_br_other_direct','D.1g Budget Review - Other Direct Costs',               'TEXT',    true, 1),

-- D.1h Additional Concerns
('form_pre_award_acquisition', 'acq_additional_has_concerns',  'Do you have any other budget-related concerns?',                               'enum', 'acq_br_additional',  'D.1h Budget Review - Additional Concerns',              'TEXT',    true, 1),
('form_pre_award_acquisition', 'acq_additional_notes',         'Note',                                                                        'text', 'acq_br_additional',  'D.1h Budget Review - Additional Concerns',              'TEXT',    true, 1),

-- D.2 Peer Review
('form_pre_award_acquisition', 'acq_peer_review_score',        'Overall Review Score (if applicable)',                                         'number','acq_peer_review',   'D.2 Peer and Programmatic Review',                      'NUMERIC', true, 1),
('form_pre_award_acquisition', 'acq_peer_review_outcome',      'Review Recommendation',                                                       'enum', 'acq_peer_review',    'D.2 Peer and Programmatic Review',                      'TEXT',    true, 1),
('form_pre_award_acquisition', 'acq_peer_comments',            'Note (Required)',                                                              'text', 'acq_peer_review',    'D.2 Peer and Programmatic Review',                      'TEXT',    true, 1),

-- D.3 SOW Concerns
('form_pre_award_acquisition', 'acq_sow_comments',             'Note (Required)',                                                              'text', 'acq_sow_concerns',   'D.3 Statement of Work Concerns',                        'TEXT',    true, 1),

-- D.4 CPS
('form_pre_award_acquisition', 'acq_cps_received',             'a. Has an updated and certified CPS support document been received?',          'enum', 'acq_cps',             'D.4 Current and Pending Support',                       'TEXT',    true, 1),
('form_pre_award_acquisition', 'acq_cps_foreign_influence',    'b. Has foreign influence screening been completed?',                           'enum', 'acq_cps',             'D.4 Current and Pending Support',                       'TEXT',    true, 1),
('form_pre_award_acquisition', 'acq_cps_overlap_identified',   'c. Has any scientific, budgetary, or commitment overlap been identified?',     'enum', 'acq_cps',             'D.4 Current and Pending Support',                       'TEXT',    true, 1),
('form_pre_award_acquisition', 'acq_cps_comments',             'Note (Required)',                                                              'text', 'acq_cps',             'D.4 Current and Pending Support',                       'TEXT',    true, 1),

-- D.5 IER
('form_pre_award_acquisition', 'acq_ier_applicable',           'a. Is the IER requirement applicable for this project?',                      'enum', 'acq_ier',             'D.5 Inclusion Enrollment Report',                       'TEXT',    true, 1),
('form_pre_award_acquisition', 'acq_ier_comment',              'Note (required if Unclear)',                                                   'text', 'acq_ier',             'D.5 Inclusion Enrollment Report',                       'TEXT',    true, 1),
('form_pre_award_acquisition', 'acq_ier_plan_included',        'b. Was a Planned IER Report Included with the proposal?',                     'enum', 'acq_ier',             'D.5 Inclusion Enrollment Report',                       'TEXT',    true, 1),
('form_pre_award_acquisition', 'acq_ier_plan_notes',           'Note',                                                                        'text', 'acq_ier',             'D.5 Inclusion Enrollment Report',                       'TEXT',    true, 1),

-- D.6 Data Management
('form_pre_award_acquisition', 'acq_dmp_received',             'a. Has an acceptable Data Management Plan been received?',                     'enum', 'acq_data_management', 'D.6 Data Management Plan',                              'TEXT',    true, 1),
('form_pre_award_acquisition', 'acq_dmp_repository_identified','b. Has a designated data repository been identified?',                         'enum', 'acq_data_management', 'D.6 Data Management Plan',                              'TEXT',    true, 1),
('form_pre_award_acquisition', 'acq_dmp_sharing_timeline',     'c. Is the proposed data sharing timeline consistent with DOD policy?',         'enum', 'acq_data_management', 'D.6 Data Management Plan',                              'TEXT',    true, 1),
('form_pre_award_acquisition', 'acq_dmp_notes',                'Note',                                                                        'text', 'acq_data_management', 'D.6 Data Management Plan',                              'TEXT',    true, 1),

-- D.7 Special Requirements
('form_pre_award_acquisition', 'acq_special_requirements',     'Selected Requirements',                                                        'jsonb','acq_special_requirements','D.7 Special Requirements',                           'JSONB',   true, 1),
('form_pre_award_acquisition', 'acq_special_notes',            'Note (optional)',                                                              'text', 'acq_special_requirements','D.7 Special Requirements',                           'TEXT',    true, 1);

-- ============================================================
-- form_pre_award_final  (template: pre-award-final v1)
-- ============================================================
INSERT INTO form_field_metadata (table_name, column_name, question_name, question_type, section_id, section_title, sql_type, nullable, template_version) VALUES
('form_pre_award_final', 'scientific_overlap',   'Was scientific overlap identified during negotiations?',                            'enum', 'final_recommendation', 'Final Recommendation to Award', 'TEXT', true, 1),
('form_pre_award_final', 'foreign_involvement',  'Was this project reported to RISG for any type of foreign involvement?',           'enum', 'final_recommendation', 'Final Recommendation to Award', 'TEXT', true, 1),
('form_pre_award_final', 'risg_approval',        'Does this project have RISG approval to proceed?',                                'enum', 'final_recommendation', 'Final Recommendation to Award', 'TEXT', true, 1),
('form_pre_award_final', 'so_recommendation',    'SO Recommendation',                                                               'enum', 'final_recommendation', 'Final Recommendation to Award', 'TEXT', true, 1),
('form_pre_award_final', 'so_comments',          'SO Comments',                                                                     'text', 'final_recommendation', 'Final Recommendation to Award', 'TEXT', true, 1),
('form_pre_award_final', 'gor_recommendation',   'GOR/COR Recommendation',                                                          'enum', 'final_recommendation', 'Final Recommendation to Award', 'TEXT', true, 1),
('form_pre_award_final', 'gor_comments',         'GOR/COR Comments',                                                                'text', 'final_recommendation', 'Final Recommendation to Award', 'TEXT', true, 1);
