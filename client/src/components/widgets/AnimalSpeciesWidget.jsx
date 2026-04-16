import { useState } from 'react';
import {
  Box, Button, RadioGroup, Radio, FormControlLabel,
  Dialog, DialogTitle, DialogContent, DialogActions,
  Typography, Checkbox, FormGroup, Chip, Alert, Snackbar,
} from '@mui/material';

const SPECIES_OPTIONS = [
  'Rodents (mouse/rat/hamster)',
  'Non-Human Primates (NHP)',
  'Pigs',
  'Rabbits',
  'Dogs',
  'Service animals',
  'Cats',
  'Marine mammals',
  'Other',
];

/**
 * Custom RJSF widget for Animal Q1 — renders a radio (yes/no) plus a
 * "Select Animal Species" button that opens a checkbox modal.
 * Selected species are saved to the database via formContext.onAnimalSpeciesChange.
 */
export default function AnimalSpeciesWidget(props) {
  const { value, onChange, disabled, readonly, options, registry } = props;
  const formContext = registry?.formContext;
  const [open, setOpen] = useState(false);
  // Local draft state for the modal — only committed on Save
  const [draft, setDraft] = useState([]);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });

  const enumValues = options?.enumOptions?.map((o) => o.value) || ['yes', 'no'];

  // The persisted species come from formContext (which reads formData.animal_species)
  const savedSpecies = formContext?.animalSpecies || [];

  const handleOpen = () => {
    // Initialize draft from the persisted data
    setDraft([...savedSpecies]);
    setOpen(true);
  };

  const handleToggle = (species) => {
    setDraft((prev) =>
      prev.includes(species)
        ? prev.filter((s) => s !== species)
        : [...prev, species]
    );
  };

  const handleSave = () => {
    formContext?.onAnimalSpeciesChange?.(draft);
    setOpen(false);
    setSnackbar({ open: true, message: 'Animal species saved.', severity: 'success' });
  };

  return (
    <Box>
      {props.label && (
        <Typography sx={{ fontSize: '0.82rem', fontWeight: 600, color: '#374151', mb: 0.5 }}>
          {props.label}
        </Typography>
      )}
      <RadioGroup
        row
        value={value || ''}
        onChange={(e) => onChange(e.target.value)}
      >
        {enumValues.map((v) => (
          <FormControlLabel
            key={v}
            value={v}
            control={<Radio size="small" disabled={disabled || readonly} />}
            label={v}
            sx={{ '& .MuiFormControlLabel-label': { fontSize: 12 } }}
          />
        ))}
      </RadioGroup>

      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 0.5, flexWrap: 'wrap' }}>
        <Button
          variant="outlined"
          size="small"
          onClick={handleOpen}
          disabled={disabled || readonly}
          sx={{ fontSize: 11, textTransform: 'none' }}
        >
          Select Animal Species
        </Button>
        {savedSpecies.map((s) => (
          <Chip key={s} label={s} size="small" sx={{ fontSize: 10, height: 20 }} />
        ))}
      </Box>

      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ bgcolor: '#428bca', color: '#fff' }}>Select Animal Species</DialogTitle>
        <DialogContent sx={{ pt: 2 }}>
          <FormGroup>
            {SPECIES_OPTIONS.map((species) => (
              <FormControlLabel
                key={species}
                control={
                  <Checkbox
                    size="small"
                    checked={draft.includes(species)}
                    onChange={() => handleToggle(species)}
                  />
                }
                label={<Typography sx={{ fontSize: 12 }}>{species}</Typography>}
              />
            ))}
          </FormGroup>
          <Alert severity="info" sx={{ mt: 1, fontSize: 11 }}>
            * Please specify in note to Animal Regulatory Agency
          </Alert>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)} size="small">Cancel</Button>
          <Button variant="contained" onClick={handleSave} size="small" sx={{ bgcolor: '#428bca' }}>
            Save
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
    </Box>
  );
}
