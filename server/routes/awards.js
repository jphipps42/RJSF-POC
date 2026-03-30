const express = require('express');
const pool = require('../db/pool');

const router = express.Router();

// GET /api/awards - List all awards
router.get('/', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM awards ORDER BY created_at DESC');
    res.json(result.rows);
  } catch (err) {
    console.error('Error fetching awards:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// GET /api/awards/:id - Get single award with submissions
router.get('/:id', async (req, res) => {
  try {
    const award = await pool.query('SELECT * FROM awards WHERE id = $1', [req.params.id]);
    if (award.rows.length === 0) {
      return res.status(404).json({ error: 'Award not found' });
    }

    const submissions = await pool.query(
      `SELECT fs.*, fc.title as form_title, fc.json_schema, fc.ui_schema
       FROM form_submissions fs
       JOIN form_configurations fc ON fs.form_config_id = fc.id
       WHERE fs.award_id = $1
       ORDER BY fc.form_key`,
      [req.params.id]
    );

    const personnel = await pool.query(
      'SELECT * FROM project_personnel WHERE award_id = $1 ORDER BY name',
      [req.params.id]
    );

    const linkedFiles = await pool.query(
      'SELECT * FROM award_linked_files WHERE award_id = $1 ORDER BY section, created_at DESC',
      [req.params.id]
    );

    res.json({
      ...award.rows[0],
      submissions: submissions.rows,
      personnel: personnel.rows,
      linked_files: linkedFiles.rows,
    });
  } catch (err) {
    console.error('Error fetching award:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// GET /api/awards/by-log/:logNumber - Get award by log number
router.get('/by-log/:logNumber', async (req, res) => {
  try {
    const award = await pool.query('SELECT * FROM awards WHERE log_number = $1', [req.params.logNumber]);
    if (award.rows.length === 0) {
      return res.status(404).json({ error: 'Award not found' });
    }

    const awardId = award.rows[0].id;

    const submissions = await pool.query(
      `SELECT fs.*, fc.title as form_title, fc.json_schema, fc.ui_schema
       FROM form_submissions fs
       JOIN form_configurations fc ON fs.form_config_id = fc.id
       WHERE fs.award_id = $1
       ORDER BY fc.form_key`,
      [awardId]
    );

    const personnel = await pool.query(
      'SELECT * FROM project_personnel WHERE award_id = $1 ORDER BY name',
      [awardId]
    );

    const linkedFiles = await pool.query(
      'SELECT * FROM award_linked_files WHERE award_id = $1 ORDER BY section, created_at DESC',
      [awardId]
    );

    res.json({
      ...award.rows[0],
      submissions: submissions.rows,
      personnel: personnel.rows,
      linked_files: linkedFiles.rows,
    });
  } catch (err) {
    console.error('Error fetching award:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// POST /api/awards - Create new award
router.post('/', async (req, res) => {
  try {
    const {
      log_number, award_number, award_mechanism, principal_investigator,
      performing_organization, contracting_organization, period_of_performance,
      award_amount, program_office, program, science_officer, gor_cor,
      pi_budget, final_recommended_budget, program_manager, prime_award_type,
    } = req.body;

    if (!log_number) {
      return res.status(400).json({ error: 'log_number is required' });
    }

    const result = await pool.query(
      `INSERT INTO awards (
        log_number, award_number, award_mechanism, principal_investigator,
        performing_organization, contracting_organization, period_of_performance,
        award_amount, program_office, program, science_officer, gor_cor,
        pi_budget, final_recommended_budget, program_manager, prime_award_type,
        created_by
      ) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,NULL)
      RETURNING *`,
      [log_number, award_number, award_mechanism, principal_investigator,
       performing_organization, contracting_organization, period_of_performance,
       award_amount, program_office, program, science_officer, gor_cor,
       pi_budget, final_recommended_budget, program_manager, prime_award_type || 'extramural']
    );

    // Create form submissions for this award
    const configs = await pool.query('SELECT id, form_key FROM form_configurations WHERE is_active = true');
    for (const config of configs.rows) {
      await pool.query(
        'INSERT INTO form_submissions (award_id, form_config_id, form_key, status) VALUES ($1, $2, $3, $4)',
        [result.rows[0].id, config.id, config.form_key, 'not_started']
      );
    }

    res.status(201).json(result.rows[0]);
  } catch (err) {
    console.error('Error creating award:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// PUT /api/awards/:id - Update award
router.put('/:id', async (req, res) => {
  try {
    const fields = req.body;
    const setClauses = [];
    const values = [];
    let i = 1;

    for (const [key, value] of Object.entries(fields)) {
      if (key !== 'id') {
        setClauses.push(`${key} = $${i}`);
        values.push(value);
        i++;
      }
    }

    if (setClauses.length === 0) {
      return res.status(400).json({ error: 'No fields to update' });
    }

    setClauses.push(`updated_at = NOW()`);
    values.push(req.params.id);

    const result = await pool.query(
      `UPDATE awards SET ${setClauses.join(', ')} WHERE id = $${i} RETURNING *`,
      values
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Award not found' });
    }
    res.json(result.rows[0]);
  } catch (err) {
    console.error('Error updating award:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

module.exports = router;
