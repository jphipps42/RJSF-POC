import { useState } from 'react';
import {
  Paper, Typography, Box, Button, TextField, Select, MenuItem,
  RadioGroup, FormControlLabel, Radio, FormLabel, FormControl,
  Table, TableHead, TableBody, TableRow, TableCell, IconButton,
  Dialog, DialogTitle, DialogContent, DialogActions,
  Snackbar, Alert,
} from '@mui/material';
import CheckIcon from '@mui/icons-material/Check';
import CloseIcon from '@mui/icons-material/Close';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import { addLinkedFile, updateLinkedFile, deleteLinkedFile } from '../services/api';

// Available file names for the dropdown
const FILE_NAME_OPTIONS = [
  'Budget_Final.xlsx',
  'Budget_Justification.pdf',
  'Statement_of_Work.pdf',
  'Data_Management_Plan.pdf',
  'Current_Pending_Support.pdf',
  'Inclusion_Enrollment_Report.pdf',
  'Overlap_Mitigation.pdf',
  'RISG_Approval.pdf',
  'Negotiation_Memo.pdf',
  'Award_Package.pdf',
  'Other',
];

// Section definitions matching the POC
const FILE_SECTIONS = [
  { key: 'final_budget', label: 'Final Budget' },
  { key: 'final_budget_justification', label: 'Final Budget Justification' },
  { key: 'final_sow', label: 'Final SOW' },
  { key: 'approved_dmp', label: 'Approved Data Management Plan' },
  { key: 'updated_cps', label: 'Updated Current/Pending Support' },
];

const cellSx = { border: '1px solid #c7ddf8', p: '4px 6px', fontSize: 11 };
const inputSx = { '& .MuiInputBase-input': { fontSize: 11, p: '3px 6px' } };

// ---- Inline-editable file grid for a single section ----
function FileGrid({ files, sectionKey, onUpdate, onDelete }) {
  const [editingId, setEditingId] = useState(null);
  const [editData, setEditData] = useState({});
  const [saving, setSaving] = useState(false);

  const sectionFiles = files.filter((f) => f.section === sectionKey);

  const handleDelete = async (fileId) => {
    setSaving(true);
    try {
      await deleteLinkedFile(fileId);
      onDelete?.(fileId);
    } catch (err) {
      console.error('Delete failed:', err);
    }
    setSaving(false);
  };

  const startEdit = (file) => {
    setEditingId(file.id);
    setEditData({ file_name: file.file_name, description: file.description || '' });
  };

  const cancelEdit = () => {
    setEditingId(null);
    setEditData({});
  };

  const saveEdit = async () => {
    setSaving(true);
    try {
      const res = await updateLinkedFile(editingId, editData);
      onUpdate?.(res.data);
      setEditingId(null);
    } catch (err) {
      console.error('Update failed:', err);
    }
    setSaving(false);
  };

  if (sectionFiles.length === 0) return null;

  return (
    <Table size="small" sx={{ mt: 1, '& th, & td': cellSx }}>
      <TableHead sx={{ bgcolor: '#eef5ff' }}>
        <TableRow>
          <TableCell>File Name</TableCell>
          <TableCell>Description</TableCell>
          <TableCell sx={{ width: 130 }}>Last Updated</TableCell>
          <TableCell sx={{ width: 80 }} align="center">Action</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {sectionFiles.map((f) => (
          <TableRow key={f.id}>
            <TableCell>
              {editingId === f.id ? (
                <Select
                  size="small"
                  value={editData.file_name}
                  onChange={(e) => setEditData((prev) => ({ ...prev, file_name: e.target.value }))}
                  fullWidth
                  sx={{ fontSize: 11 }}
                >
                  {FILE_NAME_OPTIONS.map((opt) => (
                    <MenuItem key={opt} value={opt} sx={{ fontSize: 11 }}>{opt}</MenuItem>
                  ))}
                </Select>
              ) : (
                f.file_name
              )}
            </TableCell>
            <TableCell>
              {editingId === f.id ? (
                <TextField
                  size="small"
                  value={editData.description}
                  onChange={(e) => setEditData((prev) => ({ ...prev, description: e.target.value }))}
                  fullWidth
                  sx={inputSx}
                />
              ) : (
                f.description || ''
              )}
            </TableCell>
            <TableCell>
              {f.last_updated ? new Date(f.last_updated).toLocaleDateString() : ''}
            </TableCell>
            <TableCell align="center">
              {editingId === f.id ? (
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
                  <IconButton size="small" onClick={() => startEdit(f)} sx={{ color: '#1d4ed8' }}>
                    <EditIcon fontSize="small" />
                  </IconButton>
                  <IconButton size="small" onClick={() => handleDelete(f.id)} disabled={saving} sx={{ color: '#dc2626' }}>
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                </>
              )}
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}

// ---- Main Component ----
export default function FinalRecommendation({ awardId, linkedFiles, onLinkedFilesChange }) {
  const [soRecommendation, setSoRecommendation] = useState('');
  const [gorRecommendation, setGorRecommendation] = useState('');
  const [scientificOverlap, setScientificOverlap] = useState('');
  const [foreignInvolvement, setForeignInvolvement] = useState('');
  const [risgApproval, setRisgApproval] = useState('');

  // Add file modal state
  const [addOpen, setAddOpen] = useState(false);
  const [addSection, setAddSection] = useState('');
  const [newFile, setNewFile] = useState({ file_name: '', description: '' });
  const [saving, setSaving] = useState(false);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });

  const files = linkedFiles || [];

  const openAddModal = (sectionKey) => {
    setAddSection(sectionKey);
    setNewFile({ file_name: '', description: '' });
    setAddOpen(true);
  };

  const handleAddFile = async () => {
    if (!newFile.file_name) {
      setSnackbar({ open: true, message: 'File Name is required.', severity: 'warning' });
      return;
    }
    setSaving(true);
    try {
      const res = await addLinkedFile({
        award_id: awardId,
        section: addSection,
        file_name: newFile.file_name,
        description: newFile.description,
      });
      onLinkedFilesChange?.((prev) => [...prev, res.data]);
      setAddOpen(false);
      setSnackbar({ open: true, message: 'File linked successfully.', severity: 'success' });
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.error || 'Failed to add file', severity: 'error' });
    }
    setSaving(false);
  };

  const handleFileUpdate = (updatedFile) => {
    onLinkedFilesChange?.((prev) => prev.map((f) => (f.id === updatedFile.id ? updatedFile : f)));
  };

  const handleFileDelete = (deletedId) => {
    onLinkedFilesChange?.((prev) => prev.filter((f) => f.id !== deletedId));
  };

  // All section keys that can have files (static + conditional)
  const conditionalSections = [];
  if (scientificOverlap === 'yes') {
    conditionalSections.push({ key: 'overlap_mitigation', label: 'Overlap Mitigation' });
  }

  const sectionLabel = (key) =>
    [...FILE_SECTIONS, ...conditionalSections].find((s) => s.key === key)?.label || key;

  return (
    <Paper sx={{ p: 2, mb: 2, border: '1px solid #d6e4f2', borderRadius: 2 }}>
      <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>Final Recommendation to Award</Typography>

      {/* File link sections */}
      {FILE_SECTIONS.map(({ key, label }) => (
        <Box key={key} sx={{ mb: 1.5, p: 1.5, border: '1px solid #c7ddf8', borderRadius: 2, bgcolor: '#f9fbff' }}>
          <Typography sx={{ fontWeight: 600, fontSize: 14, mb: 0.5 }}>{label}</Typography>
          <Button
            variant="contained"
            size="small"
            onClick={() => openAddModal(key)}
            sx={{ bgcolor: '#007bff', fontSize: 11 }}
          >
            Link File(s)
          </Button>
          <FileGrid files={files} sectionKey={key} onUpdate={handleFileUpdate} onDelete={handleFileDelete} />
        </Box>
      ))}

      {/* Scientific Overlap */}
      <Box sx={{ mb: 1.5, p: 1.5, border: '1px solid #c7ddf8', borderRadius: 2, bgcolor: '#f9fbff' }}>
        <FormControl component="fieldset">
          <FormLabel sx={{ fontWeight: 600, fontSize: 14 }}>Was scientific overlap identified during negotiations?</FormLabel>
          <RadioGroup row value={scientificOverlap} onChange={(e) => setScientificOverlap(e.target.value)}>
            <FormControlLabel value="yes" control={<Radio size="small" />} label="Yes" />
            <FormControlLabel value="no" control={<Radio size="small" />} label="No" />
          </RadioGroup>
        </FormControl>
        {scientificOverlap === 'yes' && (
          <>
            <Typography sx={{ fontSize: 13, mt: 1, mb: 0.5, color: '#475569' }}>
              Overlap mitigation: link any relevant files supporting the mitigation of the overlap
            </Typography>
            <Button
              variant="contained"
              size="small"
              onClick={() => openAddModal('overlap_mitigation')}
              sx={{ bgcolor: '#007bff', fontSize: 11 }}
            >
              Link File(s)
            </Button>
            <FileGrid files={files} sectionKey="overlap_mitigation" onUpdate={handleFileUpdate} onDelete={handleFileDelete} />
          </>
        )}
      </Box>

      {/* Foreign Involvement */}
      <Box sx={{ mb: 1.5, p: 1.5, border: '1px solid #c7ddf8', borderRadius: 2, bgcolor: '#f9fbff' }}>
        <FormControl component="fieldset">
          <FormLabel sx={{ fontWeight: 600, fontSize: 14 }}>Was this project reported to RISG for foreign involvement?</FormLabel>
          <RadioGroup row value={foreignInvolvement} onChange={(e) => setForeignInvolvement(e.target.value)}>
            <FormControlLabel value="yes" control={<Radio size="small" />} label="Yes" />
            <FormControlLabel value="no" control={<Radio size="small" />} label="No" />
          </RadioGroup>
        </FormControl>
        {foreignInvolvement === 'yes' && (
          <FormControl component="fieldset" sx={{ mt: 1 }}>
            <FormLabel sx={{ fontSize: 14 }}>Does this project have RISG approval to proceed?</FormLabel>
            <RadioGroup row value={risgApproval} onChange={(e) => setRisgApproval(e.target.value)}>
              <FormControlLabel value="yes" control={<Radio size="small" />} label="Yes" />
              <FormControlLabel value="no" control={<Radio size="small" />} label="No" />
            </RadioGroup>
          </FormControl>
        )}
      </Box>

      {/* SO Recommendation */}
      <Box sx={{ mb: 1.5, p: 1.5, border: '1px solid #c7ddf8', borderRadius: 2, bgcolor: '#f9fbff' }}>
        <Typography sx={{ fontWeight: 700, fontSize: 14, mb: 1 }}>SO Recommendation</Typography>
        <Select fullWidth size="small" value={soRecommendation} onChange={(e) => setSoRecommendation(e.target.value)} displayEmpty sx={{ mb: 1 }}>
          <MenuItem value="">Select Recommendation</MenuItem>
          <MenuItem value="approval">SO Recommend Approval to Award</MenuItem>
          <MenuItem value="disapproval">SO Recommend Disapproval</MenuItem>
        </Select>
        <TextField fullWidth multiline rows={3} placeholder="Enter comments..." size="small" />
        <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1, mt: 1 }}>
          <Button variant="contained" size="small" sx={{ bgcolor: '#60a5fa' }}>Save</Button>
          <Button variant="contained" size="small" sx={{ bgcolor: '#2563eb' }}>Send to GOR/COR</Button>
        </Box>
      </Box>

      {/* GOR/COR Recommendation */}
      <Box sx={{ mb: 1.5, p: 1.5, border: '1px solid #c7ddf8', borderRadius: 2, bgcolor: '#f9fbff' }}>
        <Typography sx={{ fontWeight: 700, fontSize: 14, mb: 1 }}>GOR/COR Recommendation</Typography>
        <Select fullWidth size="small" value={gorRecommendation} onChange={(e) => setGorRecommendation(e.target.value)} displayEmpty sx={{ mb: 1 }}>
          <MenuItem value="">Select Recommendation</MenuItem>
          <MenuItem value="approval">GOR/COR Recommend Approval</MenuItem>
          <MenuItem value="disapproval">GOR/COR Recommend Disapproval</MenuItem>
        </Select>
        <TextField fullWidth multiline rows={3} placeholder="Enter comments..." size="small" />
        <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1, mt: 1 }}>
          <Button variant="contained" size="small" sx={{ bgcolor: '#60a5fa' }}>Save</Button>
          <Button variant="contained" size="small" sx={{ bgcolor: '#2563eb' }}>Submit Recommendation to DHACA R&D</Button>
        </Box>
      </Box>

      {/* ====== Add File Modal ====== */}
      <Dialog open={addOpen} onClose={() => setAddOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ bgcolor: '#2563eb', color: '#fff' }}>
          Link File &mdash; {sectionLabel(addSection)}
        </DialogTitle>
        <DialogContent sx={{ pt: 3, display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
          <Box>
            <Typography variant="body2" sx={{ mb: 0.5, fontWeight: 600 }}>File Name *</Typography>
            <Select
              fullWidth
              size="small"
              value={newFile.file_name}
              onChange={(e) => setNewFile((prev) => ({ ...prev, file_name: e.target.value }))}
              displayEmpty
            >
              <MenuItem value="" disabled>Select a file...</MenuItem>
              {FILE_NAME_OPTIONS.map((opt) => (
                <MenuItem key={opt} value={opt}>{opt}</MenuItem>
              ))}
            </Select>
          </Box>
          <TextField
            label="Description"
            value={newFile.description}
            onChange={(e) => setNewFile((prev) => ({ ...prev, description: e.target.value }))}
            fullWidth
            size="small"
            multiline
            rows={2}
          />
          <TextField
            label="Last Updated"
            value={new Date().toLocaleDateString()}
            fullWidth
            size="small"
            disabled
            helperText="Auto-set to current date on save"
          />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setAddOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleAddFile} disabled={saving} sx={{ bgcolor: '#2563eb' }}>
            {saving ? 'Saving...' : 'Link File'}
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
