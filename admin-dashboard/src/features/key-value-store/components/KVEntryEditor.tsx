/**
 * Inline editor component for KV entries
 */

import { useState, useEffect } from "react";
import {
  Box,
  TextField,
  Button,
  Stack,
  Typography,
  Alert,
} from "@mui/material";
import Grid from "@mui/material/Grid";
import {
  Save as SaveIcon,
  Cancel as CancelIcon,
} from "@mui/icons-material";
import type { KVEntry, KVEncoding, PathValidationResult } from "../types";
import type { KVPutRequest } from "@lib/api/models";
import { validateKVPath } from "../types";
import { KVJsonEditor } from "./KVJsonEditor";

export interface KVEntryEditorProps {
  entry?: KVEntry;
  /** Initial path (can be edited for new entries) */
  path: string;
  /** Current prefix for pre-filling path */
  currentPrefix?: string;
  onSave: (path: string, data: KVPutRequest) => Promise<void>;
  onCancel: () => void;
  isReadOnly?: boolean;
  isSaving?: boolean;
}

export function KVEntryEditor({
  entry,
  path: initialPath,
  currentPrefix = "",
  onSave,
  onCancel,
  isReadOnly = false,
  isSaving = false,
}: KVEntryEditorProps) {
  const isEdit = !!entry;
  const [key, setKey] = useState("");
  const [value, setValue] = useState("");
  const [encoding, setEncoding] = useState<KVEncoding>("utf8");
  const [flags, setFlags] = useState<number>(0);
  const [error, setError] = useState<string | null>(null);
  const [keyValidation, setKeyValidation] = useState<PathValidationResult>({
    isValid: true,
  });

  // Initialize form from entry
  useEffect(() => {
    if (entry) {
      // For edit mode: extract key from full path for display
      const fullPath = entry.path || initialPath;
      if (currentPrefix && fullPath.startsWith(currentPrefix + "/")) {
        setKey(fullPath.slice(currentPrefix.length + 1));
      } else {
        setKey(fullPath);
      }
      // Decode base64 value
      if (entry.valueBase64) {
        try {
          const decoded = atob(entry.valueBase64);
          setValue(decoded);
        } catch {
          setValue(entry.valueBase64);
        }
      }
      setFlags(entry.flags || 0);
      setEncoding("utf8"); // Default encoding
    } else {
      // New entry: start with empty key (user will enter it)
      setKey("new-key");
      setValue("");
      setEncoding("utf8");
      setFlags(0);
    }
  }, [entry, initialPath, currentPrefix]);

  // Validate key in real-time (for new entries)
  useEffect(() => {
    if (!isEdit && key) {
      // Construct full path for validation
      const fullPath = currentPrefix
        ? `${currentPrefix}/${key}`
        : key;
      const validation = validateKVPath(fullPath, false);
      setKeyValidation(validation);
    } else {
      setKeyValidation({ isValid: true });
    }
  }, [key, isEdit, currentPrefix]);

  const handleSave = async () => {
    setError(null);

    // Construct full path from key and currentPrefix
    let fullPath: string;
    if (isEdit) {
      // In edit mode, use the original path
      fullPath = entry?.path || initialPath;
    } else {
      // In create mode, prepend currentPrefix to key
      if (currentPrefix) {
        const normalizedPrefix = currentPrefix.endsWith("/")
          ? currentPrefix.slice(0, -1)
          : currentPrefix;
        fullPath = `${normalizedPrefix}/${key}`;
      } else {
        fullPath = key;
      }
      
      // Validate path (strict validation on save)
      const strictValidation = validateKVPath(fullPath, true);
      if (!strictValidation.isValid) {
        setError(strictValidation.error || "Invalid key");
        return;
      }
    }

    if (!value.trim() && encoding !== "raw") {
      setError("Value is required");
      return;
    }

    // Validate JSON if encoding is UTF-8 and looks like JSON
    if (encoding === "utf8" && value.trim()) {
      const trimmed = value.trim();
      if (
        (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
        (trimmed.startsWith("[") && trimmed.endsWith("]"))
      ) {
        try {
          JSON.parse(value);
        } catch (e) {
          setError(
            `Invalid JSON: ${e instanceof Error ? e.message : "Parse error"}`
          );
          return;
        }
      }
    }

    try {
      const putRequest: KVPutRequest = {
        value,
        encoding,
        flags,
        // Auto-enable CAS for edits
        cas: entry?.modifyIndex,
      };

      await onSave(fullPath, putRequest);
    } catch (err) {
      setError(
        err && typeof err === "object" && "message" in err
          ? (err.message as string)
          : "Failed to save entry"
      );
    }
  };

  return (
    <Box sx={{ p: 2 }}>
      <Typography variant="h6" sx={{ mb: 2 }}>
        {isEdit ? "Edit Entry" : "Create Entry"}
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      <Grid container spacing={2}>
        <Grid size={{ xs: 12 }}>
          <TextField
            fullWidth
            label={isEdit ? "Path" : "Key"}
            value={key}
            onChange={(e) => setKey(e.target.value)}
            disabled={isEdit || isReadOnly || isSaving}
            size="small"
            error={!keyValidation.isValid}
            helperText={
              keyValidation.error ||
              keyValidation.warning ||
              (isEdit
                ? "Path cannot be changed when editing"
                : currentPrefix
                ? `Key (will be saved as: ${currentPrefix}/${key || "..."})`
                : "Enter the key (can contain '/' characters, e.g., key/subkey)")
            }
          />
        </Grid>

        <Grid size={{ xs: 12 }}>
          <KVJsonEditor
            value={value}
            encoding={encoding}
            onChange={setValue}
            onEncodingChange={setEncoding}
            readOnly={isReadOnly || isSaving}
            error={error || undefined}
            height={400}
          />
        </Grid>

        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            fullWidth
            label="Flags"
            type="number"
            value={flags}
            onChange={(e) => setFlags(parseInt(e.target.value) || 0)}
            disabled={isReadOnly || isSaving}
            size="small"
            inputProps={{ min: 0 }}
            helperText="Arbitrary metadata (uint64)"
          />
        </Grid>

        {isEdit && entry?.modifyIndex && (
          <Grid size={{ xs: 12 }}>
            <Typography variant="caption" color="text.secondary">
              CAS enabled: modifyIndex = {entry.modifyIndex}
            </Typography>
          </Grid>
        )}
      </Grid>

      {!isReadOnly && (
        <Stack direction="row" spacing={2} sx={{ mt: 3 }}>
          <Button
            variant="contained"
            startIcon={<SaveIcon />}
            onClick={handleSave}
            disabled={isSaving}
          >
            {isSaving ? "Saving..." : "Save"}
          </Button>
          <Button
            variant="outlined"
            startIcon={<CancelIcon />}
            onClick={onCancel}
            disabled={isSaving}
          >
            Cancel
          </Button>
        </Stack>
      )}
    </Box>
  );
}

