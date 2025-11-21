/**
 * Component for previewing YAML/Properties configuration.
 */

import { Box, Tabs, Tab, useTheme } from '@mui/material';
import Editor from '@monaco-editor/react';
import { useState } from 'react';

interface ConfigPreviewProps {
  yamlContent: string;
  yamlMap?: Record<string, unknown>;
  height?: string;
}

export function ConfigPreview({
  yamlContent,
  yamlMap,
  height = '300px',
}: ConfigPreviewProps) {
  const theme = useTheme();
  const editorTheme = theme.palette.mode === 'dark' ? 'vs-dark' : 'vs';
  const [tab, setTab] = useState(0);

  return (
    <Box>
      <Tabs value={tab} onChange={(_, v) => setTab(v)}>
        <Tab label="YAML" />
        {yamlMap && <Tab label="Structure" />}
      </Tabs>
      {tab === 0 && (
        <Box sx={{ mt: 2 }}>
          <Editor
            height={height}
            language="yaml"
            value={yamlContent}
            theme={editorTheme}
            options={{
              readOnly: true,
              minimap: { enabled: false },
              scrollBeyondLastLine: false,
              wordWrap: 'on',
            }}
          />
        </Box>
      )}
      {tab === 1 && yamlMap && (
        <Box sx={{ mt: 2 }}>
          <Editor
            height={height}
            language="json"
            value={JSON.stringify(yamlMap, null, 2)}
            theme={editorTheme}
            options={{
              readOnly: true,
              minimap: { enabled: false },
              scrollBeyondLastLine: false,
              wordWrap: 'on',
            }}
          />
        </Box>
      )}
    </Box>
  );
}

