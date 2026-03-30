import { Box, Typography } from '@mui/material';

const NAV_ITEMS = [
  'Summary', 'Details', 'Tools', 'Research Classifications',
  'PreAward Review', 'Agreement Details', 'Regulatory Details',
  'Deliverables and Progress Reports', 'Closeout', 'Document Management',
];

export default function TopNav() {
  return (
    <Box sx={{ bgcolor: '#1d4ed8', px: 2, py: 1, display: 'flex', gap: 2, overflowX: 'auto' }}>
      {NAV_ITEMS.map((item) => (
        <Typography
          key={item}
          sx={{
            color: item === 'PreAward Review' ? '#fff' : '#e0ecff',
            fontSize: 14,
            whiteSpace: 'nowrap',
            fontWeight: item === 'PreAward Review' ? 600 : 400,
            borderBottom: item === 'PreAward Review' ? '2px solid #fff' : 'none',
            pb: 0.3,
            cursor: 'pointer',
            '&:hover': { color: '#fff' },
          }}
        >
          {item}
        </Typography>
      ))}
    </Box>
  );
}
