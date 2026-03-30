const express = require('express');
const pool = require('../db/pool');

const router = express.Router();

// GET /api/personnel?award_id=X
router.get('/', async (req, res) => {
  try {
    const { award_id } = req.query;
    const query = award_id
      ? 'SELECT * FROM project_personnel WHERE award_id = $1 ORDER BY name'
      : 'SELECT * FROM project_personnel ORDER BY name';
    const params = award_id ? [award_id] : [];
    const result = await pool.query(query, params);
    res.json(result.rows);
  } catch (err) {
    console.error('Error fetching personnel:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// POST /api/personnel
router.post('/', async (req, res) => {
  try {
    const { award_id, organization, country, project_role, name, is_subcontract } = req.body;
    const result = await pool.query(
      `INSERT INTO project_personnel (award_id, organization, country, project_role, name, is_subcontract)
       VALUES ($1, $2, $3, $4, $5, $6) RETURNING *`,
      [award_id, organization, country || 'USA', project_role, name, is_subcontract || false]
    );
    res.status(201).json(result.rows[0]);
  } catch (err) {
    console.error('Error creating personnel:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// PUT /api/personnel/:id
router.put('/:id', async (req, res) => {
  try {
    const { organization, country, project_role, name, is_subcontract } = req.body;
    const result = await pool.query(
      `UPDATE project_personnel
       SET organization = COALESCE($1, organization),
           country = COALESCE($2, country),
           project_role = COALESCE($3, project_role),
           name = COALESCE($4, name),
           is_subcontract = COALESCE($5, is_subcontract)
       WHERE id = $6
       RETURNING *`,
      [organization, country, project_role, name, is_subcontract, req.params.id]
    );
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Personnel not found' });
    }
    res.json(result.rows[0]);
  } catch (err) {
    console.error('Error updating personnel:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// DELETE /api/personnel/:id
router.delete('/:id', async (req, res) => {
  try {
    const result = await pool.query('DELETE FROM project_personnel WHERE id = $1 RETURNING id', [req.params.id]);
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Personnel not found' });
    }
    res.json({ message: 'Personnel deleted' });
  } catch (err) {
    console.error('Error deleting personnel:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

module.exports = router;
