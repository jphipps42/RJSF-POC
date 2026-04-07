import { AppBar, Toolbar, Box, Typography, Button } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

export default function GlobalHeader() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <AppBar position="static" sx={{ bgcolor: '#1E4489', height: 40, justifyContent: 'center' }}>
      <Toolbar variant="dense" sx={{ minHeight: 40, px: 2, justifyContent: 'space-between' }}>
        <Box component="img" src="/egs-banner.png" alt="EGS Platform" sx={{ height: 40, mr: 2 }} />
        {user && (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
            <Typography sx={{ color: '#fff', fontSize: 12 }}>
              {user.display_name} &mdash; {user.role}, {user.organization}
            </Typography>
            <Button
              size="small"
              onClick={handleLogout}
              sx={{ color: '#93c5fd', fontSize: 11, minWidth: 0, '&:hover': { color: '#fff' } }}
            >
              Logout
            </Button>
          </Box>
        )}
      </Toolbar>
    </AppBar>
  );
}
