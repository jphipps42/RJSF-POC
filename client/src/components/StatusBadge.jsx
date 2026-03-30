import { Chip } from '@mui/material';

const STATUS_CONFIG = {
  not_started: { label: 'Not Started', bgcolor: '#719fcc', color: '#edeff1' },
  in_progress: { label: 'In Progress', bgcolor: '#e29032', color: '#240f03' },
  completed: { label: 'Completed', bgcolor: '#0e1d13', color: '#f5f5f5' },
  submitted: { label: 'Submitted', bgcolor: '#088835', color: '#ecf1f0' },
};

export default function StatusBadge({ status }) {
  const config = STATUS_CONFIG[status] || STATUS_CONFIG.not_started;
  return (
    <Chip
      label={config.label}
      size="small"
      sx={{
        bgcolor: config.bgcolor,
        color: config.color,
        fontWeight: 700,
        fontSize: '0.75rem',
        px: 3,
        borderRadius: 999,
      }}
    />
  );
}
