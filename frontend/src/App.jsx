import React from 'react';
import { BrowserRouter as Router, Routes, Route, useNavigate } from 'react-router-dom';
import Dashboard from './components/Dashboard';
import SpreadsheetView from './components/SpreadsheetView';
import { AppBar, Toolbar, Typography, Button, Container } from '@mui/material';
import TableChartIcon from '@mui/icons-material/TableChart';

function Navigation() {
    const navigate = useNavigate();
    return (
        <AppBar position="static" className="app-header">
            <Toolbar>
                <TableChartIcon sx={{ mr: 1 }} />
                <Typography variant="h6" component="div" sx={{ flexGrow: 1, fontWeight: 'bold' }}>
                    Dynamic Excel Platform
                </Typography>
                <Button color="inherit" onClick={() => navigate('/')}>Dashboard</Button>
            </Toolbar>
        </AppBar>
    );
}

function App() {
    return (
        <div className="app-container">
            <Router>
                <Routes>
                    {/* Workbook view gets full-screen layout — no AppBar, no Container constraints */}
                    <Route path="/workbook/:id" element={<SpreadsheetView />} />

                    {/* Dashboard and other pages use the normal AppBar + Container layout */}
                    <Route path="/*" element={
                        <>
                            <Navigation />
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
