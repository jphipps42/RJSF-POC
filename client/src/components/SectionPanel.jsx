import { forwardRef, useState } from 'react';
import Form from '@rjsf/mui';
import validator from '@rjsf/validator-ajv8';
import {
  Accordion, AccordionSummary, AccordionDetails,
  Box, Typography, Button, Chip, Alert,
  Dialog, DialogTitle, DialogContent, DialogActions,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import StatusBadge from './StatusBadge';

const SectionPanel = forwardRef(function SectionPanel({
  sectionId,
  title,
  schema,
  uiSchema,
  formData,
  onChange,
  sectionStatus,
  dirty,
  saveStatus,
  onSave,
  onSubmit,
  submitLabel,
  expanded,
  onAccordionChange,
  formContext,
  children,
}, ref) {
  const [confirmOpen, setConfirmOpen] = useState(false);
  const isLocked = sectionStatus === 'submitted';
  const saving = saveStatus === 'saving';

  const mergedUiSchema = {
    ...uiSchema,
    'ui:title': ' ',
    'ui:submitButtonOptions': { norender: true },
  };

  return (
    <>
      <Accordion
        expanded={expanded}
        onChange={onAccordionChange}
        sx={{
          border: '1px solid #d6e4f2', borderRadius: '8px !important',
          mb: 2, overflow: 'hidden', '&:before': { display: 'none' },
        }}
      >
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
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Typography sx={{ fontWeight: 700, color: '#0a2540', fontSize: '0.85rem' }}>
                {title}
              </Typography>
              {dirty && saveStatus === 'idle' && (
                <Chip label="Unsaved" size="small" sx={{ fontSize: 9, height: 18, bgcolor: '#fef3c7', color: '#92400e' }} />
              )}
              {saveStatus === 'saved' && (
                <Chip label="Saved" size="small" sx={{ fontSize: 9, height: 18, bgcolor: '#d1fae5', color: '#065f46' }} />
              )}
              {saveStatus === 'error' && (
                <Chip label="Error" size="small" sx={{ fontSize: 9, height: 18, bgcolor: '#fee2e2', color: '#991b1b' }} />
              )}
            </Box>
            <StatusBadge status={sectionStatus || 'not_started'} />
          </Box>
        </AccordionSummary>
        <AccordionDetails sx={{ bgcolor: '#fff', p: 2 }}>
          <Form
            ref={ref}
            tagName="div"
            schema={schema}
            uiSchema={mergedUiSchema}
            formData={formData}
            validator={validator}
            onChange={({ formData: d }) => onChange(d)}
            disabled={isLocked}
            readonly={isLocked}
            liveValidate={false}
            noHtml5Validate
            formContext={formContext}
          >
            <></>
          </Form>

          {children}

          {!isLocked && (
            <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', mt: 1.5 }}>
              <Button
                variant="contained"
                onClick={onSave}
                disabled={!dirty || saving}
                size="small"
                sx={{ bgcolor: '#60a5fa', '&:hover': { bgcolor: '#3b82f6' } }}
              >
                {saving ? 'Saving...' : 'Save Draft'}
              </Button>
              {submitLabel && (
                <Button
                  variant="contained"
                  onClick={() => setConfirmOpen(true)}
                  disabled={saving}
                  size="small"
                  sx={{ bgcolor: '#2563eb', '&:hover': { bgcolor: '#1d4ed8' } }}
                >
                  {submitLabel}
                </Button>
              )}
            </Box>
          )}

          {isLocked && (
            <Alert severity="info" sx={{ mt: 2 }}>
              This section has been submitted and locked. No further edits can be made.
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
          <Button variant="contained" onClick={() => { setConfirmOpen(false); onSubmit?.(); }} sx={{ bgcolor: '#2563eb' }}>
            Yes, Submit
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
});

export default SectionPanel;
