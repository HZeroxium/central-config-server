import React from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Box, 
  Typography, 
  Button, 
  Container,
  Paper,
  Stack
} from '@mui/material';
import { 
  Block as BlockIcon,
  Home as HomeIcon 
} from '@mui/icons-material';

const UnauthorizedPage: React.FC = () => {
  const navigate = useNavigate();

  const handleGoToDashboard = () => {
    navigate('/dashboard');
  };

  return (
    <Container maxWidth="sm">
      <Box
        display="flex"
        flexDirection="column"
        alignItems="center"
        justifyContent="center"
        minHeight="100vh"
        py={4}
      >
        <Paper
          elevation={3}
          sx={{
            p: 6,
            textAlign: 'center',
            borderRadius: 2,
            backgroundColor: 'background.paper',
          }}
        >
          <Stack spacing={3} alignItems="center">
            <BlockIcon 
              sx={{ 
                fontSize: 80, 
                color: 'error.main',
                opacity: 0.7 
              }} 
            />
            
            <Typography 
              variant="h4" 
              component="h1" 
              color="text.primary"
              fontWeight="bold"
            >
              Access Denied
            </Typography>
            
            <Typography 
              variant="body1" 
              color="text.secondary"
              sx={{ maxWidth: 400 }}
            >
              You don't have permission to access this page. 
              Please contact your administrator if you believe this is an error.
            </Typography>
            
            <Button
              variant="contained"
              startIcon={<HomeIcon />}
              onClick={handleGoToDashboard}
              size="large"
              sx={{
                mt: 2,
                px: 4,
                py: 1.5,
                borderRadius: 2,
                textTransform: 'none',
                fontSize: '1rem',
                fontWeight: 600,
              }}
            >
              Go to Dashboard
            </Button>
          </Stack>
        </Paper>
      </Box>
    </Container>
  );
};

export default UnauthorizedPage;
