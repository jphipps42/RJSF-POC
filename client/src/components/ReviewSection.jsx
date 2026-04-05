import { useState, useCallback } from 'react';
import Form from '@rjsf/mui';
import validator from '@rjsf/validator-ajv8';
import {
  Accordion, AccordionSummary, AccordionDetails,
  Box, Typography, Button, Snackbar, Alert, Chip,
  Dialog, DialogTitle, DialogContent, DialogActions,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import StatusBadge from './StatusBadge';
import useVersionedFormData from '../hooks/useVersionedFormData';
import { saveFormDraft, submitForm } from '../services/api';

export default function ReviewSection({ submission, onUpdate, auditMode = false }) {
  const mode = auditMode ? 'audit' : 'edit';
  const versioned = useVersionedFormData(submission, mode);

  const [formData, setFormData] = useState(versioned.formData);
  const [saving, setSaving] = useState(false);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
  const [confirmOpen, setConfirmOpen] = useState(false);

  // Sync formData when versioned data changes (e.g., after migration)
  const [lastVersionKey, setLastVersionKey] = useState(null);
  const versionKey = `${submission.id}-${versioned.schemaVersion}-${versioned.migrated}`;
  if (versionKey !== lastVersionKey) {
    setLastVersionKey(versionKey);
    setFormData(versioned.formData);
  }

  const isLocked = submission.is_locked || auditMode;
  const schema = versioned.schema;
  const uiSchema = {
    ...versioned.uiSchema,
    'ui:title': ' ',
    'ui:submitButtonOptions': { norender: true },
  };

  const handleSave = useCallback(async () => {
    setSaving(true);
    try {
      const res = await saveFormDraft(submission.id, formData);
      setSnackbar({ open: true, message: `${submission.form_title} progress saved successfully.`, severity: 'success' });
      onUpdate?.(res.data);
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.error || 'Save failed', severity: 'error' });
    }
    setSaving(false);
  }, [formData, submission.id, submission.form_title, onUpdate]);

  const handleSubmit = useCallback(async () => {
    setConfirmOpen(false);
    setSaving(true);
    try {
      const res = await submitForm(submission.id, formData);
      setSnackbar({ open: true, message: `${submission.form_title} submitted and locked successfully.`, severity: 'success' });
      onUpdate?.(res.data);
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.error || 'Submit failed', severity: 'error' });
    }
    setSaving(false);
  }, [formData, submission.id, submission.form_title, onUpdate]);

  const submitLabel = {
    safety_review: 'Submit to Safety Office',
    animal_review: 'Submit to Animal Research Regulatory Office',
    human_review: 'Submit to Human Research Regulatory Agency',
  }[submission.form_key] || 'Submit';

  if (versioned.loading) {
    return (
      <Box sx={{ p: 2, mb: 2, border: '1px solid #d6e4f2', borderRadius: 2, bgcolor: '#f8fafc' }}>
        <Typography sx={{ color: '#666' }}>Loading form data...</Typography>
      </Box>
    );
  }

  return (
    <>
      <Accordion
        sx={{
          border: '1px solid #d6e4f2',
          borderRadius: '8px !important',
          mb: 2,
          overflow: 'hidden',
          '&:before': { display: 'none' },
        }}
      >
        <AccordionSummary
          expandIcon={<ExpandMoreIcon />}
          sx={{
            bgcolor: '#e7f1ff',
            '&:hover': { bgcolor: '#dbeafe' },
            '&.Mui-expanded': {
              bgcolor: '#bfdbfe',
              border: '2px solid #2563eb',
              boxShadow: '0 0 0 3px rgba(37,99,235,0.1)',
            },
          }}
        >
          <Box sx={{ display: 'flex', justifyContent: 'space-between', width: '100%', alignItems: 'center', pr: 1 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Typography sx={{ fontWeight: 700, color: '#0a2540', fontSize: '0.85rem' }}>
                {submission.form_title}
              </Typography>
              {versioned.schemaVersion > 1 && (
                <Chip label={`v${versioned.schemaVersion}`} size="small"
                  sx={{ fontSize: 10, height: 20, bgcolor: '#dbeafe', color: '#1d4ed8' }} />
              )}
            </Box>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              {submission.submitted_at && (
                <Typography sx={{ fontSize: 12, color: '#475569' }}>
                  {new Date(submission.submitted_at).toLocaleDateString()}
                </Typography>
              )}
              <StatusBadge status={submission.status} />
            </Box>
          </Box>
        </AccordionSummary>
        <AccordionDetails sx={{ bgcolor: '#fff', p: 2 }}>
          {versioned.migrated && (
            <Alert severity="info" sx={{ mb: 2 }}>
              Data migrated from v{submission.schema_version} to v{versioned.schemaVersion}.
            </Alert>
          )}

          <Form
            schema={schema}
            uiSchema={uiSchema}
            formData={formData}
            validator={validator}
            onChange={(e) => setFormData(e.formData)}
            disabled={isLocked}
            readonly={isLocked}
            liveValidate={false}
          />

          {!isLocked && (
            <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', mt: 1.5 }}>
              <Button
                variant="contained"
                onClick={handleSave}
                disabled={saving}
                size="small" sx={{ bgcolor: '#60a5fa', '&:hover': { bgcolor: '#3b82f6' } }}
              >
                Save Draft
              </Button>
              <Button
                variant="contained"
                onClick={() => setConfirmOpen(true)}
                disabled={saving}
                size="small" sx={{ bgcolor: '#2563eb', '&:hover': { bgcolor: '#1d4ed8' } }}
              >
                {submitLabel}
              </Button>
            </Box>
          )}

          {isLocked && !auditMode && (
            <Alert severity="info" sx={{ mt: 2 }}>
              This section has been submitted and locked. No further edits can be made.
            </Alert>
          )}
          {auditMode && (
            <Alert severity="warning" sx={{ mt: 2 }}>
              Audit view — rendered with original schema v{versioned.schemaVersion}.
            </Alert>
          )}
        </AccordionDetails>
      </Accordion>

      <Dialog open={confirmOpen} onClose={() => setConfirmOpen(false)}>
        <DialogTitle sx={{ bgcolor: '#2563eb', color: '#fff' }}>Confirm Submission</DialogTitle>
        <DialogContent sx={{ pt: 3 }}>
          <Typography>Submit this section? This will lock the section.</Typography>
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
        autoHideDuration={4000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      >
        <Alert severity={snackbar.severity} onClose={() => setSnackbar({ ...snackbar, open: false })}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </>
  );
}
