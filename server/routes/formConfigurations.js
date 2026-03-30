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

// PUT /api/form-configurations/:id - Update a form config
router.put('/:id', async (req, res) => {
  try {
    const { title, description, json_schema, ui_schema, default_data } = req.body;
    const result = await pool.query(
      `UPDATE form_configurations
       SET title = COALESCE($1, title),
           description = COALESCE($2, description),
           json_schema = COALESCE($3, json_schema),
           ui_schema = COALESCE($4, ui_schema),
           default_data = COALESCE($5, default_data),
           version = version + 1,
           updated_at = NOW()
       WHERE id = $6
       RETURNING *`,
      [title, description, json_schema, ui_schema, default_data, req.params.id]
    );
    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Form configuration not found' });
    }
    res.json(result.rows[0]);
  } catch (err) {
    console.error('Error updating form configuration:', err);
    res.status(500).json({ error: 'Internal server error' });
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
