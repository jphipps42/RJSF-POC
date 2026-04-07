import { useRef, useState, useCallback, useEffect } from 'react';
import { Box, Snackbar, Alert } from '@mui/material';
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
  const [expandedSection, setExpandedSection] = useState('overview');
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'success' });
  const formRefs = useRef({});
  const sectionRefs = useRef({});

  // Use a ref to always have the latest formData for async save operations (avoids stale closures)
  const formDataRef = useRef(formData);
  useEffect(() => { formDataRef.current = formData; }, [formData]);

  // Track previous personnel state for auto-save on any mutation (add, edit, delete)
  const prevPersonnelRef = useRef(JSON.stringify(formData?.personnel || []));

  const submissionId = submission?.id;

  // Save a specific formData snapshot to the backend
  const saveToBackend = useCallback(async (dataToSave, sectionId) => {
    setSaveStatus((prev) => ({ ...prev, [sectionId]: 'saving' }));
    try {
      const res = await saveFormDraft(submissionId, dataToSave, sectionId);
      setSaveStatus((prev) => ({ ...prev, [sectionId]: 'saved' }));
      setDirtyFlags((prev) => ({ ...prev, [sectionId]: false }));
      setSectionStatus(res.data.section_status || sectionStatus);
      setSnackbar({ open: true, message: 'Section saved successfully.', severity: 'success' });
      onSubmissionUpdate?.(res.data);
    } catch (err) {
      setSaveStatus((prev) => ({ ...prev, [sectionId]: 'error' }));
      setSnackbar({ open: true, message: err.response?.data?.error || 'Save failed', severity: 'error' });
    }
  }, [submissionId, sectionStatus, onSubmissionUpdate]);

  const handleAccordionChange = useCallback((panelId) => (_event, isExpanded) => {
    setExpandedSection(isExpanded ? panelId : false);
    if (isExpanded) {
      setTimeout(() => {
        sectionRefs.current[panelId]?.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }, 300);
    }
  }, []);

  const mergeOnChange = useCallback((sectionId, sectionData) => {
    const newFormData = { ...formDataRef.current, ...sectionData };
    setFormData(newFormData);
    formDataRef.current = newFormData;
    setDirtyFlags((prev) => ({ ...prev, [sectionId]: true }));
    setSaveStatus((prev) => ({ ...prev, [sectionId]: 'idle' }));

    // Auto-save overview section when personnel array changes (add, edit, or delete)
    if (sectionId === 'overview') {
      const newPersonnelJson = JSON.stringify(newFormData.personnel || []);
      if (newPersonnelJson !== prevPersonnelRef.current) {
        prevPersonnelRef.current = newPersonnelJson;
        saveToBackend(newFormData, 'overview');
      }
    }
  }, [saveToBackend]);

  const handleSectionSave = useCallback(async (sectionId) => {
    const ref = formRefs.current[sectionId];
    if (ref && !ref.validateForm()) return;
    await saveToBackend(formDataRef.current, sectionId);
  }, [saveToBackend]);

  const handleSectionSubmit = useCallback(async (sectionId) => {
    const ref = formRefs.current[sectionId];
    if (ref && !ref.validateForm()) return;

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
    // Group header (no schema — just wraps children)
    if (section.isGroupHeader) {
      return (
        <div key={section.id} ref={(el) => { sectionRefs.current[section.id] = el; }}>
          <SectionGroup
            section={section}
            sectionStatus={sectionStatus}
            expanded={expandedSection === section.id}
            onAccordionChange={handleAccordionChange(section.id)}
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
          expanded={expandedSection === section.id}
          onAccordionChange={handleAccordionChange(section.id)}
          formContext={section.id === 'overview' ? { primeAwardType: formData?.prime_award_type } : undefined}
        >
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
  }, [formData, sectionStatus, dirtyFlags, saveStatus, expandedSection,
      handleAccordionChange, mergeOnChange, handleSectionSave, handleSectionSubmit, award, userRole]);

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
