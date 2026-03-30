import { Box, Typography, Button, IconButton } from '@mui/material';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';

export default function RightPanel({ collapsed, onToggle }) {
  return (
    <>
      <IconButton
        onClick={onToggle}
        sx={{
          position: 'fixed',
          top: '50%',
          right: collapsed ? 10 : 610,
          transform: 'translateY(-50%)',
          bgcolor: '#2563eb',
          color: 'white',
          borderRadius: '6px 0 0 6px',
          zIndex: 1000,
          transition: 'right 0.3s ease',
          '&:hover': { bgcolor: '#1d4ed8' },
        }}
      >
        {collapsed ? <ChevronLeftIcon /> : <ChevronRightIcon />}
      </IconButton>

      <Box
        sx={{
          flex: collapsed ? '0 0 0' : '0 0 600px',
          p: collapsed ? 0 : 2,
          bgcolor: '#f8fafc',
          overflow: collapsed ? 'hidden' : 'auto',
          transition: 'all 0.3s ease',
        }}
      >
        {!collapsed && (
          <>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1.5 }}>
              <Typography variant="h6">Document and File Management</Typography>
              <Box sx={{ display: 'flex', gap: 1 }}>
                <Button variant="contained" size="small" sx={{ bgcolor: '#2563eb' }}>Add File</Button>
                <Button variant="outlined" size="small" sx={{ bgcolor: '#bfdbfe', color: '#0a2540', borderColor: '#bfdbfe' }}>Add Note</Button>
              </Box>
            </Box>
            <Typography sx={{ color: '#64748b', fontStyle: 'italic' }}>
              Uploaded files and notes will appear here.
            </Typography>
          </>
        )}
      </Box>
    </>
  );
}
