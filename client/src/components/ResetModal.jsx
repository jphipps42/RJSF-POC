import { useState } from 'react';
import {
  Dialog, DialogTitle, DialogContent, DialogActions,
  Button, Box, Typography, Alert, Chip, IconButton,
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import { resetFormSubmission } from '../services/api';

const SECTION_LABELS = {
  overview: 'Pre-Award Overview',
  safety_review: 'A. Safety Review',
  animal_review: 'B. Animal Research Review',
  human_no_regulatory: 'C.a No Regulatory Review Required',
  human_anatomical: 'C.b Human Anatomical Substances',
  human_data_secondary: 'C.c Human Data - Secondary Use',
  human_subjects: 'C.d Human Subjects',
  human_special_topics: 'C.e Other/Special Topics',
  human_estimated_start: 'C.f Estimated Start',
  acq_br_personnel: 'D.1a Personnel',
  acq_br_equipment: 'D.1b Equipment',
  acq_br_travel: 'D.1c Travel',
  acq_br_materials: 'D.1d Materials/Supplies',
  acq_br_consultant: 'D.1e Consultant/Collaborator',
  acq_br_third_party: 'D.1f 3rd Party',
  acq_br_other_direct: 'D.1g Other Direct Costs',
  acq_br_additional: 'D.1h Additional Concerns',
  acq_peer_review: 'D.2 Peer and Programmatic Review',
  acq_sow_concerns: 'D.3 Statement of Work Concerns',
  acq_cps: 'D.4 Current and Pending Support',
  acq_ier: 'D.5 Inclusion Enrollment Report',
  acq_data_management: 'D.6 Data Management Plan',
  acq_special_requirements: 'D.7 Special Requirements',
  final_recommendation: 'Final Recommendation',
};

const STATUS_COLORS = {
  not_started: { bg: '#f1f5f9', color: '#475569' },
  in_progress: { bg: '#fef3c7', color: '#92400e' },
  submitted: { bg: '#d1fae5', color: '#065f46' },
};

// Ordered list matching manifest hierarchy
const SECTION_ORDER = [
  'overview', 'safety_review', 'animal_review',
  'human_no_regulatory', 'human_anatomical', 'human_data_secondary',
  'human_subjects', 'human_special_topics', 'human_estimated_start',
  'acq_br_personnel', 'acq_br_equipment', 'acq_br_travel', 'acq_br_materials',
  'acq_br_consultant', 'acq_br_third_party', 'acq_br_other_direct', 'acq_br_additional',
  'acq_peer_review', 'acq_sow_concerns', 'acq_cps', 'acq_ier',
  'acq_data_management', 'acq_special_requirements', 'final_recommendation',
];

export default function ResetModal({ open, onClose, submissions, onReset }) {
  const [resetting, setResetting] = useState(null);
  const [resettingAll, setResettingAll] = useState(false);
  const [successSection, setSuccessSection] = useState(null);

  // With composite form there's 1 submission; section_status holds per-section statuses
  const submission = submissions?.[0];
  const sectionStatus = submission?.section_status || {};
  const submissionId = submission?.id;

  const handleResetSection = async (sectionId) => {
    if (!submissionId) return;
    const label = SECTION_LABELS[sectionId] || sectionId;
    const confirmed = window.confirm(
      `Are you sure you want to clear all data for the ${label}? This cannot be undone.`
    );
    if (!confirmed) return;
    setResetting(sectionId);
    try {
      await resetFormSubmission(submissionId, sectionId);
      onReset?.();
      setSuccessSection(label);
    } catch (err) {
      console.error('Reset failed:', err);
    }
    setResetting(null);
  };

  const handleResetAll = async () => {
    if (!submissionId) return;
    const confirmed = window.confirm(
      'Are you sure you want to clear all data for all submitted sections? This cannot be undone.'
    );
    if (!confirmed) return;
    setResettingAll(true);
    try {
      await resetFormSubmission(submissionId);
      onReset?.();
      setSuccessSection('All submitted sections');
    } catch (err) {
      console.error('Reset all failed:', err);
    }
    setResettingAll(false);
  };

  const submittedSections = SECTION_ORDER.filter((id) => sectionStatus[id] === 'submitted');

  return (
    <>
      <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ bgcolor: '#428bca', color: '#fff' }}>Reset Checklist (Admin Only)</DialogTitle>
        <DialogContent sx={{ pt: 3 }}>
          <Typography sx={{ mb: 1, fontSize: 13 }}>
            Select which sections you want to reset. This will clear all saved data for the selected sections.
          </Typography>

          {submittedSections.length === 0 && (
            <Alert severity="info" sx={{ mb: 2 }}>No sections are currently submitted/locked.</Alert>
          )}

          {submittedSections.length > 1 && (
            <Box sx={{ mb: 2 }}>
              <Button
                variant="contained"
                color="warning"
                size="small"
                onClick={handleResetAll}
                disabled={resettingAll}
                sx={{ fontSize: 12, fontWeight: 700 }}
              >
                {resettingAll ? 'Clearing All...' : 'Clear All Submitted Sections'}
              </Button>
            </Box>
          )}

          {SECTION_ORDER.map((sectionId) => {
            const status = sectionStatus[sectionId] || 'not_started';
            const colors = STATUS_COLORS[status] || STATUS_COLORS.not_started;
            const isSubmitted = status === 'submitted';

            return (
              <Box
                key={sectionId}
                sx={{
                  display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                  border: '1px solid #e2e8f0', borderRadius: 1, px: 1.5, py: 0.75, mb: 0.75,
                }}
              >
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <Typography sx={{ fontSize: 12, fontWeight: 600 }}>
                    {SECTION_LABELS[sectionId] || sectionId}
                  </Typography>
                  <Chip
                    label={status.replace('_', ' ')}
                    size="small"
                    sx={{ fontSize: 9, height: 18, bgcolor: colors.bg, color: colors.color }}
                  />
                </Box>
                {isSubmitted && (
                  <Button
                    variant="outlined"
                    size="small"
                    onClick={() => handleResetSection(sectionId)}
                    disabled={resetting === sectionId || resettingAll}
                    sx={{ fontSize: 10, minWidth: 0, px: 1.5, color: '#428bca', borderColor: '#428bca' }}
                  >
                    {resetting === sectionId ? 'Clearing...' : 'Clear'}
                  </Button>
                )}
              </Box>
            );
          })}

          <Box sx={{ mt: 2, p: 1.5, bgcolor: '#fff3cd', borderRadius: 1 }}>
            <Typography component="span" sx={{ fontWeight: 700, fontSize: 11 }}>Warning: </Typography>
            <Typography component="span" sx={{ fontSize: 11 }}>
              This action cannot be undone. All progress for the selected section will be permanently deleted.
            </Typography>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button variant="contained" onClick={onClose} sx={{ bgcolor: '#428bca' }}>Close</Button>
        </DialogActions>
      </Dialog>

      {/* Success confirmation window (ReqID 24) */}
      <Dialog open={!!successSection} onClose={() => setSuccessSection(null)}>
        <DialogTitle sx={{ bgcolor: '#428bca', color: '#fff', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          Section Cleared
          <IconButton onClick={() => setSuccessSection(null)} sx={{ color: '#fff', p: 0.5 }}>
            <CloseIcon />
          </IconButton>
        </DialogTitle>
        <DialogContent sx={{ textAlign: 'center', pt: 4, pb: 2 }}>
          <CheckCircleIcon sx={{ fontSize: 48, color: '#27ae60', mb: 1 }} />
          <Typography>{successSection} section has been cleared successfully.</Typography>
        </DialogContent>
        <DialogActions sx={{ justifyContent: 'flex-start' }}>
          <Button variant="contained" onClick={() => setSuccessSection(null)} sx={{ bgcolor: '#428bca' }}>OK</Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
