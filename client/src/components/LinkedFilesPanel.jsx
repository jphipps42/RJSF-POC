import { useState, useEffect } from 'react';
import {
  Typography, Box, Button, TextField, Select, MenuItem, ListSubheader,
  Table, TableHead, TableBody, TableRow, TableCell, IconButton,
  Dialog, DialogTitle, DialogContent, DialogActions,
  Snackbar, Alert, Paper,
} from '@mui/material';
import CheckIcon from '@mui/icons-material/Check';
import CloseIcon from '@mui/icons-material/Close';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import { addLinkedFile, updateLinkedFile, deleteLinkedFile, getDocumentCatalog } from '../services/api';

const FILE_SECTIONS = [
  { key: 'final_budget', label: 'Final Budget' },
  { key: 'final_budget_justification', label: 'Final Budget Justification' },
  { key: 'final_sow', label: 'Final SOW' },
  { key: 'approved_dmp', label: 'Approved Data Management Plan' },
  { key: 'updated_cps', label: 'Updated Current/Pending Support' },
];

const cellSx = { border: '1px solid #c7ddf8', p: '4px 6px', fontSize: 11 };
const inputSx = { '& .MuiInputBase-input': { fontSize: 11, p: '3px 6px' } };

function FileGrid({ files, sectionKey, onUpdate, onDelete, catalogItems }) {
  const [editingId, setEditingId] = useState(null);
  const [editData, setEditData] = useState({});
  const [saving, setSaving] = useState(false);

  const sectionFiles = files.filter((f) => f.section === sectionKey);

  const handleDelete = async (fileId) => {
    setSaving(true);
    try { await deleteLinkedFile(fileId); onDelete?.(fileId); } catch (err) { console.error('Delete failed:', err); }
    setSaving(false);
  };

  const startEdit = (file) => { setEditingId(file.id); setEditData({ file_name: file.file_name, description: file.description || '' }); };
  const cancelEdit = () => { setEditingId(null); setEditData({}); };
  const saveEdit = async () => {
    setSaving(true);
    try { const res = await updateLinkedFile(editingId, editData); onUpdate?.(res.data); setEditingId(null); } catch (err) { console.error('Update failed:', err); }
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
            <TableCell>{editingId === f.id ? (
              <CatalogSelect
                value={editData.file_name}
                onChange={(val) => setEditData((prev) => ({ ...prev, file_name: val }))}
                catalogItems={catalogItems}
              />
            ) : f.file_name}</TableCell>
            <TableCell>{editingId === f.id ? (
              <TextField size="small" value={editData.description} onChange={(e) => setEditData((prev) => ({ ...prev, description: e.target.value }))} fullWidth sx={inputSx} />
            ) : (f.description || '')}</TableCell>
            <TableCell>{f.last_updated ? new Date(f.last_updated).toLocaleDateString() : ''}</TableCell>
            <TableCell align="center">{editingId === f.id ? (
              <>
                <IconButton size="small" onClick={saveEdit} disabled={saving} sx={{ color: '#16a34a' }}><CheckIcon fontSize="small" /></IconButton>
                <IconButton size="small" onClick={cancelEdit} sx={{ color: '#dc2626' }}><CloseIcon fontSize="small" /></IconButton>
              </>
            ) : (
              <>
                <IconButton size="small" onClick={() => startEdit(f)} sx={{ color: '#2158c6' }}><EditIcon fontSize="small" /></IconButton>
                <IconButton size="small" onClick={() => handleDelete(f.id)} disabled={saving} sx={{ color: '#dc2626' }}><DeleteIcon fontSize="small" /></IconButton>
              </>
            )}</TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}

/** Renders a Select dropdown grouped by category from the document catalog */
function CatalogSelect({ value, onChange, catalogItems }) {
  // Group items by category
  const grouped = {};
  for (const item of catalogItems) {
    if (!grouped[item.category]) grouped[item.category] = [];
    grouped[item.category].push(item);
  }
  const categories = Object.keys(grouped).sort();

  return (
    <Select
      size="small"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      fullWidth
      displayEmpty
      sx={{ fontSize: 11 }}
    >
      <MenuItem value="" disabled sx={{ fontSize: 11 }}>Select a file...</MenuItem>
      {categories.map((cat) => [
        <ListSubheader key={`header-${cat}`} sx={{ fontSize: 11, fontWeight: 700, bgcolor: '#f0f7ff', lineHeight: '28px' }}>
          {cat}
        </ListSubheader>,
        ...grouped[cat].map((item) => (
          <MenuItem key={item.id} value={item.file_name} sx={{ fontSize: 11, pl: 3 }}>
            {item.file_name}
          </MenuItem>
        )),
      ])}
    </Select>
  );
}

export default function LinkedFilesPanel({ awardId, linkedFiles, onLinkedFilesChange }) {
  const [addOpen, setAddOpen] = useState(false);
  const [addSection, setAddSection] = useState('');
  const [newFile, setNewFile] = useState({ file_name: '', description: '' });
  const [saving, setSaving] = useState(false);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
  const [catalogItems, setCatalogItems] = useState([]);

  // Fetch document catalog from the API on mount
  useEffect(() => {
    getDocumentCatalog()
      .then((res) => setCatalogItems(res.data))
      .catch((err) => console.error('Failed to load document catalog:', err));
  }, []);

  const files = linkedFiles || [];

  const openAddModal = (sectionKey) => { setAddSection(sectionKey); setNewFile({ file_name: '', description: '' }); setAddOpen(true); };

  const handleAddFile = async () => {
    if (!newFile.file_name) { setSnackbar({ open: true, message: 'File Name is required.', severity: 'warning' }); return; }
    setSaving(true);
    try {
      const res = await addLinkedFile({ award_id: awardId, section: addSection, file_name: newFile.file_name, description: newFile.description });
      onLinkedFilesChange?.((prev) => [...prev, res.data]);
      setAddOpen(false);
      setSnackbar({ open: true, message: 'File linked successfully.', severity: 'success' });
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.error || 'Failed to add file', severity: 'error' });
    }
    setSaving(false);
  };

  const handleFileUpdate = (updatedFile) => { onLinkedFilesChange?.((prev) => prev.map((f) => (f.id === updatedFile.id ? updatedFile : f))); };
  const handleFileDelete = (deletedId) => { onLinkedFilesChange?.((prev) => prev.filter((f) => f.id !== deletedId)); };

  const sectionLabel = (key) => FILE_SECTIONS.find((s) => s.key === key)?.label || key;

  return (
    <>
      <Paper sx={{ p: 2, mb: 2, border: '1px solid #d6e4f2', borderRadius: 2 }}>
        <Typography variant="h6" sx={{ fontWeight: 700, fontSize: 14, mb: 1.5 }}>Linked Files</Typography>
        {FILE_SECTIONS.map(({ key, label }) => (
          <Box key={key} sx={{ mb: 1.5, p: 1.5, border: '1px solid #c7ddf8', borderRadius: 2, bgcolor: '#f9fbff' }}>
            <Typography sx={{ fontWeight: 600, fontSize: 14, mb: 0.5 }}>{label}</Typography>
            <Button variant="contained" size="small" onClick={() => openAddModal(key)} sx={{ bgcolor: '#428bca', fontSize: 11 }}>Link File(s)</Button>
            <FileGrid files={files} sectionKey={key} onUpdate={handleFileUpdate} onDelete={handleFileDelete} catalogItems={catalogItems} />
          </Box>
        ))}
      </Paper>

      <Dialog open={addOpen} onClose={() => setAddOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ bgcolor: '#428bca', color: '#fff' }}>Link File &mdash; {sectionLabel(addSection)}</DialogTitle>
        <DialogContent sx={{ pt: 3, display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
          <Box>
            <Typography variant="body2" sx={{ mb: 0.5, fontWeight: 600 }}>File Name *</Typography>
            <CatalogSelect
              value={newFile.file_name}
              onChange={(val) => setNewFile((prev) => ({ ...prev, file_name: val }))}
              catalogItems={catalogItems}
            />
          </Box>
          <TextField label="Description" value={newFile.description} onChange={(e) => setNewFile((prev) => ({ ...prev, description: e.target.value }))} fullWidth size="small" multiline rows={2} />
          <TextField label="Last Updated" value={new Date().toLocaleDateString()} fullWidth size="small" disabled helperText="Auto-set to current date on save" />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setAddOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleAddFile} disabled={saving} sx={{ bgcolor: '#428bca' }}>{saving ? 'Saving...' : 'Link File'}</Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={snackbar.open} autoHideDuration={3000} onClose={() => setSnackbar({ ...snackbar, open: false })} anchorOrigin={{ vertical: 'top', horizontal: 'center' }}>
        <Alert severity={snackbar.severity} onClose={() => setSnackbar({ ...snackbar, open: false })}>{snackbar.message}</Alert>
      </Snackbar>
    </>
  );
}
