import { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { Box, Typography, Button, Chip, CircularProgress } from '@mui/material';
import GlobalHeader from '../components/GlobalHeader';
import TopNav from '../components/TopNav';
import AwardSummary from '../components/AwardSummary';
import OverviewPanel from '../components/OverviewPanel';
import ReviewSection from '../components/ReviewSection';
import AcquisitionSection from '../components/AcquisitionSection';
import FinalRecommendation from '../components/FinalRecommendation';
import NotesPanel from '../components/NotesPanel';
import RightPanel from '../components/RightPanel';
import ResetModal from '../components/ResetModal';
import { getAwardByLog, updateAward } from '../services/api';

// Top-level sections rendered as individual ReviewSection accordions
const STANDARD_SECTIONS = ['safety_review', 'animal_review', 'human_review'];

// Acquisition subsections rendered inside AcquisitionSection
const ACQ_PREFIX = 'acq_';

export default function ReviewPage() {
  const { logNumber } = useParams();
  const [award, setAward] = useState(null);
  const [submissions, setSubmissions] = useState([]);
  const [personnel, setPersonnel] = useState([]);
  const [linkedFiles, setLinkedFiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [rightPanelCollapsed, setRightPanelCollapsed] = useState(false);
  const [resetModalOpen, setResetModalOpen] = useState(false);

  const fetchData = useCallback(async () => {
    try {
      const res = await getAwardByLog(logNumber);
      setAward(res.data);
      setSubmissions(res.data.submissions || []);
      setPersonnel(res.data.personnel || []);
      setLinkedFiles(res.data.linked_files || []);
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to load award data');
    }
    setLoading(false);
  }, [logNumber]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleSubmissionUpdate = (updatedSubmission) => {
    setSubmissions((prev) =>
      prev.map((s) => (s.id === updatedSubmission.id ? { ...s, ...updatedSubmission } : s))
    );
  };

  const handlePrimeAwardChange = async (value) => {
    try {
      await updateAward(award.id, { prime_award_type: value });
      setAward((prev) => ({ ...prev, prime_award_type: value }));
    } catch (err) {
      console.error('Failed to update prime award type:', err);
    }
  };

  const handleReset = () => {
    fetchData();
  };

  // Standard sections (A, B, C)
  const standardSubmissions = STANDARD_SECTIONS
    .map((key) => submissions.find((s) => s.form_key === key))
    .filter(Boolean);

  // Acquisition subsections (all acq_* keys)
  const acqSubmissions = submissions.filter((s) => s.form_key.startsWith(ACQ_PREFIX));

  // All submissions for the reset modal
  const allResetable = [...standardSubmissions, ...acqSubmissions];

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Box sx={{ p: 4, textAlign: 'center' }}>
        <Typography color="error" variant="h6">{error}</Typography>
      </Box>
    );
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100vh', bgcolor: '#f4f8fc' }}>
      <GlobalHeader />
      <TopNav />

      {/* Context Bar */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', px: 2, py: 1, bgcolor: '#fff', borderBottom: '1px solid #e2e8f0' }}>
        <Box>
          <Typography sx={{ fontSize: '0.85em', color: '#475569' }}>
            Award Management &rarr; Negotiation
          </Typography>
        </Box>
        <Button sx={{ color: '#2563eb', fontWeight: 600 }}>Return to Search</Button>
      </Box>

      {/* Status Bar */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', px: 2, py: 0.5, bgcolor: '#f9f9f9' }}>
        <Chip
          label="Under Negotiation/Pre-Award Review"
          sx={{ bgcolor: '#FFD700', fontWeight: 700, fontSize: 12 }}
        />
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Typography sx={{ fontWeight: 700, fontSize: 14 }}>Quick Search</Typography>
          <input
            type="text"
            placeholder="Enter Log Number or Award"
            style={{ padding: 5, border: '1px solid #ccc', borderRadius: 3, fontSize: 13 }}
          />
        </Box>
      </Box>

      <AwardSummary award={award} />

      {/* Main Content */}
      <Box sx={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
        {/* Left Panel */}
        <Box
          sx={{
            flex: 1, minWidth: 0, bgcolor: '#f8fafc', borderRight: '1px solid #e2e8f0',
            display: 'flex', flexDirection: 'column', overflow: 'hidden',
          }}
        >
          <Box sx={{ flex: 1, overflowY: 'auto', p: 2 }}>
            <OverviewPanel
              award={award}
              personnel={personnel}
              submissions={submissions}
              onPrimeAwardChange={handlePrimeAwardChange}
              onPersonnelChange={setPersonnel}
            />

            <Typography variant="h6" sx={{ fontWeight: 700, mb: 2 }}>
              Pre-Award / Negotiations Review
            </Typography>

            {/* Sections A, B, C */}
            {standardSubmissions.map((sub) => (
              <ReviewSection
                key={sub.id}
                submission={sub}
                onUpdate={handleSubmissionUpdate}
              />
            ))}

            {/* Section D with 7 nested subsections */}
            <AcquisitionSection
              submissions={acqSubmissions}
              onUpdate={handleSubmissionUpdate}
            />

            <FinalRecommendation
              awardId={award.id}
              linkedFiles={linkedFiles}
              onLinkedFilesChange={setLinkedFiles}
            />
            <NotesPanel title="SO/GOR Notes" />
            <NotesPanel title="Change Log" />
          </Box>

          {/* Reset Button */}
          <Box sx={{ p: 2, borderTop: '1px solid #ddd' }}>
            <Button
              variant="contained"
              color="error"
              size="small"
              onClick={() => setResetModalOpen(true)}
              sx={{ fontSize: 12, fontWeight: 700 }}
            >
              Reset Checklist (Admin Only)
            </Button>
          </Box>
        </Box>

        {/* Right Panel */}
        <RightPanel
          collapsed={rightPanelCollapsed}
          onToggle={() => setRightPanelCollapsed(!rightPanelCollapsed)}
        />
      </Box>

      <ResetModal
        open={resetModalOpen}
        onClose={() => setResetModalOpen(false)}
        submissions={allResetable}
        onReset={handleReset}
      />
    </Box>
  );
}
