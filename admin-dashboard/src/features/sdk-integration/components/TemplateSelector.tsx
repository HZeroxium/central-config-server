/**
 * Component for browsing and selecting configuration templates.
 */

import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
  Button,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from '@mui/material';
import { CodeGenerator } from './CodeGenerator';
import { useListTemplatesQuery, useGetTemplate } from '../hooks/useConfigMigration';
import { useState } from 'react';
import type { TemplateInfo } from '../types';

export function TemplateSelector() {
  const [selectedTemplate, setSelectedTemplate] = useState<TemplateInfo | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const listTemplates = useListTemplatesQuery();
  const getTemplate = useGetTemplate();

  const handleTemplateClick = async (templateId: string) => {
    try {
      const template = await getTemplate.mutateAsync(templateId);
      setSelectedTemplate(template);
      setDialogOpen(true);
    } catch (error) {
      // Error handled by mutation
    }
  };

  if (listTemplates.isPending) {
    return (
      <Card>
        <CardContent>
          <Typography>Loading templates...</Typography>
        </CardContent>
      </Card>
    );
  }

  const templates = listTemplates.data || [];

  return (
    <>
      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Configuration Templates
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            Browse common configuration patterns and use them as starting points
          </Typography>

          <Grid container spacing={2}>
            {templates.map((template, index) => (
              <Grid size={{ xs: 12, sm: 6, md: 4 }} key={template.id || `template-${index}`}>
                <Card
                  sx={{
                    height: '100%',
                    cursor: template.id ? 'pointer' : 'default',
                    '&:hover': template.id
                      ? {
                          boxShadow: 3,
                        }
                      : {},
                  }}
                  onClick={() => {
                    if (template.id) {
                      handleTemplateClick(template.id);
                    }
                  }}
                  role={template.id ? 'button' : undefined}
                  tabIndex={template.id ? 0 : -1}
                  aria-label={template.id ? `View ${template.name || 'template'}` : undefined}
                  onKeyDown={(e) => {
                    if (template.id && (e.key === 'Enter' || e.key === ' ')) {
                      e.preventDefault();
                      handleTemplateClick(template.id);
                    }
                  }}
                >
                  <CardContent>
                    <Typography variant="subtitle1" gutterBottom>
                      {template.name}
                    </Typography>
                    <Chip label={template.category} size="small" sx={{ mb: 1 }} />
                    <Typography variant="body2" color="text.secondary">
                      {template.description}
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        </CardContent>
      </Card>

      <Dialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        maxWidth="lg"
        fullWidth
      >
        <DialogTitle>
          {selectedTemplate?.name} Template
        </DialogTitle>
        <DialogContent>
          {selectedTemplate && (
            <Box>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                {selectedTemplate.description}
              </Typography>
              <CodeGenerator
                code={selectedTemplate.content || ''}
                language="yaml"
                fileName={`${selectedTemplate.id || 'template'}.yml`}
                height="500px"
              />
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </>
  );
}

