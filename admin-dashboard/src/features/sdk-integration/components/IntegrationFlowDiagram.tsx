/**
 * Visual flow diagram component for SDK integration steps.
 */

import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
  Chip,
} from '@mui/material';
import {
  CheckCircle,
  RadioButtonUnchecked,
  CloudUpload,
  Transform,
  Code,
  Settings,
  Checklist,
} from '@mui/icons-material';
import { getStepAriaLabel } from '../utils/accessibility';

interface IntegrationFlowDiagramProps {
  currentStep: number;
  totalSteps: number;
  steps: string[];
  onStepClick?: (step: number) => void;
}

const stepIcons = [
  CloudUpload,
  Transform,
  Code,
  Settings,
  Checklist,
];

export function IntegrationFlowDiagram({
  currentStep,
  totalSteps,
  steps,
  onStepClick,
}: IntegrationFlowDiagramProps) {
  const getStepStatus = (stepIndex: number): 'completed' | 'active' | 'pending' => {
    const stepNumber = stepIndex + 1;
    if (stepNumber < currentStep) {
      return 'completed';
    }
    if (stepNumber === currentStep) {
      return 'active';
    }
    return 'pending';
  };

  const handleStepClick = (stepIndex: number): void => {
    if (onStepClick && stepIndex < currentStep) {
      onStepClick(stepIndex + 1);
    }
  };

  return (
    <Card>
      <CardContent>
        <Typography variant="h6" gutterBottom>
          Integration Flow
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Visual overview of your SDK integration progress
        </Typography>

        <Grid container spacing={2}>
          {steps.map((stepName, index) => {
            const stepNumber = index + 1;
            const status = getStepStatus(index);
            const StepIcon = stepIcons[index] || RadioButtonUnchecked;
            const isClickable = index < currentStep - 1;

            return (
              <Grid size={{ xs: 12, sm: 6, md: 2.4 }} key={index}>
                <Box
                  sx={{
                    position: 'relative',
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    p: 2,
                    borderRadius: 2,
                    border: status === 'active' ? 2 : 1,
                    borderColor:
                      status === 'completed'
                        ? 'success.main'
                        : status === 'active'
                        ? 'primary.main'
                        : 'divider',
                    bgcolor:
                      status === 'active'
                        ? 'primary.light'
                        : status === 'completed'
                        ? 'success.light'
                        : 'background.paper',
                    cursor: isClickable ? 'pointer' : 'default',
                    transition: 'all 0.2s ease-in-out',
                    '&:hover': isClickable
                      ? {
                          transform: 'translateY(-4px)',
                          boxShadow: 2,
                        }
                      : {},
                  }}
                  onClick={() => handleStepClick(index)}
                  role={isClickable ? 'button' : undefined}
                  tabIndex={isClickable ? 0 : -1}
                  aria-label={getStepAriaLabel(stepNumber, stepName, status === 'active', status === 'completed')}
                  onKeyDown={(e) => {
                    if (isClickable && (e.key === 'Enter' || e.key === ' ')) {
                      e.preventDefault();
                      handleStepClick(index);
                    }
                  }}
                >
                  <Box
                    sx={{
                      mb: 1,
                      color:
                        status === 'completed'
                          ? 'success.main'
                          : status === 'active'
                          ? 'primary.main'
                          : 'text.secondary',
                    }}
                  >
                    {status === 'completed' ? (
                      <CheckCircle fontSize="large" />
                    ) : (
                      <StepIcon fontSize="large" />
                    )}
                  </Box>
                  <Typography
                    variant="caption"
                    fontWeight={status === 'active' ? 'bold' : 'normal'}
                    textAlign="center"
                  >
                    Step {stepNumber}
                  </Typography>
                  <Typography
                    variant="body2"
                    textAlign="center"
                    sx={{
                      mt: 0.5,
                      fontWeight: status === 'active' ? 'medium' : 'normal',
                    }}
                  >
                    {stepName}
                  </Typography>
                  {status === 'active' && (
                    <Chip
                      label="Current"
                      size="small"
                      color="primary"
                      sx={{ mt: 1 }}
                    />
                  )}
                </Box>
              </Grid>
            );
          })}
        </Grid>

        {/* Progress indicator */}
        <Box sx={{ mt: 3, display: 'flex', alignItems: 'center', gap: 2 }}>
          <Typography variant="body2" color="text.secondary">
            Progress:
          </Typography>
          <Box sx={{ flex: 1, height: 8, bgcolor: 'divider', borderRadius: 1, overflow: 'hidden' }}>
            <Box
              sx={{
                height: '100%',
                width: `${(currentStep / totalSteps) * 100}%`,
                bgcolor: 'primary.main',
                transition: 'width 0.3s ease-in-out',
              }}
              role="progressbar"
              aria-valuenow={currentStep}
              aria-valuemin={1}
              aria-valuemax={totalSteps}
              aria-label={`Integration progress: ${currentStep} of ${totalSteps} steps completed`}
            />
          </Box>
          <Typography variant="body2" fontWeight="medium">
            {currentStep} / {totalSteps}
          </Typography>
        </Box>
      </CardContent>
    </Card>
  );
}

