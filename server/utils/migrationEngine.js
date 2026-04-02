/**
 * Applies an array of migration rules to a form data object.
 * Pure function — no DB access.
 *
 * Rule types:
 *   { op: 'rename', from: 'oldField', to: 'newField' }
 *   { op: 'set_default', field: 'name', value: 'default' }
 *   { op: 'drop', field: 'name' }
 *   { op: 'transform', field: 'name', fn: 'namedTransform' }
 */

const NAMED_TRANSFORMS = {
  toString: (v) => String(v ?? ''),
  toNumber: (v) => Number(v) || 0,
  toBoolean: (v) => Boolean(v),
  splitComma: (v) => (typeof v === 'string' ? v.split(',').map((s) => s.trim()) : v),
  joinComma: (v) => (Array.isArray(v) ? v.join(', ') : String(v ?? '')),
};

function migrateFormData(data, rules) {
  let result = { ...data };

  for (const rule of rules) {
    switch (rule.op) {
      case 'rename':
        if (rule.from in result) {
          result[rule.to] = result[rule.from];
          delete result[rule.from];
        }
        break;

      case 'set_default':
        if (!(rule.field in result)) {
          result[rule.field] = rule.value;
        }
        break;

      case 'drop':
        delete result[rule.field];
        break;

      case 'transform':
        if (rule.field in result && NAMED_TRANSFORMS[rule.fn]) {
          result[rule.field] = NAMED_TRANSFORMS[rule.fn](result[rule.field]);
        }
        break;

      default:
        console.warn(`Unknown migration rule op: ${rule.op}`);
    }
  }

  return result;
}

/**
 * Loads a submission, determines if migration is needed, and migrates
 * the form data forward to the current schema version.
 *
 * Returns { formData, json_schema, ui_schema, schemaVersion, migrated }
 */
async function migrateSubmissionToCurrentVersion(pool, submissionId) {
  // Get submission with its pinned version info
  const subResult = await pool.query(
    `SELECT fs.form_data, fs.schema_version, fs.form_config_id,
            pinned.json_schema as pinned_schema, pinned.ui_schema as pinned_ui_schema
     FROM form_submissions fs
     LEFT JOIN form_schema_versions pinned ON pinned.id = fs.schema_version_id
     WHERE fs.id = $1`,
    [submissionId]
  );

  if (subResult.rows.length === 0) {
    throw new Error('Submission not found');
  }

  const sub = subResult.rows[0];
  const formId = sub.form_config_id;
  const pinnedVersion = sub.schema_version || 1;

  // Get current version
  const curResult = await pool.query(
    'SELECT * FROM form_schema_versions WHERE form_id = $1 AND is_current = true',
    [formId]
  );

  if (curResult.rows.length === 0) {
    throw new Error('No current schema version found');
  }

  const current = curResult.rows[0];

  // No migration needed
  if (pinnedVersion >= current.version) {
    return {
      formData: sub.form_data,
      json_schema: current.json_schema,
      ui_schema: current.ui_schema,
      schemaVersion: current.version,
      migrated: false,
    };
  }

  // Load migration steps between pinned and current
  const migResult = await pool.query(
    `SELECT migration_script FROM schema_migrations
     WHERE form_id = $1 AND from_version >= $2 AND from_version < $3
     ORDER BY from_version ASC`,
    [formId, pinnedVersion, current.version]
  );

  let migratedData = sub.form_data || {};
  for (const row of migResult.rows) {
    const rules = Array.isArray(row.migration_script) ? row.migration_script : [];
    migratedData = migrateFormData(migratedData, rules);
  }

  return {
    formData: migratedData,
    json_schema: current.json_schema,
    ui_schema: current.ui_schema,
    schemaVersion: current.version,
    migrated: true,
  };
}

module.exports = { migrateFormData, migrateSubmissionToCurrentVersion, NAMED_TRANSFORMS };
