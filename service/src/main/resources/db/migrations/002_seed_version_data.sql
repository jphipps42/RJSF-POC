-- Create version 1 entries from existing form_configurations
INSERT INTO form_schema_versions (form_id, version, json_schema, ui_schema, default_data, is_current, change_notes)
SELECT id, 1, json_schema, ui_schema, COALESCE(default_data, '{}'::jsonb), true,
       'Initial version (migrated from form_configurations)'
FROM form_configurations
WHERE is_active = true
ON CONFLICT (form_id, version) DO NOTHING;

-- Pin existing form_submissions to version 1
UPDATE form_submissions fs
SET schema_version_id = fsv.id,
    schema_version = 1
FROM form_schema_versions fsv
WHERE fs.form_config_id = fsv.form_id
  AND fsv.version = 1
  AND fs.schema_version_id IS NULL;
