import axios from 'axios';

const API_BASE = 'http://localhost:3001/api';

const api = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
});

// Awards
export const getAwards = () => api.get('/awards');
export const getAward = (id) => api.get(`/awards/${id}`);
export const getAwardByLog = (logNumber) => api.get(`/awards/by-log/${logNumber}`);
export const createAward = (data) => api.post('/awards', data);
export const updateAward = (id, data) => api.put(`/awards/${id}`, data);

// Form Configurations
export const getFormConfigurations = () => api.get('/form-configurations');
export const getFormConfiguration = (formKey) => api.get(`/form-configurations/${formKey}`);

// Form Submissions (section-aware)
export const getFormSubmissions = (awardId) => api.get(`/form-submissions?award_id=${awardId}`);
export const getFormSubmission = (id) => api.get(`/form-submissions/${id}`);
export const getSubmissionByAwardAndKey = (awardId, formKey) =>
  api.get(`/form-submissions/by-award/${awardId}/${formKey}`);
export const saveFormDraft = (id, formData, sectionId) => {
  const url = sectionId
    ? `/form-submissions/${id}/save?section=${sectionId}`
    : `/form-submissions/${id}/save`;
  return api.put(url, { form_data: formData });
};
export const submitForm = (id, formData, sectionId) => {
  const url = sectionId
    ? `/form-submissions/${id}/submit?section=${sectionId}`
    : `/form-submissions/${id}/submit`;
  return api.put(url, { form_data: formData });
};
export const resetFormSubmission = (id, sectionId) => {
  const url = sectionId
    ? `/form-submissions/${id}/reset?section=${sectionId}`
    : `/form-submissions/${id}/reset`;
  return api.put(url);
};

// Personnel
export const getPersonnel = (awardId) => api.get(`/personnel?award_id=${awardId}`);
export const addPersonnel = (data) => api.post('/personnel', data);
export const updatePersonnel = (id, data) => api.put(`/personnel/${id}`, data);
export const deletePersonnel = (id) => api.delete(`/personnel/${id}`);

// Linked Files
export const getLinkedFiles = (awardId, section) => {
  let url = `/linked-files?award_id=${awardId}`;
  if (section) url += `&section=${section}`;
  return api.get(url);
};
export const addLinkedFile = (data) => api.post('/linked-files', data);
export const updateLinkedFile = (id, data) => api.put(`/linked-files/${id}`, data);
export const deleteLinkedFile = (id) => api.delete(`/linked-files/${id}`);

// Schema Versions
export const getSchemaVersions = (formId) => api.get(`/schema-versions/${formId}`);
export const getCurrentSchemaVersion = (formId) => api.get(`/schema-versions/${formId}/current`);
export const publishSchemaVersion = (formId, data) => api.post(`/schema-versions/${formId}/publish`, data);

// Page layout versions
export const getPageLayoutVersions = () =>
  api.get('/form-configurations/page_layout').then((cfg) =>
    api.get(`/schema-versions/${cfg.data.id}`)
  );
export const getPageLayoutVersion = (formId, version) =>
  api.get(`/schema-versions/${formId}/${version}`);

// Version-aware submission loading
export const getSubmissionForEdit = (id) => api.get(`/form-submissions/${id}/for-edit`);
export const getSubmissionAudit = (id) => api.get(`/form-submissions/${id}/audit`);

// Auth
export const login = (username, password) => api.post('/auth/login', { username, password });
export const getMe = (userId) => api.get('/auth/me', { headers: { 'X-User-Id': userId } });

export default api;
