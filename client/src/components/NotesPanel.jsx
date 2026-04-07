import { Typography, TextField, Button, Box, Accordion, AccordionSummary, AccordionDetails } from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';

export default function NotesPanel({ title = 'SO/GOR Notes', expanded, onAccordionChange }) {
  return (
    <Accordion expanded={expanded} onChange={onAccordionChange} sx={{ mb: 2, border: '1px solid #d6e4f2', borderRadius: '8px !important', '&:before': { display: 'none' } }}>
      <AccordionSummary expandIcon={<ExpandMoreIcon />}>
        <Typography variant="h6" sx={{ fontWeight: 700 }}>{title}</Typography>
      </AccordionSummary>
      <AccordionDetails sx={{ pt: 0 }}>
        <TextField fullWidth multiline rows={3} placeholder="Enter comment..." size="small" />
        <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1, mt: 1 }}>
          <Button variant="contained" size="small" sx={{ bgcolor: '#60a5fa' }}>Save</Button>
          {title === 'Change Log' && (
            <Button variant="contained" size="small" sx={{ bgcolor: '#60a5fa' }}>Submit</Button>
          )}
        </Box>
      </AccordionDetails>
    </Accordion>
  );
}
