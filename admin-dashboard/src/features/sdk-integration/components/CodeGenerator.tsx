/**
 * Component for displaying generated code with syntax highlighting.
 */

import { Box, IconButton, Tooltip, useTheme } from '@mui/material';
import { ContentCopy, Download } from '@mui/icons-material';
import Editor from '@monaco-editor/react';
import { copyToClipboard, downloadFile } from '../utils/fileUtils';
import { toast } from 'sonner';

interface CodeGeneratorProps {
  code: string;
  language: 'java' | 'yaml' | 'markdown' | 'json';
  fileName?: string;
  height?: string;
}

export function CodeGenerator({
  code,
  language,
  fileName,
  height = '400px',
}: CodeGeneratorProps) {
  const theme = useTheme();
  const editorTheme = theme.palette.mode === 'dark' ? 'vs-dark' : 'vs';

  const handleCopy = async () => {
    try {
      await copyToClipboard(code);
      toast.success('Code copied to clipboard');
    } catch (error) {
      toast.error('Failed to copy code');
    }
  };

  const handleDownload = () => {
    const extension = language === 'java' ? 'java' : language === 'yaml' ? 'yml' : 'md';
    const name = fileName || `generated.${extension}`;
    const mimeType =
      language === 'java'
        ? 'text/x-java-source'
        : language === 'yaml'
        ? 'text/yaml'
        : 'text/plain';
    downloadFile(code, name, mimeType);
    toast.success(`Downloaded ${name}`);
  };

  return (
    <Box sx={{ position: 'relative' }}>
      <Box
        sx={{
          display: 'flex',
          justifyContent: 'flex-end',
          gap: 1,
          mb: 1,
        }}
      >
        <Tooltip title="Copy to clipboard">
          <IconButton size="small" onClick={handleCopy}>
            <ContentCopy fontSize="small" />
          </IconButton>
        </Tooltip>
        <Tooltip title="Download file">
          <IconButton size="small" onClick={handleDownload}>
            <Download fontSize="small" />
          </IconButton>
        </Tooltip>
      </Box>
      <Editor
        height={height}
        language={language}
        value={code}
        theme={editorTheme}
        options={{
          readOnly: true,
          minimap: { enabled: false },
          scrollBeyondLastLine: false,
          wordWrap: 'on',
        }}
      />
    </Box>
  );
}

