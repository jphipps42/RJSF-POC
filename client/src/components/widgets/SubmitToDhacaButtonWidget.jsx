import { Box, Button } from '@mui/material';

/**
 * Custom RJSF widget that renders a "Submit Recommendation to DHACA R&D"
 * action button inline within the form, placed after GOR/COR Comments.
 */
export default function SubmitToDhacaButtonWidget(props) {
  const { registry, disabled, readonly } = props;
  const formContext = registry?.formContext;

  return (
    <Box sx={{ display: 'flex', justifyContent: 'flex-end', my: 1 }}>
      <Button
        variant="contained"
        size="small"
        disabled={disabled || readonly}
        onClick={() => formContext?.onSubmitToDhaca?.()}
        sx={{ bgcolor: '#2C3E50', '&:hover': { bgcolor: '#1a252f' }, fontWeight: 700, fontSize: 12 }}
      >
        Submit Recommendation to DHACA R&amp;D
      </Button>
    </Box>
  );
}
