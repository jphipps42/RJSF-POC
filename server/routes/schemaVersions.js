const express = require('express');
const pool = require('../db/pool');

const router = express.Router();

// GET /api/schema-versions/:formId - List all versions for a form
router.get('/:formId', async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT id, form_id, version, change_notes, is_current, default_data, created_at
       FROM form_schema_versions
       WHERE form_id = $1
       ORDER BY version DESC`,
      [req.params.formId]
    );
    res.json(result.rows);
  } catch (err) {
    console.error('Error fetching schema versions:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// GET /api/schema-versions/:formId/current - Get current active version
router.get('/:formId/current', async (req, res) => {
  try {
    const result = await pool.query(
      'SELECT * FROM form_schema_versions WHERE form_id = $1 AND is_current = true',
      [req.params.formId]
    );
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'No current version found' });
    }
    res.json(result.rows[0]);
  } catch (err) {
    console.error('Error fetching current schema version:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// GET /api/schema-versions/:formId/:version - Get specific version
router.get('/:formId/:version', async (req, res) => {
  try {
    const result = await pool.query(
      'SELECT * FROM form_schema_versions WHERE form_id = $1 AND version = $2',
      [req.params.formId, parseInt(req.params.version, 10)]
    );
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Version not found' });
    }
    res.json(result.rows[0]);
  } catch (err) {
    console.error('Error fetching schema version:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// POST /api/schema-versions/:formId/publish - Publish a new version
router.post('/:formId/publish', async (req, res) => {
  const client = await pool.connect();
  try {
    const { json_schema, ui_schema, default_data, change_notes, migration_rules } = req.body;
    const formId = req.params.formId;

    if (!json_schema) {
      return res.status(400).json({ error: 'json_schema is required' });
    }

    await client.query('BEGIN');

    // Get current version (lock row to prevent race)
    const curResult = await client.query(
      'SELECT id, version FROM form_schema_versions WHERE form_id = $1 AND is_current = true FOR UPDATE',
      [formId]
    );

    const currentVersion = curResult.rows.length > 0 ? curResult.rows[0].version : 0;
    const nextVersion = currentVersion + 1;

    // Demote old current
    if (curResult.rows.length > 0) {
      await client.query(
        'UPDATE form_schema_versions SET is_current = false WHERE id = $1',
        [curResult.rows[0].id]
      );
    }

    // Insert new version
    const insertResult = await client.query(
      `INSERT INTO form_schema_versions (form_id, version, json_schema, ui_schema, default_data, change_notes, is_current)
       VALUES ($1, $2, $3, $4, $5, $6, true)
       RETURNING *`,
      [formId, nextVersion, json_schema, ui_schema || {}, default_data || {}, change_notes || null]
    );

    // Insert migration rules if provided
    if (migration_rules && Array.isArray(migration_rules) && migration_rules.length > 0) {
      await client.query(
        `INSERT INTO schema_migrations (form_id, from_version, to_version, migration_script)
         VALUES ($1, $2, $3, $4)`,
        [formId, currentVersion, nextVersion, JSON.stringify(migration_rules)]
      );
    }

    // Keep form_configurations.updated_at in sync
    await client.query(
      'UPDATE form_configurations SET updated_at = NOW() WHERE id = $1',
      [formId]
    );

    await client.query('COMMIT');
    res.status(201).json(insertResult.rows[0]);
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('Error publishing schema version:', err);
    res.status(500).json({ error: 'Internal server error' });
  } finally {
    client.release();
  }
});

// PUT /api/schema-versions/:formId/:version/set-current - Rollback to a prior version
router.put('/:formId/:version/set-current', async (req, res) => {
  const client = await pool.connect();
  try {
    const formId = req.params.formId;
    const version = parseInt(req.params.version, 10);

    await client.query('BEGIN');

    // Demote current
    await client.query(
      'UPDATE form_schema_versions SET is_current = false WHERE form_id = $1 AND is_current = true',
      [formId]
    );

    // Promote target version
    const result = await client.query(
      'UPDATE form_schema_versions SET is_current = true WHERE form_id = $1 AND version = $2 RETURNING *',
      [formId, version]
    );

    if (result.rows.length === 0) {
      await client.query('ROLLBACK');
      return res.status(404).json({ error: 'Version not found' });
    }

    await client.query('COMMIT');
    res.json(result.rows[0]);
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('Error setting current version:', err);
    res.status(500).json({ error: 'Internal server error' });
  } finally {
    client.release();
  }
});

module.exports = router;
