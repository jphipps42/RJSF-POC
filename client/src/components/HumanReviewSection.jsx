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

const HUMAN_SUBSECTION_ORDER = [
  'human_no_regulatory',
  'human_anatomical',
  'human_data_secondary',
  'human_subjects',
  'human_special_topics',
  'human_estimated_start',
];

function SubsectionIndicator({ status }) {
  const config = {
    not_started: { border: '2px solid #94a3b8', bg: 'transparent', content: '' },
    in_progress: { border: '2px solid #f59e0b', bg: 'linear-gradient(to right, #f59e0b 50%, transparent 50%)', content: '' },
    submitted: { border: '2px solid #10b981', bg: '#10b981', content: '\u2713' },
  };
  const c = config[status] || config.not_started;
  return (
    <Box sx={{
      width: 20, height: 20, borderRadius: '50%', display: 'inline-flex',
      alignItems: 'center', justifyContent: 'center', fontSize: 12,
      border: c.border, background: c.bg, color: '#fff', fontWeight: 700, flexShrink: 0,
    }}>
      {c.content}
    </Box>
  );
}

function computeGroupStatus(subs) {
  if (!subs || subs.length === 0) return 'not_started';
  const allSubmitted = subs.every((s) => s.status === 'submitted');
  const anyActive = subs.some((s) => s.status === 'in_progress' || s.status === 'submitted');
  return allSubmitted ? 'submitted' : anyActive ? 'in_progress' : 'not_started';
}

function SubsectionForm({ submission, onUpdate }) {
  const versioned = useVersionedFormData(submission, 'edit');
  const [formData, setFormData] = useState(versioned.formData);
  const [saving, setSaving] = useState(false);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });

  const [lastKey, setLastKey] = useState(null);
  const key = `${submission.id}-${versioned.schemaVersion}-${versioned.migrated}`;
  if (key !== lastKey) { setLastKey(key); setFormData(versioned.formData); }

  const isLocked = submission.is_locked;
  const schema = versioned.schema;
  const uiSchema = { ...versioned.uiSchema, 'ui:title': ' ', 'ui:submitButtonOptions': { norender: true } };

  // "Default all to No" checkbox logic
  const DEFAULT_NO_KEYS = ['no_review_default_no', 'has_default_no'];

  const handleFormChange = (e) => {
    const newData = e.formData;
    const props = schema?.properties || {};
    // Check if a "default all to no" checkbox was just toggled ON
    for (const dKey of DEFAULT_NO_KEYS) {
      if (dKey in props && newData[dKey] === true && formData[dKey] !== true) {
        const updated = { ...newData };
        for (const [field, def] of Object.entries(props)) {
          if (field !== dKey && field !== 'notes' && def.enum && def.enum.includes('no')) {
            updated[field] = 'no';
          }
        }
        setFormData(updated);
        return;
      }
    }
    setFormData(newData);
  };

  const handleSave = useCallback(async () => {
    setSaving(true);
    try {
      const res = await saveFormDraft(submission.id, formData);
      setSnackbar({ open: true, message: `${submission.form_title} saved.`, severity: 'success' });
      onUpdate?.(res.data);
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.error || 'Save failed', severity: 'error' });
    }
    setSaving(false);
  }, [formData, submission.id, submission.form_title, onUpdate]);

  if (versioned.loading) {
    return <Box sx={{ p: 1, mb: 1, border: '1px solid #dbeafe', borderRadius: 1.5, bgcolor: '#f8fbff' }}>
      <Typography sx={{ color: '#666', fontSize: 11 }}>Loading...</Typography>
    </Box>;
  }

  return (
    <>
      <Accordion sx={{
        border: '1px solid #dbeafe', borderRadius: '6px !important', mb: 1, bgcolor: '#f8fbff',
        '&:before': { display: 'none' },
      }}>
        <AccordionSummary
          expandIcon={<ExpandMoreIcon fontSize="small" />}
          sx={{
            bgcolor: '#eef5ff', '&:hover': { bgcolor: '#e0ecff' },
            '& .MuiAccordionSummary-content': { alignItems: 'center', gap: 1 },
          }}
        >
          <Typography sx={{ fontWeight: 700, fontSize: '0.8rem', color: '#0a2540', flex: 1 }}>
            {submission.form_title}
          </Typography>
          {versioned.schemaVersion > 1 && (
            <Chip label={`v${versioned.schemaVersion}`} size="small"
              sx={{ fontSize: 9, height: 16, bgcolor: '#dbeafe', color: '#1d4ed8', mr: 0.5 }} />
          )}
          <SubsectionIndicator status={submission.status} />
        </AccordionSummary>
        <AccordionDetails sx={{ bgcolor: '#fff' }}>
          {versioned.migrated && (
            <Alert severity="info" sx={{ mb: 1, py: 0, fontSize: 11 }}>
              Data migrated from v{submission.schema_version} to v{versioned.schemaVersion}.
            </Alert>
          )}
          <Form
            schema={schema} uiSchema={uiSchema} formData={formData} validator={validator}
            onChange={handleFormChange}
            disabled={isLocked} readonly={isLocked} liveValidate={false}
          />
          {!isLocked && (
            <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 1 }}>
              <Button variant="contained" onClick={handleSave} disabled={saving}
                size="small" sx={{ bgcolor: '#60a5fa', '&:hover': { bgcolor: '#3b82f6' } }}>
                Save Subsection
              </Button>
            </Box>
          )}
          {isLocked && <Alert severity="info" sx={{ mt: 1, py: 0, fontSize: 11 }}>This subsection has been submitted and locked.</Alert>}
        </AccordionDetails>
      </Accordion>
      <Snackbar open={snackbar.open} autoHideDuration={3000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}>
        <Alert severity={snackbar.severity} onClose={() => setSnackbar({ ...snackbar, open: false })}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </>
  );
}

export default function HumanReviewSection({ submissions, onUpdate, expanded, onAccordionChange }) {
  const [saving, setSaving] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });

  const ordered = HUMAN_SUBSECTION_ORDER
    .map((key) => submissions.find((s) => s.form_key === key))
    .filter(Boolean);

  if (ordered.length === 0) return null;

  const allSubmitted = ordered.every((s) => s.status === 'submitted');
  const overallStatus = computeGroupStatus(ordered);

  const handleSubmitAll = useCallback(async () => {
    setConfirmOpen(false);
    setSaving(true);
    try {
      for (const sub of ordered) {
        if (!sub.is_locked) {
          const res = await submitForm(sub.id, sub.form_data);
          onUpdate?.(res.data);
        }
      }
      setSnackbar({ open: true, message: 'C. Human Research Review submitted and locked.', severity: 'success' });
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.error || 'Submit failed', severity: 'error' });
    }
    setSaving(false);
  }, [ordered, onUpdate]);

  return (
    <>
      <Accordion expanded={expanded} onChange={onAccordionChange} sx={{
        border: '1px solid #d6e4f2', borderRadius: '8px !important', mb: 2, overflow: 'hidden',
        '&:before': { display: 'none' },
      }}>
        <AccordionSummary
          expandIcon={<ExpandMoreIcon />}
          sx={{
            bgcolor: '#e7f1ff', '&:hover': { bgcolor: '#dbeafe' },
            '&.Mui-expanded': {
              bgcolor: '#bfdbfe', border: '2px solid #2563eb',
              boxShadow: '0 0 0 3px rgba(37,99,235,0.1)',
            },
          }}
        >
          <Box sx={{ display: 'flex', justifyContent: 'space-between', width: '100%', alignItems: 'center', pr: 1 }}>
            <Typography sx={{ fontWeight: 700, color: '#0a2540', fontSize: '0.85rem' }}>
              C. Human Research Review Requirements
            </Typography>
            <StatusBadge status={overallStatus} />
          </Box>
        </AccordionSummary>
        <AccordionDetails sx={{ bgcolor: '#fff' }}>
          {ordered.map((sub) => (
            <SubsectionForm key={sub.id} submission={sub} onUpdate={onUpdate} />
          ))}

          {!allSubmitted && (
            <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', mt: 1 }}>
              <Button variant="contained" onClick={() => setConfirmOpen(true)} disabled={saving}
                size="small" sx={{ bgcolor: '#2563eb', '&:hover': { bgcolor: '#1d4ed8' } }}>
                Submit to Human Research Regulatory Agency
              </Button>
            </Box>
          )}
          {allSubmitted && (
            <Alert severity="info" sx={{ mt: 1, py: 0, fontSize: 11 }}>
              All subsections have been submitted and locked. No further edits can be made.
            </Alert>
          )}
        </AccordionDetails>
      </Accordion>

      <Dialog open={confirmOpen} onClose={() => setConfirmOpen(false)}>
        <DialogTitle sx={{ bgcolor: '#2563eb', color: '#fff', fontSize: 14 }}>Confirm Submission</DialogTitle>
        <DialogContent sx={{ pt: 3 }}>
          <Typography sx={{ fontSize: 13 }}>Submit all human research subsections? This will lock the entire section.</Typography>
          <Typography variant="body2" sx={{ mt: 1, color: '#666', fontSize: 12 }}>
            <strong>Note:</strong> Once submitted, changes may be restricted.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button size="small" onClick={() => setConfirmOpen(false)}>Cancel</Button>
          <Button variant="contained" size="small" onClick={handleSubmitAll} sx={{ bgcolor: '#2563eb' }}>Yes, Submit</Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={snackbar.open} autoHideDuration={4000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}>
        <Alert severity={snackbar.severity} onClose={() => setSnackbar({ ...snackbar, open: false })}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </>
  );
}
