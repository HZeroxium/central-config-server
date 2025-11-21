/**
 * Step 2: Convert configuration format (if needed).
 */

import {
  Box,
  Card,
  CardContent,
  Typography,
  Alert,
  CircularProgress,
  Button,
} from '@mui/material';
import { CheckCircle } from '@mui/icons-material';
import { ConfigPreview } from './ConfigPreview';
import { useConvertIniToYaml, useConvertPropertiesToYaml } from '../hooks/useConfigMigration';
import { useEffect } from 'react';
import type { ConfigFormat, ConversionResult } from '../types';

interface Step2ConvertProps {
  configContent: string;
  configFormat: ConfigFormat;
  convertedYaml: string | null;
  onConverted: (yaml: string, yamlMap: Record<string, unknown>) => void;
}

export function Step2Convert({
  configContent,
  configFormat,
  convertedYaml,
  onConverted,
}: Step2ConvertProps) {
  const convertIni = useConvertIniToYaml();
  const convertProperties = useConvertPropertiesToYaml();

  useEffect(() => {
    if (configFormat === 'yaml' || !configContent) {
      return;
    }

    const performConversion = async () => {
      try {
        let result: ConversionResult | undefined;
        if (configFormat === 'ini') {
          result = await convertIni.mutateAsync(configContent);
        } else if (configFormat === 'properties') {
          result = await convertProperties.mutateAsync(configContent);
        } else {
          return;
        }

        if (result?.yamlContent) {
          // Convert yamlMap to Record<string, unknown> for type safety
          const yamlMap = (result.yamlMap as Record<string, unknown>) || {};
          onConverted(result.yamlContent, yamlMap);
        }
      } catch (error) {
        // Error handling is done by the mutation
      }
    };

    performConversion();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [configContent, configFormat]);

  if (configFormat === 'yaml') {
    return (
      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
            <CheckCircle color="success" />
            <Typography variant="h6">Configuration is already in YAML format</Typography>
          </Box>
          <ConfigPreview yamlContent={configContent} />
        </CardContent>
      </Card>
    );
  }

  const isLoading = convertIni.isPending || convertProperties.isPending;
  const error = convertIni.error || convertProperties.error;

  if (isLoading) {
    return (
      <Card>
        <CardContent>
          <Box
            sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', py: 4 }}
            role="status"
            aria-live="polite"
            aria-label={`Converting ${configFormat.toUpperCase()} to YAML`}
          >
            <CircularProgress aria-label="Loading" />
            <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
              Converting {configFormat.toUpperCase()} to YAML...
            </Typography>
          </Box>
        </CardContent>
      </Card>
    );
  }

  if (error) {
    return (
      <Card>
        <CardContent>
          <Alert
            severity="error"
            role="alert"
            aria-live="assertive"
            aria-label="Conversion error"
          >
            Conversion failed: {error instanceof Error ? error.message : 'Unknown error'}
          </Alert>
          <Button
            variant="outlined"
            onClick={async () => {
              try {
                if (configFormat === 'ini') {
                  await convertIni.mutateAsync(configContent);
                } else {
                  await convertProperties.mutateAsync(configContent);
                }
              } catch (error) {
                // Error handled by hook
              }
            }}
            sx={{ mt: 2 }}
            aria-label="Retry conversion"
          >
            Retry Conversion
          </Button>
        </CardContent>
      </Card>
    );
  }

  if (convertedYaml) {
    return (
      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
            <CheckCircle color="success" />
            <Typography variant="h6">Conversion Complete</Typography>
          </Box>
          <ConfigPreview yamlContent={convertedYaml} />
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardContent>
        <Alert severity="info">
          Ready to convert {configFormat.toUpperCase()} to YAML format
        </Alert>
      </CardContent>
    </Card>
  );
}

