import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import ReviewPage from './pages/ReviewPage';

const theme = createTheme({
  typography: {
    fontFamily: '"Segoe UI", Arial, sans-serif',
    fontSize: 12,
    body1: { fontSize: '0.82rem' },
    body2: { fontSize: '0.75rem' },
    h6: { fontSize: '1rem' },
  },
  palette: {
    background: { default: '#f4f8fc' },
  },
  components: {
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: {
          textTransform: 'none',
          borderRadius: '4px',
          fontFamily: '"Segoe UI", Arial, sans-serif',
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

export default function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <BrowserRouter>
        <Routes>
          <Route path="/review/:logNumber" element={<ReviewPage />} />
          <Route path="*" element={<Navigate to="/review/TE020005" />} />
        </Routes>
      </BrowserRouter>
    </ThemeProvider>
  );
}
