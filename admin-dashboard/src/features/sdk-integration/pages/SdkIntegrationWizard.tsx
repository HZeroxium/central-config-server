/**
 * Main SDK Integration Wizard page.
 * Multi-step wizard for migrating configuration and integrating ZCM SDK.
 */

import {
  Box,
  Container,
  Stepper,
  Step,
  StepLabel,
  Button,
  Card,
  CardContent,
  Typography,
  Alert,
} from '@mui/material';
import { ArrowBack, ArrowForward, RestartAlt } from '@mui/icons-material';
import { useMigrationWizard } from '../hooks/useMigrationWizard';
import { Step1Upload } from '../components/Step1Upload';
import { Step2Convert } from '../components/Step2Convert';
import { Step3GenerateProperties } from '../components/Step3GenerateProperties';
import { Step4GenerateSdkConfig } from '../components/Step4GenerateSdkConfig';
import { Step5Checklist } from '../components/Step5Checklist';
import { ConfigAnalyzer } from '../components/ConfigAnalyzer';
import { IntegrationFlowDiagram } from '../components/IntegrationFlowDiagram';
import { IntegrationTimeline } from '../components/IntegrationTimeline';
import { StepErrorBoundary } from '../components/StepErrorBoundary';
import { useAnalyzeConfig } from '../hooks/useConfigMigration';
import {
  announceStepChange,
  focusElement,
} from '../utils/accessibility';
import { useEffect, useRef } from 'react';

const steps = [
  'Upload Config File',
  'Convert Format',
  'Generate Properties Classes',
  'Generate SDK Config',
  'Integration Checklist',
];

export default function SdkIntegrationWizard() {
  const wizard = useMigrationWizard();
  const { state, nextStep, previousStep, reset, setStep } = wizard;
  const stepContentRef = useRef<HTMLDivElement>(null);

  // Get effective YAML content (converted or original)
  const effectiveYaml = state.convertedYaml || state.configContent;

  // Auto-analyze when YAML is available
  const analyzeMutation = useAnalyzeConfig();
  useEffect(() => {
    if (effectiveYaml && state.step >= 2) {
      analyzeMutation.mutate({
        configContent: effectiveYaml,
        format: 'yaml',
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [effectiveYaml, state.step]);

  // Announce step changes to screen readers
  useEffect(() => {
    if (state.step > 0 && state.step <= steps.length) {
      announceStepChange(state.step, steps[state.step - 1]);
      // Focus step content for accessibility
      setTimeout(() => {
        if (stepContentRef.current) {
          focusElement('step-content', { behavior: 'smooth', block: 'start' });
        }
      }, 100);
    }
  }, [state.step]);

  const handleNext = () => {
    // Validate current step before proceeding
    if (state.step === 1 && !state.configContent) {
      return;
    }
    if (state.step === 2 && !effectiveYaml) {
      return;
    }
    nextStep();
  };

  const handlePrevious = () => {
    previousStep();
  };

  const handleStepClick = (stepNumber: number): void => {
    if (stepNumber < state.step) {
      setStep(stepNumber);
    }
  };

  // Build timeline steps data
  const timelineSteps = steps.map((name, index) => {
    const stepNumber = index + 1;
    let status: 'completed' | 'active' | 'pending' = 'pending';
    if (stepNumber < state.step) {
      status = 'completed';
    } else if (stepNumber === state.step) {
      status = 'active';
    }

    const step: {
      number: number;
      name: string;
      description: string;
      status: 'completed' | 'active' | 'pending';
      estimatedTime?: string;
      details?: string[];
      suggestions?: string[];
    } = {
      number: stepNumber,
      name,
      description: getStepDescription(stepNumber),
      status,
    };

    if (stepNumber === 4 && state.generatedSdkConfig?.estimatedTime) {
      step.estimatedTime = state.generatedSdkConfig.estimatedTime;
    }

    if (stepNumber === 4 && state.generatedSdkConfig?.suggestions) {
      step.suggestions = state.generatedSdkConfig.suggestions;
    }

    return step;
  });

  function getStepDescription(stepNumber: number): string {
    switch (stepNumber) {
      case 1:
        return 'Upload your existing configuration file (INI, Properties, or YAML)';
      case 2:
        return 'Convert configuration format to YAML if needed';
      case 3:
        return 'Generate @ConfigurationProperties classes from your YAML';
      case 4:
        return 'Generate ZCM SDK configuration for your service';
      case 5:
        return 'Review integration checklist and complete setup';
      default:
        return '';
    }
  }

  const canProceed = () => {
    switch (state.step) {
      case 1:
        return !!state.configContent;
      case 2:
        return !!effectiveYaml;
      case 3:
        return !!state.generatedPropertiesClasses;
      case 4:
        return !!state.generatedSdkConfig;
      case 5:
        return true;
      default:
        return false;
    }
  };

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" gutterBottom id="wizard-title">
          SDK Integration Wizard
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Migrate your configuration and integrate ZCM Spring SDK Starter
        </Typography>
      </Box>

      {/* Visual Flow Diagram */}
      <Box sx={{ mb: 4 }}>
        <IntegrationFlowDiagram
          currentStep={state.step}
          totalSteps={steps.length}
          steps={steps}
          onStepClick={handleStepClick}
        />
      </Box>

      <Stepper activeStep={state.step - 1} sx={{ mb: 4 }} aria-label="Integration steps">
        {steps.map((label, index) => (
          <Step key={label}>
            <StepLabel
              aria-label={`Step ${index + 1}: ${label}`}
            >
              {label}
            </StepLabel>
          </Step>
        ))}
      </Stepper>

      {/* Step Content */}
      <Box
        id="step-content"
        ref={stepContentRef}
        sx={{ mb: 4 }}
        role="region"
        aria-labelledby="wizard-title"
        aria-live="polite"
      >
        <StepErrorBoundary step={state.step} stepName={steps[state.step - 1]}>
          {state.step === 1 && (
            <Step1Upload
              configFile={state.configFile}
              configContent={state.configContent}
              configFormat={state.configFormat}
              onFileSelect={wizard.setConfigFile}
              onContentChange={wizard.setConfigContent}
              onFormatChange={wizard.setConfigFormat}
            />
          )}

          {state.step === 2 && (
            <StepErrorBoundary step={2} stepName={steps[1]}>
              <Step2Convert
                configContent={state.configContent}
                configFormat={state.configFormat}
                convertedYaml={state.convertedYaml}
                onConverted={(yaml, _yamlMap) => {
                  wizard.setConvertedYaml(yaml);
                }}
              />
            </StepErrorBoundary>
          )}

          {state.step === 3 && effectiveYaml && (
            <StepErrorBoundary step={3} stepName={steps[2]}>
              <Step3GenerateProperties
                yamlContent={effectiveYaml}
                generatedClasses={state.generatedPropertiesClasses}
                onGenerated={wizard.setGeneratedPropertiesClasses}
              />
            </StepErrorBoundary>
          )}

          {state.step === 4 && effectiveYaml && (
            <StepErrorBoundary step={4} stepName={steps[3]}>
              <Step4GenerateSdkConfig
                yamlContent={effectiveYaml}
                generatedSdkConfig={state.generatedSdkConfig}
                onGenerated={wizard.setGeneratedSdkConfig}
              />
            </StepErrorBoundary>
          )}

        {state.step === 5 && state.generatedSdkConfig && (
          <StepErrorBoundary step={5} stepName={steps[4]}>
            <Step5Checklist
              integrationSteps={state.generatedSdkConfig.integrationSteps || []}
              serviceName={state.generatedSdkConfig.serviceName || 'Unknown Service'}
              generatedSdkConfig={state.generatedSdkConfig}
            />
          </StepErrorBoundary>
        )}
        
        {state.step === 5 && !state.generatedSdkConfig && (
          <Card>
            <CardContent>
              <Alert severity="warning">
                Please complete previous steps to view the integration checklist.
              </Alert>
            </CardContent>
          </Card>
        )}
        </StepErrorBoundary>
      </Box>

      {/* Analysis Results (shown on steps 2+) */}
      {state.step >= 2 && analyzeMutation.data && (
        <Box sx={{ mb: 4 }}>
          <ConfigAnalyzer analysisResult={analyzeMutation.data} />
        </Box>
      )}

      {/* Interactive Timeline */}
      <Box sx={{ mb: 4 }}>
        <IntegrationTimeline
          currentStep={state.step}
          steps={timelineSteps}
          generatedSdkConfig={state.generatedSdkConfig}
        />
      </Box>

      {/* Navigation Buttons */}
      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Button
              startIcon={<RestartAlt />}
              onClick={reset}
              variant="outlined"
              aria-label="Start over and reset wizard"
            >
              Start Over
            </Button>

            <Box sx={{ display: 'flex', gap: 2 }}>
              <Button
                startIcon={<ArrowBack />}
                onClick={handlePrevious}
                disabled={state.step === 1}
                variant="outlined"
                aria-label="Go to previous step"
              >
                Previous
              </Button>
              <Button
                endIcon={<ArrowForward />}
                onClick={handleNext}
                disabled={state.step === steps.length || !canProceed()}
                variant="contained"
                aria-label={
                  state.step === steps.length
                    ? 'Complete integration'
                    : 'Go to next step'
                }
              >
                {state.step === steps.length ? 'Complete' : 'Next'}
              </Button>
            </Box>
          </Box>
        </CardContent>
      </Card>

      {/* Help Text */}
      <Alert severity="info" sx={{ mt: 3 }}>
        <Typography variant="body2">
          <strong>Tip:</strong> You can navigate back to previous steps to review or modify your configuration.
          All progress is automatically saved in your browser.
        </Typography>
      </Alert>
    </Container>
  );
}

