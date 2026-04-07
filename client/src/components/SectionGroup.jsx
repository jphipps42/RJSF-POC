import {
  Accordion, AccordionSummary, AccordionDetails,
  Box, Typography,
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import StatusBadge from './StatusBadge';

function computeGroupStatus(sectionStatus, childIds) {
  if (!childIds || childIds.length === 0) return 'not_started';
  const statuses = childIds.map((id) => sectionStatus?.[id] || 'not_started');
  const allSubmitted = statuses.every((s) => s === 'submitted');
  const anyActive = statuses.some((s) => s === 'in_progress' || s === 'submitted');
  return allSubmitted ? 'submitted' : anyActive ? 'in_progress' : 'not_started';
}

function collectLeafIds(section) {
  if (section.children) {
    return section.children.flatMap(collectLeafIds);
  }
  return [section.id];
}

export default function SectionGroup({
  section,
  sectionStatus,
  expanded,
  onAccordionChange,
  children,
}) {
  const leafIds = collectLeafIds(section);
  const groupStatus = computeGroupStatus(sectionStatus, leafIds);

  return (
    <Accordion
      expanded={expanded}
      onChange={onAccordionChange}
      sx={{
        border: '1px solid #d6e4f2', borderRadius: '8px !important',
        mb: 2, overflow: 'hidden', '&:before': { display: 'none' },
      }}
    >
      <AccordionSummary
        expandIcon={<ExpandMoreIcon />}
        sx={{
          bgcolor: '#e7f1ff', '&:hover': { bgcolor: '#dbeafe' },
          '&.Mui-expanded': {
            bgcolor: '#bfdbfe', border: '2px solid #2563eb',
            boxShadow: '0 0 0 3px rgba(37,99,235,0.1)',
          },
        }}
      >
        <Box sx={{ display: 'flex', justifyContent: 'space-between', width: '100%', alignItems: 'center', pr: 1 }}>
          <Typography sx={{ fontWeight: 700, color: '#0a2540', fontSize: '0.85rem' }}>
            {section.title}
          </Typography>
          <StatusBadge status={groupStatus} />
        </Box>
      </AccordionSummary>
      <AccordionDetails sx={{ bgcolor: '#fff', p: 2 }}>
        {children}
      </AccordionDetails>
    </Accordion>
  );
}
