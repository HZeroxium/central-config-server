/**
 * Step 5: Integration checklist.
 */

import {
  Box,
  Card,
  CardContent,
  Typography,
  Checkbox,
  FormControlLabel,
  Button,
  List,
  ListItem,
  ListItemText,
  Divider,
  Alert,
} from '@mui/material';
import { CheckCircle, Download } from '@mui/icons-material';
import { formatChecklist, downloadFile } from '../utils/fileUtils';
import { toast } from 'sonner';
import { useState } from 'react';
import type { SdkConfigGenerationResponse } from '../types';

interface Step5ChecklistProps {
  integrationSteps: string[];
  serviceName: string;
  generatedSdkConfig: SdkConfigGenerationResponse | null;
}

export function Step5Checklist({
  integrationSteps,
  serviceName,
  generatedSdkConfig,
}: Step5ChecklistProps) {
  const [checkedItems, setCheckedItems] = useState<Set<number>>(new Set());

  const handleToggle = (index: number) => {
    setCheckedItems((prev) => {
      const newSet = new Set(prev);
      if (newSet.has(index)) {
        newSet.delete(index);
      } else {
        newSet.add(index);
      }
      return newSet;
    });
  };

  const handleDownloadChecklist = () => {
    const checklist = formatChecklist(integrationSteps);
    const markdown = `# SDK Integration Checklist for ${serviceName}\n\n${checklist}\n\n## Generated Configuration\n\n\`\`\`yaml\n${generatedSdkConfig?.generatedConfigYaml || ''}\n\`\`\`\n`;
    downloadFile(markdown, `sdk-integration-checklist-${serviceName}.md`, 'text/markdown');
    toast.success('Checklist downloaded');
  };

  return (
    <Card>
      <CardContent>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
          <Typography variant="h6">Integration Checklist</Typography>
          <Button
            variant="outlined"
            startIcon={<Download />}
            onClick={handleDownloadChecklist}
            aria-label="Download integration checklist"
          >
            Download Checklist
          </Button>
        </Box>

        <Alert severity="info" sx={{ mb: 3 }} role="region" aria-label="Checklist instructions">
          Follow these steps to complete SDK integration. Check off items as you complete them.
        </Alert>

        <List role="list" aria-label="Integration steps checklist">
          {integrationSteps.map((step, index) => (
            <ListItem key={index} disablePadding role="listitem">
              <FormControlLabel
                control={
                  <Checkbox
                    checked={checkedItems.has(index)}
                    onChange={() => handleToggle(index)}
                    icon={<Box sx={{ width: 24, height: 24, border: '2px solid', borderColor: 'divider', borderRadius: 1 }} />}
                    checkedIcon={<CheckCircle color="success" />}
                    aria-label={`${step} - ${checkedItems.has(index) ? 'completed' : 'not completed'}`}
                  />
                }
                label={
                  <ListItemText
                    primary={step}
                    primaryTypographyProps={{
                      sx: {
                        textDecoration: checkedItems.has(index) ? 'line-through' : 'none',
                        color: checkedItems.has(index) ? 'text.secondary' : 'text.primary',
                      },
                    }}
                  />
                }
                sx={{ width: '100%', ml: 0 }}
              />
            </ListItem>
          ))}
        </List>

        <Divider sx={{ my: 3 }} />

        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="body2" color="text.secondary">
            Progress: {checkedItems.size} / {integrationSteps.length} completed
          </Typography>
          {checkedItems.size === integrationSteps.length && (
            <Alert
              severity="success"
              sx={{ flex: 1, ml: 2 }}
              role="status"
              aria-live="polite"
              aria-label="All steps completed"
            >
              All steps completed! Your SDK integration is ready.
            </Alert>
          )}
        </Box>
      </CardContent>
    </Card>
  );
}

