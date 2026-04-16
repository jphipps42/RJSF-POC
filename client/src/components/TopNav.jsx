import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Box, Typography, Select, MenuItem } from '@mui/material';
import { getAwards } from '../services/api';

const NAV_ITEMS = [
  'Summary', 'Details', 'Tools', 'Research Classifications',
  'PreAward Review', 'Agreement Details', 'Regulatory Details',
  'Deliverables and Progress Reports', 'Closeout', 'Document Management',
];

export default function TopNav() {
  const navigate = useNavigate();
  const { logNumber } = useParams();
  const [awards, setAwards] = useState([]);

  useEffect(() => {
    getAwards()
      .then((res) => setAwards(res.data || []))
      .catch(() => {});
  }, []);

  const handleAwardChange = (e) => {
    const selectedLog = e.target.value;
    if (selectedLog && selectedLog !== logNumber) {
      navigate(`/review/${selectedLog}`);
    }
  };

  return (
    <Box sx={{ bgcolor: '#428bca', px: 2, py: 1, display: 'flex', alignItems: 'center', gap: 2, overflowX: 'auto' }}>
      {NAV_ITEMS.map((item) => (
        <Typography
          key={item}
          sx={{
            color: item === 'PreAward Review' ? '#fff' : 'rgba(255,255,255,0.75)',
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

      {/* Award Switcher */}
      {awards.length > 0 && (
        <Select
          value={logNumber || ''}
          onChange={handleAwardChange}
          size="small"
          displayEmpty
          sx={{
            ml: 'auto',
            minWidth: 240,
            height: 28,
            fontSize: 12,
            color: '#fff',
            bgcolor: 'rgba(255,255,255,0.15)',
            '& .MuiSelect-icon': { color: '#fff' },
            '& .MuiOutlinedInput-notchedOutline': { borderColor: 'rgba(255,255,255,0.3)' },
            '&:hover .MuiOutlinedInput-notchedOutline': { borderColor: 'rgba(255,255,255,0.6)' },
            '&.Mui-focused .MuiOutlinedInput-notchedOutline': { borderColor: '#fff' },
          }}
          renderValue={(val) => {
            if (!val) return 'Select Award...';
            const a = awards.find((aw) => aw.log_number === val);
            return a ? `${a.log_number} — ${a.principal_investigator}` : val;
          }}
        >
          {awards.map((a) => (
            <MenuItem key={a.log_number} value={a.log_number} sx={{ fontSize: 12 }}>
              <Box>
                <Typography sx={{ fontSize: 12, fontWeight: 600 }}>
                  {a.log_number} — {a.principal_investigator}
                </Typography>
                <Typography sx={{ fontSize: 10, color: '#666' }}>
                  {a.program || 'No Program'} | ${(a.award_amount || 0).toLocaleString()}
                </Typography>
              </Box>
            </MenuItem>
          ))}
        </Select>
      )}
    </Box>
  );
}
