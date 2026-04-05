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

// Budget Review sub-items (a-h), rendered as nested accordions inside subsection 1
const BUDGET_REVIEW_ORDER = [
  'acq_br_personnel',
  'acq_br_equipment',
  'acq_br_travel',
  'acq_br_materials',
  'acq_br_consultant',
  'acq_br_third_party',
  'acq_br_other_direct',
  'acq_br_additional',
];

// Subsections 2-7 (non-budget)
const OTHER_SUBSECTION_ORDER = [
  'acq_peer_review',
  'acq_sow_concerns',
  'acq_cps',
  'acq_ier',
  'acq_data_management',
  'acq_special_requirements',
];

// All keys for overall status computation
const ALL_ACQ_KEYS = [...BUDGET_REVIEW_ORDER, ...OTHER_SUBSECTION_ORDER];

// ---- Status indicator circle ----
function SubsectionIndicator({ status }) {
  const config = {
    not_started: { border: '2px solid #94a3b8', bg: 'transparent', content: '' },
    in_progress: { border: '2px solid #f59e0b', bg: 'linear-gradient(to right, #f59e0b 50%, transparent 50%)', content: '' },
    submitted: { border: '2px solid #10b981', bg: '#10b981', content: '\u2713' },
  };
  const c = config[status] || config.not_started;
  return (
    <Box sx={{
      width: 22, height: 22, borderRadius: '50%', display: 'inline-flex',
      alignItems: 'center', justifyContent: 'center', fontSize: 13,
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

// ---- Leaf-level form accordion (used for budget sub-items a-h AND subsections 2-7) ----
function SubsectionForm({ submission, onUpdate }) {
  const versioned = useVersionedFormData(submission, 'edit');
  const [formData, setFormData] = useState(versioned.formData);
  const [saving, setSaving] = useState(false);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });

  // Sync formData when versioned data changes
  const [lastKey, setLastKey] = useState(null);
  const key = `${submission.id}-${versioned.schemaVersion}-${versioned.migrated}`;
  if (key !== lastKey) { setLastKey(key); setFormData(versioned.formData); }

  const isLocked = submission.is_locked;
  const schema = versioned.schema;
  const uiSchema = { ...versioned.uiSchema, 'ui:title': ' ', 'ui:submitButtonOptions': { norender: true } };

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
    return <Box sx={{ p: 1.5, mb: 1.5, border: '1px solid #dbeafe', borderRadius: 1.5, bgcolor: '#f8fbff' }}>
      <Typography sx={{ color: '#666', fontSize: 13 }}>Loading...</Typography>
    </Box>;
  }

  return (
    <>
      <Accordion sx={{
        border: '1px solid #dbeafe', borderRadius: '6px !important', mb: 1.5, bgcolor: '#f8fbff',
        '&:before': { display: 'none' },
      }}>
        <AccordionSummary
          expandIcon={<ExpandMoreIcon fontSize="small" />}
          sx={{
            bgcolor: '#eef5ff', '&:hover': { bgcolor: '#e0ecff' }, minHeight: 40,
            '& .MuiAccordionSummary-content': { alignItems: 'center', gap: 1.5 },
          }}
        >
          <Typography sx={{ fontWeight: 700, fontSize: '0.8rem', color: '#0a2540', flex: 1 }}>
            {submission.form_title}
          </Typography>
          {versioned.schemaVersion > 1 && (
            <Chip label={`v${versioned.schemaVersion}`} size="small"
              sx={{ fontSize: 10, height: 18, bgcolor: '#dbeafe', color: '#1d4ed8', mr: 1 }} />
          )}
          <SubsectionIndicator status={submission.status} />
        </AccordionSummary>
        <AccordionDetails sx={{ bgcolor: '#fff', p: 2 }}>
          {versioned.migrated && (
            <Alert severity="info" sx={{ mb: 1, py: 0 }}>
              Data migrated from v{submission.schema_version} to v{versioned.schemaVersion}.
            </Alert>
          )}
          <Form
            schema={schema} uiSchema={uiSchema} formData={formData} validator={validator}
            onChange={(e) => setFormData(e.formData)}
            disabled={isLocked} readonly={isLocked} liveValidate={false}
          />
          {!isLocked && (
            <Box sx={{ display: 'flex', justifyContent: 'flex-end', mt: 1.5 }}>
              <Button variant="contained" onClick={handleSave} disabled={saving}
                size="small" sx={{ bgcolor: '#60a5fa', '&:hover': { bgcolor: '#3b82f6' } }}>
                Save Subsection
              </Button>
            </Box>
          )}
          {isLocked && <Alert severity="info" sx={{ mt: 1 }}>This subsection has been submitted and locked.</Alert>}
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

// ---- Budget Review wrapper accordion (subsection 1) containing a-h ----
function BudgetReviewSection({ budgetSubs, onUpdate }) {
  const status = computeGroupStatus(budgetSubs);

  return (
    <Accordion sx={{
      border: '1px solid #dbeafe', borderRadius: '6px !important', mb: 1.5, bgcolor: '#f8fbff',
      '&:before': { display: 'none' },
    }}>
      <AccordionSummary
        expandIcon={<ExpandMoreIcon fontSize="small" />}
        sx={{
          bgcolor: '#eef5ff', '&:hover': { bgcolor: '#e0ecff' }, minHeight: 40,
          '& .MuiAccordionSummary-content': { alignItems: 'center', gap: 1.5 },
        }}
      >
        <Typography sx={{ fontWeight: 700, fontSize: '0.8rem', color: '#0a2540', flex: 1 }}>
          1. Budget Review
        </Typography>
        <SubsectionIndicator status={status} />
      </AccordionSummary>
      <AccordionDetails sx={{ bgcolor: '#fff', p: 1.5 }}>
        {budgetSubs.map((sub) => (
          <SubsectionForm key={sub.id} submission={sub} onUpdate={onUpdate} />
        ))}
      </AccordionDetails>
    </Accordion>
  );
}

// ---- Parent Section D accordion ----
export default function AcquisitionSection({ submissions, onUpdate }) {
  const [saving, setSaving] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });

  const budgetSubs = BUDGET_REVIEW_ORDER
    .map((key) => submissions.find((s) => s.form_key === key))
    .filter(Boolean);

  const otherSubs = OTHER_SUBSECTION_ORDER
    .map((key) => submissions.find((s) => s.form_key === key))
    .filter(Boolean);

  const allSubs = [...budgetSubs, ...otherSubs];
  if (allSubs.length === 0) return null;

  const allSubmitted = allSubs.every((s) => s.status === 'submitted');
  const overallStatus = computeGroupStatus(allSubs);

  const handleSubmitAll = useCallback(async () => {
    setConfirmOpen(false);
    setSaving(true);
    try {
      for (const sub of allSubs) {
        if (!sub.is_locked) {
          const res = await submitForm(sub.id, sub.form_data);
          onUpdate?.(res.data);
        }
      }
      setSnackbar({ open: true, message: 'D. Acquisition/Contracting Review submitted and locked.', severity: 'success' });
    } catch (err) {
      setSnackbar({ open: true, message: err.response?.data?.error || 'Submit failed', severity: 'error' });
    }
    setSaving(false);
  }, [allSubs, onUpdate]);

  return (
    <>
      <Accordion sx={{
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
              D. Acquisition/Contracting Review
            </Typography>
            <StatusBadge status={overallStatus} />
          </Box>
        </AccordionSummary>
        <AccordionDetails sx={{ bgcolor: '#fff', p: 2 }}>
          {/* Subsection 1: Budget Review with nested a-h accordions */}
          <BudgetReviewSection budgetSubs={budgetSubs} onUpdate={onUpdate} />

          {/* Subsections 2-7 */}
          {otherSubs.map((sub) => (
            <SubsectionForm key={sub.id} submission={sub} onUpdate={onUpdate} />
          ))}

          {!allSubmitted && (
            <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', mt: 1.5 }}>
              <Button variant="contained" onClick={() => setConfirmOpen(true)} disabled={saving}
                size="small" sx={{ bgcolor: '#2563eb', '&:hover': { bgcolor: '#1d4ed8' } }}>
                Submit to Finance for processing
              </Button>
            </Box>
          )}
          {allSubmitted && (
            <Alert severity="info" sx={{ mt: 2 }}>
              All subsections have been submitted and locked. No further edits can be made.
            </Alert>
          )}
        </AccordionDetails>
      </Accordion>

      <Dialog open={confirmOpen} onClose={() => setConfirmOpen(false)}>
        <DialogTitle sx={{ bgcolor: '#2563eb', color: '#fff' }}>Confirm Submission</DialogTitle>
        <DialogContent sx={{ pt: 3 }}>
          <Typography>Submit all acquisition subsections? This will lock the entire section.</Typography>
          <Typography variant="body2" sx={{ mt: 1, color: '#666' }}>
            <strong>Note:</strong> Once submitted, changes may be restricted.
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConfirmOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleSubmitAll} sx={{ bgcolor: '#2563eb' }}>Yes, Submit</Button>
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
