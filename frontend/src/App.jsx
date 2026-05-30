import React, { useState } from 'react';
import { BrowserRouter as Router, Routes, Route, useNavigate } from 'react-router-dom';
import Dashboard from './components/Dashboard';
import SpreadsheetView from './components/SpreadsheetView';
import Login from './components/Login';
import { AppBar, Toolbar, Typography, Button, Container, Box } from '@mui/material';
import TableChartIcon from '@mui/icons-material/TableChart';

function Navigation({ setAuthToken }) {
    const navigate = useNavigate();
    return (
        <AppBar position="static" className="app-header">
            <Toolbar>
                <TableChartIcon sx={{ mr: 1 }} />
                <Typography variant="h6" component="div" sx={{ flexGrow: 1, fontWeight: 'bold' }}>
                    Dynamic Excel Platform
                </Typography>
                <Button color="inherit" onClick={() => navigate('/')}>Dashboard</Button>
                <Button color="inherit" onClick={() => {
                    localStorage.removeItem('token');
                    setAuthToken(null);
                }}>Logout</Button>
            </Toolbar>
        </AppBar>
    );
}

function App() {
    const [authToken, setAuthToken] = useState(localStorage.getItem('token'));

    if (!authToken) {
        return (
            <div className="app-container">
                <Login setAuthToken={setAuthToken} />
            </div>
        );
    }

    return (
        <div className="app-container">
            <Router>
                <Routes>
                    {/* Workbook view gets full-screen layout — no AppBar, no Container constraints */}
                    <Route path="/workbook/:id" element={<SpreadsheetView />} />

                    {/* Dashboard and other pages use the normal AppBar + Container layout */}
                    <Route path="/*" element={
                        <>
                            <Navigation setAuthToken={setAuthToken} />
                            <Container maxWidth="xl" sx={{ mt: 4, mb: 4 }}>
                                <Routes>
                                    <Route path="/" element={<Dashboard />} />
                                </Routes>
                            </Container>
                        </>
                    } />
                </Routes>
            </Router>
        </div>
    );
}

export default App;
