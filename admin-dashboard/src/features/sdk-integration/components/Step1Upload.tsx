/**
 * Step 1: Upload configuration file.
 */

import {
  Box,
  Card,
  CardContent,
  Typography,
  Alert,
  TextField,
} from '@mui/material';
import { CloudUpload } from '@mui/icons-material';
import { useRef, useState } from 'react';
import { readFileAsText, detectConfigFormat } from '../utils/fileUtils';
import type { ConfigFormat } from '../types';

interface Step1UploadProps {
  configFile: File | null;
  configContent: string;
  configFormat: ConfigFormat;
  onFileSelect: (file: File) => void;
  onContentChange: (content: string) => void;
  onFormatChange: (format: ConfigFormat) => void;
}

export function Step1Upload({
  configFile,
  configContent,
  configFormat,
  onFileSelect,
  onContentChange,
  onFormatChange,
}: Step1UploadProps) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [error, setError] = useState<string | null>(null);

  const handleFileSelect = async (file: File) => {
    try {
      setError(null);
      const content = await readFileAsText(file);
      const format = detectConfigFormat(file.name, content);
      
      onFileSelect(file);
      onContentChange(content);
      onFormatChange(format);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to read file');
    }
  };

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      handleFileSelect(file);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    const file = e.dataTransfer.files[0];
    if (file) {
      handleFileSelect(file);
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
  };

  return (
    <Card>
      <CardContent>
        <Typography variant="h6" gutterBottom>
          Step 1: Upload Configuration File
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Upload your existing configuration file (INI, Properties, or YAML)
        </Typography>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        <Box
          sx={{
            border: '2px dashed',
            borderColor: 'divider',
            borderRadius: 2,
            p: 4,
            textAlign: 'center',
            cursor: 'pointer',
            '&:hover': {
              borderColor: 'primary.main',
              backgroundColor: 'action.hover',
            },
            '&:focus-visible': {
              outline: '2px solid',
              outlineColor: 'primary.main',
              outlineOffset: 2,
            },
          }}
          onDrop={handleDrop}
          onDragOver={handleDragOver}
          onClick={() => fileInputRef.current?.click()}
          role="button"
          tabIndex={0}
          aria-label="Upload configuration file"
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') {
              e.preventDefault();
              fileInputRef.current?.click();
            }
          }}
        >
          <CloudUpload sx={{ fontSize: 48, color: 'text.secondary', mb: 2 }} />
          <Typography variant="body1" gutterBottom>
            {configFile ? configFile.name : 'Click or drag file here to upload'}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Supports: .ini, .properties, .yml, .yaml
          </Typography>
          <input
            ref={fileInputRef}
            type="file"
            accept=".ini,.properties,.props,.yml,.yaml"
            onChange={handleFileInputChange}
            style={{ display: 'none' }}
            aria-label="File input for configuration file"
          />
        </Box>

        {configFile && (
          <Box sx={{ mt: 3 }}>
            <Typography variant="subtitle2" gutterBottom>
              Detected Format: {configFormat.toUpperCase()}
            </Typography>
            <TextField
              fullWidth
              multiline
              rows={10}
              label="File Content"
              value={configContent}
              onChange={(e) => onContentChange(e.target.value)}
              sx={{ mt: 1 }}
            />
          </Box>
        )}
      </CardContent>
    </Card>
  );
}

