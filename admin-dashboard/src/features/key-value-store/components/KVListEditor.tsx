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
import { Skeleton } from "@mui/material";
import {
  fromGeneratedKVListItemArray,
  fromKVListManifestMetadata,
  type UIListItem,
} from "../utils/typeAdapters";
import { normalizePath, validateKVPath } from "../types";

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
  isReadOnly?: boolean;
}

function SortableRow({
  item,
  index,
  onEdit,
  onDelete,
  isReadOnly = false,
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

  return (
    <TableRow ref={setNodeRef} style={style}>
      <TableCell>
        <IconButton
          size="small"
          {...attributes}
          {...listeners}
          disabled={isReadOnly}
          sx={{ cursor: isReadOnly ? "default" : "grab" }}
          aria-label={`Drag to reorder item ${item.id}`}
        >
          <DragIcon fontSize="small" />
        </IconButton>
      </TableCell>
      <TableCell>{index + 1}</TableCell>
      <TableCell>
        <Chip label={item.id} size="small" aria-label={`Item ID: ${item.id}`} />
      </TableCell>
      <TableCell>
        <Typography variant="body2" noWrap sx={{ maxWidth: { xs: 150, sm: 300 } }}>
          {Object.keys(item.data).length} field(s)
        </Typography>
      </TableCell>
      <TableCell>
        <Stack direction="row" spacing={1}>
          <IconButton
            size="small"
            onClick={() => onEdit(item)}
            disabled={isReadOnly}
            aria-label={`Edit item ${item.id}`}
          >
            <EditIcon fontSize="small" />
          </IconButton>
          {!isReadOnly && (
            <IconButton
              size="small"
              onClick={() => onDelete(item.id)}
              color="error"
              aria-label={`Delete item ${item.id}`}
            >
              <DeleteIcon fontSize="small" />
            </IconButton>
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
}

function ItemEditorDialog({
  open,
  item,
  onClose,
  onSave,
  isReadOnly = false,
}: ItemEditorDialogProps) {
  const [id, setId] = useState("");
  const [fields, setFields] = useState<Array<{ key: string; value: string }>>([]);

  useEffect(() => {
    if (item) {
      setId(item.id);
      setFields(
        Object.entries(item.data).map(([key, value]) => ({
          key,
          value: typeof value === "string" ? value : JSON.stringify(value),
        }))
      );
    } else {
      setId("");
      setFields([{ key: "", value: "" }]);
    }
  }, [item, open]);

  const handleAddField = () => {
    setFields([...fields, { key: "", value: "" }]);
  };

  const handleRemoveField = (index: number) => {
    setFields(fields.filter((_, i) => i !== index));
  };

  const handleFieldChange = (
    index: number,
    field: "key" | "value",
    newValue: string
  ) => {
    const updated = [...fields];
    updated[index] = { ...updated[index], [field]: newValue };
    setFields(updated);
  };

  const handleSave = () => {
    if (!id.trim()) {
      toast.error("Item ID is required");
      return;
    }

    const data: Record<string, unknown> = {};
    fields.forEach((field) => {
      if (field.key) {
        try {
          data[field.key] = JSON.parse(field.value);
        } catch {
          data[field.key] = field.value;
        }
      }
    });

    onSave({ id: id.trim(), data });
    onClose();
  };

  return (
    <Dialog 
      open={open} 
      onClose={onClose} 
      maxWidth="md" 
      fullWidth
      aria-labelledby="item-editor-dialog-title"
    >
      <DialogTitle id="item-editor-dialog-title">
        {item ? "Edit Item" : "Add New Item"}
      </DialogTitle>
      <DialogContent>
        <Stack spacing={2} sx={{ mt: 1 }}>
          <TextField
            label="Item ID"
            value={id}
            onChange={(e) => setId(e.target.value)}
            disabled={isReadOnly || !!item}
            fullWidth
            required
            helperText={item ? "ID cannot be changed" : "Unique identifier for this item"}
            aria-describedby={item ? "item-id-readonly" : "item-id-help"}
          />

          <Typography variant="subtitle2" component="h3">
            Fields
          </Typography>
          {fields.map((field, index) => (
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
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} aria-label="Cancel editing">
          Cancel
        </Button>
        {!isReadOnly && (
          <Button onClick={handleSave} variant="contained" aria-label="Save item">
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
  initialItems = [],
  initialManifest,
  onSave,
  onCancel,
  isReadOnly = false,
  isSaving = false,
  isCreateMode = false,
}: KVListEditorProps) {
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
          {!isReadOnly && (
            <Button
              startIcon={<AddIcon />}
              onClick={handleAddItem}
              variant="outlined"
              size="small"
              aria-label="Add new list item"
            >
              Add Item
            </Button>
          )}
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
                  "Prefix where the list will be stored (e.g., config/app)"
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
                  <TableCell width={50} aria-label="Drag handle"></TableCell>
                  <TableCell width={50}>#</TableCell>
                  <TableCell>ID</TableCell>
                  <TableCell>Fields</TableCell>
                  <TableCell width={120}>Actions</TableCell>
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
                        return `Picked up item ${active.id}`;
                      },
                      onDragOver({ active, over }) {
                        return `Moving item ${active.id} over ${over?.id ?? "position"}`;
                      },
                      onDragEnd({ active, over }) {
                        if (over) {
                          return `Moved item ${active.id} to position ${over.id}`;
                        }
                        return `Cancelled moving item ${active.id}`;
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
                        isReadOnly={isReadOnly}
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
        />
      </CardContent>
    </Card>
  );
}

