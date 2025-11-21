/**
 * Error boundary component for SDK integration wizard steps.
 */

import React, { Component, type ReactNode } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Alert,
} from '@mui/material';
import { ErrorOutline, Refresh } from '@mui/icons-material';
import { logSdkIntegrationError } from '../utils/errorLogger';

interface StepErrorBoundaryProps {
  children: ReactNode;
  step: number;
  stepName: string;
  onRetry?: () => void;
}

interface StepErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
}

export class StepErrorBoundary extends Component<StepErrorBoundaryProps, StepErrorBoundaryState> {
  constructor(props: StepErrorBoundaryProps) {
    super(props);
    this.state = {
      hasError: false,
      error: null,
    };
  }

  static getDerivedStateFromError(error: Error): StepErrorBoundaryState {
    return {
      hasError: true,
      error,
    };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo): void {
    // Log error
    logSdkIntegrationError(error, this.props.step, {
      componentStack: errorInfo.componentStack,
      stepName: this.props.stepName,
    });
  }

  handleRetry = (): void => {
    this.setState({
      hasError: false,
      error: null,
    });
    
    if (this.props.onRetry) {
      this.props.onRetry();
    }
  };

  render(): ReactNode {
    if (this.state.hasError) {
      return (
        <Card>
          <CardContent>
            <Alert
              severity="error"
              icon={<ErrorOutline />}
              sx={{ mb: 2 }}
              role="alert"
              aria-live="assertive"
            >
              <Typography variant="h6" gutterBottom>
                Error in {this.props.stepName}
              </Typography>
              <Typography variant="body2" sx={{ mb: 2 }}>
                {this.state.error?.message || 'An unexpected error occurred'}
              </Typography>
              <Button
                variant="contained"
                startIcon={<Refresh />}
                onClick={this.handleRetry}
                aria-label={`Retry ${this.props.stepName}`}
              >
                Retry Step
              </Button>
            </Alert>
            
            {import.meta.env.DEV && this.state.error && (
              <Box
                sx={{
                  mt: 2,
                  p: 2,
                  bgcolor: 'error.light',
                  borderRadius: 1,
                  maxHeight: '200px',
                  overflow: 'auto',
                }}
              >
                <Typography variant="caption" component="pre" sx={{ fontSize: '0.75rem' }}>
                  {this.state.error.stack}
                </Typography>
              </Box>
            )}
          </CardContent>
        </Card>
      );
    }

    return this.props.children;
  }
}

