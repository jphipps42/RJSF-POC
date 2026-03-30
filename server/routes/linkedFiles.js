const express = require('express');
const pool = require('../db/pool');

const router = express.Router();

// GET /api/linked-files?award_id=X&section=Y
router.get('/', async (req, res) => {
  try {
    const { award_id, section } = req.query;
    let query = 'SELECT * FROM award_linked_files WHERE 1=1';
    const params = [];
    let i = 1;

    if (award_id) {
      query += ` AND award_id = $${i++}`;
      params.push(award_id);
    }
    if (section) {
      query += ` AND section = $${i++}`;
      params.push(section);
    }
    query += ' ORDER BY created_at DESC';

    const result = await pool.query(query, params);
    res.json(result.rows);
  } catch (err) {
    console.error('Error fetching linked files:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// POST /api/linked-files
router.post('/', async (req, res) => {
  try {
    const { award_id, section, file_name, description } = req.body;
    if (!award_id || !section || !file_name) {
      return res.status(400).json({ error: 'award_id, section, and file_name are required' });
    }

    const result = await pool.query(
      `INSERT INTO award_linked_files (award_id, section, file_name, description, last_updated)
       VALUES ($1, $2, $3, $4, NOW())
       RETURNING *`,
      [award_id, section, file_name, description || null]
    );
    res.status(201).json(result.rows[0]);
  } catch (err) {
    console.error('Error creating linked file:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// PUT /api/linked-files/:id
router.put('/:id', async (req, res) => {
  try {
    const { file_name, description } = req.body;
    const result = await pool.query(
      `UPDATE award_linked_files
       SET file_name = COALESCE($1, file_name),
           description = COALESCE($2, description),
           last_updated = NOW()
       WHERE id = $3
       RETURNING *`,
      [file_name, description, req.params.id]
    );
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Linked file not found' });
    }
    res.json(result.rows[0]);
  } catch (err) {
    console.error('Error updating linked file:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// DELETE /api/linked-files/:id
router.delete('/:id', async (req, res) => {
  try {
    const result = await pool.query(
      'DELETE FROM award_linked_files WHERE id = $1 RETURNING id',
      [req.params.id]
    );
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Linked file not found' });
    }
    res.json({ message: 'Linked file deleted' });
  } catch (err) {
    console.error('Error deleting linked file:', err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

module.exports = router;
