import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box, Paper, Typography, TextField, Button, Alert,
} from '@mui/material';
import { useAuth } from '../contexts/AuthContext';
import { login } from '../services/api';

export default function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { loginUser } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await login(username, password);
      loginUser(res.data);
      navigate('/review/TE020005');
    } catch (err) {
      setError(err.response?.data?.error || 'Login failed. Please try again.');
    }
    setLoading(false);
  };

  return (
    <Box sx={{
      minHeight: '100vh', display: 'flex', flexDirection: 'column',
      alignItems: 'center', justifyContent: 'center', bgcolor: '#f4f8fc',
    }}>
      {/* Header bar */}
      <Box sx={{
        position: 'absolute', top: 0, left: 0, right: 0, height: 40,
        bgcolor: '#2C3E50', display: 'flex', alignItems: 'center', px: 2,
      }}>
        <Box component="img" src="/egs-banner.png" alt="EGS Platform" sx={{ height: 40 }} />
      </Box>

      <Paper elevation={3} sx={{ p: 4, width: 380, mt: 4, borderRadius: 2 }}>
        <Typography variant="h5" sx={{ fontWeight: 700, mb: 0.5, color: '#2C3E50', textAlign: 'center' }}>
          Award Management System
        </Typography>
        <Typography sx={{ mb: 3, color: '#a6a6a8', textAlign: 'center', fontSize: 13 }}>
          Sign in to continue
        </Typography>

        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

        <form onSubmit={handleSubmit}>
          <TextField
            label="Username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            fullWidth
            required
            autoFocus
            sx={{ mb: 2 }}
          />
          <TextField
            label="Password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            fullWidth
            required
            sx={{ mb: 3 }}
          />
          <Button
            type="submit"
            variant="contained"
            fullWidth
            disabled={loading}
            sx={{
              bgcolor: '#428bca', fontWeight: 700, py: 1.2,
              '&:hover': { bgcolor: '#2158c6' },
            }}
          >
            {loading ? 'Signing in...' : 'Sign In'}
          </Button>
        </form>
      </Paper>
    </Box>
  );
}
