import { Box, Button, RadioGroup, Radio, FormControlLabel, Typography } from '@mui/material';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';

const BSAT_URL = 'https://www.selectagents.gov/sat/list.htm';

/**
 * Custom RJSF widget for Safety Q3 — renders a radio (yes/no) plus a
 * "BSAT Reference" button that opens the federal Select Agents list in a new tab.
 */
export default function BsatReferenceWidget(props) {
  const { value, onChange, disabled, readonly, options } = props;
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
        endIcon={<OpenInNewIcon sx={{ fontSize: 12 }} />}
        onClick={() => window.open(BSAT_URL, '_blank', 'noopener')}
        sx={{ mt: 0.5, fontSize: 11, textTransform: 'none' }}
      >
        BSAT Reference
      </Button>
    </Box>
  );
}
