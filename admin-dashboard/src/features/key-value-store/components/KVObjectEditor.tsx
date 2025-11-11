/**
 * Component for editing KV objects (structured key-value pairs)
 */

import { useState, useEffect } from "react";
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Stack,
  TextField,
  IconButton,
  Alert,
  ToggleButtonGroup,
  ToggleButton,
} from "@mui/material";
import {
  Save as SaveIcon,
  Cancel as CancelIcon,
  Add as AddIcon,
  Delete as DeleteIcon,
  Code as CodeIcon,
  List as ListIcon,
} from "@mui/icons-material";
import { KVJsonEditor } from "./KVJsonEditor";
import { toast } from "@lib/toast/toast";
import { KVPutRequestEncoding } from "@lib/api/models";
import { useGetKVObject, type GetKVObjectParams } from "../hooks";
import { fromKVObjectResponseData } from "../utils/typeAdapters";
import { Skeleton } from "@mui/material";
import { normalizePath, validateKVPath } from "../types";

export type ObjectEditorMode = "form" | "json";

export interface KVObjectEditorProps {
  serviceId: string;
  prefix: string;
  /** Initial object data */
  initialData?: Record<string, unknown>;
  /** Callback when save is triggered */
  onSave: (prefix: string, data: Record<string, unknown>) => Promise<void>;
  /** Callback when cancel is triggered */
  onCancel: () => void;
  /** Whether editor is read-only */
  isReadOnly?: boolean;
  /** Whether save is in progress */
  isSaving?: boolean;
  /** Whether this is create mode (don't auto-load) */
  isCreateMode?: boolean;
}

interface FormField {
  key: string;
  value: string;
}

export function KVObjectEditor({
  serviceId,
  prefix: initialPrefix,
  initialData = {},
  onSave,
  onCancel,
  isReadOnly = false,
  isSaving = false,
  isCreateMode = false,
}: KVObjectEditorProps) {
  const [mode, setMode] = useState<ObjectEditorMode>("form");
  const [formFields, setFormFields] = useState<FormField[]>([]);
  const [jsonValue, setJsonValue] = useState("");
  const [jsonError, setJsonError] = useState<string | null>(null);
  const [prefix, setPrefix] = useState(initialPrefix);
  const [prefixError, setPrefixError] = useState<string | null>(null);

  // Auto-load data if initialData is empty and not in create mode
  const shouldAutoLoad = !isCreateMode && Object.keys(initialData).length === 0;
  const params: GetKVObjectParams | undefined = shouldAutoLoad && serviceId && prefix
    ? { prefix, consistent: false, stale: false }
    : undefined;

  const { data: objectData, isLoading: isLoadingObject, error: objectError } = useGetKVObject(
    serviceId,
    params,
    {
      query: {
        enabled: shouldAutoLoad && !!serviceId && !!prefix,
        staleTime: 10_000,
      },
    }
  );

  // Initialize form fields from loaded or initial data
  useEffect(() => {
    if (shouldAutoLoad && !isLoadingObject && objectData) {
      // Data was just loaded, update form fields
      const data = objectData.data ? fromKVObjectResponseData(objectData.data) : {};
      if (Object.keys(data).length > 0) {
        setFormFields(
          Object.entries(data).map(([key, value]) => ({
            key,
            value: typeof value === "string" ? value : JSON.stringify(value),
          }))
        );
        setJsonValue(JSON.stringify(data, null, 2));
      } else {
        setFormFields([{ key: "", value: "" }]);
        setJsonValue("{}");
      }
    } else if (!shouldAutoLoad && Object.keys(initialData).length > 0) {
      // Using initial data, update form fields
      setFormFields(
        Object.entries(initialData).map(([key, value]) => ({
          key,
          value: typeof value === "string" ? value : JSON.stringify(value),
        }))
      );
      setJsonValue(JSON.stringify(initialData, null, 2));
    } else if (!shouldAutoLoad && (Object.keys(initialData).length === 0 || isCreateMode)) {
      // Empty initial data or create mode - start with empty form
      setFormFields([{ key: "", value: "" }]);
      setJsonValue("{}");
    }
  }, [shouldAutoLoad, isLoadingObject, objectData, initialData, isCreateMode]);

  const handleModeChange = (
    _event: React.MouseEvent<HTMLElement>,
    newMode: ObjectEditorMode | null
  ) => {
    if (newMode) {
      // When switching to JSON mode, convert form fields to JSON
      if (newMode === "json" && formFields.length > 0) {
        try {
          const obj: Record<string, unknown> = {};
          formFields.forEach((field) => {
            if (field.key) {
              // Try to parse value as JSON, fallback to string
              try {
                obj[field.key] = JSON.parse(field.value);
              } catch {
                obj[field.key] = field.value;
              }
            }
          });
          setJsonValue(JSON.stringify(obj, null, 2));
          setJsonError(null);
        } catch {
          setJsonError("Failed to convert form to JSON");
        }
      }
      // When switching to form mode, parse JSON to form fields
      else if (newMode === "form" && jsonValue) {
        try {
          const parsed = JSON.parse(jsonValue);
          if (typeof parsed === "object" && parsed !== null && !Array.isArray(parsed)) {
            setFormFields(
              Object.entries(parsed).map(([key, value]) => ({
                key,
                value: typeof value === "string" ? value : JSON.stringify(value),
              }))
            );
            setJsonError(null);
          } else {
            setJsonError("JSON must be an object");
          }
        } catch (e) {
          setJsonError("Invalid JSON format");
        }
      }
      setMode(newMode);
    }
  };

  const handleAddField = () => {
    setFormFields([...formFields, { key: "", value: "" }]);
  };

  const handleRemoveField = (index: number) => {
    setFormFields(formFields.filter((_, i) => i !== index));
  };

  const handleFieldChange = (index: number, field: "key" | "value", newValue: string) => {
    const updated = [...formFields];
    updated[index] = { ...updated[index], [field]: newValue };
    setFormFields(updated);
  };

  const handleJsonChange = (value: string) => {
    setJsonValue(value);
    // Validate JSON
    try {
      const parsed = JSON.parse(value);
      if (typeof parsed === "object" && parsed !== null && !Array.isArray(parsed)) {
        setJsonError(null);
      } else {
        setJsonError("JSON must be an object");
      }
    } catch {
      setJsonError("Invalid JSON format");
    }
  };

  // Validate prefix in create mode
  useEffect(() => {
    if (isCreateMode && prefix) {
      const normalized = normalizePath(prefix);
      const validation = validateKVPath(normalized, false);
      if (!validation.isValid) {
        setPrefixError(validation.error || validation.warning || null);
      } else {
        setPrefixError(null);
      }
    } else {
      setPrefixError(null);
    }
  }, [prefix, isCreateMode]);

  const handleSave = async () => {
    // Validate prefix in create mode
    if (isCreateMode) {
      const normalized = normalizePath(prefix);
      const validation = validateKVPath(normalized, true);
      if (!validation.isValid) {
        setPrefixError(validation.error || "Invalid prefix");
        toast.error(validation.error || "Invalid prefix");
        return;
      }
    }

    try {
      let data: Record<string, unknown>;

      if (mode === "form") {
        // Convert form fields to object
        data = {};
        formFields.forEach((field) => {
          if (field.key) {
            // Try to parse value as JSON, fallback to string
            try {
              data[field.key] = JSON.parse(field.value);
            } catch {
              data[field.key] = field.value;
            }
          }
        });
      } else {
        // Parse JSON
        if (jsonError) {
          toast.error("Please fix JSON errors before saving");
          return;
        }
        const parsed = JSON.parse(jsonValue);
        if (typeof parsed === "object" && parsed !== null && !Array.isArray(parsed)) {
          data = parsed;
        } else {
          toast.error("JSON must be an object");
          return;
        }
      }

      const normalizedPrefix = normalizePath(prefix);
      await onSave(normalizedPrefix, data);
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "Failed to save object"
      );
    }
  };

  const hasEmptyFields = formFields.some((f) => !f.key || !f.value);

  // Show loading state with skeleton
  if (isLoadingObject && shouldAutoLoad) {
    return (
      <Card>
        <CardContent>
          <Box sx={{ py: 2 }}>
            <Skeleton variant="text" width="40%" height={32} sx={{ mb: 2 }} />
            <Skeleton variant="rectangular" height={300} sx={{ mb: 2, borderRadius: 1 }} />
            <Box sx={{ display: "flex", gap: 1, justifyContent: "flex-end" }}>
              <Skeleton variant="rectangular" width={100} height={36} sx={{ borderRadius: 1 }} />
              <Skeleton variant="rectangular" width={100} height={36} sx={{ borderRadius: 1 }} />
            </Box>
          </Box>
        </CardContent>
      </Card>
    );
  }

  // Show error state
  if (objectError && shouldAutoLoad) {
    const errorMessage = objectError instanceof Error 
      ? objectError.message 
      : typeof objectError === "object" && objectError !== null && "detail" in objectError
      ? String(objectError.detail)
      : "Unknown error";
    
    return (
      <Card>
        <CardContent>
          <Alert severity="error" sx={{ mb: 2 }}>
            <Typography variant="body2" fontWeight="medium" gutterBottom>
              Failed to load object data
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {errorMessage}
            </Typography>
          </Alert>
          <Button onClick={onCancel} variant="outlined" fullWidth>
            Cancel
          </Button>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardContent>
        <Box
          sx={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            mb: 2,
          }}
        >
          <Typography variant="h6" component="h2">
            {isCreateMode ? "Create Object" : "Edit Object"}
          </Typography>
        </Box>

        {isCreateMode && (
          <Box sx={{ mb: 2 }}>
            <TextField
              fullWidth
              label="Prefix"
              value={prefix}
              onChange={(e) => setPrefix(e.target.value)}
              disabled={isReadOnly || isSaving}
              size="small"
              error={!!prefixError}
              helperText={
                prefixError ||
                "Prefix where the object will be stored (e.g., config/app)"
              }
              aria-describedby={prefixError ? "prefix-error" : "prefix-help"}
            />
          </Box>
        )}

        <Box
          sx={{
            display: "flex",
            justifyContent: "flex-end",
            alignItems: "center",
            mb: 2,
          }}
        >
          <ToggleButtonGroup
            value={mode}
            exclusive
            onChange={handleModeChange}
            size="small"
            aria-label="editor mode"
          >
            <ToggleButton value="form" aria-label="form mode">
              <ListIcon fontSize="small" sx={{ mr: 1 }} />
              Form
            </ToggleButton>
            <ToggleButton value="json" aria-label="json mode">
              <CodeIcon fontSize="small" sx={{ mr: 1 }} />
              JSON
            </ToggleButton>
          </ToggleButtonGroup>
        </Box>

        {mode === "form" ? (
          <Box>
            <Stack spacing={2}>
              {formFields.map((field, index) => (
                <Box
                  key={index}
                  sx={{
                    display: "flex",
                    gap: 1,
                    alignItems: "flex-start",
                  }}
                >
                  <TextField
                    label="Key"
                    value={field.key}
                    onChange={(e) =>
                      handleFieldChange(index, "key", e.target.value)
                    }
                    disabled={isReadOnly}
                    size="small"
                    sx={{ flex: 1 }}
                    aria-label={`Field ${index + 1} key`}
                  />
                  <TextField
                    label="Value"
                    value={field.value}
                    onChange={(e) =>
                      handleFieldChange(index, "value", e.target.value)
                    }
                    disabled={isReadOnly}
                    size="small"
                    sx={{ flex: 2 }}
                    multiline
                    minRows={1}
                    aria-label={`Field ${index + 1} value`}
                  />
                  {!isReadOnly && (
                    <IconButton
                      onClick={() => handleRemoveField(index)}
                      color="error"
                      size="small"
                      sx={{ mt: 0.5 }}
                      aria-label={`Remove field ${index + 1}`}
                    >
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  )}
                </Box>
              ))}
            </Stack>

            {!isReadOnly && (
              <Button
                startIcon={<AddIcon />}
                onClick={handleAddField}
                variant="outlined"
                size="small"
                sx={{ mt: 2 }}
                aria-label="Add new field"
              >
                Add Field
              </Button>
            )}

            {hasEmptyFields && (
              <Alert severity="warning" sx={{ mt: 2 }} role="alert">
                Some fields have empty keys or values. They will be skipped on save.
              </Alert>
            )}
          </Box>
        ) : (
          <Box>
            <KVJsonEditor
              value={jsonValue}
              encoding={KVPutRequestEncoding.utf8}
              onChange={handleJsonChange}
              onEncodingChange={() => {}}
              readOnly={isReadOnly}
              error={jsonError || undefined}
              height={400}
            />
          </Box>
        )}

        <Stack direction="row" spacing={1} sx={{ mt: 3 }} justifyContent="flex-end">
          <Button
            startIcon={<CancelIcon />}
            onClick={onCancel}
            disabled={isSaving}
            variant="outlined"
            aria-label="Cancel editing"
          >
            Cancel
          </Button>
          {!isReadOnly && (
            <Button
              startIcon={<SaveIcon />}
              onClick={handleSave}
              disabled={isSaving || (mode === "json" && !!jsonError) || (isCreateMode && !!prefixError)}
              variant="contained"
              aria-label="Save object"
            >
              {isSaving ? "Saving..." : "Save"}
            </Button>
          )}
        </Stack>
      </CardContent>
    </Card>
  );
}

