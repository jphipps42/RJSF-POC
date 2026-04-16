import { useState } from 'react';
import {
  Box, Button, RadioGroup, Radio, FormControlLabel,
  Dialog, DialogTitle, DialogContent, DialogActions,
  Typography, List, ListItem, ListItemText, Alert,
} from '@mui/material';

const CHEMICAL_AGENTS = [
  'Sarin (GB)',
  'VX nerve agent',
  'Sulfur Mustard (HD)',
  'Nitrogen Mustard (HN)',
  'Lewisite',
  'Ricin',
  'Saxitoxin',
  'Phosgene',
  'Chlorine (toxic industrial chemical)',
  'Hydrogen Cyanide',
];

/**
 * Custom RJSF widget for Safety Q4 — renders a radio (yes/no) plus a
 * "Chemical Agents" button that opens an informational reference modal.
 */
export default function ChemicalAgentsWidget(props) {
  const { value, onChange, disabled, readonly, options } = props;
  const [open, setOpen] = useState(false);
  const enumValues = options?.enumOptions?.map((o) => o.value) || ['yes', 'no'];

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

      <Button
        variant="outlined"
        size="small"
        onClick={() => setOpen(true)}
        sx={{ mt: 0.5, fontSize: 11, textTransform: 'none' }}
      >
        Chemical Agents
      </Button>

      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ bgcolor: '#428bca', color: '#fff' }}>Chemical Agents</DialogTitle>
        <DialogContent sx={{ pt: 2 }}>
          <Typography sx={{ fontSize: 13, mb: 1 }}>
            The following are examples of chemical agents that may require additional review or approval.
            Select or reference any that apply to your research.
          </Typography>
          <List dense>
            {CHEMICAL_AGENTS.map((agent) => (
              <ListItem key={agent} sx={{ py: 0.25 }}>
                <ListItemText primary={agent} primaryTypographyProps={{ fontSize: 12 }} />
              </ListItem>
            ))}
          </List>
          <Alert severity="info" sx={{ mt: 1, fontSize: 11 }}>
            This list is not exhaustive. Investigators are responsible for identifying and disclosing
            any hazardous chemical agents associated with the proposed work.
          </Alert>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)} size="small">Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
