import { useState } from 'react';
import {
  Box, Typography, Button, IconButton,
  Table, TableHead, TableBody, TableRow, TableCell,
  TextField, Checkbox,
  Dialog, DialogTitle, DialogContent, DialogActions,
  FormControlLabel, Snackbar, Alert,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import CheckIcon from '@mui/icons-material/Check';
import CloseIcon from '@mui/icons-material/Close';
import DescriptionIcon from '@mui/icons-material/Description';
import { addPersonnel, updatePersonnel, deletePersonnel } from '../services/api';

const EMPTY_PERSON = { name: '', organization: '', country: 'USA', project_role: '', is_subcontract: false };

const cellSx = { border: '1px solid #93c5fd', p: '4px 6px', fontSize: 11 };
const headerCellSx = { ...cellSx, fontWeight: 700 };
const inputSx = { '& .MuiInputBase-input': { fontSize: 11, p: '3px 6px' } };

/**
 * Custom RJSF field component that renders the personnel array as a data grid table.
 * Calls REST API endpoints directly for add/edit/delete, then syncs via onChange.
 */
export default function PersonnelDataGrid({ formData, onChange, readonly, disabled, schema, formContext }) {
  const [editingIdx, setEditingIdx] = useState(null);
  const [editData, setEditData] = useState({});
  const [addOpen, setAddOpen] = useState(false);
  const [newPerson, setNewPerson] = useState({ ...EMPTY_PERSON });
  const [deleteConfirmIdx, setDeleteConfirmIdx] = useState(null);
  const [saving, setSaving] = useState(false);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });

  const isLocked = readonly || disabled;
  const personnel = formData || [];
  const awardId = formContext?.awardId;

  // Show "Create Record" button when prime award type includes a mixed component
  const primeAwardType = formContext?.primeAwardType || '';
  const showCreateRecord = primeAwardType === 'extramural_intramural' || primeAwardType === 'intramural_extramural';

  // --- Inline Edit ---
  const startEdit = (idx) => {
    const person = personnel[idx];
    setEditingIdx(idx);
    setEditData({
      name: person.name || '',
      organization: person.organization || '',
      country: person.country || 'USA',
      project_role: person.project_role || '',
      is_subcontract: person.is_subcontract || false,
    });
  };

  const cancelEdit = () => {
    setEditingIdx(null);
    setEditData({});
  };

  const saveEdit = async () => {
    if (editingIdx === null) return;
    const person = personnel[editingIdx];
    setSaving(true);
    try {
      if (person.id) {
        // Update existing record via API
        const res = await updatePersonnel(person.id, editData);
        const updated = [...personnel];
        updated[editingIdx] = res.data;
        onChange(updated);
        setSnackbar({ open: true, message: 'Personnel updated.', severity: 'success' });
      } else {
        // Record without an id (shouldn't happen normally, but handle gracefully)
        const updated = [...personnel];
        updated[editingIdx] = { ...person, ...editData };
        onChange(updated);
      }
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.error || 'Update failed', severity: 'error' });
    }
    setEditingIdx(null);
    setEditData({});
    setSaving(false);
  };

  const handleEditField = (field, value) => {
    setEditData((prev) => ({ ...prev, [field]: value }));
  };

  // --- Add ---
  const openAdd = () => {
    setNewPerson({ ...EMPTY_PERSON });
    setAddOpen(true);
  };

  const handleAddField = (field, value) => {
    setNewPerson((prev) => ({ ...prev, [field]: value }));
  };

  const saveNewPerson = async () => {
    if (!newPerson.name || !newPerson.organization || !newPerson.project_role) return;
    if (!awardId) return;
    setSaving(true);
    try {
      const res = await addPersonnel({
        award_id: awardId,
        name: newPerson.name,
        organization: newPerson.organization,
        country: newPerson.country || 'USA',
        project_role: newPerson.project_role,
        is_subcontract: newPerson.is_subcontract || false,
      });
      const updated = [...personnel, res.data];
      onChange(updated);
      setAddOpen(false);
      setSnackbar({ open: true, message: 'Personnel added.', severity: 'success' });
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.error || 'Add failed', severity: 'error' });
    }
    setSaving(false);
  };

  // --- Delete ---
  const handleDelete = async () => {
    if (deleteConfirmIdx === null) return;
    const person = personnel[deleteConfirmIdx];
    setSaving(true);
    try {
      if (person.id) {
        await deletePersonnel(person.id);
      }
      const updated = personnel.filter((_, i) => i !== deleteConfirmIdx);
      onChange(updated);
      setSnackbar({ open: true, message: 'Personnel removed.', severity: 'success' });
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.error || 'Delete failed', severity: 'error' });
    }
    setDeleteConfirmIdx(null);
    setSaving(false);
  };

  // --- Render Cell ---
  const renderCell = (person, field, idx) => {
    if (editingIdx === idx) {
      if (field === 'is_subcontract') {
        return (
          <Checkbox
            size="small"
            checked={editData.is_subcontract}
            onChange={(e) => handleEditField('is_subcontract', e.target.checked)}
            sx={{ p: 0 }}
          />
        );
      }
      return (
        <TextField
          size="small"
          value={editData[field] || ''}
          onChange={(e) => handleEditField(field, e.target.value)}
          sx={inputSx}
          fullWidth
          variant="outlined"
        />
      );
    }
    if (field === 'is_subcontract') {
      return <Checkbox size="small" checked={person.is_subcontract || false} disabled sx={{ p: 0 }} />;
    }
    return person[field] || '';
  };

  return (
    <Box sx={{ mt: 1.5, mb: 1 }}>
      <Typography sx={{ fontWeight: 700, fontSize: 13, mb: 0.5 }}>
        {schema?.title || 'Project Personnel'}
      </Typography>

      <Table size="small" sx={{ '& th, & td': cellSx, mb: 1 }}>
        <TableHead sx={{ bgcolor: '#c8ddf3' }}>
          <TableRow>
            <TableCell sx={headerCellSx}>Organization</TableCell>
            <TableCell sx={headerCellSx}>Country</TableCell>
            <TableCell sx={headerCellSx}>Project Role</TableCell>
            <TableCell sx={headerCellSx}>Name</TableCell>
            <TableCell sx={headerCellSx} align="center">Subcontract</TableCell>
            {!isLocked && <TableCell sx={headerCellSx} align="center">Action</TableCell>}
          </TableRow>
        </TableHead>
        <TableBody>
          {personnel.length === 0 && (
            <TableRow>
              <TableCell colSpan={isLocked ? 5 : 6} sx={{ ...cellSx, textAlign: 'center', color: '#999' }}>
                No personnel added yet.
              </TableCell>
            </TableRow>
          )}
          {personnel.map((person, idx) => (
            <TableRow key={person.id || idx} sx={{ '&:hover': { bgcolor: '#f0f7ff' } }}>
              <TableCell>{renderCell(person, 'organization', idx)}</TableCell>
              <TableCell>{renderCell(person, 'country', idx)}</TableCell>
              <TableCell>{renderCell(person, 'project_role', idx)}</TableCell>
              <TableCell>{renderCell(person, 'name', idx)}</TableCell>
              <TableCell align="center">{renderCell(person, 'is_subcontract', idx)}</TableCell>
              {!isLocked && (
                <TableCell align="center" sx={{ whiteSpace: 'nowrap' }}>
                  {editingIdx === idx ? (
                    <>
                      <IconButton size="small" onClick={saveEdit} disabled={saving} sx={{ color: '#16a34a', p: 0.25 }}>
                        <CheckIcon sx={{ fontSize: 16 }} />
                      </IconButton>
                      <IconButton size="small" onClick={cancelEdit} sx={{ color: '#dc2626', p: 0.25 }}>
                        <CloseIcon sx={{ fontSize: 16 }} />
                      </IconButton>
                    </>
                  ) : (
                    <>
                      <IconButton size="small" onClick={() => startEdit(idx)} disabled={saving} sx={{ color: '#1d4ed8', p: 0.25 }}>
                        <EditIcon sx={{ fontSize: 16 }} />
                      </IconButton>
                      <IconButton size="small" onClick={() => setDeleteConfirmIdx(idx)} disabled={saving} sx={{ color: '#dc2626', p: 0.25 }}>
                        <DeleteIcon sx={{ fontSize: 16 }} />
                      </IconButton>
                      {showCreateRecord && (
                        <Button
                          variant="outlined"
                          size="small"
                          startIcon={<DescriptionIcon sx={{ fontSize: 12 }} />}
                          sx={{
                            fontSize: 9, py: 0, px: 0.75, ml: 0.5, minWidth: 0,
                            lineHeight: 1.8, bgcolor: '#e0ecff', color: '#1d4ed8',
                            borderColor: '#93c5fd', textTransform: 'none',
                          }}
                        >
                          Create Record
                        </Button>
                      )}
                    </>
                  )}
                </TableCell>
              )}
            </TableRow>
          ))}
        </TableBody>
      </Table>

      {!isLocked && (
        <Button
          variant="outlined"
          size="small"
          startIcon={<AddIcon />}
          onClick={openAdd}
          disabled={saving}
          sx={{ mt: 0.5, fontSize: 11 }}
        >
          Add Personnel
        </Button>
      )}

      {/* Add Personnel Modal */}
      <Dialog open={addOpen} onClose={() => setAddOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ bgcolor: '#2563eb', color: '#fff' }}>Add Personnel</DialogTitle>
        <DialogContent sx={{ pt: 3, display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
          <TextField
            label="Name" value={newPerson.name} required fullWidth size="small"
            onChange={(e) => handleAddField('name', e.target.value)}
          />
          <TextField
            label="Organization" value={newPerson.organization} required fullWidth size="small"
            onChange={(e) => handleAddField('organization', e.target.value)}
          />
          <TextField
            label="Country" value={newPerson.country} fullWidth size="small"
            onChange={(e) => handleAddField('country', e.target.value)}
          />
          <TextField
            label="Project Role" value={newPerson.project_role} required fullWidth size="small"
            onChange={(e) => handleAddField('project_role', e.target.value)}
          />
          <FormControlLabel
            control={
              <Checkbox
                checked={newPerson.is_subcontract}
                onChange={(e) => handleAddField('is_subcontract', e.target.checked)}
              />
            }
            label="Subcontract"
          />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setAddOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={saveNewPerson}
            disabled={saving || !newPerson.name || !newPerson.organization || !newPerson.project_role}
            sx={{ bgcolor: '#2563eb' }}
          >
            {saving ? 'Adding...' : 'Add Personnel'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Delete Confirmation Modal */}
      <Dialog open={deleteConfirmIdx !== null} onClose={() => setDeleteConfirmIdx(null)} maxWidth="xs" fullWidth>
        <DialogTitle sx={{ bgcolor: '#dc2626', color: '#fff' }}>Confirm Delete</DialogTitle>
        <DialogContent sx={{ pt: 3 }}>
          <Typography>
            Are you sure you want to remove{' '}
            <strong>{deleteConfirmIdx !== null ? personnel[deleteConfirmIdx]?.name : ''}</strong>{' '}
            ({deleteConfirmIdx !== null ? personnel[deleteConfirmIdx]?.project_role : ''}){' '}
            from the project personnel?
          </Typography>
          <Typography variant="body2" sx={{ mt: 1, color: '#666' }}>
            This action cannot be undone.
          </Typography>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setDeleteConfirmIdx(null)}>Cancel</Button>
          <Button variant="contained" color="error" onClick={handleDelete} disabled={saving}>
            {saving ? 'Deleting...' : 'Delete'}
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={snackbar.open}
        autoHideDuration={3000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      >
        <Alert severity={snackbar.severity} onClose={() => setSnackbar({ ...snackbar, open: false })}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
}
