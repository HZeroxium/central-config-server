import { Box, Typography, Button } from '@mui/material'
import { Link } from 'react-router-dom'
import { Home as HomeIcon } from '@mui/icons-material'

export default function NotFoundPage() {
  return (
    <Box sx={{ 
      display: 'flex', 
      flexDirection: 'column', 
      alignItems: 'center', 
      justifyContent: 'center',
      minHeight: '60vh',
      textAlign: 'center',
      p: 3
    }}>
      <Typography 
        variant="h1" 
        sx={{ 
          fontSize: { xs: '4rem', sm: '6rem' }, 
          fontWeight: 700,
          color: 'primary.main',
          mb: 2
        }}
      >
        404
      </Typography>
      <Typography variant="h5" sx={{ mb: 2, color: 'text.secondary' }}>
        Page Not Found
      </Typography>
      <Typography variant="body1" sx={{ mb: 4, color: 'text.secondary', maxWidth: 400 }}>
        The page you're looking for doesn't exist or has been moved.
      </Typography>
      <Button 
        component={Link} 
        to="/" 
        variant="contained" 
        startIcon={<HomeIcon />}
        size="large"
        className="btn-primary"
      >
        Go to Dashboard
      </Button>
    </Box>
  )
}
