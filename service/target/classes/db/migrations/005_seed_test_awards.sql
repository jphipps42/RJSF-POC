-- ============================================
-- Seed 10 test awards with personnel and composite submissions
-- ============================================

-- Helper: section_status template (all not_started)
-- Used for each new composite submission

DO $$
DECLARE
    v_award_id UUID;
    v_fc_id UUID;
    v_section_status JSONB := '{
        "overview": "not_started",
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
    }'::jsonb;

BEGIN
    SELECT id INTO v_fc_id FROM form_configurations WHERE form_key = 'pre_award_composite' LIMIT 1;

    -- Award 1: TE020010
    INSERT INTO awards (log_number, award_number, award_mechanism, principal_investigator, performing_organization, contracting_organization, period_of_performance, award_amount, program_office, program, science_officer, gor_cor, pi_budget, final_recommended_budget, program_manager, contract_grants_specialist, branch_chief, prime_award_type)
    VALUES ('TE020010', '24001', 'Peer Reviewed Cancer Research Program', 'Dr. Sarah Chen', 'Massachusetts General Hospital', 'Partners Healthcare', '10/1/2024 to 9/30/2027', 750000, 'CDMRP', 'PRCRP', 'Mark Davis', 'Lisa Park', 850000, 825000, 'Mark Davis', 'Tom Richards', 'Karen Wilson', 'extramural')
    ON CONFLICT (log_number) DO NOTHING
    RETURNING id INTO v_award_id;
    IF v_award_id IS NOT NULL THEN
        INSERT INTO project_personnel (award_id, organization, country, project_role, name, is_subcontract) VALUES (v_award_id, 'Massachusetts General Hospital', 'USA', 'PI/PD', 'Dr. Sarah Chen', false);
        INSERT INTO project_personnel (award_id, organization, country, project_role, name, is_subcontract) VALUES (v_award_id, 'MIT Lincoln Lab', 'USA', 'Co-Investigator', 'Dr. James Liu', false);
        INSERT INTO form_submissions (award_id, form_config_id, form_key, status, section_status, form_data) VALUES (v_award_id, v_fc_id, 'pre_award_composite', 'not_started', v_section_status, jsonb_build_object('pi_budget', 850000, 'final_recommended_budget', 825000, 'program_manager', 'Mark Davis', 'contract_grants_specialist', 'Tom Richards', 'branch_chief', 'Karen Wilson', 'prime_award_type', 'extramural'));
    END IF;

    -- Award 2: TE020011
    INSERT INTO awards (log_number, award_number, award_mechanism, principal_investigator, performing_organization, contracting_organization, period_of_performance, award_amount, program_office, program, science_officer, gor_cor, pi_budget, final_recommended_budget, program_manager, contract_grants_specialist, branch_chief, prime_award_type)
    VALUES ('TE020011', '24002', 'Traumatic Brain Injury Research Program', 'Dr. Michael Torres', 'Walter Reed Army Institute of Research', 'US Army Medical Research', '1/1/2025 to 12/31/2027', 1200000, 'CDMRP', 'TBIR', 'Anna Kowalski', 'Robert Kim', 1350000, 1200000, 'Anna Kowalski', 'Jennifer Adams', 'David Brown', 'intramural')
    ON CONFLICT (log_number) DO NOTHING
    RETURNING id INTO v_award_id;
    IF v_award_id IS NOT NULL THEN
        INSERT INTO project_personnel (award_id, organization, country, project_role, name, is_subcontract) VALUES (v_award_id, 'Walter Reed Army Institute of Research', 'USA', 'PI/PD', 'Dr. Michael Torres', false);
        INSERT INTO project_personnel (award_id, organization, country, project_role, name, is_subcontract) VALUES (v_award_id, 'Uniformed Services University', 'USA', 'Co-Investigator', 'Dr. Emily Sato', false);
        INSERT INTO form_submissions (award_id, form_config_id, form_key, status, section_status, form_data) VALUES (v_award_id, v_fc_id, 'pre_award_composite', 'not_started', v_section_status, jsonb_build_object('pi_budget', 1350000, 'final_recommended_budget', 1200000, 'program_manager', 'Anna Kowalski', 'contract_grants_specialist', 'Jennifer Adams', 'branch_chief', 'David Brown', 'prime_award_type', 'intramural'));
    END IF;

    -- Award 3: TE020012
    INSERT INTO awards (log_number, award_number, award_mechanism, principal_investigator, performing_organization, contracting_organization, period_of_performance, award_amount, program_office, program, science_officer, gor_cor, pi_budget, final_recommended_budget, program_manager, contract_grants_specialist, branch_chief, prime_award_type)
    VALUES ('TE020012', '24003', 'Military Burn Research Program', 'Dr. Patricia Nguyen', 'Stanford University', 'Stanford Medical Center', '7/1/2024 to 6/30/2028', 2100000, 'CDMRP', 'MBRP', 'Steven Hall', 'Maria Garcia', 2250000, 2100000, 'Steven Hall', 'Andrew Clark', 'Susan Martinez', 'extramural')
    ON CONFLICT (log_number) DO NOTHING
    RETURNING id INTO v_award_id;
    IF v_award_id IS NOT NULL THEN
        INSERT INTO project_personnel (award_id, organization, country, project_role, name, is_subcontract) VALUES (v_award_id, 'Stanford University', 'USA', 'PI/PD', 'Dr. Patricia Nguyen', false);
        INSERT INTO project_personnel (award_id, organization, country, project_role, name, is_subcontract) VALUES (v_award_id, 'UC San Francisco', 'USA', 'Co-Investigator', 'Dr. Robert Chang', true);
        INSERT INTO form_submissions (award_id, form_config_id, form_key, status, section_status, form_data) VALUES (v_award_id, v_fc_id, 'pre_award_composite', 'not_started', v_section_status, jsonb_build_object('pi_budget', 2250000, 'final_recommended_budget', 2100000, 'program_manager', 'Steven Hall', 'contract_grants_specialist', 'Andrew Clark', 'branch_chief', 'Susan Martinez', 'prime_award_type', 'extramural'));
    END IF;

    -- Award 4: TE020013
    INSERT INTO awards (log_number, award_number, award_mechanism, principal_investigator, performing_organization, contracting_organization, period_of_performance, award_amount, program_office, program, science_officer, gor_cor, pi_budget, final_recommended_budget, program_manager, contract_grants_specialist, branch_chief, prime_award_type)
    VALUES ('TE020013', '24004', 'Spinal Cord Injury Research Program', 'Dr. David Washington', 'University of Miami', 'Miami Project to Cure Paralysis', '4/1/2025 to 3/31/2028', 980000, 'CDMRP', 'SCIRP', 'Rachel Green', 'Naba Bora', 1050000, 980000, 'Rachel Green', 'Pending Assignment', 'Pending Assignment', 'extramural')
    ON CONFLICT (log_number) DO NOTHING
    RETURNING id INTO v_award_id;
    IF v_award_id IS NOT NULL THEN
        INSERT INTO project_personnel (award_id, organization, country, project_role, name, is_subcontract) VALUES (v_award_id, 'University of Miami', 'USA', 'PI/PD', 'Dr. David Washington', false);
        INSERT INTO form_submissions (award_id, form_config_id, form_key, status, section_status, form_data) VALUES (v_award_id, v_fc_id, 'pre_award_composite', 'not_started', v_section_status, jsonb_build_object('pi_budget', 1050000, 'final_recommended_budget', 980000, 'program_manager', 'Rachel Green', 'contract_grants_specialist', 'Pending Assignment', 'branch_chief', 'Pending Assignment', 'prime_award_type', 'extramural'));
    END IF;

    -- Award 5: TE020014
    INSERT INTO awards (log_number, award_number, award_mechanism, principal_investigator, performing_organization, contracting_organization, period_of_performance, award_amount, program_office, program, science_officer, gor_cor, pi_budget, final_recommended_budget, program_manager, contract_grants_specialist, branch_chief, prime_award_type)
    VALUES ('TE020014', '24005', 'Psychological Health and Resilience Program', 'Dr. Amanda Foster', 'Yale University', 'Yale School of Medicine', '10/1/2025 to 9/30/2028', 650000, 'CDMRP', 'PHRP', 'Naba Bora', 'Lisa Park', 720000, 650000, 'Naba Bora', 'Tom Richards', 'Karen Wilson', 'extramural')
    ON CONFLICT (log_number) DO NOTHING
    RETURNING id INTO v_award_id;
    IF v_award_id IS NOT NULL THEN
        INSERT INTO project_personnel (award_id, organization, country, project_role, name, is_subcontract) VALUES (v_award_id, 'Yale University', 'USA', 'PI/PD', 'Dr. Amanda Foster', false);
        INSERT INTO project_personnel (award_id, organization, country, project_role, name, is_subcontract) VALUES (v_award_id, 'VA Connecticut Healthcare', 'USA', 'Co-Investigator', 'Dr. Kevin O''Brien', false);
        INSERT INTO form_submissions (award_id, form_config_id, form_key, status, section_status, form_data) VALUES (v_award_id, v_fc_id, 'pre_award_composite', 'not_started', v_section_status, jsonb_build_object('pi_budget', 720000, 'final_recommended_budget', 650000, 'program_manager', 'Naba Bora', 'contract_grants_specialist', 'Tom Richards', 'branch_chief', 'Karen Wilson', 'prime_award_type', 'extramural'));
    END IF;

    -- Award 6: TE020015
    INSERT INTO awards (log_number, award_number, award_mechanism, principal_investigator, performing_organization, contracting_organization, period_of_performance, award_amount, program_office, program, science_officer, gor_cor, pi_budget, final_recommended_budget, program_manager, contract_grants_specialist, branch_chief, prime_award_type)
    VALUES ('TE020015', '24006', 'Tick-Borne Disease Research Program', 'Dr. Jonathan Meyer', 'Johns Hopkins University', 'JHU Applied Physics Lab', '1/1/2026 to 12/31/2028', 450000, 'CDMRP', 'TBDRP', 'Mark Davis', 'Robert Kim', 500000, 450000, 'Mark Davis', 'Jennifer Adams', 'David Brown', 'extramural')
    ON CONFLICT (log_number) DO NOTHING
    RETURNING id INTO v_award_id;
    IF v_award_id IS NOT NULL THEN
        INSERT INTO project_personnel (award_id, organization, country, project_role, name, is_subcontract) VALUES (v_award_id, 'Johns Hopkins University', 'USA', 'PI/PD', 'Dr. Jonathan Meyer', false);
        INSERT INTO form_submissions (award_id, form_config_id, form_key, status, section_status, form_data) VALUES (v_award_id, v_fc_id, 'pre_award_composite', 'not_started', v_section_status, jsonb_build_object('pi_budget', 500000, 'final_recommended_budget', 450000, 'program_manager', 'Mark Davis', 'contract_grants_specialist', 'Jennifer Adams', 'branch_chief', 'David Brown', 'prime_award_type', 'extramural'));
    END IF;

    -- Award 7: TE020016
    INSERT INTO awards (log_number, award_number, award_mechanism, principal_investigator, performing_organization, contracting_organization, period_of_performance, award_amount, program_office, program, science_officer, gor_cor, pi_budget, final_recommended_budget, program_manager, contract_grants_specialist, branch_chief, prime_award_type)
    VALUES ('TE020016', '24007', 'Neurofibromatosis Research Program', 'Dr. Lisa Yamamoto', 'Children''s National Medical Center', 'George Washington University', '7/1/2025 to 6/30/2029', 1800000, 'CDMRP', 'NFRP', 'Anna Kowalski', 'Maria Garcia', 1950000, 1800000, 'Anna Kowalski', 'Andrew Clark', 'Susan Martinez', 'extramural')
    ON CONFLICT (log_number) DO NOTHING
    RETURNING id INTO v_award_id;
    IF v_award_id IS NOT NULL THEN
        INSERT INTO project_personnel (award_id, organization, country, project_role, name, is_subcontract) VALUES (v_award_id, 'Children''s National Medical Center', 'USA', 'PI/PD', 'Dr. Lisa Yamamoto', false);
        INSERT INTO project_personnel (award_id, organization, country, project_role, name, is_subcontract) VALUES (v_award_id, 'NIH National Cancer Institute', 'USA', 'Collaborator', 'Dr. Paul Rodriguez', false);
        INSERT INTO project_personnel (award_id, organization, country, project_role, name, is_subcontract) VALUES (v_award_id, 'University of Oxford', 'United Kingdom', 'Co-Investigator', 'Dr. Helen Barnes', true);
        INSERT INTO form_submissions (award_id, form_config_id, form_key, status, section_status, form_data) VALUES (v_award_id, v_fc_id, 'pre_award_composite', 'not_started', v_section_status, jsonb_build_object('pi_budget', 1950000, 'final_recommended_budget', 1800000, 'program_manager', 'Anna Kowalski', 'contract_grants_specialist', 'Andrew Clark', 'branch_chief', 'Susan Martinez', 'prime_award_type', 'extramural'));
    END IF;

    -- Award 8: TE020017
    INSERT INTO awards (log_number, award_number, award_mechanism, principal_investigator, performing_organization, contracting_organization, period_of_performance, award_amount, program_office, program, science_officer, gor_cor, pi_budget, final_recommended_budget, program_manager, contract_grants_specialist, branch_chief, prime_award_type)
    VALUES ('TE020017', '24008', 'Epilepsy Research Program', 'Dr. Christopher Blake', 'Mayo Clinic', 'Mayo Foundation', '10/1/2025 to 9/30/2027', 550000, 'CDMRP', 'ERP', 'Steven Hall', 'Naba Bora', 600000, 550000, 'Steven Hall', 'Tom Richards', 'Karen Wilson', 'extramural')
    ON CONFLICT (log_number) DO NOTHING
    RETURNING id INTO v_award_id;
    IF v_award_id IS NOT NULL THEN
        INSERT INTO project_personnel (award_id, organization, country, project_role, name, is_subcontract) VALUES (v_award_id, 'Mayo Clinic', 'USA', 'PI/PD', 'Dr. Christopher Blake', false);
        INSERT INTO form_submissions (award_id, form_config_id, form_key, status, section_status, form_data) VALUES (v_award_id, v_fc_id, 'pre_award_composite', 'not_started', v_section_status, jsonb_build_object('pi_budget', 600000, 'final_recommended_budget', 550000, 'program_manager', 'Steven Hall', 'contract_grants_specialist', 'Tom Richards', 'branch_chief', 'Karen Wilson', 'prime_award_type', 'extramural'));
    END IF;

    -- Award 9: TE020018
    INSERT INTO awards (log_number, award_number, award_mechanism, principal_investigator, performing_organization, contracting_organization, period_of_performance, award_amount, program_office, program, science_officer, gor_cor, pi_budget, final_recommended_budget, program_manager, contract_grants_specialist, branch_chief, prime_award_type)
    VALUES ('TE020018', '24009', 'Lung Cancer Research Program', 'Dr. Maria Santos', 'Dana-Farber Cancer Institute', 'Harvard Medical School', '4/1/2026 to 3/31/2030', 3200000, 'CDMRP', 'LCRP', 'Rachel Green', 'Lisa Park', 3500000, 3200000, 'Rachel Green', 'Jennifer Adams', 'David Brown', 'extramural')
    ON CONFLICT (log_number) DO NOTHING
    RETURNING id INTO v_award_id;
    IF v_award_id IS NOT NULL THEN
        INSERT INTO project_personnel (award_id, organization, country, project_role, name, is_subcontract) VALUES (v_award_id, 'Dana-Farber Cancer Institute', 'USA', 'PI/PD', 'Dr. Maria Santos', false);
        INSERT INTO project_personnel (award_id, organization, country, project_role, name, is_subcontract) VALUES (v_award_id, 'Brigham and Women''s Hospital', 'USA', 'Co-Investigator', 'Dr. Thomas Chen', false);
        INSERT INTO project_personnel (award_id, organization, country, project_role, name, is_subcontract) VALUES (v_award_id, 'University of Toronto', 'Canada', 'Consultant', 'Dr. Priya Sharma', true);
        INSERT INTO form_submissions (award_id, form_config_id, form_key, status, section_status, form_data) VALUES (v_award_id, v_fc_id, 'pre_award_composite', 'not_started', v_section_status, jsonb_build_object('pi_budget', 3500000, 'final_recommended_budget', 3200000, 'program_manager', 'Rachel Green', 'contract_grants_specialist', 'Jennifer Adams', 'branch_chief', 'David Brown', 'prime_award_type', 'extramural'));
    END IF;

    -- Award 10: TE020019
    INSERT INTO awards (log_number, award_number, award_mechanism, principal_investigator, performing_organization, contracting_organization, period_of_performance, award_amount, program_office, program, science_officer, gor_cor, pi_budget, final_recommended_budget, program_manager, contract_grants_specialist, branch_chief, prime_award_type)
    VALUES ('TE020019', '24010', 'Vision Research Program', 'Dr. Angela Patel', 'Bascom Palmer Eye Institute', 'University of Miami Health', '1/1/2026 to 12/31/2028', 890000, 'CDMRP', 'VRP', 'Naba Bora', 'Robert Kim', 950000, 890000, 'Naba Bora', 'Andrew Clark', 'Susan Martinez', 'extramural_intramural')
    ON CONFLICT (log_number) DO NOTHING
    RETURNING id INTO v_award_id;
    IF v_award_id IS NOT NULL THEN
        INSERT INTO project_personnel (award_id, organization, country, project_role, name, is_subcontract) VALUES (v_award_id, 'Bascom Palmer Eye Institute', 'USA', 'PI/PD', 'Dr. Angela Patel', false);
        INSERT INTO project_personnel (award_id, organization, country, project_role, name, is_subcontract) VALUES (v_award_id, 'US Army Aeromedical Research Lab', 'USA', 'Co-Investigator', 'Dr. William Ross', false);
        INSERT INTO form_submissions (award_id, form_config_id, form_key, status, section_status, form_data) VALUES (v_award_id, v_fc_id, 'pre_award_composite', 'not_started', v_section_status, jsonb_build_object('pi_budget', 950000, 'final_recommended_budget', 890000, 'program_manager', 'Naba Bora', 'contract_grants_specialist', 'Andrew Clark', 'branch_chief', 'Susan Martinez', 'prime_award_type', 'extramural_intramural'));
    END IF;

END $$;
