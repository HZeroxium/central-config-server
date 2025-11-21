/**
 * Step 3: Generate @ConfigurationProperties classes.
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
  Tabs,
  Tab,
  Accordion,
  AccordionSummary,
  AccordionDetails,
} from '@mui/material';
import { ExpandMore, CheckCircle } from '@mui/icons-material';
import { CodeGenerator } from './CodeGenerator';
import { useGeneratePropertiesClass } from '../hooks/useConfigMigration';
import { useState, useEffect } from 'react';
import type { GeneratedClass } from '../types';

interface Step3GeneratePropertiesProps {
  yamlContent: string;
  generatedClasses: GeneratedClass[] | null;
  onGenerated: (classes: GeneratedClass[]) => void;
}

export function Step3GenerateProperties({
  yamlContent,
  generatedClasses,
  onGenerated,
}: Step3GeneratePropertiesProps) {
  const [packageName, setPackageName] = useState('com.example.config');
  const [selectedClassIndex, setSelectedClassIndex] = useState(0);
  const generateMutation = useGeneratePropertiesClass();

  const handleGenerate = async () => {
    try {
      const result = await generateMutation.mutateAsync({
        yamlContent,
        packageName: packageName || 'com.example.config',
      });
      if (result?.classes) {
        onGenerated(result.classes);
      }
    } catch (error) {
      // Error handled by mutation
    }
  };

  useEffect(() => {
    if (yamlContent && !generatedClasses) {
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
            aria-label="Generating ConfigurationProperties classes"
          >
            <CircularProgress aria-label="Loading" />
            <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
              Generating @ConfigurationProperties classes...
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
            aria-label="Retry generation"
          >
            Retry Generation
          </Button>
        </CardContent>
      </Card>
    );
  }

  if (generatedClasses && generatedClasses.length > 0) {
    return (
      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
            <CheckCircle color="success" />
            <Typography variant="h6">
              Generated {generatedClasses.length} @ConfigurationProperties {generatedClasses.length === 1 ? 'Class' : 'Classes'}
            </Typography>
          </Box>

          <TextField
            fullWidth
            label="Package Name"
            value={packageName}
            onChange={(e) => setPackageName(e.target.value)}
            sx={{ mb: 2 }}
            helperText="Package name for generated classes"
          />

          {generatedClasses.length > 1 && (
            <Tabs
              value={selectedClassIndex}
              onChange={(_, v) => setSelectedClassIndex(v)}
              sx={{ mb: 2 }}
            >
              {generatedClasses.map((cls, idx) => (
                <Tab key={idx} label={cls.className} />
              ))}
            </Tabs>
          )}

          {generatedClasses.map((cls, idx) => (
            <Accordion
              key={idx}
              expanded={selectedClassIndex === idx}
              onChange={(_, isExpanded) => {
                if (isExpanded) setSelectedClassIndex(idx);
              }}
            >
              <AccordionSummary expandIcon={<ExpandMore />}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, width: '100%' }}>
                  <Typography variant="subtitle1">{cls.className || 'Unnamed Class'}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    Prefix: {cls.prefix || 'N/A'}
                  </Typography>
                </Box>
              </AccordionSummary>
              <AccordionDetails>
                <CodeGenerator
                  code={cls.code || ''}
                  language="java"
                  fileName={`${cls.className || 'Generated'}.java`}
                  height="500px"
                />
              </AccordionDetails>
            </Accordion>
          ))}

          <Alert severity="info" sx={{ mt: 2 }}>
            <Typography variant="body2">
              <strong>Next steps:</strong>
            </Typography>
            <Typography variant="body2" component="ul" sx={{ mt: 1, pl: 2 }}>
              <li>Copy the generated classes to your project</li>
              <li>Add @ConfigurationPropertiesScan to your main class</li>
              <li>Ensure required dependencies are in build.gradle</li>
            </Typography>
          </Alert>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardContent>
        <Typography variant="h6" gutterBottom>
          Step 3: Generate @ConfigurationProperties Classes
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Generate Java classes with @ConfigurationProperties annotations from your YAML configuration
        </Typography>

        <TextField
          fullWidth
          label="Package Name"
          value={packageName}
          onChange={(e) => setPackageName(e.target.value)}
          sx={{ mb: 2 }}
          helperText="Package name for generated classes (e.g., com.example.config)"
        />

        <Button
          variant="contained"
          onClick={handleGenerate}
          disabled={!yamlContent}
          fullWidth
          aria-label="Generate ConfigurationProperties classes"
          aria-describedby={!yamlContent ? 'generate-classes-disabled-help' : undefined}
        >
          Generate Classes
        </Button>
        {!yamlContent && (
          <Typography
            id="generate-classes-disabled-help"
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

