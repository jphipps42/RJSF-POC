-- Add Pre-Award Overview as an RJSF form configuration
-- This captures the overview panel fields (budgets, personnel assignments, award classification)
-- that were previously stored only in the awards table and project_personnel table.
-- Personnel are now embedded as an array within the form data.

INSERT INTO form_configurations (form_key, title, description, json_schema, ui_schema, default_data) VALUES
('pre_award_overview', 'Pre-Award Overview', 'Pre-award review overview panel — budgets, personnel assignments, award classification, and project personnel', '{
  "title": "Pre-Award Overview",
  "type": "object",
  "properties": {
    "pi_budget": { "type": "number", "title": "PI Budget" },
    "final_recommended_budget": { "type": "number", "title": "Final Recommended Budget" },
    "program_manager": { "type": "string", "title": "Program Manager" },
    "contract_grants_specialist": { "type": "string", "title": "Contract/Grants Specialist" },
    "branch_chief": { "type": "string", "title": "Branch Chief" },
    "prime_award_type": {
      "type": "string", "title": "Prime Award (Intra/Extra)",
      "enum": ["extramural", "intramural", "extramural_intramural", "intramural_extramural"],
      "default": "extramural"
    },
    "pi_notification_date": { "type": "string", "title": "PI Notification Date", "format": "date" },
    "personnel": {
      "type": "array", "title": "Project Personnel",
      "items": {
        "type": "object",
        "required": ["name", "organization", "project_role"],
        "properties": {
          "name": { "type": "string", "title": "Name" },
          "organization": { "type": "string", "title": "Organization" },
          "country": { "type": "string", "title": "Country", "default": "USA" },
          "project_role": { "type": "string", "title": "Project Role" },
          "is_subcontract": { "type": "boolean", "title": "Subcontract", "default": false }
        }
      }
    },
    "notes": { "type": "string", "title": "Overview Notes" }
  }
}'::jsonb, '{
  "pi_budget": {"ui:placeholder": "Enter PI budget amount"},
  "final_recommended_budget": {"ui:placeholder": "Enter final recommended budget"},
  "program_manager": {"ui:placeholder": "Enter program manager name"},
  "contract_grants_specialist": {"ui:placeholder": "Enter contract/grants specialist name"},
  "branch_chief": {"ui:placeholder": "Enter branch chief name"},
  "prime_award_type": {"ui:widget": "select", "ui:enumNames": ["Extramural Only", "Intragovernmental Only", "Extramural w/Intragovernmental Component", "Intragovernmental w/Extramural Component"]},
  "pi_notification_date": {"ui:widget": "date"},
  "personnel": {"items": {"country": {"ui:placeholder": "USA"}, "is_subcontract": {"ui:widget": "checkbox"}}},
  "notes": {"ui:widget": "textarea", "ui:options": {"rows": 4}},
  "ui:order": ["pi_budget", "final_recommended_budget", "program_manager", "contract_grants_specialist", "branch_chief", "pi_notification_date", "prime_award_type", "personnel", "notes"]
}'::jsonb, '{}'::jsonb)
ON CONFLICT (form_key) DO NOTHING;

-- Create a form_submission for each existing award that does not yet have a pre_award_overview
-- Include personnel from project_personnel table as embedded array
INSERT INTO form_submissions (award_id, form_config_id, form_key, status, form_data)
SELECT a.id, fc.id, 'pre_award_overview', 'in_progress',
       jsonb_build_object(
         'pi_budget', a.pi_budget,
         'final_recommended_budget', a.final_recommended_budget,
         'program_manager', a.program_manager,
         'contract_grants_specialist', a.contract_grants_specialist,
         'branch_chief', a.branch_chief,
         'prime_award_type', a.prime_award_type,
         'personnel', (
           SELECT COALESCE(jsonb_agg(jsonb_build_object(
             'name', pp.name,
             'organization', pp.organization,
             'country', pp.country,
             'project_role', pp.project_role,
             'is_subcontract', pp.is_subcontract
           ) ORDER BY pp.name), '[]'::jsonb)
           FROM project_personnel pp WHERE pp.award_id = a.id
         )
       )
FROM awards a
CROSS JOIN form_configurations fc
WHERE fc.form_key = 'pre_award_overview'
  AND NOT EXISTS (
    SELECT 1 FROM form_submissions fs
    WHERE fs.award_id = a.id AND fs.form_key = 'pre_award_overview'
  );

-- Create schema version 1 for the new form
INSERT INTO form_schema_versions (form_id, version, json_schema, ui_schema, default_data, is_current, change_notes)
SELECT id, 1, json_schema, ui_schema, COALESCE(default_data, '{}'::jsonb), true, 'Initial version'
FROM form_configurations
WHERE form_key = 'pre_award_overview'
ON CONFLICT (form_id, version) DO NOTHING;
