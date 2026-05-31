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
        <Box>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={4}>
                <Typography variant="h4">Your Workbooks</Typography>
                <Box display="flex" gap={1}>
                    <Button
                        variant="outlined"
                        startIcon={<AddIcon />}
                        onClick={() => setCreateDialogOpen(true)}
                    >
                        Create New Workbook
                    </Button>
                    <Button
                        component="label"
                        variant="contained"
                        startIcon={loading ? <CircularProgress size={20} color="inherit" /> : <CloudUploadIcon />}
                        disabled={loading}
                    >
                        Upload Excel File
                        <input type="file" hidden accept=".xlsx,.xls" onChange={handleUpload} />
                    </Button>
                </Box>
            </Box>

            <TableContainer component={Paper}>
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableCell><b>File Name</b></TableCell>
                            <TableCell><b>Upload Date</b></TableCell>
                            <TableCell><b>Version</b></TableCell>
                            <TableCell><b>Status</b></TableCell>
                            <TableCell align="right"><b>Actions</b></TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {workbooks.map((wb) => (
                            <TableRow key={wb.id} hover>
                                <TableCell>
                                    <Box display="flex" alignItems="center" gap={1}>
                                        <InsertDriveFileIcon color="primary" />
                                        {wb.fileName}
                                    </Box>
                                </TableCell>
                                <TableCell>{new Date(wb.uploadDate).toLocaleString()}</TableCell>
                                <TableCell>v{wb.currentVersion}</TableCell>
                                <TableCell><Chip label={wb.status} color="success" size="small" /></TableCell>
                                <TableCell align="right">
                                    <Box display="flex" justifyContent="flex-end" alignItems="center" gap={1}>
                                        <Button variant="outlined" onClick={() => navigate(`/workbook/${wb.id}`)}>
                                            Open
                                        </Button>
                                        <IconButton
                                            color="error"
                                            onClick={() => handleDeleteWorkbook(wb)}
                                            disabled={deleting === wb.id}
                                            size="small"
                                        >
                                            {deleting === wb.id ? <CircularProgress size={20} /> : <DeleteIcon />}
                                        </IconButton>
                                    </Box>
                                </TableCell>
                            </TableRow>
                        ))}
                        {workbooks.length === 0 && (
                            <TableRow>
                                <TableCell colSpan={5} align="center" sx={{ py: 3 }}>
                                    <Typography color="textSecondary">No workbooks found. Upload one to get started!</Typography>
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
