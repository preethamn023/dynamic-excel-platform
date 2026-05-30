import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
    Box, Button, Typography, Paper, Table, TableBody, TableCell, 
    TableContainer, TableHead, TableRow, Chip, CircularProgress 
} from '@mui/material';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import InsertDriveFileIcon from '@mui/icons-material/InsertDriveFile';
import { uploadWorkbook, getWorkbooks } from '../api';

const Dashboard = () => {
    const [workbooks, setWorkbooks] = useState([]);
    const [loading, setLoading] = useState(false);
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
            if (e.response && e.response.status === 403) {
                // Token might be expired
                localStorage.removeItem('token');
                window.location.reload();
            }
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

    return (
        <Box>
            <Box display="flex" justifyContent="space-between" alignItems="center" mb={4}>
                <Typography variant="h4">Your Workbooks</Typography>
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
                                    <Button variant="outlined" onClick={() => navigate(`/workbook/${wb.id}`)}>
                                        Open
                                    </Button>
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
        </Box>
    );
};

export default Dashboard;
