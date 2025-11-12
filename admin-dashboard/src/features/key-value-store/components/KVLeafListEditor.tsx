/**
 * Component for editing KV Leaf Lists (comma-separated lists)
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
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Switch,
  FormControlLabel,
  Divider,
} from "@mui/material";
import {
  Save as SaveIcon,
  Cancel as CancelIcon,
  Add as AddIcon,
  Delete as DeleteIcon,
  Edit as EditIcon,
  ContentPaste as PasteIcon,
} from "@mui/icons-material";
import { toast } from "@lib/toast/toast";
import { useGetKVEntry } from "@lib/api/generated/key-value-store/key-value-store";
import { 
  parseLeafList, 
  formatLeafList, 
  findDuplicates, 
  removeDuplicates,
  isValidLeafList,
} from "../utils/leafListParser";
import { decodeBase64, validateKVPath } from "../types";
import type { KVPutRequest } from "@lib/api/models";
import type { PathValidationResult } from "../types";
import {
  FileUpload as ImportIcon,
  FileDownload as ExportIcon,
  CheckCircle as CheckIcon,
  Warning as WarningIcon,
} from "@mui/icons-material";

export interface KVLeafListEditorProps {
  serviceId: string;
  path: string;
  /** Current prefix for auto-prepending to key in create mode */
  currentPrefix?: string;
  /** Initial elements (for edit mode) */
  initialElements?: string[];
  /** Callback when save is triggered */
  onSave: (path: string, data: KVPutRequest) => Promise<void>;
  /** Callback when cancel is triggered */
  onCancel: () => void;
  /** Whether editor is read-only */
  isReadOnly?: boolean;
  /** Whether save is in progress */
  isSaving?: boolean;
  /** Whether this is create mode (don't auto-load) */
  isCreateMode?: boolean;
}

export function KVLeafListEditor({
  serviceId,
  path: initialPath,
  currentPrefix = "",
  initialElements,
  onSave,
  onCancel,
  isReadOnly = false,
  isSaving = false,
  isCreateMode = false,
}: KVLeafListEditorProps) {
  // Extract key from path for create mode
  const getKeyFromPath = (fullPath: string): string => {
    if (currentPrefix && fullPath.startsWith(currentPrefix + "/")) {
      return fullPath.slice(currentPrefix.length + 1);
    }
    return fullPath;
  };

  const [key, setKey] = useState(isCreateMode ? getKeyFromPath(initialPath) : "");
  const [keyValidation, setKeyValidation] = useState<PathValidationResult>({
    isValid: true,
  });
  const [elements, setElements] = useState<string[]>(initialElements || []);
  const [newElement, setNewElement] = useState("");
  const [editingIndex, setEditingIndex] = useState<number | null>(null);
  const [editValue, setEditValue] = useState("");
  const [pasteDialogOpen, setPasteDialogOpen] = useState(false);
  const [pasteText, setPasteText] = useState("");
  const [autoParse, setAutoParse] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [duplicates, setDuplicates] = useState<string[]>([]);
  const [showDuplicates, setShowDuplicates] = useState(false);

  // Initialize key in create mode
  useEffect(() => {
    if (isCreateMode) {
      const extractedKey = getKeyFromPath(initialPath);
      setKey(extractedKey);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isCreateMode, initialPath, currentPrefix]);

  // Validate key in create mode
  useEffect(() => {
    if (isCreateMode && key) {
      const fullPath = currentPrefix
        ? `${currentPrefix}/${key}`
        : key;
      const validation = validateKVPath(fullPath, false);
      setKeyValidation(validation);
    } else {
      setKeyValidation({ isValid: true });
    }
  }, [key, isCreateMode, currentPrefix]);


  // Load existing entry if not in create mode
  const {
    data: entryData,
    isLoading: isLoadingEntry,
  } = useGetKVEntry(
    serviceId,
    initialPath,
    {},
    {
      query: {
        enabled: !isCreateMode && !!serviceId && !!initialPath,
        staleTime: 10_000,
      },
    }
  );

  // Initialize elements from entry data
  useEffect(() => {
    // Check if entryData is a Blob (raw response) or KVEntryResponse
    if (!isCreateMode && entryData && !(entryData instanceof Blob) && entryData.valueBase64 && elements.length === 0) {
      try {
        const value = decodeBase64(entryData.valueBase64);
        const parsed = parseLeafList(value);
        setElements(parsed);
        // Check for duplicates
        const dupes = findDuplicates(parsed);
        if (dupes.length > 0) {
          setDuplicates(dupes);
          setShowDuplicates(true);
        }
      } catch (e) {
        console.error("Failed to parse LEAF_LIST value:", e);
        setError("Failed to parse existing LEAF_LIST value");
      }
    }
  }, [entryData, isCreateMode, elements.length]);

  // Check for duplicates whenever elements change
  useEffect(() => {
    const dupes = findDuplicates(elements);
    setDuplicates(dupes);
    if (dupes.length > 0) {
      setShowDuplicates(true);
    }
  }, [elements]);

  // Initialize from initialElements prop
  useEffect(() => {
    if (initialElements && initialElements.length > 0) {
      setElements(initialElements);
    }
  }, [initialElements]);

  const handleAddElement = () => {
    if (newElement.trim()) {
      // Check for duplicates
      const trimmed = newElement.trim();
      if (elements.includes(trimmed)) {
        toast.warning("Element already exists");
        return;
      }
      setElements([...elements, trimmed]);
      setNewElement("");
      setError(null);
    }
  };

  const handleRemoveDuplicates = () => {
    const unique = removeDuplicates(elements);
    const removedCount = elements.length - unique.length;
    setElements(unique);
    setShowDuplicates(false);
    toast.success(`Removed ${removedCount} duplicate(s)`);
  };

  const handleExportJSON = () => {
    const exportData = {
      elements,
      count: elements.length,
      timestamp: new Date().toISOString(),
    };
    const blob = new Blob([JSON.stringify(exportData, null, 2)], {
      type: "application/json",
    });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    const pathForExport = isCreateMode && currentPrefix && key
      ? `${currentPrefix}/${key}`
      : initialPath;
    a.href = url;
    a.download = `leaf-list-${pathForExport.replace(/\//g, "-")}-${Date.now()}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    toast.success("Leaf list exported to JSON");
  };

  const handleExportCSV = () => {
    const csv = elements.join("\n");
    const blob = new Blob([csv], { type: "text/csv" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    const pathForExport = isCreateMode && currentPrefix && key
      ? `${currentPrefix}/${key}`
      : initialPath;
    a.href = url;
    a.download = `leaf-list-${pathForExport.replace(/\//g, "-")}-${Date.now()}.csv`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    toast.success("Leaf list exported to CSV");
  };

  const handleImportJSON = () => {
    const input = document.createElement("input");
    input.type = "file";
    input.accept = "application/json";
    input.onchange = (e) => {
      const file = (e.target as HTMLInputElement).files?.[0];
      if (!file) return;

      const reader = new FileReader();
      reader.onload = (event) => {
        try {
          const content = event.target?.result as string;
          const imported = JSON.parse(content);
          
          if (imported.elements && Array.isArray(imported.elements)) {
            // Merge with existing elements, removing duplicates
            const merged = removeDuplicates([...elements, ...imported.elements]);
            setElements(merged);
            toast.success(`Imported ${imported.elements.length} element(s)`);
          } else if (Array.isArray(imported)) {
            // If it's just an array
            const merged = removeDuplicates([...elements, ...imported]);
            setElements(merged);
            toast.success(`Imported ${imported.length} element(s)`);
          } else {
            toast.error("Invalid JSON format");
          }
        } catch (error) {
          console.error("Failed to import JSON:", error);
          toast.error("Failed to import JSON: Invalid format");
        }
      };
      reader.readAsText(file);
    };
    input.click();
  };

  const handleDeleteElement = (index: number) => {
    setElements(elements.filter((_, i) => i !== index));
  };

  const handleStartEdit = (index: number) => {
    setEditingIndex(index);
    setEditValue(elements[index]);
  };

  const handleSaveEdit = () => {
    if (editingIndex !== null && editValue.trim()) {
      const updated = [...elements];
      updated[editingIndex] = editValue.trim();
      setElements(updated);
      setEditingIndex(null);
      setEditValue("");
    }
  };

  const handleCancelEdit = () => {
    setEditingIndex(null);
    setEditValue("");
  };

  const handlePasteParse = () => {
    if (!pasteText.trim()) {
      toast.warning("Please enter text to parse");
      return;
    }

    try {
      // Validate the pasted text
      if (!isValidLeafList(pasteText)) {
        toast.warning("Invalid format. Please check your input.");
        return;
      }

      const parsed = parseLeafList(pasteText);
      if (parsed.length === 0) {
        toast.warning("No valid elements found in pasted text");
        return;
      }

      // Merge with existing elements (avoid duplicates)
      const merged = removeDuplicates([...elements, ...parsed]);
      const addedCount = merged.length - elements.length;
      setElements(merged);
      setPasteText("");
      setPasteDialogOpen(false);
      toast.success(`Parsed ${parsed.length} element(s), added ${addedCount} new`);
    } catch (e) {
      setError("Failed to parse pasted text");
      toast.error("Failed to parse pasted text");
    }
  };

  const handlePasteTextChange = (value: string) => {
    setPasteText(value);
    if (autoParse && value.trim()) {
      // Auto-parse on paste
      try {
        const parsed = parseLeafList(value);
        if (parsed.length > 0) {
          const merged = [...new Set([...elements, ...parsed])];
          setElements(merged);
          setPasteText("");
          toast.success(`Auto-parsed ${parsed.length} element(s)`);
        }
      } catch (e) {
        // Silent fail for auto-parse
      }
    }
  };

  const handleSave = async () => {
    setError(null);

    // Construct full path from key and currentPrefix in create mode
    let fullPath: string;
    if (isCreateMode) {
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
        toast.error(strictValidation.error || "Invalid key");
        return;
      }
    } else {
      fullPath = initialPath;
    }

    try {
      const formatted = formatLeafList(elements);
      const putRequest: KVPutRequest = {
        value: formatted,
        encoding: "utf8",
        flags: 3, // LEAF_LIST flag - explicitly set to 3
        cas: entryData && !(entryData instanceof Blob) ? entryData.modifyIndex : undefined,
      };

      // Validate flags are set correctly before saving
      if (putRequest.flags !== 3) {
        const errorMsg = "Invalid flags for LEAF_LIST. Expected 3, got " + putRequest.flags;
        setError(errorMsg);
        toast.error(errorMsg);
        return;
      }

      await onSave(fullPath, putRequest);
    } catch (err) {
      const errorMessage =
        err && typeof err === "object" && "message" in err
          ? (err.message as string)
          : "Failed to save LEAF_LIST";
      setError(errorMessage);
      toast.error(errorMessage);
    }
  };

  const isLoading = isLoadingEntry && !isCreateMode;

  return (
    <Card>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2 }}>
          {isCreateMode ? "Create Leaf List" : "Edit Leaf List"}
        </Typography>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
            {error}
          </Alert>
        )}

        {showDuplicates && duplicates.length > 0 && (
          <Alert 
            severity="warning" 
            sx={{ mb: 2 }} 
            icon={<WarningIcon />}
            action={
              <Button 
                color="inherit" 
                size="small" 
                onClick={handleRemoveDuplicates}
                startIcon={<CheckIcon />}
              >
                Remove Duplicates
              </Button>
            }
            onClose={() => setShowDuplicates(false)}
          >
            Found {duplicates.length} duplicate element(s). Click to remove.
          </Alert>
        )}

        {isLoading ? (
          <Box sx={{ textAlign: "center", py: 4 }}>
            <Typography variant="body2" color="text.secondary">
              Loading...
            </Typography>
          </Box>
        ) : (
          <>
            {/* Key input (create mode) or Path display (edit mode) */}
            <Box sx={{ mb: 3 }}>
              {isCreateMode ? (
                <>
                  <TextField
                    fullWidth
                    label="Key"
                    value={key}
                    onChange={(e) => setKey(e.target.value)}
                    disabled={isReadOnly || isSaving}
                    size="small"
                    error={!keyValidation.isValid}
                    helperText={
                      keyValidation.error ||
                      keyValidation.warning ||
                      (currentPrefix
                        ? `Key (will be saved as: ${currentPrefix}/${key || "..."})`
                        : "Enter the key (can contain '/' characters, e.g., key/subkey)")
                    }
                  />
                </>
              ) : (
                <>
                  <Typography variant="caption" color="text.secondary">
                    Path
                  </Typography>
                  <Typography variant="body2">{initialPath}</Typography>
                </>
              )}
            </Box>

            {/* Elements list */}
            <Box sx={{ mb: 3 }}>
              <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 2 }} flexWrap="wrap">
                <Typography variant="subtitle2">
                  Elements ({elements.length})
                </Typography>
                {!isReadOnly && (
                  <>
                    <Button
                      size="small"
                      startIcon={<PasteIcon />}
                      onClick={() => setPasteDialogOpen(true)}
                      disabled={isSaving}
                      variant="outlined"
                    >
                      Paste/Parse
                    </Button>
                    <Button
                      size="small"
                      startIcon={<ExportIcon />}
                      onClick={handleExportJSON}
                      disabled={isSaving || elements.length === 0}
                      variant="outlined"
                    >
                      Export JSON
                    </Button>
                    <Button
                      size="small"
                      startIcon={<ExportIcon />}
                      onClick={handleExportCSV}
                      disabled={isSaving || elements.length === 0}
                      variant="outlined"
                    >
                      Export CSV
                    </Button>
                    <Button
                      size="small"
                      startIcon={<ImportIcon />}
                      onClick={handleImportJSON}
                      disabled={isSaving}
                      variant="outlined"
                    >
                      Import
                    </Button>
                  </>
                )}
              </Stack>

              {elements.length === 0 ? (
                <Alert severity="info">No elements. Add elements below or paste/parse a comma-separated string.</Alert>
              ) : (
                <Stack spacing={1}>
                  {elements.map((element, index) => (
                    <Box
                      key={index}
                      sx={{
                        display: "flex",
                        alignItems: "center",
                        gap: 1,
                        p: 1,
                        border: "1px solid",
                        borderColor: "divider",
                        borderRadius: 1,
                      }}
                    >
                      {editingIndex === index ? (
                        <>
                          <TextField
                            size="small"
                            value={editValue}
                            onChange={(e) => setEditValue(e.target.value)}
                            onKeyDown={(e) => {
                              if (e.key === "Enter") {
                                handleSaveEdit();
                              } else if (e.key === "Escape") {
                                handleCancelEdit();
                              }
                            }}
                            autoFocus
                            fullWidth
                            sx={{ flex: 1 }}
                          />
                          <IconButton
                            size="small"
                            onClick={handleSaveEdit}
                            color="primary"
                            aria-label="Save edit"
                          >
                            <SaveIcon fontSize="small" />
                          </IconButton>
                          <IconButton
                            size="small"
                            onClick={handleCancelEdit}
                            aria-label="Cancel edit"
                          >
                            <CancelIcon fontSize="small" />
                          </IconButton>
                        </>
                      ) : (
                        <>
                          <Chip
                            label={element}
                            size="small"
                            sx={{ flex: 1, justifyContent: "flex-start" }}
                            aria-label={`Element ${index + 1}: ${element}`}
                          />
                          {!isReadOnly && (
                            <IconButton
                              size="small"
                              onClick={() => handleStartEdit(index)}
                              aria-label={`Edit element ${index + 1}`}
                            >
                              <EditIcon fontSize="small" />
                            </IconButton>
                          )}
                          {!isReadOnly && (
                            <IconButton
                              size="small"
                              onClick={() => handleDeleteElement(index)}
                              color="error"
                              aria-label={`Delete element ${index + 1}`}
                            >
                              <DeleteIcon fontSize="small" />
                            </IconButton>
                          )}
                        </>
                      )}
                    </Box>
                  ))}
                </Stack>
              )}
            </Box>

            <Divider sx={{ my: 2 }} />

            {/* Add new element */}
            {!isReadOnly && (
              <Box sx={{ mb: 3 }}>
                <Stack direction="row" spacing={1}>
                  <TextField
                    fullWidth
                    size="small"
                    placeholder="Enter new element"
                    value={newElement}
                    onChange={(e) => setNewElement(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") {
                        handleAddElement();
                      }
                    }}
                    disabled={isSaving}
                  />
                  <Button
                    variant="contained"
                    startIcon={<AddIcon />}
                    onClick={handleAddElement}
                    disabled={!newElement.trim() || isSaving}
                    aria-label="Add element"
                  >
                    Add
                  </Button>
                </Stack>
              </Box>
            )}

            {/* Action buttons */}
            {!isReadOnly && (
              <Stack direction="row" spacing={2} justifyContent="flex-end">
                <Button
                  variant="outlined"
                  startIcon={<CancelIcon />}
                  onClick={onCancel}
                  disabled={isSaving}
                >
                  Cancel
                </Button>
                <Button
                  variant="contained"
                  startIcon={<SaveIcon />}
                  onClick={handleSave}
                  disabled={isSaving}
                >
                  {isSaving ? "Saving..." : "Save"}
                </Button>
              </Stack>
            )}
          </>
        )}

        {/* Paste/Parse Dialog */}
        <Dialog
          open={pasteDialogOpen}
          onClose={() => {
            setPasteDialogOpen(false);
            setPasteText("");
          }}
          maxWidth="sm"
          fullWidth
        >
          <DialogTitle>Paste/Parse Comma-Separated List</DialogTitle>
          <DialogContent>
            <Stack spacing={2} sx={{ mt: 1 }}>
              <FormControlLabel
                control={
                  <Switch
                    checked={autoParse}
                    onChange={(e) => setAutoParse(e.target.checked)}
                  />
                }
                label="Auto-parse on paste"
              />
              <TextField
                multiline
                rows={6}
                fullWidth
                placeholder="Paste comma-separated values here (e.g., item1, item2, item3)"
                value={pasteText}
                onChange={(e) => handlePasteTextChange(e.target.value)}
                helperText="Elements will be trimmed and empty elements will be filtered out"
              />
            </Stack>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => {
              setPasteDialogOpen(false);
              setPasteText("");
            }}>
              Cancel
            </Button>
            <Button
              variant="contained"
              onClick={handlePasteParse}
              disabled={!pasteText.trim()}
            >
              Parse
            </Button>
          </DialogActions>
        </Dialog>
      </CardContent>
    </Card>
  );
}

