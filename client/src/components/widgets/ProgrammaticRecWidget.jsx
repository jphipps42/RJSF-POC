import { useState } from 'react';
import {
  Box, Button, RadioGroup, Radio, FormControlLabel,
  Dialog, DialogTitle, DialogContent, DialogActions,
  Typography, Select, MenuItem, FormControl, InputLabel,
} from '@mui/material';

const REC_DOCUMENTS = [
  { value: 'ALSRP FY22', label: 'FY22 ALSRP_Programmatic REC_Oct_1 2022 signed.pdf', year: 2022 },
  { value: 'ARP FY22', label: 'FY22 ARP_Programmatic REC_Oct_1 2022 signed.pdf', year: 2022 },
  { value: 'ASUDRP FY22', label: 'FY22 ASUDRP_Programmatic REC_Oct_1 2022 signed.pdf', year: 2022 },
  { value: 'BCRP FY22', label: 'FY22 BCRP_Programmatic REC_Oct_1 2022 signed.pdf', year: 2022 },
];

/**
 * Custom RJSF widget for Safety Q1 — renders a radio (yes/no) plus a
 * "Programmatic REC" button that opens a modal to select a REC document.
 * The selected REC value is written to the `programmatic_rec` field via formContext.
 */
export default function ProgrammaticRecWidget(props) {
  const { value, onChange, disabled, readonly, options, registry } = props;
  const formContext = registry?.formContext;
  const [open, setOpen] = useState(false);
  const [selectedRec, setSelectedRec] = useState(formContext?.programmaticRec || '');
  const [yearFilter, setYearFilter] = useState('all');

  const enumValues = options?.enumOptions?.map((o) => o.value) || ['yes', 'no'];

  const filteredDocs = yearFilter === 'all'
    ? REC_DOCUMENTS
    : REC_DOCUMENTS.filter((d) => d.year === Number(yearFilter));

  const handleSelect = () => {
    if (selectedRec) {
      formContext?.onProgrammaticRecChange?.(selectedRec);
    }
    setOpen(false);
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

      <Button
        variant="outlined"
        size="small"
        onClick={() => setOpen(true)}
        disabled={disabled || readonly}
        sx={{ mt: 0.5, fontSize: 11, textTransform: 'none' }}
      >
        Programmatic REC{selectedRec ? `: ${selectedRec}` : ''}
      </Button>

      <Dialog open={open} onClose={() => setOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle sx={{ bgcolor: '#428bca', color: '#fff' }}>Programmatic REC Documents</DialogTitle>
        <DialogContent sx={{ pt: 3 }}>
          <FormControl size="small" sx={{ mb: 2, minWidth: 160 }}>
            <InputLabel>Filter by Year</InputLabel>
            <Select
              value={yearFilter}
              label="Filter by Year"
              onChange={(e) => setYearFilter(e.target.value)}
              sx={{ fontSize: 12 }}
            >
              <MenuItem value="all" sx={{ fontSize: 12 }}>All Years</MenuItem>
              <MenuItem value="2022" sx={{ fontSize: 12 }}>FY 2022</MenuItem>
              <MenuItem value="2023" sx={{ fontSize: 12 }}>FY 2023</MenuItem>
              <MenuItem value="2024" sx={{ fontSize: 12 }}>FY 2024</MenuItem>
            </Select>
          </FormControl>

          <RadioGroup value={selectedRec} onChange={(e) => setSelectedRec(e.target.value)}>
            {filteredDocs.map((doc) => (
              <FormControlLabel
                key={doc.value}
                value={doc.value}
                control={<Radio size="small" />}
                label={<Typography sx={{ fontSize: 12 }}>{doc.label}</Typography>}
              />
            ))}
          </RadioGroup>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpen(false)} size="small">Cancel</Button>
          <Button variant="contained" onClick={handleSelect} disabled={!selectedRec} size="small" sx={{ bgcolor: '#428bca' }}>
            Select
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
