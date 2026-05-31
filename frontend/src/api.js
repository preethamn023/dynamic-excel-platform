import axios from 'axios';

// Set the base URL directly to the live backend
const BASE_URL = 'https://dynamic-excel-platform-production.up.railway.app/api';

const api = axios.create({
    baseURL: BASE_URL,
});



export const uploadWorkbook = (file) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post('/workbooks/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
    });
};

export const getWorkbooks = () => api.get('/workbooks');
export const getWorkbook = (id) => api.get(`/workbooks/${id}`);
export const getSheets = (workbookId) => api.get(`/workbooks/${workbookId}/sheets`);
export const getCells = (sheetId) => api.get(`/cells/sheet/${sheetId}`);

export const updateCell = (workbookId, sheetId, cellRef, newValue, isFormula) => {
    return api.put(`/cells/update`, { cellRef, newValue, isFormula }, {
        params: { workbookId, sheetId }
    });
};

export const getAuditHistory = (workbookId) => api.get(`/audit/history/${workbookId}`);
export const downloadWorkbookUrl = (workbookId) => `${BASE_URL}/workbooks/download/${workbookId}`;

export default api;
