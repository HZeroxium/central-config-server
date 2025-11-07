/**
 * Inline editor component for KV entries
 */

import { useState, useEffect } from "react";
import {
  Box,
  TextField,
  Button,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Stack,
  Typography,
  Alert,
} from "@mui/material";
import Grid from "@mui/material/Grid";
import {
  Save as SaveIcon,
  Cancel as CancelIcon,
} from "@mui/icons-material";
import type { KVEntry, KVEncoding } from "../types";
import type { KVPutRequest } from "@lib/api/models";
import { KVPutRequestEncoding } from "@lib/api/models";

export interface KVEntryEditorProps {
  entry?: KVEntry;
  path: string;
  onSave: (data: KVPutRequest) => Promise<void>;
  onCancel: () => void;
  isReadOnly?: boolean;
  isSaving?: boolean;
}

export function KVEntryEditor({
  entry,
  path,
  onSave,
  onCancel,
  isReadOnly = false,
  isSaving = false,
}: KVEntryEditorProps) {
  const isEdit = !!entry;
  const [value, setValue] = useState("");
  const [encoding, setEncoding] = useState<KVEncoding>("utf8");
  const [flags, setFlags] = useState<number>(0);
  const [error, setError] = useState<string | null>(null);

  // Initialize form from entry
  useEffect(() => {
    if (entry) {
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
      // New entry
      setValue("");
      setEncoding("utf8");
      setFlags(0);
    }
  }, [entry]);

  const handleSave = async () => {
    setError(null);

    if (!value.trim() && encoding !== "raw") {
      setError("Value is required");
      return;
    }

    try {
      const putRequest: KVPutRequest = {
        value,
        encoding,
        flags,
        // Auto-enable CAS for edits
        cas: entry?.modifyIndex,
      };

      await onSave(putRequest);
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
            label="Path"
            value={path}
            disabled
            size="small"
            helperText="Path cannot be changed"
          />
        </Grid>

        <Grid size={{ xs: 12 }}>
          <TextField
            fullWidth
            label="Value"
            value={value}
            onChange={(e) => setValue(e.target.value)}
            disabled={isReadOnly || isSaving}
            multiline
            rows={8}
            size="small"
            placeholder="Enter value..."
            helperText={
              encoding === "base64"
                ? "Enter base64-encoded value"
                : encoding === "utf8"
                ? "Enter UTF-8 text"
                : "Enter raw value"
            }
          />
        </Grid>

        <Grid size={{ xs: 12, sm: 6 }}>
          <FormControl fullWidth size="small">
            <InputLabel>Encoding</InputLabel>
            <Select
              value={encoding}
              onChange={(e) =>
                setEncoding(e.target.value as KVEncoding)
              }
              disabled={isReadOnly || isSaving}
              label="Encoding"
            >
              <MenuItem value={KVPutRequestEncoding.utf8}>
                UTF-8
              </MenuItem>
              <MenuItem value={KVPutRequestEncoding.base64}>
                Base64
              </MenuItem>
              <MenuItem value={KVPutRequestEncoding.raw}>
                Raw
              </MenuItem>
            </Select>
          </FormControl>
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

