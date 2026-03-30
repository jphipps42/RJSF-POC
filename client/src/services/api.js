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

// Form Submissions
export const getFormSubmissions = (awardId) => api.get(`/form-submissions?award_id=${awardId}`);
export const getFormSubmission = (id) => api.get(`/form-submissions/${id}`);
export const getSubmissionByAwardAndKey = (awardId, formKey) =>
  api.get(`/form-submissions/by-award/${awardId}/${formKey}`);
export const saveFormDraft = (id, formData) => api.put(`/form-submissions/${id}/save`, { form_data: formData });
export const submitForm = (id, formData) => api.put(`/form-submissions/${id}/submit`, { form_data: formData });
export const resetFormSubmission = (id) => api.put(`/form-submissions/${id}/reset`);

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

export default api;
