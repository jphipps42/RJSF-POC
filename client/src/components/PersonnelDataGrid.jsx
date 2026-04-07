import { useState } from 'react';
import {
  Box, Typography, Button, IconButton,
  Table, TableHead, TableBody, TableRow, TableCell,
  TextField, Checkbox,
  Dialog, DialogTitle, DialogContent, DialogActions,
  FormControlLabel,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import CheckIcon from '@mui/icons-material/Check';
import CloseIcon from '@mui/icons-material/Close';
import DescriptionIcon from '@mui/icons-material/Description';

const EMPTY_PERSON = { name: '', organization: '', country: 'USA', project_role: '', is_subcontract: false };

const cellSx = { border: '1px solid #93c5fd', p: '4px 6px', fontSize: 11 };
const headerCellSx = { ...cellSx, fontWeight: 700 };
const inputSx = { '& .MuiInputBase-input': { fontSize: 11, p: '3px 6px' } };

/**
 * Custom RJSF field component that renders the personnel array as a data grid table.
 *
 * This is registered as a custom field via the `fields` prop on the RJSF Form component
 * and wired to the "personnel" property via uiSchema: { "ui:field": "personnelGrid" }.
 *
 * It receives the standard RJSF field props and calls onChange(newArray) to update
 * the form data through the RJSF engine.
 */
export default function PersonnelDataGrid({ formData, onChange, readonly, disabled, schema, formContext }) {
  const [editingIdx, setEditingIdx] = useState(null);
  const [editData, setEditData] = useState({});
  const [addOpen, setAddOpen] = useState(false);
  const [newPerson, setNewPerson] = useState({ ...EMPTY_PERSON });
  const [deleteConfirmIdx, setDeleteConfirmIdx] = useState(null);

  const isLocked = readonly || disabled;
  const personnel = formData || [];

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

  const saveEdit = () => {
    if (editingIdx !== null) {
      const currentPersonnel = Array.isArray(formData) ? formData : [];
      const updated = [...currentPersonnel];
      updated[editingIdx] = { ...editData };
      setEditingIdx(null);
      setEditData({});
      onChange(updated);
    }
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

  const saveNewPerson = () => {
    if (!newPerson.name || !newPerson.organization || !newPerson.project_role) return;
    const currentPersonnel = Array.isArray(formData) ? formData : [];
    const updated = [...currentPersonnel, {
      name: newPerson.name,
      organization: newPerson.organization,
      country: newPerson.country || 'USA',
      project_role: newPerson.project_role,
      is_subcontract: newPerson.is_subcontract || false,
    }];
    onChange(updated);
    setAddOpen(false);
  };

  // --- Delete ---
  const handleDelete = () => {
    if (deleteConfirmIdx !== null) {
      const currentPersonnel = Array.isArray(formData) ? formData : [];
      const updated = currentPersonnel.filter((_, i) => i !== deleteConfirmIdx);
      setDeleteConfirmIdx(null);
      onChange(updated);
    }
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
            <TableRow key={idx} sx={{ '&:hover': { bgcolor: '#f0f7ff' } }}>
              <TableCell>{renderCell(person, 'organization', idx)}</TableCell>
              <TableCell>{renderCell(person, 'country', idx)}</TableCell>
              <TableCell>{renderCell(person, 'project_role', idx)}</TableCell>
              <TableCell>{renderCell(person, 'name', idx)}</TableCell>
              <TableCell align="center">{renderCell(person, 'is_subcontract', idx)}</TableCell>
              {!isLocked && (
                <TableCell align="center" sx={{ whiteSpace: 'nowrap' }}>
                  {editingIdx === idx ? (
                    <>
                      <IconButton size="small" onClick={saveEdit} sx={{ color: '#16a34a', p: 0.25 }}>
                        <CheckIcon sx={{ fontSize: 16 }} />
                      </IconButton>
                      <IconButton size="small" onClick={cancelEdit} sx={{ color: '#dc2626', p: 0.25 }}>
                        <CloseIcon sx={{ fontSize: 16 }} />
                      </IconButton>
                    </>
                  ) : (
                    <>
                      <IconButton size="small" onClick={() => startEdit(idx)} sx={{ color: '#1d4ed8', p: 0.25 }}>
                        <EditIcon sx={{ fontSize: 16 }} />
                      </IconButton>
                      <IconButton size="small" onClick={() => setDeleteConfirmIdx(idx)} sx={{ color: '#dc2626', p: 0.25 }}>
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
            disabled={!newPerson.name || !newPerson.organization || !newPerson.project_role}
            sx={{ bgcolor: '#2563eb' }}
          >
            Add Personnel
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
          <Button variant="contained" color="error" onClick={handleDelete}>Delete</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
