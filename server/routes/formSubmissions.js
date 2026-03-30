const express = require('express');
const pool = require('../db/pool');

const router = express.Router();

// GET /api/form-submissions?award_id=X - Get all submissions for an award
router.get('/', async (req, res) => {
  try {
    const { award_id } = req.query;
    let query = 'SELECT fs.*, fc.title as form_title, fc.json_schema, fc.ui_schema FROM form_submissions fs JOIN form_configurations fc ON fs.form_config_id = fc.id';
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
      `SELECT fs.*, fc.title as form_title, fc.json_schema, fc.ui_schema
       FROM form_submissions fs
       JOIN form_configurations fc ON fs.form_config_id = fc.id
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

// GET /api/form-submissions/by-award/:awardId/:formKey - Get submission by award + form key
router.get('/by-award/:awardId/:formKey', async (req, res) => {
  try {
    const result = await pool.query(
      `SELECT fs.*, fc.title as form_title, fc.json_schema, fc.ui_schema
       FROM form_submissions fs
       JOIN form_configurations fc ON fs.form_config_id = fc.id
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

// PUT /api/form-submissions/:id/save - Save draft (update form data)
router.put('/:id/save', async (req, res) => {
  try {
    const { form_data } = req.body;

    // Check if locked
    const check = await pool.query('SELECT is_locked FROM form_submissions WHERE id = $1', [req.params.id]);
    if (check.rows.length === 0) {
      return res.status(404).json({ error: 'Submission not found' });
    }
    if (check.rows[0].is_locked) {
      return res.status(403).json({ error: 'This section has been submitted and is locked' });
    }

    const result = await pool.query(
      `UPDATE form_submissions
       SET form_data = $1, status = 'in_progress', updated_at = NOW()
       WHERE id = $2
       RETURNING *`,
      [JSON.stringify(form_data), req.params.id]
    );
    res.json(result.rows[0]);
  } catch (err) {
    console.error('Error saving submission:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// PUT /api/form-submissions/:id/submit - Submit and lock
router.put('/:id/submit', async (req, res) => {
  try {
    const { form_data } = req.body;

    const check = await pool.query('SELECT is_locked FROM form_submissions WHERE id = $1', [req.params.id]);
    if (check.rows.length === 0) {
      return res.status(404).json({ error: 'Submission not found' });
    }
    if (check.rows[0].is_locked) {
      return res.status(403).json({ error: 'This section has already been submitted' });
    }

    const result = await pool.query(
      `UPDATE form_submissions
       SET form_data = $1,
           status = 'submitted',
           is_locked = true,
           submitted_at = NOW(),
           completion_date = NOW(),
           updated_at = NOW()
       WHERE id = $2
       RETURNING *`,
      [JSON.stringify(form_data), req.params.id]
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
           submitted_by = NULL,
           submitted_at = NULL,
           completion_date = NULL,
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
