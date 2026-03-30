import { Paper, Typography, TextField, Button, Box } from '@mui/material';

export default function NotesPanel({ title = 'SO/GOR Notes' }) {
  return (
    <Paper sx={{ p: 2, mb: 2, border: '1px solid #d6e4f2', borderRadius: 2 }}>
      <Typography variant="h6" sx={{ fontWeight: 700, mb: 1 }}>{title}</Typography>
      <TextField fullWidth multiline rows={3} placeholder="Enter comment..." size="small" />
      <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1, mt: 1 }}>
        <Button variant="contained" size="small" sx={{ bgcolor: '#60a5fa' }}>Save</Button>
        {title === 'Change Log' && (
          <Button variant="contained" size="small" sx={{ bgcolor: '#60a5fa' }}>Submit</Button>
        )}
      </Box>
    </Paper>
  );
}
