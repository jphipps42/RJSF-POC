-- form_schema_versions: stores each published version of a form definition
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

-- Only one is_current=true per form_id
CREATE UNIQUE INDEX IF NOT EXISTS idx_one_current_version
  ON form_schema_versions (form_id) WHERE is_current = true;

CREATE INDEX IF NOT EXISTS idx_fsv_form_id ON form_schema_versions(form_id);

-- schema_migrations: transformation rules between consecutive versions
CREATE TABLE IF NOT EXISTS schema_migrations (
  id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  form_id          UUID NOT NULL REFERENCES form_configurations(id),
  from_version     INTEGER NOT NULL,
  to_version       INTEGER NOT NULL,
  migration_script JSONB NOT NULL DEFAULT '[]'::jsonb,
  created_at       TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE (form_id, from_version, to_version)
);

-- Add version pinning columns to form_submissions
ALTER TABLE form_submissions
  ADD COLUMN IF NOT EXISTS schema_version_id UUID REFERENCES form_schema_versions(id),
  ADD COLUMN IF NOT EXISTS schema_version INTEGER;
