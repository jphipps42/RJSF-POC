import { Box, Typography, Paper } from '@mui/material';

function SummaryItem({ label, value }) {
  return (
    <Box sx={{ display: 'flex', mb: 0 }}>
      <Typography sx={{ fontWeight: 'bold', fontSize: 11, width: 150, flexShrink: 0, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{label}:</Typography>
      <Typography sx={{ fontSize: 11, color: '#333' }}>{value || ''}</Typography>
    </Box>
  );
}

export default function AwardSummary({ award }) {
  if (!award) return null;

  return (
    <Paper
      elevation={0}
      sx={{
        display: 'flex',
        py: '16px',
        px: '20px',
        gap: '20px',
        mb: '20px',
        fontSize: 11,
        bgcolor: '#fff',
        borderRadius: '5px',
        boxShadow: '0px 10px 10px rgba(0, 0, 0, 0.2)',
      }}
    >
      <Box sx={{ flex: 1 }}>
        <SummaryItem label="Log Number" value={award.log_number} />
        <SummaryItem label="Award Number" value={award.award_number} />
        <SummaryItem label="Award Mechanism" value={award.award_mechanism} />
        <SummaryItem label="Funding Opportunity" value={award.funding_opportunity} />
      </Box>
      <Box sx={{ flex: 1 }}>
        <SummaryItem label="Principal Investigator" value={award.principal_investigator} />
        <SummaryItem label="Performing Organization" value={award.performing_organization} />
        <SummaryItem label="Contracting Organization" value={award.contracting_organization} />
        <SummaryItem label="Period of Performance" value={award.period_of_performance} />
        <SummaryItem label="Award Amount" value={award.award_amount ? `$${Number(award.award_amount).toLocaleString('en-US', { minimumFractionDigits: 2 })}` : ''} />
      </Box>
      <Box sx={{ flex: 1 }}>
        <SummaryItem label="Program Office" value={award.program_office} />
        <SummaryItem label="Program" value={award.program} />
        <SummaryItem label="Science Officer" value={award.science_officer} />
        <SummaryItem label="GOR/COR" value={award.gor_cor} />
      </Box>
    </Paper>
  );
}
