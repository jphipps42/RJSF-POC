import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import ReviewPage from './pages/ReviewPage';
import LoginPage from './pages/LoginPage';

const theme = createTheme({
  typography: {
    fontFamily: '"Ubuntu", Arial, sans-serif',
    fontSize: 12,
    body1: { fontSize: '0.82rem' },
    body2: { fontSize: '0.75rem' },
    h1: { fontSize: '24px', fontWeight: 700 },
    h2: { fontSize: '21px', fontWeight: 700 },
    h3: { fontSize: '16px', fontWeight: 400, textTransform: 'uppercase' },
    h6: { fontSize: '1rem' },
  },
  palette: {
    primary: { main: '#428bca', dark: '#2158c6' },
    background: { default: '#f4f8fc' },
    text: { primary: '#2C3E50', secondary: '#a6a6a8' },
  },
  components: {
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: {
          textTransform: 'none',
          borderRadius: '4px',
          fontFamily: '"Ubuntu", Arial, sans-serif',
        },
        sizeSmall: {
          padding: '3px 8px',
          fontSize: 11,
        },
        sizeMedium: {
          padding: '4px 12px',
          fontSize: 12,
        },
      },
    },
    MuiAccordion: {
      defaultProps: { disableGutters: true },
    },
    MuiAccordionSummary: {
      styleOverrides: {
        root: { minHeight: 36, padding: '0 12px' },
        content: { margin: '4px 0' },
      },
    },
    MuiAccordionDetails: {
      styleOverrides: {
        root: { padding: '8px 12px' },
      },
    },
    MuiTextField: {
      defaultProps: { size: 'small' },
    },
    MuiSelect: {
      defaultProps: { size: 'small' },
    },
    MuiChip: {
      styleOverrides: {
        sizeSmall: { height: 20, fontSize: '0.65rem' },
      },
    },
  },
});

function ProtectedRoute({ children }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  return children;
}

export default function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route
              path="/review/:logNumber"
              element={
                <ProtectedRoute>
                  <ReviewPage />
                </ProtectedRoute>
              }
            />
            <Route path="*" element={<Navigate to="/review/TE020005" />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </ThemeProvider>
  );
}
