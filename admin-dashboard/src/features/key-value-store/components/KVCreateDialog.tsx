/**
 * Dialog component for selecting KV entry type and prefix for creation
 */

import { useState, useEffect } from "react";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  RadioGroup,
  FormControlLabel,
  Radio,
  Box,
  Typography,
  Alert,
  Stack,
  Card,
  CardContent,
} from "@mui/material";
import {
  InsertDriveFile as LeafIcon,
  Code as ObjectIcon,
  List as ListIcon,
} from "@mui/icons-material";
import { validateKVPath, normalizePath } from "../types";

export type KVCreateType = "leaf" | "object" | "list";

export interface KVCreateDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: (type: KVCreateType, prefix: string) => void;
  currentPrefix?: string;
  initialType?: KVCreateType;
}

export function KVCreateDialog({
  open,
  onClose,
  onConfirm,
  currentPrefix = "",
  initialType,
}: KVCreateDialogProps) {
  const [selectedType, setSelectedType] = useState<KVCreateType>(
    initialType || "leaf"
  );
  const [prefix, setPrefix] = useState(currentPrefix);
  const [validationError, setValidationError] = useState<string | null>(null);

  // Reset form when dialog opens/closes or currentPrefix changes
  useEffect(() => {
    if (open) {
      setPrefix(currentPrefix);
      setSelectedType(initialType || "leaf");
      setValidationError(null);
    }
  }, [open, currentPrefix, initialType]);

  // Validate prefix in real-time
  useEffect(() => {
    if (open && prefix) {
      const normalized = normalizePath(prefix);
      if (normalized !== prefix) {
        // Path was normalized, update it
        setPrefix(normalized);
      }
      const validation = validateKVPath(prefix, false);
      if (!validation.isValid) {
        setValidationError(validation.error || validation.warning || null);
      } else {
        setValidationError(null);
      }
    } else {
      setValidationError(null);
    }
  }, [prefix, open]);

  const handleConfirm = () => {
    // Strict validation on confirm
    const normalized = normalizePath(prefix);
    const validation = validateKVPath(normalized, true);
    
    if (!validation.isValid) {
      setValidationError(validation.error || "Invalid path");
      return;
    }

    onConfirm(selectedType, normalized);
  };

  const handlePrefixChange = (value: string) => {
    setPrefix(value);
  };

  const typeDescriptions = {
    leaf: "A simple key-value entry with a single value (text, JSON, binary, etc.)",
    object: "A structured object stored as multiple key-value pairs under a prefix",
    list: "An ordered list of items with a manifest for ordering and metadata",
  };

  const typeIcons = {
    leaf: <LeafIcon />,
    object: <ObjectIcon />,
    list: <ListIcon />,
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="sm"
      fullWidth
      aria-labelledby="kv-create-dialog-title"
    >
      <DialogTitle id="kv-create-dialog-title">
        Create New KV Entry
      </DialogTitle>
      <DialogContent>
        <Stack spacing={3} sx={{ mt: 1 }}>
          <Box>
            <Typography variant="subtitle2" gutterBottom>
              Entry Type
            </Typography>
            <RadioGroup
              value={selectedType}
              onChange={(e) => setSelectedType(e.target.value as KVCreateType)}
              aria-label="entry type"
            >
              {(["leaf", "object", "list"] as const).map((type) => (
                <Card
                  key={type}
                  variant="outlined"
                  sx={{
                    mb: 1,
                    cursor: "pointer",
                    "&:hover": {
                      borderColor: "primary.main",
                      bgcolor: "action.hover",
                    },
                    ...(selectedType === type && {
                      borderColor: "primary.main",
                      bgcolor: "action.selected",
                    }),
                  }}
                  onClick={() => setSelectedType(type)}
                >
                  <CardContent sx={{ p: 1.5, "&:last-child": { pb: 1.5 } }}>
                    <FormControlLabel
                      value={type}
                      control={<Radio />}
                      label={
                        <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                          <Box sx={{ color: "primary.main" }}>
                            {typeIcons[type]}
                          </Box>
                          <Box>
                            <Typography variant="body1" fontWeight="medium">
                              {type === "leaf"
                                ? "Leaf Entry"
                                : type === "object"
                                ? "Object"
                                : "List"}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                              {typeDescriptions[type]}
                            </Typography>
                          </Box>
                        </Box>
                      }
                      sx={{ m: 0, width: "100%" }}
                    />
                  </CardContent>
                </Card>
              ))}
            </RadioGroup>
          </Box>

          <Box>
            <TextField
              fullWidth
              label="Prefix"
              value={prefix}
              onChange={(e) => handlePrefixChange(e.target.value)}
              placeholder={
                selectedType === "leaf"
                  ? "e.g., config/database/url"
                  : "e.g., config/app"
              }
              helperText={
                validationError ||
                (selectedType === "leaf"
                  ? "Full path for the key (e.g., config/database/url)"
                  : "Prefix where the structure will be stored (e.g., config/app)")
              }
              error={!!validationError}
              size="small"
              aria-describedby={
                validationError ? "prefix-error" : "prefix-help"
              }
            />
            {selectedType !== "leaf" && (
              <Alert severity="info" sx={{ mt: 1 }} role="note">
                Objects and Lists are stored under a prefix. All key-value pairs
                will be created under this prefix path.
              </Alert>
            )}
          </Box>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} aria-label="Cancel creation">
          Cancel
        </Button>
        <Button
          onClick={handleConfirm}
          variant="contained"
          disabled={!!validationError}
          aria-label="Create entry"
        >
          Create
        </Button>
      </DialogActions>
    </Dialog>
  );
}

