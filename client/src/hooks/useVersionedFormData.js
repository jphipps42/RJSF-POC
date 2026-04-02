import { useState, useEffect } from 'react';
import { getSubmissionForEdit } from '../services/api';

/**
 * Hook that resolves the correct schema and form data for a submission,
 * handling version migration when needed.
 *
 * @param {Object} submission - The submission object from the API
 * @param {'edit'|'audit'} mode - 'edit' migrates forward; 'audit' uses pinned schema
 * @returns {{ schema, uiSchema, formData, loading, migrated, schemaVersion, currentVersion }}
 */
export default function useVersionedFormData(submission, mode = 'edit') {
  const [state, setState] = useState({
    schema: submission.json_schema,
    uiSchema: submission.ui_schema,
    formData: submission.form_data || {},
    loading: false,
    migrated: false,
    schemaVersion: submission.schema_version,
    currentVersion: submission.current_version,
  });

  const needsMigration = mode === 'edit'
    && submission.schema_version != null
    && submission.current_version != null
    && submission.schema_version < submission.current_version;

  useEffect(() => {
    if (!needsMigration || mode === 'audit') {
      // Use data directly from submission (already has correct schema for the mode)
      setState({
        schema: submission.json_schema,
        uiSchema: submission.ui_schema,
        formData: submission.form_data || {},
        loading: false,
        migrated: false,
        schemaVersion: submission.schema_version,
        currentVersion: submission.current_version,
      });
      return;
    }

    // Fetch migrated data from server
    let cancelled = false;
    setState((prev) => ({ ...prev, loading: true }));

    getSubmissionForEdit(submission.id)
      .then((res) => {
        if (cancelled) return;
        setState({
          schema: res.data.json_schema,
          uiSchema: res.data.ui_schema,
          formData: res.data.formData,
          loading: false,
          migrated: res.data.migrated,
          schemaVersion: res.data.schemaVersion,
          currentVersion: submission.current_version,
        });
      })
      .catch(() => {
        if (cancelled) return;
        // Fallback to submission data on error
        setState({
          schema: submission.json_schema,
          uiSchema: submission.ui_schema,
          formData: submission.form_data || {},
          loading: false,
          migrated: false,
          schemaVersion: submission.schema_version,
          currentVersion: submission.current_version,
        });
      });

    return () => { cancelled = true; };
  }, [submission.id, submission.schema_version, submission.current_version, needsMigration, mode]);

  return state;
}
