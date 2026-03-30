import { AppBar, Toolbar, Box } from '@mui/material';

export default function GlobalHeader() {
  return (
    <AppBar position="static" sx={{ bgcolor: '#1E4489', height: 40, justifyContent: 'center' }}>
      <Toolbar variant="dense" sx={{ minHeight: 40, px: 2 }}>
        <Box component="img" src="/egs-banner.png" alt="EGS Platform" sx={{ height: 40, mr: 2 }} />
      </Toolbar>
    </AppBar>
  );
}
