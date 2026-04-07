import { useState, useCallback, useRef } from 'react';
import {
  Box, Typography, Divider, Button, TextField,
  Snackbar, Alert, Chip, Select, MenuItem,
  Dialog, DialogTitle, DialogContent, DialogActions,
  Accordion, AccordionSummary, AccordionDetails,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import StatusBadge from './StatusBadge';
import PersonnelDataGrid from './PersonnelDataGrid';
import useVersionedFormData from '../hooks/useVersionedFormData';
import { saveFormDraft, submitForm } from '../services/api';

const LABEL_WIDTH = 220;
const INPUT_MAX = 340;
const inputSx = { '& .MuiInputBase-input': { fontSize: 11, p: '4px 8px' } };

function InfoRow({ label, value }) {
  return (
    <Box sx={{ display: 'flex', alignItems: 'center', mb: 0.5 }}>
      <Typography sx={{ fontWeight: 700, fontSize: 11, width: LABEL_WIDTH, flexShrink: 0 }}>{label}:</Typography>
      <Typography sx={{ fontSize: 11, color: '#333' }}>{value || ''}</Typography>
    </Box>
  );
}

function FieldRow({ label, children }) {
  return (
    <Box sx={{ display: 'flex', alignItems: 'center', mb: 0.75 }}>
      <Typography sx={{ fontWeight: 700, fontSize: 11, width: LABEL_WIDTH, flexShrink: 0 }}>{label}:</Typography>
      {children}
    </Box>
  );
}

const PRIME_AWARD_OPTIONS = [
  { value: 'extramural', label: 'Extramural Only' },
  { value: 'intramural', label: 'Intragovernmental Only' },
  { value: 'extramural_intramural', label: 'Extramural w/Intragovernmental Component' },
  { value: 'intramural_extramural', label: 'Intragovernmental w/Extramural Component' },
];

export default function OverviewPanel({ overviewSubmission, submissions, onUpdate, overviewHeader = 'Overview', expanded, onAccordionChange }) {
  const versioned = useVersionedFormData(overviewSubmission, 'edit');

  const [formData, setFormData] = useState(versioned.formData);
  const [saving, setSaving] = useState(false);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
  const [confirmOpen, setConfirmOpen] = useState(false);

  const formDataRef = useRef(formData);

  // Sync on initial load or schema migration only
  const [lastMigrationKey, setLastMigrationKey] = useState(null);
  const migrationKey = `${overviewSubmission.id}-${versioned.schemaVersion}-${versioned.migrated}`;
  if (migrationKey !== lastMigrationKey) {
    setLastMigrationKey(migrationKey);
    if (lastMigrationKey === null || versioned.migrated) {
      setFormData(versioned.formData);
      formDataRef.current = versioned.formData;
    }
  }

  const isLocked = overviewSubmission.is_locked;

  // Generic field change handler
  const handleFieldChange = useCallback((field, value) => {
    setFormData((prev) => {
      const updated = { ...prev, [field]: value };
      formDataRef.current = updated;
      return updated;
    });
  }, []);

  // Personnel grid onChange — saves to DB immediately
  const handlePersonnelChange = useCallback(async (newPersonnel) => {
    const updated = { ...formDataRef.current, personnel: newPersonnel };
    setFormData(updated);
    formDataRef.current = updated;

    setSaving(true);
    try {
      const res = await saveFormDraft(overviewSubmission.id, updated);
      setSnackbar({ open: true, message: 'Personnel saved.', severity: 'success' });
      onUpdate?.(res.data);
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.error || 'Save failed', severity: 'error' });
    }
    setSaving(false);
  }, [overviewSubmission.id, onUpdate]);

  const handleSave = useCallback(async () => {
    setSaving(true);
    try {
      const res = await saveFormDraft(overviewSubmission.id, formDataRef.current);
      setSnackbar({ open: true, message: 'Overview saved successfully.', severity: 'success' });
      onUpdate?.(res.data);
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.error || 'Save failed', severity: 'error' });
    }
    setSaving(false);
  }, [overviewSubmission.id, onUpdate]);

  const handleSubmit = useCallback(async () => {
    setConfirmOpen(false);
    setSaving(true);
    try {
      const res = await submitForm(overviewSubmission.id, formDataRef.current);
      setSnackbar({ open: true, message: 'Overview submitted and locked.', severity: 'success' });
      onUpdate?.(res.data);
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.error || 'Submit failed', severity: 'error' });
    }
    setSaving(false);
  }, [overviewSubmission.id, onUpdate]);

  const getSubmissionDate = (formKey) => {
    const sub = submissions?.find(s => s.form_key === formKey);
    return sub?.submitted_at ? new Date(sub.submitted_at).toLocaleDateString() : '';
  };

  if (versioned.loading) {
    return (
      <Accordion expanded={expanded} onChange={onAccordionChange} sx={{ mb: 2, border: '1px solid #d6e4f2', borderRadius: '8px !important', '&:before': { display: 'none' } }}>
        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
          <Typography sx={{ color: '#666' }}>Loading overview data...</Typography>
        </AccordionSummary>
      </Accordion>
    );
  }

  return (
    <Accordion expanded={expanded} onChange={onAccordionChange} sx={{ mb: 2, border: '1px solid #d6e4f2', borderRadius: '8px !important', '&:before': { display: 'none' } }}>
      <AccordionSummary expandIcon={<ExpandMoreIcon />}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%', mr: 1 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Typography variant="h6" sx={{ fontWeight: 700, fontSize: 16 }}>
              {overviewHeader}
            </Typography>
            {versioned.schemaVersion > 1 && (
              <Chip label={`v${versioned.schemaVersion}`} size="small"
                sx={{ fontSize: 10, height: 20, bgcolor: '#dbeafe', color: '#1d4ed8' }} />
            )}
          </Box>
          <StatusBadge status={overviewSubmission.status} />
        </Box>
      </AccordionSummary>
      <AccordionDetails sx={{ pt: 0 }}>
        {versioned.migrated && (
          <Alert severity="info" sx={{ mb: 2 }}>
            Data migrated from v{overviewSubmission.schema_version} to v{versioned.schemaVersion}.
          </Alert>
        )}

        {/* Checklist dates (read-only) */}
        <InfoRow label="Safety Checklist Submitted" value={getSubmissionDate('safety_review')} />
        <InfoRow label="Animal Research Checklist Submitted" value={getSubmissionDate('animal_review')} />

        <Divider sx={{ my: 1.5 }} />

        {/* Inline form fields — label left, input right, compact */}
        <FieldRow label="PI Budget">
          <TextField
            size="small" type="number" sx={{ ...inputSx, maxWidth: INPUT_MAX }}
            value={formData?.pi_budget ?? ''} disabled={isLocked}
            inputProps={{ maxLength: 100 }}
            onChange={(e) => handleFieldChange('pi_budget', e.target.value ? Number(e.target.value) : null)}
          />
        </FieldRow>

        <FieldRow label="Final Recommended Budget">
          <TextField
            size="small" type="number" sx={{ ...inputSx, maxWidth: INPUT_MAX }}
            value={formData?.final_recommended_budget ?? ''} disabled={isLocked}
            inputProps={{ maxLength: 100 }}
            onChange={(e) => handleFieldChange('final_recommended_budget', e.target.value ? Number(e.target.value) : null)}
          />
        </FieldRow>

        <FieldRow label="Program Manager">
          <TextField
            size="small" sx={{ ...inputSx, maxWidth: INPUT_MAX }}
            value={formData?.program_manager || ''} disabled={isLocked}
            inputProps={{ maxLength: 100 }}
            onChange={(e) => handleFieldChange('program_manager', e.target.value)}
          />
        </FieldRow>

        <FieldRow label="Contract/Grants Specialist">
          <TextField
            size="small" sx={{ ...inputSx, maxWidth: INPUT_MAX }}
            value={formData?.contract_grants_specialist || ''} disabled={isLocked}
            inputProps={{ maxLength: 100 }}
            onChange={(e) => handleFieldChange('contract_grants_specialist', e.target.value)}
          />
        </FieldRow>

        <FieldRow label="Branch Chief">
          <TextField
            size="small" sx={{ ...inputSx, maxWidth: INPUT_MAX }}
            value={formData?.branch_chief || ''} disabled={isLocked}
            inputProps={{ maxLength: 100 }}
            onChange={(e) => handleFieldChange('branch_chief', e.target.value)}
          />
        </FieldRow>

        <FieldRow label="PI Notification Date">
          <TextField
            size="small" type="date" sx={{ ...inputSx, maxWidth: 180 }}
            value={formData?.pi_notification_date || ''} disabled={isLocked}
            InputLabelProps={{ shrink: true }}
            onChange={(e) => handleFieldChange('pi_notification_date', e.target.value)}
          />
        </FieldRow>

        <FieldRow label="Prime Award (Intra/Extra)">
          <Select
            size="small" sx={{ fontSize: 11, maxWidth: INPUT_MAX }}
            value={formData?.prime_award_type || 'extramural'} disabled={isLocked}
            onChange={(e) => handleFieldChange('prime_award_type', e.target.value)}
          >
            {PRIME_AWARD_OPTIONS.map((opt) => (
              <MenuItem key={opt.value} value={opt.value} sx={{ fontSize: 11 }}>{opt.label}</MenuItem>
            ))}
          </Select>
        </FieldRow>

        {/* Personnel data grid */}
        <PersonnelDataGrid
          formData={formData?.personnel || []}
          onChange={handlePersonnelChange}
          readonly={isLocked}
          disabled={isLocked}
          schema={versioned.schema?.properties?.personnel}
          formContext={{ primeAwardType: formData?.prime_award_type }}
        />

        {/* Overview Notes — below personnel */}
        <Box sx={{ mt: 2, display: 'flex', mb: 1 }}>
          <Typography sx={{ fontWeight: 700, fontSize: 11, width: LABEL_WIDTH, flexShrink: 0, pt: 0.5 }}>Overview Notes:</Typography>
          <TextField
            multiline rows={3} size="small" sx={{ ...inputSx, flex: 1, maxWidth: 500 }}
            placeholder="Enter overview notes..."
            value={formData?.notes || ''} disabled={isLocked}
            inputProps={{ maxLength: 500 }}
            onChange={(e) => handleFieldChange('notes', e.target.value)}
          />
        </Box>

        {!isLocked && (
          <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', mt: 1.5 }}>
            <Button
              variant="contained" onClick={handleSave} disabled={saving} size="small"
              sx={{ bgcolor: '#60a5fa', '&:hover': { bgcolor: '#3b82f6' } }}
            >
              {saving ? 'Saving...' : 'Save Draft'}
            </Button>
            <Button
              variant="contained" onClick={() => setConfirmOpen(true)} disabled={saving} size="small"
              sx={{ bgcolor: '#2563eb', '&:hover': { bgcolor: '#1d4ed8' } }}
            >
              Submit Overview
            </Button>
          </Box>
        )}

        {isLocked && (
          <Alert severity="info" sx={{ mt: 2 }}>
            This section has been submitted and locked. No further edits can be made.
          </Alert>
        )}
      </AccordionDetails>

      <Dialog open={confirmOpen} onClose={() => setConfirmOpen(false)}>
        <DialogTitle sx={{ bgcolor: '#2563eb', color: '#fff' }}>Confirm Submission</DialogTitle>
        <DialogContent sx={{ pt: 3 }}>
          <Typography>Submit the Pre-Award Overview? This will lock the section.</Typography>
          <Typography variant="body2" sx={{ mt: 1, color: '#666' }}>
            <strong>Note:</strong> Once submitted, changes may be restricted.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSubmit} sx={{ bgcolor: '#2563eb' }}>
            Yes, Submit
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
    </Accordion>
  );
}
