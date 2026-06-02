import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    Box, Button, Typography, Paper, Table, TableBody, TableCell,
    TableContainer, TableHead, TableRow, Chip, CircularProgress,
    Dialog, DialogTitle, DialogContent, DialogActions, TextField, IconButton
} from '@mui/material';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import InsertDriveFileIcon from '@mui/icons-material/InsertDriveFile';
import DeleteIcon from '@mui/icons-material/Delete';
import AddIcon from '@mui/icons-material/Add';
import { uploadWorkbook, getWorkbooks, createWorkbook, deleteWorkbook } from '../api';

const Dashboard = () => {
    const [workbooks, setWorkbooks] = useState([]);
    const [loading, setLoading] = useState(false);
    const [createDialogOpen, setCreateDialogOpen] = useState(false);
    const [newWorkbookName, setNewWorkbookName] = useState('');
    const [deleting, setDeleting] = useState(null);
    const navigate = useNavigate();

    useEffect(() => {
        fetchWorkbooks();
    }, []);

    const fetchWorkbooks = async () => {
        try {
            const res = await getWorkbooks();
            setWorkbooks(res.data);
        } catch (e) {
            console.error(e);
        }
    };

    const handleUpload = async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        setLoading(true);
        try {
            await uploadWorkbook(file);
            fetchWorkbooks();
        } catch (e) {
            alert('Upload failed');
        } finally {
            setLoading(false);
        }
        e.target.value = null;
    };

    const handleCreateWorkbook = async () => {
        if (!newWorkbookName.trim()) return;
        setLoading(true);
        try {
            await createWorkbook(newWorkbookName.trim());
            await fetchWorkbooks();
        } catch (e) {
            alert('Failed to create workbook');
        } finally {
            setLoading(false);
            setCreateDialogOpen(false);
            setNewWorkbookName('');
        }
    };

    const handleDeleteWorkbook = async (wb) => {
        if (!window.confirm('Are you sure you want to delete this workbook?')) return;
        setDeleting(wb.id);
        try {
            await deleteWorkbook(wb.id);
            await fetchWorkbooks();
        } catch (e) {
            alert('Failed to delete workbook');
        } finally {
            setDeleting(null);
        }
    };

    return (
        <Box sx={{ animation: 'fadeIn 0.5s ease' }}>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={4}>
                <Typography variant="h4" className="app-header-title">Your Workbooks</Typography>
                <Box display="flex" gap={2}>
                    <Button
                        className="glass-panel"
                        variant="outlined"
                        startIcon={<AddIcon />}
                        onClick={() => setCreateDialogOpen(true)}
                        sx={{ borderRadius: '12px', border: '1px solid #1e3c72', color: '#1e3c72', fontWeight: 600, textTransform: 'none' }}
                    >
                        Create Blank
                    </Button>
                    <Button
                        component="label"
                        className="gradient-btn"
                        startIcon={loading ? <CircularProgress size={20} color="inherit" /> : <CloudUploadIcon />}
                        disabled={loading}
                        sx={{ borderRadius: '12px', px: 3, fontWeight: 600, textTransform: 'none' }}
                    >
                        Upload Excel
                        <input type="file" hidden accept=".xlsx,.xls" onChange={handleUpload} />
                    </Button>
                </Box>
            </Box>

            <TableContainer component={Paper} className="glass-panel" sx={{ borderRadius: '16px', overflow: 'hidden' }}>
                <Table>
                    <TableHead sx={{ backgroundColor: 'rgba(255,255,255,0.5)' }}>
                        <TableRow>
                            <TableCell sx={{ fontWeight: 600, color: '#2a5298' }}>File Name</TableCell>
                            <TableCell sx={{ fontWeight: 600, color: '#2a5298' }}>Upload Date</TableCell>
                            <TableCell sx={{ fontWeight: 600, color: '#2a5298' }}>Version</TableCell>
                            <TableCell sx={{ fontWeight: 600, color: '#2a5298' }}>Status</TableCell>
                            <TableCell align="right" sx={{ fontWeight: 600, color: '#2a5298' }}>Actions</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {workbooks.map((wb) => (
                            <TableRow key={wb.id} className="hover-row">
                                <TableCell>
                                    <Box display="flex" alignItems="center" gap={1.5} sx={{ fontWeight: 500 }}>
                                        <Box sx={{ p: 1, borderRadius: '8px', background: 'linear-gradient(135deg, #4caf50 0%, #2e7d32 100%)', display: 'flex' }}>
                                            <InsertDriveFileIcon sx={{ color: 'white', fontSize: 20 }} />
                                        </Box>
                                        {wb.fileName}
                                    </Box>
                                </TableCell>
                                <TableCell sx={{ color: '#555' }}>{new Date(wb.uploadDate).toLocaleString()}</TableCell>
                                <TableCell sx={{ color: '#555' }}>v{wb.currentVersion}</TableCell>
                                <TableCell>
                                    <Chip label={wb.status} size="small" sx={{ backgroundColor: 'rgba(76, 175, 80, 0.1)', color: '#2e7d32', fontWeight: 600, border: '1px solid rgba(76, 175, 80, 0.3)' }} />
                                </TableCell>
                                <TableCell align="right">
                                    <Box display="flex" justifyContent="flex-end" alignItems="center" gap={1}>
                                        <Button 
                                            variant="contained" 
                                            size="small"
                                            className="gradient-btn"
                                            sx={{ borderRadius: '8px', textTransform: 'none', px: 2 }}
                                            onClick={() => navigate(`/workbook/${wb.id}`)}
                                        >
                                            Open
                                        </Button>
                                        <IconButton
                                            onClick={() => handleDeleteWorkbook(wb)}
                                            disabled={deleting === wb.id}
                                            size="small"
                                            sx={{ color: '#d32f2f', '&:hover': { backgroundColor: 'rgba(211, 47, 47, 0.1)' } }}
                                        >
                                            {deleting === wb.id ? <CircularProgress size={20} /> : <DeleteIcon />}
                                        </IconButton>
                                    </Box>
                                </TableCell>
                            </TableRow>
                        ))}
                        {workbooks.length === 0 && (
                            <TableRow>
                                <TableCell colSpan={5} align="center" sx={{ py: 6 }}>
                                    <Typography color="textSecondary" sx={{ fontSize: '1.1rem' }}>No workbooks found. Upload one to get started!</Typography>
                                </TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
            </TableContainer>

            {/* Create Workbook Dialog */}
            <Dialog open={createDialogOpen} onClose={() => setCreateDialogOpen(false)} maxWidth="sm" fullWidth>
                <DialogTitle>Create New Workbook</DialogTitle>
                <DialogContent>
                    <TextField
                        autoFocus
                        margin="dense"
                        label="Workbook Name"
                        fullWidth
                        variant="outlined"
                        value={newWorkbookName}
                        onChange={(e) => setNewWorkbookName(e.target.value)}
                        onKeyDown={(e) => { if (e.key === 'Enter') handleCreateWorkbook(); }}
                    />
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => { setCreateDialogOpen(false); setNewWorkbookName(''); }}>Cancel</Button>
                    <Button onClick={handleCreateWorkbook} variant="contained" disabled={!newWorkbookName.trim() || loading}>
                        Create
                    </Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
};

export default Dashboard;
