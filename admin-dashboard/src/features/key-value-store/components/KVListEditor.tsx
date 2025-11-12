/**
 * Component for editing KV lists (ordered items with manifest)
 */

import { useState, useEffect, useMemo } from "react";
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
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from "@mui/material";
import {
  Save as SaveIcon,
  Cancel as CancelIcon,
  Add as AddIcon,
  Delete as DeleteIcon,
  Edit as EditIcon,
  DragIndicator as DragIcon,
} from "@mui/icons-material";
import {
  DndContext,
  closestCenter,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
} from "@dnd-kit/core";
import {
  arrayMove,
  SortableContext,
  sortableKeyboardCoordinates,
  useSortable,
  verticalListSortingStrategy,
} from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { toast } from "@lib/toast/toast";
import { useGetKVList, type GetKVListParams } from "../hooks";
import { Skeleton, Tooltip, Checkbox, Collapse, Divider } from "@mui/material";
import {
  fromGeneratedKVListItemArray,
  fromKVListManifestMetadata,
  type UIListItem,
} from "../utils/typeAdapters";
import { normalizePath, validateKVPath } from "../types";
import { generateUniqueItemId } from "../utils/itemIdGenerator";
import {
  ContentCopy as DuplicateIcon,
  FileUpload as ImportIcon,
  FileDownload as ExportIcon,
  SelectAll as SelectAllIcon,
  Deselect as DeselectIcon,
} from "@mui/icons-material";

// UI representation of manifest (with Record<string, unknown> metadata)
export type UIListManifest = {
  order?: string[];
  version?: number;
  etag?: string | null;
  metadata?: Record<string, unknown>;
};

export interface KVListEditorProps {
  serviceId: string;
  prefix: string;
  /** Current prefix for auto-prepending to key in create mode */
  currentPrefix?: string;
  /** Initial list items */
  initialItems?: UIListItem[];
  /** Initial manifest */
  initialManifest?: UIListManifest;
  /** Callback when save is triggered */
  onSave: (prefix: string, items: UIListItem[], manifest: UIListManifest, deletes: string[]) => Promise<void>;
  /** Callback when cancel is triggered */
  onCancel: () => void;
  /** Whether editor is read-only */
  isReadOnly?: boolean;
  /** Whether save is in progress */
  isSaving?: boolean;
  /** Whether this is create mode (don't auto-load) */
  isCreateMode?: boolean;
}

interface SortableRowProps {
  item: UIListItem;
  index: number;
  onEdit: (item: UIListItem) => void;
  onDelete: (id: string) => void;
  onDuplicate: (item: UIListItem) => void;
  isReadOnly?: boolean;
  isSelected?: boolean;
  onSelect?: (id: string, selected: boolean) => void;
  showSelection?: boolean;
}

function SortableRow({
  item,
  index,
  onEdit,
  onDelete,
  onDuplicate,
  isReadOnly = false,
  isSelected = false,
  onSelect,
  showSelection = false,
}: SortableRowProps) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: item.id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  };

  const fieldCount = Object.keys(item.data).length;
  const fieldPreview = Object.entries(item.data)
    .slice(0, 2)
    .map(([key, value]) => `${key}: ${typeof value === "string" ? value : JSON.stringify(value)}`)
    .join(", ");
  const hasMoreFields = fieldCount > 2;

  return (
    <TableRow 
      ref={setNodeRef} 
      style={style}
      selected={isSelected}
      sx={{
        "&:hover": {
          backgroundColor: "action.hover",
        },
      }}
    >
      {showSelection && onSelect && (
        <TableCell padding="checkbox">
          <Checkbox
            checked={isSelected}
            onChange={(e) => onSelect(item.id, e.target.checked)}
            aria-label={`Select item ${index + 1}`}
          />
        </TableCell>
      )}
      <TableCell>
        <Tooltip title="Drag to reorder">
          <IconButton
            size="small"
            {...attributes}
            {...listeners}
            disabled={isReadOnly}
            sx={{ cursor: isReadOnly ? "default" : "grab" }}
            aria-label={`Drag to reorder item ${index + 1}`}
          >
            <DragIcon fontSize="small" />
          </IconButton>
        </Tooltip>
      </TableCell>
      <TableCell>
        <Typography variant="body2" fontWeight="medium">
          {index + 1}
        </Typography>
      </TableCell>
      <TableCell>
        <Tooltip title={`Item ID: ${item.id}`} arrow>
          <Box>
            <Typography 
              variant="body2" 
              sx={{ 
                maxWidth: { xs: 200, sm: 400 },
                overflow: "hidden",
                textOverflow: "ellipsis",
                whiteSpace: "nowrap",
              }}
            >
              {fieldPreview}
              {hasMoreFields && ` (+${fieldCount - 2} more)`}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              {fieldCount} field{fieldCount !== 1 ? "s" : ""}
            </Typography>
          </Box>
        </Tooltip>
      </TableCell>
      <TableCell>
        <Stack direction="row" spacing={0.5}>
          <Tooltip title="Edit item">
            <IconButton
              size="small"
              onClick={() => onEdit(item)}
              disabled={isReadOnly}
              aria-label={`Edit item ${index + 1}`}
            >
              <EditIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          {!isReadOnly && (
            <>
              <Tooltip title="Duplicate item">
                <IconButton
                  size="small"
                  onClick={() => onDuplicate(item)}
                  aria-label={`Duplicate item ${index + 1}`}
                >
                  <DuplicateIcon fontSize="small" />
                </IconButton>
              </Tooltip>
              <Tooltip title="Delete item">
                <IconButton
                  size="small"
                  onClick={() => onDelete(item.id)}
                  color="error"
                  aria-label={`Delete item ${index + 1}`}
                >
                  <DeleteIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            </>
          )}
        </Stack>
      </TableCell>
    </TableRow>
  );
}

interface ItemEditorDialogProps {
  open: boolean;
  item: UIListItem | null;
  onClose: () => void;
  onSave: (item: UIListItem) => void;
  isReadOnly?: boolean;
  existingItems?: UIListItem[];
}

function ItemEditorDialog({
  open,
  item,
  onClose,
  onSave,
  isReadOnly = false,
  existingItems = [],
}: ItemEditorDialogProps) {
  const [id, setId] = useState("");
  const [fields, setFields] = useState<Array<{ key: string; value: string }>>([]);
  const [showItemId, setShowItemId] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<number, string>>({});

  const isNewItem = !item;

  useEffect(() => {
    if (item) {
      // Editing existing item: use existing itemId
      setId(item.id);
      setFields(
        Object.entries(item.data).map(([key, value]) => ({
          key,
          value: typeof value === "string" ? value : JSON.stringify(value),
        }))
      );
      setShowItemId(false); // Hide itemId by default for existing items
    } else {
      // Creating new item: auto-generate itemId
      const newId = generateUniqueItemId(existingItems);
      setId(newId);
      setFields([{ key: "", value: "" }]);
      setShowItemId(false); // Hide itemId for new items
    }
    setFieldErrors({});
  }, [item, open, existingItems]);

  const handleAddField = () => {
    setFields([...fields, { key: "", value: "" }]);
  };

  const handleRemoveField = (index: number) => {
    setFields(fields.filter((_, i) => i !== index));
    // Remove error for this field
    const newErrors = { ...fieldErrors };
    delete newErrors[index];
    setFieldErrors(newErrors);
  };

  const handleFieldChange = (
    index: number,
    field: "key" | "value",
    newValue: string
  ) => {
    const updated = [...fields];
    updated[index] = { ...updated[index], [field]: newValue };
    setFields(updated);
    
    // Validate field
    if (field === "key" && newValue.trim()) {
      // Check for duplicate keys
      const duplicateIndex = updated.findIndex(
        (f, i) => i !== index && f.key === newValue.trim()
      );
      if (duplicateIndex !== -1) {
        setFieldErrors({ ...fieldErrors, [index]: "Duplicate key" });
      } else {
        const newErrors = { ...fieldErrors };
        delete newErrors[index];
        setFieldErrors(newErrors);
      }
    } else if (field === "key" && !newValue.trim()) {
      const newErrors = { ...fieldErrors };
      delete newErrors[index];
      setFieldErrors(newErrors);
    }
  };

  const handleSave = () => {
    // Validate fields
    const errors: Record<number, string> = {};
    fields.forEach((field, index) => {
      if (!field.key.trim()) {
        errors[index] = "Key is required";
      } else {
        // Check for duplicate keys
        const duplicateIndex = fields.findIndex(
          (f, i) => i !== index && f.key === field.key.trim()
        );
        if (duplicateIndex !== -1) {
          errors[index] = "Duplicate key";
        }
      }
    });

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      toast.error("Please fix field errors before saving");
      return;
    }

    // Build data object
    const data: Record<string, unknown> = {};
    fields.forEach((field) => {
      if (field.key.trim()) {
        try {
          // Try to parse as JSON
          data[field.key.trim()] = JSON.parse(field.value);
        } catch {
          // Not valid JSON, use as string
          data[field.key.trim()] = field.value;
        }
      }
    });

    // Use auto-generated ID for new items, existing ID for edits
    const itemId = isNewItem ? generateUniqueItemId(existingItems) : id.trim();

    onSave({ id: itemId, data });
    onClose();
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" && (e.ctrlKey || e.metaKey)) {
      handleSave();
    } else if (e.key === "Escape") {
      onClose();
    }
  };

  return (
    <Dialog 
      open={open} 
      onClose={onClose} 
      maxWidth="md" 
      fullWidth
      aria-labelledby="item-editor-dialog-title"
      onKeyDown={handleKeyDown}
    >
      <DialogTitle id="item-editor-dialog-title">
        {item ? "Edit Item" : "Add New Item"}
        {!isNewItem && (
          <Typography variant="caption" color="text.secondary" sx={{ display: "block", mt: 0.5 }}>
            Item ID: {id}
          </Typography>
        )}
      </DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          {/* Item ID section - collapsible for existing items, hidden for new items */}
          {!isNewItem && (
            <Box>
              <Button
                size="small"
                onClick={() => setShowItemId(!showItemId)}
                sx={{ mb: 1 }}
                aria-label={showItemId ? "Hide item ID" : "Show item ID"}
              >
                {showItemId ? "Hide" : "Show"} Item ID
              </Button>
              <Collapse in={showItemId}>
                <TextField
                  label="Item ID"
                  value={id}
                  disabled
                  fullWidth
                  size="small"
                  helperText="Item ID is auto-generated and cannot be changed"
                  aria-describedby="item-id-readonly"
                />
              </Collapse>
            </Box>
          )}

          <Divider />

          <Typography variant="subtitle2" component="h3">
            Fields
          </Typography>
          {fields.length === 0 ? (
            <Alert severity="info">
              No fields yet. Add a field to get started.
            </Alert>
          ) : (
            fields.map((field, index) => (
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
                  error={!!fieldErrors[index]}
                  helperText={fieldErrors[index]}
                  required
                  aria-label={`Field ${index + 1} key`}
                  aria-required="true"
                  aria-invalid={!!fieldErrors[index]}
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
                  placeholder="Enter value (JSON will be auto-parsed)"
                  aria-label={`Field ${index + 1} value`}
                />
                {!isReadOnly && (
                  <Tooltip title="Remove field">
                    <IconButton
                      onClick={() => handleRemoveField(index)}
                      color="error"
                      size="small"
                      sx={{ mt: 0.5 }}
                      aria-label={`Remove field ${index + 1}`}
                    >
                      <DeleteIcon fontSize="small" />
                    </IconButton>
                  </Tooltip>
                )}
              </Box>
            ))
          )}

          {!isReadOnly && (
            <Button
              startIcon={<AddIcon />}
              onClick={handleAddField}
              variant="outlined"
              size="small"
              aria-label="Add new field"
            >
              Add Field
            </Button>
          )}

          <Typography variant="caption" color="text.secondary" sx={{ mt: 1 }}>
            Tip: Use Ctrl+Enter (Cmd+Enter on Mac) to save, Escape to cancel
          </Typography>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} aria-label="Cancel editing">
          Cancel
        </Button>
        {!isReadOnly && (
          <Button 
            onClick={handleSave} 
            variant="contained" 
            aria-label="Save item"
            disabled={fields.length === 0 || Object.keys(fieldErrors).length > 0}
          >
            Save
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
}

export function KVListEditor({
  serviceId,
  prefix: initialPrefix,
  currentPrefix = "",
  initialItems = [],
  initialManifest,
  onSave,
  onCancel,
  isReadOnly = false,
  isSaving = false,
  isCreateMode = false,
}: KVListEditorProps) {
  // Extract key from prefix for create mode
  const getKeyFromPrefix = (fullPrefix: string): string => {
    if (currentPrefix && fullPrefix.startsWith(currentPrefix + "/")) {
      return fullPrefix.slice(currentPrefix.length + 1);
    }
    return fullPrefix;
  };

  const [key, setKey] = useState(isCreateMode ? getKeyFromPrefix(initialPrefix) : "");
  const [prefix, setPrefix] = useState(initialPrefix);
  const [prefixError, setPrefixError] = useState<string | null>(null);
  
  // Auto-load data if initialItems is empty and not in create mode
  // Also auto-load if initialManifest is empty/minimal (order empty and version 0)
  const shouldAutoLoad = !isCreateMode && initialItems.length === 0 && (
    !initialManifest || 
    (initialManifest.order?.length === 0 && initialManifest.version === 0)
  );
  const params: GetKVListParams | undefined = shouldAutoLoad && serviceId && prefix
    ? { prefix, consistent: false, stale: false }
    : undefined;

  const { data: listData, isLoading: isLoadingList, error: listError } = useGetKVList(
    serviceId,
    params,
    {
      query: {
        enabled: shouldAutoLoad && !!serviceId && !!prefix,
        staleTime: 10_000,
      },
    }
  );

  // Convert loaded data to UI representation (memoized to avoid unnecessary recalculations)
  const loadedItems = useMemo(() => {
    return listData?.items ? fromGeneratedKVListItemArray(listData.items) : [];
  }, [listData?.items]);
  
  const loadedManifest = useMemo(() => {
    return listData?.manifest 
      ? {
          order: listData.manifest.order ?? [],
          version: listData.manifest.version ?? 0,
          etag: listData.manifest.etag ?? null,
          metadata: listData.manifest.metadata ? fromKVListManifestMetadata(listData.manifest.metadata) : undefined,
        }
      : undefined;
  }, [listData?.manifest]);

  // Use loaded data or initial data
  const effectiveItems = loadedItems.length > 0 ? loadedItems : initialItems;
  const effectiveManifest: UIListManifest = loadedManifest || initialManifest || { order: [], version: 0, etag: null };

  // Initialize state with effective items (will be updated by useEffect when data loads)
  const [items, setItems] = useState<UIListItem[]>(effectiveItems);
  const [deletedIds, setDeletedIds] = useState<string[]>([]);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<UIListItem | null>(null);
  const [selectedItems, setSelectedItems] = useState<Set<string>>(new Set());
  const [showSelection, setShowSelection] = useState(false);

  const sensors = useSensors(
    useSensor(PointerSensor),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    })
  );

  // Update items when data loads or initial items change
  useEffect(() => {
    if (shouldAutoLoad) {
      // Auto-load mode: update when data finishes loading
      if (!isLoadingList && listData) {
        // Data was just loaded successfully - update items with loaded data
        setItems(loadedItems);
        setDeletedIds([]);
      }
      // If still loading or no data yet, keep current state (don't reset)
    } else {
      // Not auto-loading: use initial items
      if (initialItems.length > 0) {
        setItems(initialItems);
        setDeletedIds([]);
      } else if (isCreateMode) {
        // Create mode: start with empty list
        setItems([]);
        setDeletedIds([]);
      }
    }
  }, [shouldAutoLoad, isLoadingList, listData, loadedItems, initialItems, isCreateMode]);

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;

    if (over && active.id !== over.id) {
      setItems((items) => {
        const oldIndex = items.findIndex((item) => item.id === active.id);
        const newIndex = items.findIndex((item) => item.id === over.id);
        return arrayMove(items, oldIndex, newIndex);
      });
    }
  };

  const handleAddItem = () => {
    setEditingItem(null);
    setEditDialogOpen(true);
  };

  const handleEditItem = (item: UIListItem) => {
    setEditingItem(item);
    setEditDialogOpen(true);
  };

  const handleDeleteItem = (id: string) => {
    setItems(items.filter((item) => item.id !== id));
    setDeletedIds([...deletedIds, id]);
    // Remove from selection if selected
    const newSelected = new Set(selectedItems);
    newSelected.delete(id);
    setSelectedItems(newSelected);
  };

  const handleDuplicateItem = (item: UIListItem) => {
    const newId = generateUniqueItemId(items);
    const duplicatedItem: UIListItem = {
      id: newId,
      data: { ...item.data },
    };
    // Insert after the original item
    const index = items.findIndex((i) => i.id === item.id);
    const newItems = [...items];
    newItems.splice(index + 1, 0, duplicatedItem);
    setItems(newItems);
    toast.success("Item duplicated");
  };


  const handleSelectItem = (id: string, selected: boolean) => {
    const newSelected = new Set(selectedItems);
    if (selected) {
      newSelected.add(id);
    } else {
      newSelected.delete(id);
    }
    setSelectedItems(newSelected);
  };

  const handleSelectAll = () => {
    if (selectedItems.size === items.length) {
      setSelectedItems(new Set());
    } else {
      setSelectedItems(new Set(items.map((item) => item.id)));
    }
  };

  const handleDeleteSelected = () => {
    const idsToDelete = Array.from(selectedItems);
    setItems(items.filter((item) => !selectedItems.has(item.id)));
    setDeletedIds([...deletedIds, ...idsToDelete]);
    setSelectedItems(new Set());
    toast.success(`Deleted ${idsToDelete.length} item(s)`);
  };

  const handleExportJSON = () => {
    const exportData = {
      items: items.map((item) => ({
        id: item.id,
        data: item.data,
      })),
      manifest: effectiveManifest,
    };
    const blob = new Blob([JSON.stringify(exportData, null, 2)], {
      type: "application/json",
    });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `kv-list-${prefix.replace(/\//g, "-")}-${Date.now()}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    toast.success("List exported to JSON");
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
          
          if (imported.items && Array.isArray(imported.items)) {
            // Generate new IDs for imported items to avoid conflicts
            const importedItems: UIListItem[] = imported.items.map((item: any) => ({
              id: generateUniqueItemId(items),
              data: item.data || {},
            }));
            
            setItems([...items, ...importedItems]);
            toast.success(`Imported ${importedItems.length} item(s)`);
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

  const handleSaveItem = (item: UIListItem) => {
    if (editingItem) {
      // Update existing item
      setItems(
        items.map((i) => (i.id === editingItem.id ? item : i))
      );
    } else {
      // Add new item
      setItems([...items, item]);
    }
    setEditDialogOpen(false);
    setEditingItem(null);
  };

  // Update prefix when key changes in create mode
  useEffect(() => {
    if (isCreateMode && key) {
      if (currentPrefix) {
        const normalizedPrefix = currentPrefix.endsWith("/")
          ? currentPrefix.slice(0, -1)
          : currentPrefix;
        setPrefix(`${normalizedPrefix}/${key}`);
      } else {
        setPrefix(key);
      }
    }
  }, [key, isCreateMode, currentPrefix]);

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

    const order = items.map((item) => item.id);
    // Create UI manifest for callback (metadata is already in UI format)
    const manifest: UIListManifest = {
      order,
      version: (effectiveManifest?.version ?? 0) + 1,
      etag: effectiveManifest?.etag ?? null,
      metadata: effectiveManifest?.metadata,
    };

    const normalizedPrefix = normalizePath(prefix);
    await onSave(normalizedPrefix, items, manifest, deletedIds);
  };

  // Show loading state with skeleton
  if (isLoadingList && shouldAutoLoad) {
    return (
      <Card>
        <CardContent>
          <Box sx={{ py: 2 }}>
            <Skeleton variant="text" width="40%" height={32} sx={{ mb: 2 }} />
            <Skeleton variant="rectangular" height={200} sx={{ mb: 2, borderRadius: 1 }} />
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
  if (listError && shouldAutoLoad) {
    const errorMessage = listError instanceof Error 
      ? listError.message 
      : typeof listError === "object" && listError !== null && "detail" in listError
      ? String(listError.detail)
      : "Unknown error";
    
    return (
      <Card>
        <CardContent>
          <Alert severity="error" sx={{ mb: 2 }}>
            <Typography variant="body2" fontWeight="medium" gutterBottom>
              Failed to load list data
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
            {isCreateMode ? "Create List" : "Edit List"}
          </Typography>
          <Stack direction="row" spacing={1}>
            {!isReadOnly && items.length > 0 && (
              <>
                <Button
                  startIcon={showSelection ? <DeselectIcon /> : <SelectAllIcon />}
                  onClick={() => {
                    setShowSelection(!showSelection);
                    if (!showSelection) {
                      setSelectedItems(new Set());
                    }
                  }}
                  variant="outlined"
                  size="small"
                  aria-label={showSelection ? "Hide selection" : "Show selection"}
                >
                  {showSelection ? "Cancel" : "Select"}
                </Button>
                {showSelection && selectedItems.size > 0 && (
                  <Button
                    startIcon={<DeleteIcon />}
                    onClick={handleDeleteSelected}
                    variant="outlined"
                    color="error"
                    size="small"
                    aria-label={`Delete ${selectedItems.size} selected item(s)`}
                  >
                    Delete ({selectedItems.size})
                  </Button>
                )}
                <Button
                  startIcon={<ExportIcon />}
                  onClick={handleExportJSON}
                  variant="outlined"
                  size="small"
                  aria-label="Export to JSON"
                >
                  Export
                </Button>
                <Button
                  startIcon={<ImportIcon />}
                  onClick={handleImportJSON}
                  variant="outlined"
                  size="small"
                  aria-label="Import from JSON"
                >
                  Import
                </Button>
              </>
            )}
            {!isReadOnly && (
              <Button
                startIcon={<AddIcon />}
                onClick={handleAddItem}
                variant="contained"
                size="small"
                aria-label="Add new list item"
              >
                Add Item
              </Button>
            )}
          </Stack>
        </Box>

          {isCreateMode && (
            <Box sx={{ mb: 2 }}>
              <TextField
                fullWidth
                label="Key"
                value={key}
                onChange={(e) => setKey(e.target.value)}
                disabled={isReadOnly || isSaving}
                size="small"
                error={!!prefixError}
                helperText={
                  prefixError ||
                  (currentPrefix
                    ? `Key (will be saved as: ${currentPrefix}/${key || "..."})`
                    : "Enter the key (can contain '/' characters, e.g., key/subkey)")
                }
                aria-describedby={prefixError ? "prefix-error" : "prefix-help"}
              />
            </Box>
          )}


        {effectiveManifest && (
          <Stack direction="row" spacing={1} sx={{ mb: 2 }}>
            <Chip
              label={`Version: ${effectiveManifest.version}`}
              size="small"
              variant="outlined"
            />
            {effectiveManifest.etag && (
              <Chip
                label={`ETag: ${effectiveManifest.etag}`}
                size="small"
                variant="outlined"
              />
            )}
          </Stack>
        )}

        {items.length === 0 ? (
          <Alert severity="info" role="status">
            No items in list. Add items to get started.
          </Alert>
        ) : (
          <TableContainer 
            component={Paper} 
            variant="outlined"
            sx={{ 
              maxHeight: { xs: 400, md: 600 },
              overflow: "auto",
            }}
          >
            <Table size="small" aria-label="List items table">
              <TableHead>
                <TableRow>
                  {showSelection && (
                    <TableCell padding="checkbox" width={50}>
                      <Checkbox
                        indeterminate={
                          selectedItems.size > 0 && selectedItems.size < items.length
                        }
                        checked={items.length > 0 && selectedItems.size === items.length}
                        onChange={handleSelectAll}
                        aria-label="Select all items"
                      />
                    </TableCell>
                  )}
                  <TableCell width={50} aria-label="Drag handle"></TableCell>
                  <TableCell width={60}>#</TableCell>
                  <TableCell>Data Preview</TableCell>
                  <TableCell width={showSelection ? 180 : 140}>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                <DndContext
                  sensors={sensors}
                  collisionDetection={closestCenter}
                  onDragEnd={handleDragEnd}
                  accessibility={{
                    announcements: {
                      onDragStart({ active }) {
                        const item = items.find((i) => i.id === active.id);
                        const index = items.findIndex((i) => i.id === active.id);
                        return `Picked up item ${index + 1}${item ? ` (ID: ${item.id})` : ""}`;
                      },
                      onDragOver({ active, over }) {
                        const activeIndex = items.findIndex((i) => i.id === active.id);
                        const overIndex = over
                          ? items.findIndex((i) => i.id === over.id)
                          : -1;
                        return `Moving item ${activeIndex + 1} to position ${overIndex + 1}`;
                      },
                      onDragEnd({ active, over }) {
                        if (over) {
                          const activeIndex = items.findIndex((i) => i.id === active.id);
                          const overIndex = items.findIndex((i) => i.id === over.id);
                          return `Moved item ${activeIndex + 1} to position ${overIndex + 1}`;
                        }
                        return "Drag cancelled";
                      },
                      onDragCancel() {
                        return "Drag cancelled";
                      },
                    },
                  }}
                >
                  <SortableContext
                    items={items.map((item) => item.id)}
                    strategy={verticalListSortingStrategy}
                  >
                    {items.map((item, index) => (
                      <SortableRow
                        key={item.id}
                        item={item}
                        index={index}
                        onEdit={handleEditItem}
                        onDelete={handleDeleteItem}
                        onDuplicate={handleDuplicateItem}
                        isReadOnly={isReadOnly}
                        isSelected={selectedItems.has(item.id)}
                        onSelect={handleSelectItem}
                        showSelection={showSelection}
                      />
                    ))}
                  </SortableContext>
                </DndContext>
              </TableBody>
            </Table>
          </TableContainer>
        )}

        <Stack direction="row" spacing={1} sx={{ mt: 3 }} justifyContent="flex-end">
          <Button
            startIcon={<CancelIcon />}
            onClick={onCancel}
            disabled={isSaving}
            variant="outlined"
          >
            Cancel
          </Button>
          {!isReadOnly && (
            <Button
              startIcon={<SaveIcon />}
              onClick={handleSave}
              disabled={isSaving || (isCreateMode && !!prefixError)}
              variant="contained"
            >
              {isSaving ? "Saving..." : "Save"}
            </Button>
          )}
        </Stack>

        <ItemEditorDialog
          open={editDialogOpen}
          item={editingItem}
          onClose={() => {
            setEditDialogOpen(false);
            setEditingItem(null);
          }}
          onSave={handleSaveItem}
          isReadOnly={isReadOnly}
          existingItems={items}
        />
      </CardContent>
    </Card>
  );
}

