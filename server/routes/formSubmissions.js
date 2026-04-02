const express = require('express');
const pool = require('../db/pool');
const { migrateSubmissionToCurrentVersion } = require('../utils/migrationEngine');

const router = express.Router();

// Helper: get current version for a form config
async function getCurrentVersion(formConfigId) {
  const result = await pool.query(
    'SELECT id, version FROM form_schema_versions WHERE form_id = $1 AND is_current = true',
    [formConfigId]
  );
  return result.rows[0] || null;
}

// GET /api/form-submissions?award_id=X - Get all submissions for an award
router.get('/', async (req, res) => {
  try {
    const { award_id } = req.query;
    let query = `
      SELECT fs.*,
             fc.title as form_title,
             COALESCE(pinned.json_schema, cur.json_schema) as json_schema,
             COALESCE(pinned.ui_schema, cur.ui_schema) as ui_schema,
             COALESCE(fs.schema_version, cur.version) as schema_version,
             cur.version as current_version
      FROM form_submissions fs
      JOIN form_configurations fc ON fs.form_config_id = fc.id
      LEFT JOIN form_schema_versions pinned ON pinned.id = fs.schema_version_id
      LEFT JOIN form_schema_versions cur ON cur.form_id = fc.id AND cur.is_current = true`;
    const params = [];

    if (award_id) {
      query += ' WHERE fs.award_id = $1';
      params.push(award_id);
    }
    query += ' ORDER BY fc.form_key';

    const result = await pool.query(query, params);
    res.json(result.rows);
  } catch (err) {
    console.error('Error fetching form submissions:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// GET /api/form-submissions/:id - Get single submission
router.get('/:id', async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT fs.*,
              fc.title as form_title,
              COALESCE(pinned.json_schema, cur.json_schema) as json_schema,
              COALESCE(pinned.ui_schema, cur.ui_schema) as ui_schema,
              COALESCE(fs.schema_version, cur.version) as schema_version,
              cur.version as current_version
       FROM form_submissions fs
       JOIN form_configurations fc ON fs.form_config_id = fc.id
       LEFT JOIN form_schema_versions pinned ON pinned.id = fs.schema_version_id
       LEFT JOIN form_schema_versions cur ON cur.form_id = fc.id AND cur.is_current = true
       WHERE fs.id = $1`,
      [req.params.id]
    );
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Submission not found' });
    }
    res.json(result.rows[0]);
  } catch (err) {
    console.error('Error fetching submission:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// GET /api/form-submissions/by-award/:awardId/:formKey
router.get('/by-award/:awardId/:formKey', async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT fs.*,
              fc.title as form_title,
              COALESCE(pinned.json_schema, cur.json_schema) as json_schema,
              COALESCE(pinned.ui_schema, cur.ui_schema) as ui_schema,
              COALESCE(fs.schema_version, cur.version) as schema_version,
              cur.version as current_version
       FROM form_submissions fs
       JOIN form_configurations fc ON fs.form_config_id = fc.id
       LEFT JOIN form_schema_versions pinned ON pinned.id = fs.schema_version_id
       LEFT JOIN form_schema_versions cur ON cur.form_id = fc.id AND cur.is_current = true
       WHERE fs.award_id = $1 AND fs.form_key = $2`,
      [req.params.awardId, req.params.formKey]
    );
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Submission not found' });
    }
    res.json(result.rows[0]);
  } catch (err) {
    console.error('Error fetching submission:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// GET /api/form-submissions/:id/for-edit - Get submission with migrated data for editing
router.get('/:id/for-edit', async (req, res) => {
  try {
    const result = await migrateSubmissionToCurrentVersion(pool, req.params.id);
    res.json(result);
  } catch (err) {
    console.error('Error getting submission for edit:', err);
    res.status(500).json({ error: err.message || 'Internal server error' });
  }
});

// GET /api/form-submissions/:id/audit - Get submission with original pinned schema (read-only)
router.get('/:id/audit', async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT fs.form_data, fs.schema_version,
              pinned.json_schema, pinned.ui_schema, pinned.version
       FROM form_submissions fs
       JOIN form_schema_versions pinned ON pinned.id = fs.schema_version_id
       WHERE fs.id = $1`,
      [req.params.id]
    );
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Submission not found or no pinned version' });
    }
    const row = result.rows[0];
    res.json({
      formData: row.form_data,
      json_schema: row.json_schema,
      ui_schema: row.ui_schema,
      schemaVersion: row.version,
    });
  } catch (err) {
    console.error('Error fetching audit submission:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// PUT /api/form-submissions/:id/save - Save draft (pins version on first save)
router.put('/:id/save', async (req, res) => {
  try {
    const { form_data } = req.body;

    const check = await pool.query(
      'SELECT is_locked, form_config_id, schema_version_id FROM form_submissions WHERE id = $1',
      [req.params.id]
    );
    if (check.rows.length === 0) {
      return res.status(404).json({ error: 'Submission not found' });
    }
    if (check.rows[0].is_locked) {
      return res.status(403).json({ error: 'This section has been submitted and is locked' });
    }

    // Pin to current version on first save (draft pinning)
    let versionUpdate = '';
    const params = [JSON.stringify(form_data), req.params.id];

    if (!check.rows[0].schema_version_id) {
      const curVersion = await getCurrentVersion(check.rows[0].form_config_id);
      if (curVersion) {
        versionUpdate = ', schema_version_id = $3, schema_version = $4';
        params.push(curVersion.id, curVersion.version);
      }
    }

    const result = await pool.query(
      `UPDATE form_submissions
       SET form_data = $1, status = 'in_progress', updated_at = NOW()${versionUpdate}
       WHERE id = $2
       RETURNING *`,
      params
    );
    res.json(result.rows[0]);
  } catch (err) {
    console.error('Error saving submission:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// PUT /api/form-submissions/:id/submit - Submit and lock (pins to current version)
router.put('/:id/submit', async (req, res) => {
  try {
    const { form_data } = req.body;

    const check = await pool.query(
      'SELECT is_locked, form_config_id FROM form_submissions WHERE id = $1',
      [req.params.id]
    );
    if (check.rows.length === 0) {
      return res.status(404).json({ error: 'Submission not found' });
    }
    if (check.rows[0].is_locked) {
      return res.status(403).json({ error: 'This section has already been submitted' });
    }

    // Always pin to current version at submit time
    const curVersion = await getCurrentVersion(check.rows[0].form_config_id);

    const result = await pool.query(
      `UPDATE form_submissions
       SET form_data = $1,
           status = 'submitted',
           is_locked = true,
           submitted_at = NOW(),
           completion_date = NOW(),
           schema_version_id = $2,
           schema_version = $3,
           updated_at = NOW()
       WHERE id = $4
       RETURNING *`,
      [JSON.stringify(form_data), curVersion?.id || null, curVersion?.version || null, req.params.id]
    );
    res.json(result.rows[0]);
  } catch (err) {
    console.error('Error submitting:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// PUT /api/form-submissions/:id/reset - Reset a submission
router.put('/:id/reset', async (req, res) => {
  try {
    const result = await pool.query(
      `UPDATE form_submissions
       SET form_data = '{}'::jsonb,
           status = 'not_started',
           is_locked = false,
           submitted_at = NULL,
           completion_date = NULL,
           schema_version_id = NULL,
           schema_version = NULL,
           updated_at = NOW()
       WHERE id = $1
       RETURNING *`,
      [req.params.id]
    );
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Submission not found' });
    }
    res.json(result.rows[0]);
  } catch (err) {
    console.error('Error resetting submission:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

module.exports = router;
