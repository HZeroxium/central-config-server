import { Box, Typography, Alert } from "@mui/material";
import Grid from "@mui/material/Grid";
import Editor from "@monaco-editor/react";
import { useTheme } from "@mui/material/styles";

interface DiffViewerProps {
  original: string;
  modified: string;
  height?: string | number;
}

export function DiffViewer({
  original,
  modified,
  height = 400,
}: DiffViewerProps) {
  const theme = useTheme();

  if (original === modified) {
    return (
      <Alert severity="info">No changes detected between versions.</Alert>
    );
  }

  return (
    <Box>
      <Typography variant="subtitle2" gutterBottom>
        Diff View
      </Typography>
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, md: 6 }}>
          <Typography variant="caption" color="text.secondary" gutterBottom>
            Original
          </Typography>
          <Box
            sx={{
              border: 1,
              borderColor: "divider",
              borderRadius: 1,
              overflow: "hidden",
            }}
          >
            <Editor
              height={typeof height === "number" ? `${height}px` : height}
              language="yaml"
              value={original}
              options={{
                readOnly: true,
                minimap: { enabled: false },
                scrollBeyondLastLine: false,
                fontSize: 14,
                lineNumbers: "on",
                wordWrap: "on",
                automaticLayout: true,
              }}
              theme={theme.palette.mode === "dark" ? "vs-dark" : "vs"}
            />
          </Box>
        </Grid>
        <Grid size={{ xs: 12, md: 6 }}>
          <Typography variant="caption" color="text.secondary" gutterBottom>
            Modified
          </Typography>
          <Box
            sx={{
              border: 1,
              borderColor: "divider",
              borderRadius: 1,
              overflow: "hidden",
            }}
          >
            <Editor
              height={typeof height === "number" ? `${height}px` : height}
              language="yaml"
              value={modified}
              options={{
                readOnly: true,
                minimap: { enabled: false },
                scrollBeyondLastLine: false,
                fontSize: 14,
                lineNumbers: "on",
                wordWrap: "on",
                automaticLayout: true,
              }}
              theme={theme.palette.mode === "dark" ? "vs-dark" : "vs"}
            />
          </Box>
        </Grid>
      </Grid>
    </Box>
  );
}

