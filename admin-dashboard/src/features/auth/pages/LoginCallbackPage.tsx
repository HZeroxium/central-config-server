import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, CircularProgress, Typography } from '@mui/material';
import { useKeycloak } from '@react-keycloak/web';

export const LoginCallbackPage: React.FC = () => {
  const navigate = useNavigate();
  const { keycloak } = useKeycloak();

  useEffect(() => {
    const handleCallback = async () => {
      try {
        if (keycloak?.authenticated) {
          // User is authenticated, redirect to dashboard
          navigate('/dashboard', { replace: true });
        } else {
          // Authentication failed, redirect to login
          navigate('/login', { replace: true });
        }
      } catch (error) {
        console.error('Login callback error:', error);
        navigate('/login', { replace: true });
      }
    };

    handleCallback();
  }, [keycloak?.authenticated, navigate]);

  return (
    <Box
      display="flex"
      flexDirection="column"
      justifyContent="center"
      alignItems="center"
      minHeight="100vh"
      gap={2}
    >
      <CircularProgress size={40} />
      <Typography variant="body1" color="text.secondary">
        Processing login...
      </Typography>
    </Box>
  );
};

export default LoginCallbackPage;
