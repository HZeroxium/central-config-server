/**
 * Step 4: Generate SDK configuration.
 */

import {
  Box,
  Card,
  CardContent,
  Typography,
  Alert,
  CircularProgress,
  Button,
  TextField,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
} from '@mui/material';
import { CheckCircle, Check } from '@mui/icons-material';
import { CodeGenerator } from './CodeGenerator';
import { useGenerateSdkConfig } from '../hooks/useConfigMigration';
import { useState, useEffect } from 'react';
import type { SdkConfigGenerationResponse } from '../types';

interface Step4GenerateSdkConfigProps {
  yamlContent: string;
  generatedSdkConfig: SdkConfigGenerationResponse | null;
  onGenerated: (config: SdkConfigGenerationResponse) => void;
}

export function Step4GenerateSdkConfig({
  yamlContent,
  generatedSdkConfig,
  onGenerated,
}: Step4GenerateSdkConfigProps) {
  const [serviceName, setServiceName] = useState('');
  const generateMutation = useGenerateSdkConfig();

  const handleGenerate = async () => {
    try {
      const result = await generateMutation.mutateAsync({
        applicationYml: yamlContent,
        serviceName: serviceName || undefined,
      });
      if (result) {
        onGenerated(result);
        if (result.serviceName && !serviceName) {
          setServiceName(result.serviceName);
        }
      }
    } catch (error) {
      // Error handled by mutation
    }
  };

  useEffect(() => {
    if (yamlContent && !generatedSdkConfig && !generateMutation.isPending) {
      // Auto-generate on mount if YAML is available
      handleGenerate();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [yamlContent]);

  if (generateMutation.isPending) {
    return (
      <Card>
        <CardContent>
          <Box
            sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', py: 4 }}
            role="status"
            aria-live="polite"
            aria-label="Generating SDK configuration"
          >
            <CircularProgress aria-label="Loading" />
            <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
              Generating SDK configuration...
            </Typography>
          </Box>
        </CardContent>
      </Card>
    );
  }

  if (generateMutation.error) {
    return (
      <Card>
        <CardContent>
          <Alert
            severity="error"
            role="alert"
            aria-live="assertive"
            aria-label="Generation error"
          >
            Generation failed: {generateMutation.error instanceof Error ? generateMutation.error.message : 'Unknown error'}
          </Alert>
          <Button
            variant="outlined"
            onClick={handleGenerate}
            sx={{ mt: 2 }}
            aria-label="Retry SDK configuration generation"
          >
            Retry Generation
          </Button>
        </CardContent>
      </Card>
    );
  }

  if (generatedSdkConfig) {
    return (
      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
            <CheckCircle color="success" />
            <Typography variant="h6">SDK Configuration Generated</Typography>
          </Box>

          <TextField
            fullWidth
            label="Service Name"
            value={serviceName}
            onChange={(e) => setServiceName(e.target.value)}
            sx={{ mb: 2 }}
            helperText="Service name for SDK configuration"
            aria-label="Service name input"
          />

          <Typography variant="subtitle2" gutterBottom sx={{ mt: 2 }}>
            Generated SDK Configuration:
          </Typography>
          <CodeGenerator
            code={generatedSdkConfig.generatedConfigYaml || ''}
            language="yaml"
            fileName="zcm-sdk-config.yml"
            height="400px"
          />

          {generatedSdkConfig.suggestions && generatedSdkConfig.suggestions.length > 0 && (
            <Alert severity="info" sx={{ mt: 2 }}>
              <Typography variant="body2" component="strong">Suggestions:</Typography>
              <List dense>
                {generatedSdkConfig.suggestions.map((suggestion, idx) => (
                  <ListItem key={idx}>
                    <ListItemIcon>
                      <Check fontSize="small" />
                    </ListItemIcon>
                    <ListItemText primary={suggestion || ''} />
                  </ListItem>
                ))}
              </List>
            </Alert>
          )}

          <Button
            variant="outlined"
            onClick={handleGenerate}
            sx={{ mt: 2 }}
            disabled={generateMutation.isPending}
            aria-label="Regenerate SDK configuration with updated service name"
          >
            Regenerate with Updated Service Name
          </Button>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardContent>
        <Typography variant="h6" gutterBottom>
          Step 4: Generate SDK Configuration
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Generate ZCM SDK configuration snippet from your existing application.yml
        </Typography>

        <TextField
          fullWidth
          label="Service Name (Optional)"
          value={serviceName}
          onChange={(e) => setServiceName(e.target.value)}
          sx={{ mb: 2 }}
          helperText="Service name will be extracted from config if not provided"
        />

        <Button
          variant="contained"
          onClick={handleGenerate}
          disabled={!yamlContent}
          fullWidth
          aria-label="Generate SDK configuration"
          aria-describedby={!yamlContent ? 'generate-disabled-help' : undefined}
        >
          Generate SDK Config
        </Button>
        {!yamlContent && (
          <Typography
            id="generate-disabled-help"
            variant="caption"
            color="text.secondary"
            sx={{ mt: 1, display: 'block' }}
          >
            Please provide YAML content first
          </Typography>
        )}
      </CardContent>
    </Card>
  );
}

