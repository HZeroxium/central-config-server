import { Component, ReactNode } from 'react'
import { Box, Typography, Button, Alert, AlertTitle } from '@mui/material'
import { Refresh as RefreshIcon } from '@mui/icons-material'

interface Props {
  children: ReactNode
  fallback?: ReactNode
}

interface State {
  hasError: boolean
  error?: Error
}

export default class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = { hasError: false }
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, errorInfo: any) {
    console.error('ErrorBoundary caught an error:', error, errorInfo)
  }

  handleReset = () => {
    this.setState({ hasError: false, error: undefined })
  }

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback
      }

      return (
        <Box sx={{ 
          display: 'flex', 
          flexDirection: 'column', 
          alignItems: 'center', 
          justifyContent: 'center',
          minHeight: '60vh',
          p: 3,
          textAlign: 'center'
        }}>
          <Alert severity="error" sx={{ mb: 3, maxWidth: 600 }}>
            <AlertTitle>Something went wrong</AlertTitle>
            An unexpected error occurred. Please try refreshing the page.
          </Alert>
          
          <Button 
            variant="contained" 
            startIcon={<RefreshIcon />}
            onClick={this.handleReset}
            className="btn-primary"
            sx={{ mb: 2 }}
          >
            Try Again
          </Button>
          
          <Button 
            variant="outlined" 
            onClick={() => window.location.reload()}
            className="btn-secondary"
          >
            Refresh Page
          </Button>
          
          {process.env.NODE_ENV === 'development' && this.state.error && (
            <Box sx={{ mt: 3, p: 2, bgcolor: 'grey.100', borderRadius: 1, maxWidth: 600 }}>
              <Typography variant="caption" component="pre" sx={{ textAlign: 'left', overflow: 'auto' }}>
                {this.state.error.stack}
              </Typography>
            </Box>
          )}
        </Box>
      )
    }

    return this.props.children
  }
}
