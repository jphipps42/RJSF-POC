import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import ReviewPage from './pages/ReviewPage';

const theme = createTheme({
  typography: {
    fontFamily: '"Segoe UI", Arial, sans-serif',
  },
  palette: {
    background: { default: '#f4f8fc' },
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
