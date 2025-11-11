/**
 * Detail panel component for KV entries with type detection and routing
 */

import { useState, useMemo } from "react";
import {
  Box,
  Card,
  CardContent,
  Tabs,
  Tab,
  Typography,
  Button,
  IconButton,
  Tooltip,
  Alert,
  Stack,
  Chip,
} from "@mui/material";
import {
  Edit as EditIcon,
  Delete as DeleteIcon,
  Download as DownloadIcon,
  Refresh as RefreshIcon,
} from "@mui/icons-material";
import { TabPanel } from "@components/common/TabPanel";
import { KVEntryEditor } from "./KVEntryEditor";
import { KVListEditor } from "./KVListEditor";
import { KVObjectEditor } from "./KVObjectEditor";
import { KVListView } from "./KVListView";
import { KVObjectView } from "./KVObjectView";
import { KVPrefixView } from "./KVPrefixView";
import type { KVEntry } from "../types";
import { decodeBase64, normalizePath } from "../types";
import type { KVPutRequest } from "@lib/api/models";
import { useKVTypeDetection, KVType } from "../hooks/useKVTypeDetection";
import type { UIListItem } from "../utils/typeAdapters";
import type { UIListManifest } from "./KVListEditor";

export interface KVDetailPanelProps {
  entry?: KVEntry;
  path: string;
  serviceId: string;
  /** List of child keys (for structure-based type detection) */
  childKeys?: string[];
  /** All keys (for better type detection) */
  allKeys?: string[];
  /** Callback for editing leaf entries */
  onEdit: (path: string, data: KVPutRequest) => Promise<void>;
  /** Callback for editing objects */
  onEditObject?: (prefix: string, data: Record<string, unknown>) => Promise<void>;
  /** Callback for editing lists */
  onEditList?: (prefix: string, items: UIListItem[], manifest: UIListManifest, deletes: string[]) => Promise<void>;
  /** Callback for deleting */
  onDelete: () => Promise<void>;
  /** Callback for refreshing */
  onRefresh: () => void;
  /** Whether this is a prefix (has children) */
  isPrefix?: boolean;
  isReadOnly?: boolean;
  isEditing?: boolean;
  isDeleting?: boolean;
  isLoading?: boolean;
}

export function KVDetailPanel({
  entry,
  path,
  serviceId,
  childKeys = [],
  allKeys = [],
  onEdit,
  onEditObject,
  onEditList,
  onDelete,
  onRefresh,
  isPrefix = false,
  isReadOnly = false,
  isEditing = false,
  isDeleting = false,
  isLoading = false,
}: KVDetailPanelProps) {
  const [tabValue, setTabValue] = useState(0);
  const [editMode, setEditMode] = useState(false);

  // Detect type - use allKeys if available for better detection
  const keysForDetection = allKeys.length > 0 ? allKeys : childKeys;
  const detectedType = useKVTypeDetection({
    entry,
    childKeys,
    isPrefix,
    allKeys: keysForDetection,
    path,
  });

  // Check if this is actually a prefix (has children)
  const isActualPrefix = useMemo(() => {
    if (isPrefix) return true;
    if (childKeys.length > 0) return true;
    // Check if path is a folder based on child keys
    const normalizedPath = normalizePath(path);
    return childKeys.some((key) => {
      const normalizedKey = normalizePath(key);
      return normalizedKey.startsWith(normalizedPath + "/");
    });
  }, [path, childKeys, isPrefix]);

  const handleEdit = async (editPath: string, data: KVPutRequest) => {
    await onEdit(editPath, data);
    setEditMode(false);
  };

  const handleEditObject = async (editPrefix: string, data: Record<string, unknown>) => {
    if (onEditObject) {
      await onEditObject(editPrefix, data);
      setEditMode(false);
    }
  };

  const handleEditList = async (
    editPrefix: string,
    items: UIListItem[],
    manifest: UIListManifest,
    deletes: string[]
  ) => {
    if (onEditList) {
      await onEditList(editPrefix, items, manifest, deletes);
      setEditMode(false);
    }
  };

  const handleCancel = () => {
    setEditMode(false);
  };

  const handleDownload = () => {
    if (!entry?.valueBase64) return;

    try {
      const decoded = decodeBase64(entry.valueBase64);
      const blob = new Blob([decoded], { type: "application/octet-stream" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = path.split("/").pop() || "kv-entry";
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (err) {
      console.error("Failed to download:", err);
    }
  };

  // Get type badge color
  const getTypeBadgeColor = (type: KVType) => {
    switch (type) {
      case KVType.LEAF:
        return "default";
      case KVType.OBJECT:
        return "primary";
      case KVType.LIST:
        return "secondary";
      case KVType.FOLDER:
        return "info";
      default:
        return "default";
    }
  };

  // If this is a prefix and not in edit mode, route to appropriate view
  if (isActualPrefix && !editMode && detectedType !== KVType.LEAF) {
    // LIST type: show List view
    if (detectedType === KVType.LIST) {
      return (
        <KVListView
          serviceId={serviceId}
          prefix={path}
          onEdit={!isReadOnly ? () => setEditMode(true) : undefined}
          onRefresh={onRefresh}
          isReadOnly={isReadOnly}
          isLoading={isLoading}
        />
      );
    }

    // OBJECT type: show Object view
    if (detectedType === KVType.OBJECT) {
      return (
        <KVObjectView
          serviceId={serviceId}
          prefix={path}
          onEdit={!isReadOnly ? () => setEditMode(true) : undefined}
          onRefresh={onRefresh}
          isReadOnly={isReadOnly}
          isLoading={isLoading}
        />
      );
    }

    // FOLDER type: show prefix view (for navigation)
    if (detectedType === KVType.FOLDER) {
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
              <Stack direction="row" spacing={1} alignItems="center">
                <Typography variant="h6">Folder View</Typography>
                <Chip
                  label={detectedType}
                  size="small"
                  color={getTypeBadgeColor(detectedType)}
                />
              </Stack>
              <Stack direction="row" spacing={1}>
                <Tooltip title="Refresh">
                  <IconButton size="small" onClick={onRefresh} aria-label="Refresh">
                    <RefreshIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
              </Stack>
            </Box>
            <KVPrefixView
              serviceId={serviceId}
              prefix={path}
              initialFormat="json"
              isLoading={isLoading}
            />
          </CardContent>
        </Card>
      );
    }
  }

  // Edit mode - route to appropriate editor
  if (editMode) {
    if (detectedType === KVType.OBJECT) {
      return (
        <KVObjectEditor
          serviceId={serviceId}
          prefix={path}
          initialData={{}}
          onSave={handleEditObject}
          onCancel={handleCancel}
          isReadOnly={isReadOnly}
          isSaving={isEditing}
        />
      );
    }

    if (detectedType === KVType.LIST) {
      return (
        <KVListEditor
          serviceId={serviceId}
          prefix={path}
          initialItems={[]}
          initialManifest={{ order: [], version: 0 }}
          onSave={handleEditList}
          onCancel={handleCancel}
          isReadOnly={isReadOnly}
          isSaving={isEditing}
        />
      );
    }

    // Default to leaf editor
    return (
      <Card>
        <CardContent>
          <KVEntryEditor
            entry={entry}
            path={path}
            onSave={handleEdit}
            onCancel={handleCancel}
            isReadOnly={isReadOnly}
            isSaving={isEditing}
          />
        </CardContent>
      </Card>
    );
  }

  if (isLoading) {
    return (
      <Card>
        <CardContent>
          <Box sx={{ textAlign: "center", py: 4 }}>
            <Typography variant="body2" color="text.secondary">
              Loading...
            </Typography>
          </Box>
        </CardContent>
      </Card>
    );
  }

  if (!entry && !isActualPrefix) {
    return (
      <Card>
        <CardContent>
          <Box sx={{ textAlign: "center", py: 4 }}>
            <Typography variant="body2" color="text.secondary">
              Select an entry to view details
            </Typography>
          </Box>
        </CardContent>
      </Card>
    );
  }

  const decodedValue = entry?.valueBase64
    ? decodeBase64(entry.valueBase64)
    : "";

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
          <Stack direction="row" spacing={1} alignItems="center">
            <Typography variant="h6">Entry Details</Typography>
            <Chip
              label={detectedType}
              size="small"
              color={getTypeBadgeColor(detectedType)}
            />
          </Stack>
          <Stack direction="row" spacing={1}>
            <Tooltip title="Refresh">
              <IconButton size="small" onClick={onRefresh} aria-label="Refresh">
                <RefreshIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            {!isReadOnly && (
              <>
                <Tooltip title="Edit">
                  <IconButton
                    size="small"
                    onClick={() => setEditMode(true)}
                    aria-label="Edit"
                  >
                    <EditIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
                <Tooltip title="Delete">
                  <IconButton
                    size="small"
                    onClick={onDelete}
                    disabled={isDeleting}
                    aria-label="Delete"
                    color="error"
                  >
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
              </>
            )}
          </Stack>
        </Box>

        {isReadOnly && (
          <Alert severity="info" sx={{ mb: 2 }}>
            You have read-only access to this service
          </Alert>
        )}

        {entry && (
          <>
            <Tabs value={tabValue} onChange={(_, v) => setTabValue(v)}>
              <Tab label="Metadata" />
              <Tab label="Raw Value" />
            </Tabs>

            <TabPanel value={tabValue} index={0}>
              <Stack spacing={2}>
                <Box>
                  <Typography variant="caption" color="text.secondary">
                    Path
                  </Typography>
                  <Typography variant="body1" sx={{ wordBreak: "break-word" }}>
                    {path}
                  </Typography>
                </Box>

                <Box>
                  <Typography variant="caption" color="text.secondary">
                    Type
                  </Typography>
                  <Typography variant="body1">
                    <Chip
                      label={detectedType}
                      size="small"
                      color={getTypeBadgeColor(detectedType)}
                    />
                  </Typography>
                </Box>

                <Box>
                  <Typography variant="caption" color="text.secondary">
                    Modify Index
                  </Typography>
                  <Typography variant="body1">
                    {entry.modifyIndex ?? "N/A"}
                  </Typography>
                </Box>

                <Box>
                  <Typography variant="caption" color="text.secondary">
                    Create Index
                  </Typography>
                  <Typography variant="body1">
                    {entry.createIndex ?? "N/A"}
                  </Typography>
                </Box>

                <Box>
                  <Typography variant="caption" color="text.secondary">
                    Flags
                  </Typography>
                  <Typography variant="body1">{entry.flags ?? 0}</Typography>
                </Box>

                <Box>
                  <Typography variant="caption" color="text.secondary">
                    Value (Decoded)
                  </Typography>
                  <Box
                    sx={{
                      mt: 1,
                      p: 2,
                      bgcolor: "background.default",
                      borderRadius: 1,
                      border: 1,
                      borderColor: "divider",
                      maxHeight: 300,
                      overflow: "auto",
                    }}
                  >
                    <Typography
                      variant="body2"
                      component="pre"
                      sx={{
                        fontFamily: "monospace",
                        whiteSpace: "pre-wrap",
                        wordBreak: "break-word",
                        m: 0,
                      }}
                    >
                      {decodedValue || "(empty)"}
                    </Typography>
                  </Box>
                </Box>
              </Stack>
            </TabPanel>

            <TabPanel value={tabValue} index={1}>
              <Stack spacing={2}>
                <Box
                  sx={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                  }}
                >
                  <Typography variant="body2" color="text.secondary">
                    Raw Value (Base64)
                  </Typography>
                  <Button
                    size="small"
                    startIcon={<DownloadIcon />}
                    onClick={handleDownload}
                    variant="outlined"
                  >
                    Download
                  </Button>
                </Box>

                <Box
                  sx={{
                    p: 2,
                    bgcolor: "background.default",
                    borderRadius: 1,
                    border: 1,
                    borderColor: "divider",
                    maxHeight: 400,
                    overflow: "auto",
                  }}
                >
                  <Typography
                    variant="body2"
                    component="pre"
                    sx={{
                      fontFamily: "monospace",
                      whiteSpace: "pre-wrap",
                      wordBreak: "break-word",
                      m: 0,
                    }}
                  >
                    {entry.valueBase64 || "(empty)"}
                  </Typography>
                </Box>
              </Stack>
            </TabPanel>
          </>
        )}
      </CardContent>
    </Card>
  );
}
