import { Box, Typography, Button, IconButton, Tooltip } from '@mui/material';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';

/**
 * 3-state panel layout:
 *   'both'       — both panels visible (default)
 *   'hide-right' — right panel hidden, left full width
 *   'hide-left'  — left panel hidden, right full width
 */
export default function RightPanel({ panelMode, onPanelModeChange }) {
  const isDefault = panelMode === 'both';
  const isHideRight = panelMode === 'hide-right';
  const isHideLeft = panelMode === 'hide-left';

  // Left arrow: visible in 'both' and 'hide-right'; hidden in 'hide-left'
  const showLeftArrow = !isHideLeft;
  // Right arrow: visible in 'both' and 'hide-left'; hidden in 'hide-right'
  const showRightArrow = !isHideRight;

  // Tooltip text
  const leftArrowTooltip = isDefault ? 'Hide Left Panel' : 'Show Both Panels';
  const rightArrowTooltip = isDefault ? 'Hide Right Panel' : 'Show Both Panels';

  // Click handlers
  const handleLeftArrowClick = () => {
    onPanelModeChange(isDefault ? 'hide-left' : 'both');
  };
  const handleRightArrowClick = () => {
    onPanelModeChange(isDefault ? 'hide-right' : 'both');
  };

  // Button cluster position — anchored to the right panel's left frame border
  // hide-right: small offset from right edge (panel collapsed)
  // hide-left: right panel is full width, so button sits at calc(100% - 10px) from left = 10px from right edge won't work
  //            Use left positioning instead for hide-left state
  const useLeftPosition = isHideLeft;
  const buttonRight = isHideRight ? 10 : 610;

  return (
    <>
      {/* Arrow button cluster — fixed to right panel frame border */}
      <Box
        sx={{
          position: 'fixed',
          top: '50%',
          ...(useLeftPosition
            ? { left: 10 }
            : { right: buttonRight }),
          transform: 'translateY(-50%)',
          display: 'flex',
          flexDirection: 'column',
          gap: 0.5,
          zIndex: 1000,
          transition: 'all 0.3s ease',
        }}
      >
        {showLeftArrow && (
          <Tooltip title={leftArrowTooltip} placement="left" arrow>
            <IconButton
              onClick={handleLeftArrowClick}
              sx={{
                bgcolor: '#428bca',
                color: 'white',
                borderRadius: '6px 0 0 6px',
                '&:hover': { bgcolor: '#2158c6' },
              }}
            >
              <ChevronLeftIcon />
            </IconButton>
          </Tooltip>
        )}
        {showRightArrow && (
          <Tooltip title={rightArrowTooltip} placement="left" arrow>
            <IconButton
              onClick={handleRightArrowClick}
              sx={{
                bgcolor: '#428bca',
                color: 'white',
                borderRadius: '6px 0 0 6px',
                '&:hover': { bgcolor: '#2158c6' },
              }}
            >
              <ChevronRightIcon />
            </IconButton>
          </Tooltip>
        )}
      </Box>

      {/* Right panel content */}
      <Box
        sx={{
          flex: isHideRight ? '0 0 0' : isHideLeft ? 1 : '0 0 600px',
          p: isHideRight ? 0 : 2,
          bgcolor: '#f8fafc',
          overflow: isHideRight ? 'hidden' : 'auto',
          transition: 'all 0.3s ease',
        }}
      >
        {!isHideRight && (
          <>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1.5 }}>
              <Typography variant="h6">Document and File Management</Typography>
              <Box sx={{ display: 'flex', gap: 1 }}>
                <Button variant="contained" size="small" sx={{ bgcolor: '#428bca' }}>Add File</Button>
                <Button variant="outlined" size="small" sx={{ bgcolor: '#c5daea', color: '#2C3E50', borderColor: '#c5daea' }}>Add Note</Button>
              </Box>
            </Box>
            <Typography sx={{ color: '#a6a6a8', fontStyle: 'italic' }}>
              Uploaded files and notes will appear here.
            </Typography>
          </>
        )}
      </Box>
    </>
  );
}
