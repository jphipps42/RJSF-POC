import { useState, useCallback } from 'react';
import Form from '@rjsf/mui';
import validator from '@rjsf/validator-ajv8';
import {
  Accordion, AccordionSummary, AccordionDetails,
  Box, Typography, Button, Snackbar, Alert,
  Dialog, DialogTitle, DialogContent, DialogActions,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import StatusBadge from './StatusBadge';
import { saveFormDraft, submitForm } from '../services/api';

export default function ReviewSection({ submission, onUpdate }) {
  const [formData, setFormData] = useState(submission.form_data || {});
  const [saving, setSaving] = useState(false);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
  const [confirmOpen, setConfirmOpen] = useState(false);

  const isLocked = submission.is_locked;
  const schema = submission.json_schema;
  const uiSchema = {
    ...submission.ui_schema,
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
            <Typography sx={{ fontWeight: 700, color: '#0a2540' }}>
              {submission.form_title}
            </Typography>
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
            <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1, mt: 2 }}>
              <Button
                variant="contained"
                onClick={handleSave}
                disabled={saving}
                sx={{ bgcolor: '#60a5fa', '&:hover': { bgcolor: '#3b82f6' } }}
              >
                Save Draft
              </Button>
              <Button
                variant="contained"
                onClick={() => setConfirmOpen(true)}
                disabled={saving}
                sx={{ bgcolor: '#2563eb', '&:hover': { bgcolor: '#1d4ed8' } }}
              >
                {submitLabel}
              </Button>
            </Box>
          )}

          {isLocked && (
            <Alert severity="info" sx={{ mt: 2 }}>
              This section has been submitted and locked. No further edits can be made.
            </Alert>
          )}
        </AccordionDetails>
      </Accordion>

      {/* Submit Confirmation Dialog */}
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
