import React, { useState } from 'react';
import { Box, Button, TextField, Typography, Paper, Container } from '@mui/material';
import { loginUser, registerUser } from '../api';

const Login = ({ setAuthToken }) => {
    const [isLogin, setIsLogin] = useState(true);
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        try {
            const req = isLogin ? loginUser : registerUser;
            const res = await req(username, password);
            const token = res.data.token;
            localStorage.setItem('token', token);
            setAuthToken(token);
        } catch (err) {
            setError(err.response?.data?.message || 'Authentication failed. Make sure to register first.');
        }
    };

    return (
        <Container component="main" maxWidth="xs">
            <Paper elevation={6} sx={{ p: 4, mt: 8, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                <Typography component="h1" variant="h5" color="primary" fontWeight="bold">
                    {isLogin ? 'Sign In' : 'Register'}
                </Typography>
                <Box component="form" onSubmit={handleSubmit} sx={{ mt: 1 }}>
                    <TextField
                        margin="normal"
                        required
                        fullWidth
                        label="Username"
                        autoFocus
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                    />
                    <TextField
                        margin="normal"
                        required
                        fullWidth
                        label="Password"
                        type="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                    />
                    {error && <Typography color="error" variant="body2">{error}</Typography>}
                    <Button type="submit" fullWidth variant="contained" sx={{ mt: 3, mb: 2 }}>
                        {isLogin ? 'Sign In' : 'Register'}
                    </Button>
                    <Button fullWidth variant="text" onClick={() => setIsLogin(!isLogin)}>
                        {isLogin ? "Don't have an account? Register" : "Already have an account? Sign in"}
                    </Button>
                </Box>
            </Paper>
        </Container>
    );
};

export default Login;
