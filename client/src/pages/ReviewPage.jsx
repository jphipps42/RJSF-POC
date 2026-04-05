import { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import {
  Box, Typography, Button, Chip, CircularProgress,
  Select, MenuItem, FormControl, InputLabel,
} from '@mui/material';
import GlobalHeader from '../components/GlobalHeader';
import TopNav from '../components/TopNav';
import AwardSummary from '../components/AwardSummary';
import OverviewPanel from '../components/OverviewPanel';
import ReviewSection from '../components/ReviewSection';
import HumanReviewSection from '../components/HumanReviewSection';
import AcquisitionSection from '../components/AcquisitionSection';
import FinalRecommendation from '../components/FinalRecommendation';
import NotesPanel from '../components/NotesPanel';
import RightPanel from '../components/RightPanel';
import ResetModal from '../components/ResetModal';
import {
  getAwardByLog, updateAward, getFormConfiguration, getSchemaVersions,
} from '../services/api';

const STANDARD_SECTIONS = ['safety_review', 'animal_review'];
const HUMAN_PREFIX = 'human_';
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

  // Page layout versioning
  const [layoutVersions, setLayoutVersions] = useState([]);
  const [selectedVersion, setSelectedVersion] = useState(null);
  const [layoutConfig, setLayoutConfig] = useState(null);
  const [layoutFormId, setLayoutFormId] = useState(null);

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

  // Fetch layout versions on mount
  useEffect(() => {
    async function loadLayoutVersions() {
      try {
        const cfgRes = await getFormConfiguration('page_layout');
        const formId = cfgRes.data.id;
        setLayoutFormId(formId);
        const versionsRes = await getSchemaVersions(formId);
        const versions = versionsRes.data;
        setLayoutVersions(versions);
        // Default to the current (is_current=true) version
        const current = versions.find((v) => v.is_current) || versions[0];
        if (current) {
          setSelectedVersion(current.version);
          setLayoutConfig(current.default_data);
        }
      } catch (err) {
        console.error('Failed to load layout versions:', err);
      }
    }
    loadLayoutVersions();
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  // When user changes version dropdown
  const handleVersionChange = (newVersion) => {
    setSelectedVersion(newVersion);
    const ver = layoutVersions.find((v) => v.version === newVersion);
    if (ver) {
      setLayoutConfig(ver.default_data);
    }
  };

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

  const standardSubmissions = STANDARD_SECTIONS
    .map((key) => submissions.find((s) => s.form_key === key))
    .filter(Boolean);

  const humanSubmissions = submissions.filter((s) => s.form_key.startsWith(HUMAN_PREFIX));
  const acqSubmissions = submissions.filter((s) => s.form_key.startsWith(ACQ_PREFIX));
  const allResetable = [...standardSubmissions, ...humanSubmissions, ...acqSubmissions];

  // Derive labels from layout config
  const overviewHeader = layoutConfig?.overview_header || 'Overview';
  const reviewHeader = layoutConfig?.review_header || 'Pre-Award / Negotiations Review';

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
        <Button variant="text" sx={{ color: '#2563eb', fontWeight: 600, p: 0, minWidth: 0, '&:hover': { bgcolor: 'transparent', textDecoration: 'underline' } }}>Return to Search</Button>
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
              overviewHeader={overviewHeader}
            />

            <Typography variant="h6" sx={{ fontWeight: 700, fontSize: 14, mb: 1 }}>
              {reviewHeader}
            </Typography>

            {/* Sections A, B */}
            {standardSubmissions.map((sub) => (
              <ReviewSection
                key={sub.id}
                submission={sub}
                onUpdate={handleSubmissionUpdate}
              />
            ))}

            {/* Section C with 6 nested subsections */}
            <HumanReviewSection
              submissions={humanSubmissions}
              onUpdate={handleSubmissionUpdate}
            />

            {/* Section D with nested subsections */}
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

          {/* Footer: Reset + Version Number */}
          <Box sx={{ p: 2, borderTop: '1px solid #ddd', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Button
              variant="contained"
              color="error"
              size="small"
              onClick={() => setResetModalOpen(true)}
              sx={{ fontSize: 12, fontWeight: 700 }}
            >
              Reset Checklist (Admin Only)
            </Button>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
              {selectedVersion && (
                <Typography sx={{ fontSize: 12, color: '#64748b' }}>
                  Form Version: v{selectedVersion}
                </Typography>
              )}
              {layoutVersions.length > 1 && (
                <FormControl size="small" sx={{ minWidth: 140 }}>
                  <Select
                    value={selectedVersion || ''}
                    onChange={(e) => handleVersionChange(e.target.value)}
                    sx={{ fontSize: 12, height: 28 }}
                    displayEmpty
                  >
                    {layoutVersions
                      .sort((a, b) => b.version - a.version)
                      .map((v) => (
                        <MenuItem key={v.version} value={v.version} sx={{ fontSize: 12 }}>
                          v{v.version}{v.is_current ? ' (current)' : ''}
                        </MenuItem>
                      ))}
                  </Select>
                </FormControl>
              )}
            </Box>
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
