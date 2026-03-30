import { useState } from 'react';
import {
  Box, Typography, Divider, Select, MenuItem,
  Table, TableHead, TableBody, TableRow, TableCell,
  Button, Paper, Checkbox, TextField, IconButton,
  Dialog, DialogTitle, DialogContent, DialogActions,
  FormControlLabel, Snackbar, Alert,
} from '@mui/material';
import CheckIcon from '@mui/icons-material/Check';
import CloseIcon from '@mui/icons-material/Close';
import { addPersonnel, updatePersonnel } from '../services/api';

function InfoRow({ label, value }) {
  return (
    <Box sx={{ display: 'flex', mb: 0.5 }}>
      <Typography sx={{ fontWeight: 700, fontSize: 12, width: 250, flexShrink: 0 }}>{label}:</Typography>
      <Typography sx={{ fontSize: 12, color: '#333' }}>{value || ''}</Typography>
    </Box>
  );
}

const EMPTY_PERSON = { organization: '', country: 'USA', project_role: '', name: '', is_subcontract: false };

const cellSx = { border: '1px solid #93c5fd', p: 1, fontSize: 12 };
const inputSx = { '& .MuiInputBase-input': { fontSize: 12, p: '4px 8px' } };

export default function OverviewPanel({ award, personnel, submissions, onPrimeAwardChange, onPersonnelChange }) {
  const [editingId, setEditingId] = useState(null);
  const [editData, setEditData] = useState({});
  const [addOpen, setAddOpen] = useState(false);
  const [newPerson, setNewPerson] = useState({ ...EMPTY_PERSON });
  const [saving, setSaving] = useState(false);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });

  if (!award) return null;

  const primeAwardType = award.prime_award_type || 'extramural';
  const showCreateRecord = primeAwardType === 'extramural_intramural';

  const getSubmissionDate = (formKey) => {
    const sub = submissions?.find(s => s.form_key === formKey);
    return sub?.submitted_at ? new Date(sub.submitted_at).toLocaleDateString() : '';
  };

  // --- Inline Edit ---
  const startEdit = (person) => {
    setEditingId(person.id);
    setEditData({
      organization: person.organization,
      country: person.country,
      project_role: person.project_role,
      name: person.name,
      is_subcontract: person.is_subcontract,
    });
  };

  const cancelEdit = () => {
    setEditingId(null);
    setEditData({});
  };

  const saveEdit = async () => {
    setSaving(true);
    try {
      const res = await updatePersonnel(editingId, editData);
      onPersonnelChange?.((prev) => prev.map((p) => (p.id === editingId ? res.data : p)));
      setEditingId(null);
      setEditData({});
      setSnackbar({ open: true, message: 'Personnel updated.', severity: 'success' });
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.error || 'Update failed', severity: 'error' });
    }
    setSaving(false);
  };

  const handleEditField = (field, value) => {
    setEditData((prev) => ({ ...prev, [field]: value }));
  };

  // --- Add Personnel ---
  const openAdd = () => {
    setNewPerson({ ...EMPTY_PERSON });
    setAddOpen(true);
  };

  const handleAddField = (field, value) => {
    setNewPerson((prev) => ({ ...prev, [field]: value }));
  };

  const saveNewPerson = async () => {
    if (!newPerson.name || !newPerson.organization || !newPerson.project_role) {
      setSnackbar({ open: true, message: 'Name, Organization, and Project Role are required.', severity: 'warning' });
      return;
    }
    setSaving(true);
    try {
      const res = await addPersonnel({ ...newPerson, award_id: award.id });
      onPersonnelChange?.((prev) => [...prev, res.data]);
      setAddOpen(false);
      setSnackbar({ open: true, message: 'Personnel added.', severity: 'success' });
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.error || 'Add failed', severity: 'error' });
    }
    setSaving(false);
  };

  // --- Render helpers ---
  const renderCell = (person, field) => {
    if (editingId === person.id) {
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
          value={editData[field]}
          onChange={(e) => handleEditField(field, e.target.value)}
          sx={inputSx}
          fullWidth
          variant="outlined"
        />
      );
    }
    if (field === 'is_subcontract') {
      return <Checkbox size="small" checked={person.is_subcontract} disabled sx={{ p: 0 }} />;
    }
    return person[field];
  };

  return (
    <Paper sx={{ p: 2, mb: 2, border: '1px solid #d6e4f2', borderRadius: 2 }}>
      <Typography variant="h6" sx={{ fontWeight: 700, fontSize: 20, mb: 1 }}>Overview</Typography>

      <InfoRow label="PI Budget" value={award.pi_budget ? `$${Number(award.pi_budget).toLocaleString('en-US', { minimumFractionDigits: 2 })}` : ''} />
      <InfoRow label="Final Recommended Budget" value={award.final_recommended_budget ? `$${Number(award.final_recommended_budget).toLocaleString('en-US', { minimumFractionDigits: 2 })}` : ''} />

      <Divider sx={{ my: 1 }} />

      <InfoRow label="Program Manager" value={award.program_manager} />
      <InfoRow label="Contract/Grants Specialist" value={award.contract_grants_specialist} />
      <InfoRow label="Branch Chief" value={award.branch_chief} />

      <Divider sx={{ my: 1 }} />

      <InfoRow label="PI Notification Date" value="" />
      <InfoRow label="Safety Checklist Submitted" value={getSubmissionDate('safety_review')} />
      <InfoRow label="Animal Research Checklist Submitted" value={getSubmissionDate('animal_review')} />
      <InfoRow label="Human Research Checklist Submitted" value={getSubmissionDate('human_review')} />
      <InfoRow label="Acquisition Checklist Submitted" value={getSubmissionDate('acquisition_review')} />

      <Divider sx={{ my: 1 }} />

      <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
        <Typography sx={{ fontWeight: 700, fontSize: 12, width: 250, flexShrink: 0 }}>Prime Award (Intra/Extra):</Typography>
        <Select
          size="small"
          value={primeAwardType}
          onChange={(e) => onPrimeAwardChange?.(e.target.value)}
          sx={{ fontSize: 12 }}
        >
          <MenuItem value="extramural">Extramural Only</MenuItem>
          <MenuItem value="intramural">Intragovernmental Only</MenuItem>
          <MenuItem value="extramural_intramural">Extramural w/Intragovernmental Component</MenuItem>
          <MenuItem value="intramural_extramural">Intragovernmental w/Extramural Component</MenuItem>
        </Select>
      </Box>

      {/* Project Personnel Table */}
      <Typography sx={{ fontWeight: 700, fontSize: 12, mt: 2, mb: 1 }}>Project Personnel</Typography>
      <Table size="small" sx={{ '& th, & td': cellSx }}>
        <TableHead sx={{ bgcolor: '#c8ddf3' }}>
          <TableRow>
            <TableCell>Organization</TableCell>
            <TableCell>Country</TableCell>
            <TableCell>Project Role</TableCell>
            <TableCell>Name</TableCell>
            <TableCell align="center">Subcontract</TableCell>
            <TableCell align="center">Action</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {personnel?.map((p) => (
            <TableRow key={p.id}>
              <TableCell>{renderCell(p, 'organization')}</TableCell>
              <TableCell>{renderCell(p, 'country')}</TableCell>
              <TableCell>{renderCell(p, 'project_role')}</TableCell>
              <TableCell>{renderCell(p, 'name')}</TableCell>
              <TableCell align="center">{renderCell(p, 'is_subcontract')}</TableCell>
              <TableCell align="center" sx={{ whiteSpace: 'nowrap' }}>
                {editingId === p.id ? (
                  <>
                    <IconButton size="small" onClick={saveEdit} disabled={saving} sx={{ color: '#16a34a' }}>
                      <CheckIcon fontSize="small" />
                    </IconButton>
                    <IconButton size="small" onClick={cancelEdit} sx={{ color: '#dc2626' }}>
                      <CloseIcon fontSize="small" />
                    </IconButton>
                  </>
                ) : (
                  <>
                    <Button
                      variant="outlined"
                      size="small"
                      onClick={() => startEdit(p)}
                      sx={{ fontSize: 12, py: 0.25, px: 1, mr: 0.5, bgcolor: '#e0ecff', color: '#1d4ed8', borderColor: '#93c5fd' }}
                    >
                      Edit
                    </Button>
                    {showCreateRecord && (
                      <Button
                        variant="outlined"
                        size="small"
                        sx={{ fontSize: 12, py: 0.25, px: 1, bgcolor: '#e0ecff', color: '#1d4ed8', borderColor: '#93c5fd' }}
                      >
                        Create Record
                      </Button>
                    )}
                  </>
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
      <Button variant="outlined" size="small" onClick={openAdd} sx={{ mt: 1, fontSize: 12 }}>
        Add Personnel
      </Button>

      {/* ====== Add Personnel Modal ====== */}
      <Dialog open={addOpen} onClose={() => setAddOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ bgcolor: '#2563eb', color: '#fff' }}>Add Personnel</DialogTitle>
        <DialogContent sx={{ pt: 3, display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
          <TextField
            label="Name"
            value={newPerson.name}
            onChange={(e) => handleAddField('name', e.target.value)}
            required
            fullWidth
            size="small"
          />
          <TextField
            label="Organization"
            value={newPerson.organization}
            onChange={(e) => handleAddField('organization', e.target.value)}
            required
            fullWidth
            size="small"
          />
          <TextField
            label="Country"
            value={newPerson.country}
            onChange={(e) => handleAddField('country', e.target.value)}
            fullWidth
            size="small"
          />
          <TextField
            label="Project Role"
            value={newPerson.project_role}
            onChange={(e) => handleAddField('project_role', e.target.value)}
            required
            fullWidth
            size="small"
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
          <Button variant="contained" onClick={saveNewPerson} disabled={saving} sx={{ bgcolor: '#2563eb' }}>
            {saving ? 'Saving...' : 'Add Personnel'}
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
    </Paper>
  );
}
