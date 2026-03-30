import { useState } from 'react';
import {
  Dialog, DialogTitle, DialogContent, DialogActions,
  Button, Box, Typography, Alert,
} from '@mui/material';
import { resetFormSubmission } from '../services/api';

const SECTION_LABELS = {
  safety_review: 'A. Safety Review',
  animal_review: 'B. Animal Research Review',
  human_review: 'C. Human Research Review',
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
};

export default function ResetModal({ open, onClose, submissions, onReset }) {
  const [resetting, setResetting] = useState(null);

  const handleReset = async (submission) => {
    setResetting(submission.id);
    try {
      await resetFormSubmission(submission.id);
      onReset?.(submission);
    } catch (err) {
      console.error('Reset failed:', err);
    }
    setResetting(null);
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle sx={{ bgcolor: '#2563eb', color: '#fff' }}>Reset Checklist</DialogTitle>
      <DialogContent sx={{ pt: 3 }}>
        <Typography sx={{ mb: 2 }}>
          Select which sections you want to reset. This will clear all saved data for the selected sections.
        </Typography>
        {submissions?.map((sub) => (
          <Box key={sub.id} sx={{ border: '1px solid #ddd', borderRadius: 1, p: 2, mb: 1.5 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <Box>
                <Typography sx={{ fontWeight: 700 }}>
                  {SECTION_LABELS[sub.form_key] || sub.form_title}
                </Typography>
                <Typography variant="caption" sx={{ color: '#666' }}>
                  Status: {sub.status.replace('_', ' ')}
                </Typography>
              </Box>
              <Button
                variant="outlined"
                size="small"
                onClick={() => handleReset(sub)}
                disabled={resetting === sub.id}
              >
                {resetting === sub.id ? 'Clearing...' : 'Clear'}
              </Button>
            </Box>
          </Box>
        ))}
        <Alert severity="warning" sx={{ mt: 2 }}>
          <strong>Warning:</strong> This action cannot be undone. All progress for the selected section will be permanently deleted.
        </Alert>
      </DialogContent>
      <DialogActions>
        <Button variant="contained" onClick={onClose} sx={{ bgcolor: '#2563eb' }}>Close</Button>
      </DialogActions>
    </Dialog>
  );
}
