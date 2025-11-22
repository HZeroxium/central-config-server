import { useState, useEffect, useRef } from "react";
import {
  Box,
  Button,
  Stack,
  Alert,
  CircularProgress,
  Typography,
} from "@mui/material";
import {
  Save as SaveIcon,
  Refresh as RefreshIcon,
  CheckCircle as SavedIcon,
} from "@mui/icons-material";
import Editor from "@monaco-editor/react";
import { useTheme } from "@mui/material/styles";

interface ConfigEditorProps {
  value: string;
  onChange: (value: string) => void;
  onSave: () => void;
  onReset: () => void;
  isLoading?: boolean;
  isSaving?: boolean;
  error?: string | null;
  readOnly?: boolean;
  height?: string | number;
  expectedSha?: string;
}

export function ConfigEditor({
  value,
  onChange,
  onSave,
  onReset,
  isLoading = false,
  isSaving = false,
  error = null,
  readOnly = false,
  height = 500,
  expectedSha,
}: ConfigEditorProps) {
  const theme = useTheme();
  const [hasChanges, setHasChanges] = useState(false);
  const [originalValue, setOriginalValue] = useState(value);
  const [lastSaved, setLastSaved] = useState(false);
  const isUserEditingRef = useRef<boolean>(false);
  const previousValueRef = useRef<string>(value);

  // Only reset originalValue when value changes from external source (e.g., file loaded from server)
  // Not when user types (which also updates value prop via onChange)
  useEffect(() => {
    // If value changed and it's NOT from user editing, it's an external change
    // (e.g., from server load, profile change, or after save refetch)
    if (value !== previousValueRef.current && !isUserEditingRef.current) {
      // This is an external change (not from user typing)
      setOriginalValue(value);
      setHasChanges(false);
      setLastSaved(false);
    }
    // Always update the ref to track current value
    previousValueRef.current = value;
    // Reset the editing flag after processing
    isUserEditingRef.current = false;
  }, [value]);

  // Update hasChanges whenever value or originalValue changes
  useEffect(() => {
    setHasChanges(value !== originalValue);
  }, [value, originalValue]);

  const handleEditorChange = (newValue: string | undefined) => {
    const updatedValue = newValue || "";
    // Mark that this change is from user editing
    isUserEditingRef.current = true;
    onChange(updatedValue);
    // hasChanges will be updated by useEffect above
    setLastSaved(false);
  };

  const handleSave = () => {
    onSave();
    // Note: originalValue will be updated after successful save via useEffect
    // when the parent refetches and updates the value prop
  };

  const handleReset = () => {
    onChange(originalValue);
    setHasChanges(false);
    setLastSaved(false);
    onReset();
  };

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" p={4}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Stack
        direction="row"
        spacing={2}
        justifyContent="space-between"
        alignItems="center"
        sx={{ mb: 2 }}
      >
        <Stack direction="row" spacing={2} alignItems="center">
          <Button
            variant="contained"
            startIcon={lastSaved ? <SavedIcon /> : <SaveIcon />}
            onClick={handleSave}
            disabled={!hasChanges || isSaving || readOnly}
            color={lastSaved ? "success" : "primary"}
          >
            {isSaving ? "Saving..." : lastSaved ? "Saved" : "Save"}
          </Button>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={handleReset}
            disabled={!hasChanges || isSaving || readOnly}
          >
            Reset
          </Button>
          {hasChanges && (
            <Typography variant="body2" color="text.secondary">
              Unsaved changes
            </Typography>
          )}
          {expectedSha && (
            <Typography variant="caption" color="text.secondary">
              SHA: {expectedSha.substring(0, 8)}...
            </Typography>
          )}
        </Stack>
      </Stack>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

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
          value={value}
          onChange={handleEditorChange}
          theme={theme.palette.mode === "dark" ? "vs-dark" : "vs"}
          options={{
            readOnly,
            minimap: { enabled: false },
            scrollBeyondLastLine: false,
            fontSize: 14,
            lineNumbers: "on",
            wordWrap: "on",
            automaticLayout: true,
            formatOnPaste: true,
            tabSize: 2,
            insertSpaces: true,
            trimAutoWhitespace: true,
          }}
        />
      </Box>
    </Box>
  );
}

