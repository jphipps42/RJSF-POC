const express = require('express');
const pool = require('../db/pool');

const router = express.Router();

// GET /api/form-configurations - List all form configs
router.get('/', async (req, res) => {
  try {
    const result = await pool.query(
      'SELECT * FROM form_configurations WHERE is_active = true ORDER BY form_key'
    );
    res.json(result.rows);
  } catch (err) {
    console.error('Error fetching form configurations:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// GET /api/form-configurations/:formKey - Get single form config by key
router.get('/:formKey', async (req, res) => {
  try {
    const result = await pool.query(
      'SELECT * FROM form_configurations WHERE form_key = $1 AND is_active = true',
      [req.params.formKey]
    );
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Form configuration not found' });
    }
    res.json(result.rows[0]);
  } catch (err) {
    console.error('Error fetching form configuration:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// POST /api/form-configurations - Create a new form config
router.post('/', async (req, res) => {
  try {
    const { form_key, title, description, json_schema, ui_schema, default_data } = req.body;
    if (!form_key || !title || !json_schema) {
      return res.status(400).json({ error: 'form_key, title, and json_schema are required' });
    }

    const result = await pool.query(
      `INSERT INTO form_configurations (form_key, title, description, json_schema, ui_schema, default_data)
       VALUES ($1, $2, $3, $4, $5, $6)
       RETURNING *`,
      [form_key, title, description, json_schema, ui_schema || {}, default_data || {}]
    );
    res.status(201).json(result.rows[0]);
  } catch (err) {
    console.error('Error creating form configuration:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// PUT /api/form-configurations/:id - Update config metadata and publish new schema version
router.put('/:id', async (req, res) => {
  const client = await pool.connect();
  try {
    const { title, description, json_schema, ui_schema, default_data, change_notes, migration_rules } = req.body;
    const formId = req.params.id;

    await client.query('BEGIN');

    // Update form_configurations metadata
    await client.query(
      `UPDATE form_configurations
       SET title = COALESCE($1, title),
           description = COALESCE($2, description),
           json_schema = COALESCE($3, json_schema),
           ui_schema = COALESCE($4, ui_schema),
           default_data = COALESCE($5, default_data),
           version = version + 1,
           updated_at = NOW()
       WHERE id = $6`,
      [title, description, json_schema, ui_schema, default_data, formId]
    );

    // If schema content changed, publish a new version
    if (json_schema) {
      const curResult = await client.query(
        'SELECT id, version FROM form_schema_versions WHERE form_id = $1 AND is_current = true FOR UPDATE',
        [formId]
      );

      const currentVersion = curResult.rows.length > 0 ? curResult.rows[0].version : 0;
      const nextVersion = currentVersion + 1;

      if (curResult.rows.length > 0) {
        await client.query(
          'UPDATE form_schema_versions SET is_current = false WHERE id = $1',
          [curResult.rows[0].id]
        );
      }

      await client.query(
        `INSERT INTO form_schema_versions (form_id, version, json_schema, ui_schema, default_data, change_notes, is_current)
         VALUES ($1, $2, $3, $4, $5, $6, true)`,
        [formId, nextVersion, json_schema, ui_schema || {}, default_data || {}, change_notes || null]
      );

      if (migration_rules && Array.isArray(migration_rules) && migration_rules.length > 0) {
        await client.query(
          `INSERT INTO schema_migrations (form_id, from_version, to_version, migration_script)
           VALUES ($1, $2, $3, $4)`,
          [formId, currentVersion, nextVersion, JSON.stringify(migration_rules)]
        );
      }
    }

    await client.query('COMMIT');

    const result = await pool.query('SELECT * FROM form_configurations WHERE id = $1', [formId]);
    res.json(result.rows[0]);
  } catch (err) {
    await client.query('ROLLBACK');
    console.error('Error updating form configuration:', err);
    res.status(500).json({ error: 'Internal server error' });
  } finally {
    client.release();
  }
});

// DELETE /api/form-configurations/:id - Soft delete
router.delete('/:id', async (req, res) => {
  try {
    const result = await pool.query(
      'UPDATE form_configurations SET is_active = false, updated_at = NOW() WHERE id = $1 RETURNING id',
      [req.params.id]
    );
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Form configuration not found' });
    }
    res.json({ message: 'Form configuration deleted' });
  } catch (err) {
    console.error('Error deleting form configuration:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

module.exports = router;
