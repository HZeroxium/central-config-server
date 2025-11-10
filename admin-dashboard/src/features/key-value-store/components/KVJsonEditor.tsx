/**
 * JSON Editor component with Monaco Editor
 */

import { useState, useEffect, useMemo } from "react";
import {
  Box,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Button,
  Stack,
  Alert,
  Typography,
  ToggleButtonGroup,
  ToggleButton,
} from "@mui/material";
import {
  FormatAlignLeft as FormatIcon,
  Code as CodeIcon,
  Description as TextIcon,
} from "@mui/icons-material";
import Editor from "@monaco-editor/react";
import type { KVEncoding } from "../types";
import { KVPutRequestEncoding } from "@lib/api/models";

export type EditorMode = "text" | "json" | "raw";

export interface KVJsonEditorProps {
  /** Initial value */
  value: string;
  /** Current encoding */
  encoding: KVEncoding;
  /** Callback when value changes */
  onChange: (value: string) => void;
  /** Callback when encoding changes */
  onEncodingChange: (encoding: KVEncoding) => void;
  /** Whether editor is read-only */
  readOnly?: boolean;
  /** Error message */
  error?: string;
  /** Height of editor */
  height?: string | number;
}

/**
 * Check if a string is valid JSON
 */
function isValidJSON(str: string): boolean {
  if (!str || !str.trim()) return false;
  try {
    JSON.parse(str);
    return true;
  } catch {
    return false;
  }
}

/**
 * Format JSON string
 */
function formatJSON(str: string): string {
  try {
    const parsed = JSON.parse(str);
    return JSON.stringify(parsed, null, 2);
  } catch {
    return str;
  }
}

export function KVJsonEditor({
  value,
  encoding,
  onChange,
  onEncodingChange,
  readOnly = false,
  error,
  height = 400,
}: KVJsonEditorProps) {
  const [editorMode, setEditorMode] = useState<EditorMode>("text");
  const [jsonError, setJsonError] = useState<string | null>(null);

  // Auto-detect JSON mode
  useEffect(() => {
    if (value && isValidJSON(value)) {
      setEditorMode("json");
      setJsonError(null);
    } else if (value) {
      // Check if it looks like JSON but is invalid
      const trimmed = value.trim();
      if (
        (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
        (trimmed.startsWith("[") && trimmed.endsWith("]"))
      ) {
        setEditorMode("json");
        try {
          JSON.parse(value);
          setJsonError(null);
        } catch (e) {
          setJsonError(
            e instanceof Error ? e.message : "Invalid JSON format"
          );
        }
      }
    }
  }, [value]);

  const handleModeChange = (
    _event: React.MouseEvent<HTMLElement>,
    newMode: EditorMode | null
  ) => {
    if (newMode) {
      setEditorMode(newMode);
      if (newMode === "json") {
        // Validate JSON when switching to JSON mode
        if (value && !isValidJSON(value)) {
          try {
            JSON.parse(value);
          } catch (e) {
            setJsonError(
              e instanceof Error ? e.message : "Invalid JSON format"
            );
          }
        }
      } else {
        setJsonError(null);
      }
    }
  };

  const handleFormat = () => {
    if (editorMode === "json" && value) {
      try {
        const formatted = formatJSON(value);
        onChange(formatted);
        setJsonError(null);
      } catch (e) {
        setJsonError(
          e instanceof Error ? e.message : "Failed to format JSON"
        );
      }
    }
  };

  const handleEditorChange = (newValue: string | undefined) => {
    const val = newValue || "";
    onChange(val);

    // Validate JSON in real-time if in JSON mode
    if (editorMode === "json" && val) {
      try {
        JSON.parse(val);
        setJsonError(null);
      } catch (e) {
        setJsonError(
          e instanceof Error ? e.message : "Invalid JSON format"
        );
      }
    }
  };

  const language = useMemo(() => {
    switch (editorMode) {
      case "json":
        return "json";
      case "text":
        return "plaintext";
      case "raw":
        return "plaintext";
      default:
        return "plaintext";
    }
  }, [editorMode]);

  return (
    <Box>
      <Stack direction="row" spacing={2} sx={{ mb: 2 }} alignItems="center">
        <ToggleButtonGroup
          value={editorMode}
          exclusive
          onChange={handleModeChange}
          size="small"
          aria-label="editor mode"
        >
          <ToggleButton value="text" aria-label="text mode">
            <TextIcon fontSize="small" sx={{ mr: 1 }} />
            Text
          </ToggleButton>
          <ToggleButton value="json" aria-label="json mode">
            <CodeIcon fontSize="small" sx={{ mr: 1 }} />
            JSON
          </ToggleButton>
          <ToggleButton value="raw" aria-label="raw mode">
            <CodeIcon fontSize="small" sx={{ mr: 1 }} />
            Raw
          </ToggleButton>
        </ToggleButtonGroup>

        {editorMode === "json" && (
          <Button
            size="small"
            startIcon={<FormatIcon />}
            onClick={handleFormat}
            disabled={readOnly || !value}
            variant="outlined"
          >
            Format
          </Button>
        )}

        <FormControl size="small" sx={{ minWidth: 120 }}>
          <InputLabel>Encoding</InputLabel>
          <Select
            value={encoding}
            onChange={(e) =>
              onEncodingChange(e.target.value as KVEncoding)
            }
            disabled={readOnly}
            label="Encoding"
          >
            <MenuItem value={KVPutRequestEncoding.utf8}>UTF-8</MenuItem>
            <MenuItem value={KVPutRequestEncoding.base64}>
              Base64
            </MenuItem>
            <MenuItem value={KVPutRequestEncoding.raw}>Raw</MenuItem>
          </Select>
        </FormControl>
      </Stack>

      {(error || jsonError) && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error || jsonError}
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
          height={typeof height === "number" ? height : height}
          language={language}
          value={value}
          onChange={handleEditorChange}
          options={{
            readOnly,
            minimap: { enabled: false },
            scrollBeyondLastLine: false,
            fontSize: 14,
            lineNumbers: "on",
            wordWrap: "on",
            automaticLayout: true,
            formatOnPaste: editorMode === "json",
            formatOnType: editorMode === "json",
          }}
          theme="vs"
        />
      </Box>

      {editorMode === "json" && (
        <Typography variant="caption" color="text.secondary" sx={{ mt: 1 }}>
          JSON mode: Auto-formatting enabled. Invalid JSON will be highlighted.
        </Typography>
      )}
    </Box>
  );
}

