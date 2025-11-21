/**
 * Interactive timeline component for SDK integration steps.
 */

import {
  Box,
  Card,
  CardContent,
  Typography,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Chip,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  LinearProgress,
} from '@mui/material';
import {
  ExpandMore,
  CheckCircle,
  RadioButtonUnchecked,
  Schedule,
  Info,
} from '@mui/icons-material';
import type { SdkConfigGenerationResponse } from '../types';

interface TimelineStep {
  number: number;
  name: string;
  description: string;
  status: 'completed' | 'active' | 'pending';
  estimatedTime?: string;
  details?: string[];
  suggestions?: string[];
}

interface IntegrationTimelineProps {
  currentStep: number;
  steps: TimelineStep[];
  generatedSdkConfig: SdkConfigGenerationResponse | null;
}

export function IntegrationTimeline({
  currentStep,
  steps,
  generatedSdkConfig,
}: IntegrationTimelineProps) {
  const getStepStatusIcon = (status: TimelineStep['status']) => {
    switch (status) {
      case 'completed':
        return <CheckCircle color="success" />;
      case 'active':
        return <RadioButtonUnchecked color="primary" />;
      default:
        return <RadioButtonUnchecked color="disabled" />;
    }
  };

  const getStepStatusColor = (status: TimelineStep['status']) => {
    switch (status) {
      case 'completed':
        return 'success';
      case 'active':
        return 'primary';
      default:
        return 'default';
    }
  };

  return (
    <Card>
      <CardContent>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 3 }}>
          <Typography variant="h6">Integration Timeline</Typography>
          {generatedSdkConfig?.estimatedTime && (
            <Chip
              icon={<Schedule />}
              label={`Estimated: ${generatedSdkConfig.estimatedTime}`}
              color="primary"
              variant="outlined"
            />
          )}
        </Box>

        <Box sx={{ mb: 2 }}>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
            Overall Progress
          </Typography>
          <LinearProgress
            variant="determinate"
            value={(currentStep / steps.length) * 100}
            sx={{ height: 8, borderRadius: 1 }}
            aria-label={`Integration progress: ${currentStep} of ${steps.length} steps`}
          />
          <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: 'block' }}>
            {currentStep} of {steps.length} steps completed
          </Typography>
        </Box>

        <Box>
          {steps.map((step, index) => {
            const isExpanded = step.status === 'active' || (step.status === 'completed' && index === currentStep - 1);
            
            return (
              <Accordion
                key={step.number}
                expanded={isExpanded}
                sx={{
                  mb: 1,
                  '&:before': {
                    display: 'none',
                  },
                  border: step.status === 'active' ? 1 : 0,
                  borderColor: 'primary.main',
                }}
                aria-label={`Step ${step.number}: ${step.name}`}
              >
                <AccordionSummary
                  expandIcon={<ExpandMore />}
                  sx={{
                    '& .MuiAccordionSummary-content': {
                      alignItems: 'center',
                    },
                  }}
                >
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, width: '100%' }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', minWidth: 40 }}>
                      {getStepStatusIcon(step.status)}
                    </Box>
                    <Box sx={{ flex: 1 }}>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
                        <Typography variant="subtitle1" fontWeight="medium">
                          Step {step.number}: {step.name}
                        </Typography>
                        <Chip
                          label={step.status}
                          size="small"
                          color={getStepStatusColor(step.status)}
                          variant={step.status === 'active' ? 'filled' : 'outlined'}
                        />
                        {step.estimatedTime && (
                          <Chip
                            icon={<Schedule fontSize="small" />}
                            label={step.estimatedTime}
                            size="small"
                            variant="outlined"
                          />
                        )}
                      </Box>
                      <Typography variant="body2" color="text.secondary">
                        {step.description}
                      </Typography>
                    </Box>
                  </Box>
                </AccordionSummary>
                <AccordionDetails>
                  <Box sx={{ pl: 7 }}>
                    {step.details && step.details.length > 0 && (
                      <Box sx={{ mb: 2 }}>
                        <Typography variant="subtitle2" gutterBottom>
                          Details:
                        </Typography>
                        <List dense>
                          {step.details.map((detail, idx) => (
                            <ListItem key={idx}>
                              <ListItemIcon>
                                <Info fontSize="small" color="primary" />
                              </ListItemIcon>
                              <ListItemText primary={detail || ''} />
                            </ListItem>
                          ))}
                        </List>
                      </Box>
                    )}

                    {step.suggestions && step.suggestions.length > 0 && (
                      <Box>
                        <Typography variant="subtitle2" gutterBottom>
                          Suggestions:
                        </Typography>
                        <List dense>
                          {step.suggestions.map((suggestion, idx) => (
                            <ListItem key={idx}>
                              <ListItemIcon>
                                <Info fontSize="small" color="info" />
                              </ListItemIcon>
                              <ListItemText primary={suggestion || ''} />
                            </ListItem>
                          ))}
                        </List>
                      </Box>
                    )}

                    {step.number === 4 && generatedSdkConfig?.suggestions && generatedSdkConfig.suggestions.length > 0 && (
                      <Box sx={{ mt: 2 }}>
                        <Typography variant="subtitle2" gutterBottom>
                          Additional Suggestions:
                        </Typography>
                        <List dense>
                          {generatedSdkConfig.suggestions.map((suggestion, idx) => (
                            <ListItem key={idx}>
                              <ListItemIcon>
                                <Info fontSize="small" color="info" />
                              </ListItemIcon>
                              <ListItemText primary={suggestion || ''} />
                            </ListItem>
                          ))}
                        </List>
                      </Box>
                    )}
                  </Box>
                </AccordionDetails>
              </Accordion>
            );
          })}
        </Box>
      </CardContent>
    </Card>
  );
}

