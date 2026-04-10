import { useRef, useState, useCallback, useEffect } from 'react';
import { Box, Typography, Divider, Snackbar, Alert } from '@mui/material';
import SectionPanel from './SectionPanel';
import SectionGroup from './SectionGroup';
import LinkedFilesPanel from './LinkedFilesPanel';
import PersonnelDataGrid from './PersonnelDataGrid';
import manifest from '../schemas/section-manifest.json';
import { saveFormDraft, submitForm } from '../services/api';
import { useAuth } from '../contexts/AuthContext';

// Dynamically import all section schemas and uiSchemas
const sectionSchemas = import.meta.glob('../schemas/sections/*.schema.json', { eager: true });
const sectionUiSchemas = import.meta.glob('../schemas/ui/*.ui.json', { eager: true });

function resolveSchema(schemaFile) {
  const key = `../schemas/sections/${schemaFile}`;
  return sectionSchemas[key]?.default || sectionSchemas[key];
}

function resolveUiSchema(uiFile) {
  const key = `../schemas/ui/${uiFile}`;
  return sectionUiSchemas[key]?.default || sectionUiSchemas[key] || {};
}

function sliceFor(schema, formData) {
  const keys = Object.keys(schema?.properties || {});
  const slice = {};
  for (const k of keys) {
    if (k in formData) slice[k] = formData[k];
  }
  return slice;
}

export default function CompositeForm({
  submission,
  award,
  personnel: initialPersonnel,
  linkedFiles,
  onLinkedFilesChange,
  onSubmissionUpdate,
}) {
  // Seed formData with personnel from project_personnel table (records have real UUIDs)
  const { user } = useAuth();
  const userRole = user?.role?.toUpperCase() || '';

  const [formData, setFormData] = useState(() => {
    const data = submission?.form_data || {};
    if (initialPersonnel && initialPersonnel.length > 0) {
      return { ...data, personnel: initialPersonnel };
    }
    return data;
  });
  const [sectionStatus, setSectionStatus] = useState(submission?.section_status || {});
  const [dirtyFlags, setDirtyFlags] = useState({});
  const [saveStatus, setSaveStatus] = useState({});
  // Track expanded state per panel independently — multiple can be open at different levels
  const [expandedPanels, setExpandedPanels] = useState({ overview: true });
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
  const formRefs = useRef({});
  const sectionRefs = useRef({});

  // Sync state when submission is refetched (detected by updated_at change)
  const lastSyncRef = useRef(submission?.updated_at);
  useEffect(() => {
    if (submission?.updated_at && submission.updated_at !== lastSyncRef.current) {
      lastSyncRef.current = submission.updated_at;
      if (submission.section_status) {
        setSectionStatus(submission.section_status);
      }
      if (submission.form_data) {
        const data = { ...submission.form_data };
        if (initialPersonnel && initialPersonnel.length > 0) {
          data.personnel = initialPersonnel;
        }
        setFormData(data);
        formDataRef.current = data;
      }
    }
  }, [submission?.updated_at]);

  // Use a ref to always have the latest formData for async save operations (avoids stale closures)
  const formDataRef = useRef(formData);
  useEffect(() => { formDataRef.current = formData; }, [formData]);

  // Track previous personnel state for auto-save on any mutation (add, edit, delete)
  const prevPersonnelRef = useRef(JSON.stringify(formData?.personnel || []));

  const submissionId = submission?.id;

  // Collect top-level section IDs from manifest for exclusive accordion behavior
  const topLevelIds = useRef(manifest.sections.map((s) => s.id));

  const handleAccordionChange = useCallback((panelId, isTopLevel) => (_event, isExpanded) => {
    setExpandedPanels((prev) => {
      const next = { ...prev };
      if (isTopLevel) {
        // Close all other top-level sections when opening one
        for (const id of topLevelIds.current) {
          next[id] = false;
        }
      }
      next[panelId] = isExpanded;
      return next;
    });
    if (isExpanded) {
      setTimeout(() => {
        sectionRefs.current[panelId]?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }, 300);
    }
  }, []);

  // Save a specific formData snapshot to the backend
  const saveToBackend = useCallback(async (dataToSave, sectionId) => {
    setSaveStatus((prev) => ({ ...prev, [sectionId]: 'saving' }));
    try {
      const res = await saveFormDraft(submissionId, dataToSave, sectionId);
      setSaveStatus((prev) => ({ ...prev, [sectionId]: 'saved' }));
      setDirtyFlags((prev) => ({ ...prev, [sectionId]: false }));
      if (res.data.section_status) {
        setSectionStatus(res.data.section_status);
      }
      setSnackbar({ open: true, message: 'Section saved successfully.', severity: 'success' });
      onSubmissionUpdate?.(res.data);
    } catch (err) {
      setSaveStatus((prev) => ({ ...prev, [sectionId]: 'error' }));
      setSnackbar({ open: true, message: err.response?.data?.error || 'Save failed', severity: 'error' });
    }
  }, [submissionId, onSubmissionUpdate]);

  // "Default all to No" checkbox fields and the radio fields they control
  const DEFAULT_NO_CONFIG = {
    human_no_regulatory: {
      checkboxField: 'no_review_default_no',
      radioFields: ['human_s1_q1', 'human_s1_q2', 'human_s1_q3', 'human_s1_q4', 'human_s1_q5'],
    },
    human_anatomical: {
      checkboxField: 'has_default_no',
      radioFields: ['human_has_q1', 'human_has_q2', 'human_has_q3', 'human_has_q4', 'human_has_q5', 'human_has_q6', 'human_has_q7'],
    },
  };

  const mergeOnChange = useCallback((sectionId, sectionData) => {
    let merged = { ...formDataRef.current, ...sectionData };

    // Handle "default all to No" checkbox toggle
    const config = DEFAULT_NO_CONFIG[sectionId];
    if (config) {
      const wasChecked = formDataRef.current[config.checkboxField];
      const isChecked = merged[config.checkboxField];
      if (isChecked && !wasChecked) {
        for (const field of config.radioFields) {
          merged[field] = 'no';
        }
      }
    }

    setFormData(merged);
    formDataRef.current = merged;
    setDirtyFlags((prev) => ({ ...prev, [sectionId]: true }));
    setSaveStatus((prev) => ({ ...prev, [sectionId]: 'idle' }));

    // Auto-save overview section when personnel array changes (add, edit, or delete)
    if (sectionId === 'overview') {
      const newPersonnelJson = JSON.stringify(merged.personnel || []);
      if (newPersonnelJson !== prevPersonnelRef.current) {
        prevPersonnelRef.current = newPersonnelJson;
        saveToBackend(merged, 'overview');
      }
    }
  }, [saveToBackend]);

  const handleSectionSave = useCallback(async (sectionId) => {
    const ref = formRefs.current[sectionId];
    if (ref && !ref.validateForm()) return;
    await saveToBackend(formDataRef.current, sectionId);
  }, [saveToBackend]);

  // Map section IDs to their submission date field in formData
  const SECTION_DATE_MAP = {
    safety_review: 'safety_submitted_at',
    animal_review: 'animal_submitted_at',
    // Human subsections all stamp the same field
    human_no_regulatory: 'human_submitted_at',
    human_anatomical: 'human_submitted_at',
    human_data_secondary: 'human_submitted_at',
    human_subjects: 'human_submitted_at',
    human_special_topics: 'human_submitted_at',
    human_estimated_start: 'human_submitted_at',
    // Acquisition subsections all stamp the same field
    acq_br_personnel: 'acquisition_submitted_at',
    acq_br_equipment: 'acquisition_submitted_at',
    acq_br_travel: 'acquisition_submitted_at',
    acq_br_materials: 'acquisition_submitted_at',
    acq_br_consultant: 'acquisition_submitted_at',
    acq_br_third_party: 'acquisition_submitted_at',
    acq_br_other_direct: 'acquisition_submitted_at',
    acq_br_additional: 'acquisition_submitted_at',
    acq_peer_review: 'acquisition_submitted_at',
    acq_sow_concerns: 'acquisition_submitted_at',
    acq_cps: 'acquisition_submitted_at',
    acq_ier: 'acquisition_submitted_at',
    acq_data_management: 'acquisition_submitted_at',
    acq_special_requirements: 'acquisition_submitted_at',
  };

  const handleSectionSubmit = useCallback(async (sectionId) => {
    const ref = formRefs.current[sectionId];
    if (ref && !ref.validateForm()) return;

    // Stamp submission date into formData for checklist tracking
    const dateField = SECTION_DATE_MAP[sectionId];
    if (dateField) {
      const now = new Date().toISOString();
      const updated = { ...formDataRef.current, [dateField]: now };
      setFormData(updated);
      formDataRef.current = updated;
    }

    setSaveStatus((prev) => ({ ...prev, [sectionId]: 'saving' }));
    try {
      const res = await submitForm(submissionId, formDataRef.current, sectionId);
      setSaveStatus((prev) => ({ ...prev, [sectionId]: 'saved' }));
      setDirtyFlags((prev) => ({ ...prev, [sectionId]: false }));
      setSectionStatus(res.data.section_status || {});
      setSnackbar({ open: true, message: 'Section submitted and locked.', severity: 'success' });
      onSubmissionUpdate?.(res.data);
    } catch (err) {
      setSaveStatus((prev) => ({ ...prev, [sectionId]: 'error' }));
      setSnackbar({ open: true, message: err.response?.data?.error || 'Submit failed', severity: 'error' });
    }
  }, [submissionId, onSubmissionUpdate]);

  const renderSection = useCallback((section, depth = 0) => {
    const isTopLevel = depth === 0;

    // Group header (no schema — just wraps children)
    if (section.isGroupHeader) {
      return (
        <div key={section.id} ref={(el) => { sectionRefs.current[section.id] = el; }}>
          <SectionGroup
            section={section}
            sectionStatus={sectionStatus}
            expanded={!!expandedPanels[section.id]}
            onAccordionChange={handleAccordionChange(section.id, isTopLevel)}
          >
            {section.children?.map((child) => renderSection(child, depth + 1))}
          </SectionGroup>
        </div>
      );
    }

    // Leaf section with schema
    const schema = resolveSchema(section.schemaFile);
    let uiSchema = resolveUiSchema(section.uiSchemaFile);
    if (!schema) return null;

    // Conditional visibility for Animal Review — hide Q2–Q5 when Q1 is "no"
    if (section.id === 'animal_review' && formData.animal_q1 === 'no') {
      const hiddenField = { 'ui:widget': 'hidden' };
      uiSchema = {
        ...uiSchema,
        animal_q2: hiddenField,
        animal_q3: hiddenField,
        animal_q4: hiddenField,
        iacuc_protocol_number: hiddenField,
        animal_q5: hiddenField,
        animal_start_date: hiddenField,
      };
    }

    // Role-based field access for Final Recommendation
    if (section.id === 'final_recommendation') {
      const isSO = userRole === 'SO';
      const isGOR = userRole === 'GOR' || userRole === 'COR';
      uiSchema = {
        ...uiSchema,
        so_recommendation: { ...uiSchema.so_recommendation, 'ui:disabled': !isSO },
        so_comments: { ...uiSchema.so_comments, 'ui:disabled': !isSO },
        gor_recommendation: { ...uiSchema.gor_recommendation, 'ui:disabled': !isGOR },
        gor_comments: { ...uiSchema.gor_comments, 'ui:disabled': !isGOR },
      };
    }

    const sectionFormData = sliceFor(schema, formData);

    return (
      <div key={section.id} ref={(el) => { sectionRefs.current[section.id] = el; }}>
        <SectionPanel
          ref={(el) => { formRefs.current[section.id] = el; }}
          sectionId={section.id}
          title={section.title}
          schema={schema}
          uiSchema={uiSchema}
          formData={sectionFormData}
          onChange={(data) => mergeOnChange(section.id, data)}
          sectionStatus={sectionStatus[section.id]}
          dirty={!!dirtyFlags[section.id]}
          saveStatus={saveStatus[section.id] || 'idle'}
          onSave={() => handleSectionSave(section.id)}
          onSubmit={() => handleSectionSubmit(section.id)}
          submitLabel={section.submitLabel}
          expanded={!!expandedPanels[section.id]}
          onAccordionChange={handleAccordionChange(section.id, isTopLevel)}
          formContext={
            section.id === 'overview' ? { primeAwardType: formData?.prime_award_type } :
            section.id === 'safety_review' ? {
              programmaticRec: formData.programmatic_rec || '',
              onProgrammaticRecChange: (val) => {
                const updated = { ...formDataRef.current, programmatic_rec: val };
                setFormData(updated);
                formDataRef.current = updated;
                setDirtyFlags((prev) => ({ ...prev, safety_review: true }));
                saveToBackend(updated, 'safety_review');
              },
            } :
            section.id === 'animal_review' ? {
              animalSpecies: formData.animal_species || [],
              onAnimalSpeciesChange: (val) => {
                const updated = { ...formDataRef.current, animal_species: val };
                setFormData(updated);
                formDataRef.current = updated;
                setDirtyFlags((prev) => ({ ...prev, animal_review: true }));
                saveToBackend(updated, 'animal_review');
              },
            } :
            undefined
          }
        >
          {/* Checklist submission dates in overview header */}
          {section.id === 'overview' && (
            <Box sx={{ mb: 2 }}>
              <Divider sx={{ my: 1.5 }} />
              <Typography sx={{ fontWeight: 700, fontSize: 13, mb: 1 }}>Checklist Submission Dates</Typography>
              {[
                { label: 'PI Notification Date', value: formData.pi_notification_date },
                { label: 'Safety Checklist Submitted', value: formData.safety_submitted_at },
                { label: 'Animal Research Checklist Submitted', value: formData.animal_submitted_at },
                { label: 'Human Research Checklist Submitted', value: formData.human_submitted_at },
                { label: 'Acquisition Checklist Submitted to Finance', value: formData.acquisition_submitted_at },
              ].map(({ label, value }) => (
                <Box key={label} sx={{ display: 'flex', mb: 0.5 }}>
                  <Typography sx={{ fontWeight: 700, fontSize: 11, width: 280, flexShrink: 0 }}>{label}:</Typography>
                  <Typography sx={{ fontSize: 11, color: '#333' }}>
                    {value ? new Date(value).toLocaleDateString() : ''}
                  </Typography>
                </Box>
              ))}
              <Divider sx={{ my: 1.5 }} />
            </Box>
          )}
          {/* Render PersonnelDataGrid directly below overview form fields */}
          {section.id === 'overview' && (
            <PersonnelDataGrid
              formData={formData.personnel || []}
              onChange={(updatedPersonnel) => mergeOnChange('overview', { ...sectionFormData, personnel: updatedPersonnel })}
              readonly={sectionStatus[section.id] === 'submitted'}
              disabled={sectionStatus[section.id] === 'submitted'}
              schema={{ title: 'Project Personnel' }}
              formContext={{ primeAwardType: formData?.prime_award_type, awardId: award?.id }}
            />
          )}
          {/* Render LinkedFilesPanel inside the Final Recommendation accordion */}
          {section.id === 'final_recommendation' && (
            <LinkedFilesPanel
              awardId={award?.id}
              linkedFiles={linkedFiles}
              onLinkedFilesChange={onLinkedFilesChange}
            />
          )}
        </SectionPanel>
      </div>
    );
  }, [formData, sectionStatus, dirtyFlags, saveStatus, expandedPanels,
      handleAccordionChange, mergeOnChange, handleSectionSave, handleSectionSubmit, saveToBackend, award, userRole]);

  return (
    <Box>
      {manifest.sections.map((section) => renderSection(section))}

      <Snackbar
        open={snackbar.open}
        autoHideDuration={4000}
        onClose={() => setSnackbar({ ...snackbar, open: false })}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
      >
        <Alert severity={snackbar.severity} onClose={() => setSnackbar({ ...snackbar, open: false })}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
}
