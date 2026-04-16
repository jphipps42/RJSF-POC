import { Box, Button } from '@mui/material';

/**
 * Custom RJSF widget that renders a "Send to GOR/COR" action button
 * inline within the form, placed between SO Comments and GOR Recommendation.
 */
export default function SendToGorButtonWidget(props) {
  const { registry, disabled, readonly } = props;
  const formContext = registry?.formContext;

  return (
    <Box sx={{ display: 'flex', justifyContent: 'flex-end', my: 1 }}>
      <Button
        variant="contained"
        size="small"
        disabled={disabled || readonly}
        onClick={() => formContext?.onSendToGor?.()}
        sx={{ bgcolor: '#428bca', '&:hover': { bgcolor: '#2158c6' }, fontWeight: 700, fontSize: 12 }}
      >
        Send to GOR/COR
      </Button>
    </Box>
  );
}
