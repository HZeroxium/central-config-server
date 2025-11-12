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
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
  Divider,
} from "@mui/material";
import {
  Edit as EditIcon,
  Delete as DeleteIcon,
  Download as DownloadIcon,
  Refresh as RefreshIcon,
  ContentCopy as ContentCopyIcon,
  FileDownload as FileDownloadIcon,
  Code as CodeIcon,
} from "@mui/icons-material";
import { TabPanel } from "@components/common/TabPanel";
import { KVEntryEditor } from "./KVEntryEditor";
import { KVListEditor } from "./KVListEditor";
import { KVListView } from "./KVListView";
import { KVLeafListEditor } from "./KVLeafListEditor";
import { KVLeafListView } from "./KVLeafListView";
import { KVPrefixView } from "./KVPrefixView";
import type { KVEntry } from "../types";
import { decodeBase64, normalizePath } from "../types";
import type { KVPutRequest } from "@lib/api/models";
import { useKVTypeDetection, KVType } from "../hooks/useKVTypeDetection";
import type { UIListItem } from "../utils/typeAdapters";
import type { UIListManifest } from "./KVListEditor";
import { parseLeafList } from "../utils/leafListParser";
import { copyPath, copyValue, copyAsCurl, copyToClipboard } from "../utils/copyUtils";
import {
  exportAsJSON,
  exportAsYAML,
  exportAsProperties,
  downloadFile,
  generateExportFilename,
  getMimeType,
} from "../utils/exportUtils";
import { toast } from "@lib/toast/toast";
import { useViewKVPrefix } from "@lib/api/generated/key-value-store/key-value-store";

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
  const [exportMenuAnchor, setExportMenuAnchor] = useState<null | HTMLElement>(null);
  const [viewPrefixFormat, setViewPrefixFormat] = useState<"json" | "yaml" | "properties">("json");

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

  // viewKVPrefix query for FOLDER types (defined early for use in FOLDER view)
  const shouldFetchPrefix = detectedType === KVType.FOLDER && isActualPrefix;
  const prefixViewQuery = useViewKVPrefix(
    serviceId,
    shouldFetchPrefix ? { prefix: path, format: viewPrefixFormat } : undefined,
    {
      query: {
        enabled: shouldFetchPrefix,
        staleTime: 10_000,
      },
    }
  );

  // Copy handlers (defined early for use in FOLDER view)
  const handleCopyPath = async (separator: "/" | "." = "/") => {
    try {
      const pathToCopy = copyPath(path, separator);
      await copyToClipboard(pathToCopy);
      toast.success(`Path copied (${separator === "/" ? "path" : "nested"} format)`);
    } catch (err) {
      toast.error("Failed to copy path");
    }
  };

  const handleCopyValue = async (format?: "json" | "yaml") => {
    try {
      const valueToCopy = copyValue(decodedValue, format);
      await copyToClipboard(valueToCopy);
      toast.success(`Value copied${format ? ` as ${format.toUpperCase()}` : ""}`);
    } catch (err) {
      toast.error("Failed to copy value");
    }
  };

  const handleCopyCurl = async () => {
    try {
      const curlCommand = copyAsCurl(serviceId, path, "GET");
      await copyToClipboard(curlCommand);
      toast.success("curl command copied");
    } catch (err) {
      toast.error("Failed to copy curl command");
    }
  };

  // Export handlers (defined early for use in FOLDER view)
  const handleExport = (format: "json" | "yaml" | "properties" | "csv") => {
    try {
      let content: string;
      const metadata = {
        path,
        type: detectedType,
        modifyIndex: entry?.modifyIndex,
        createIndex: entry?.createIndex,
        flags: entry?.flags,
        timestamp: new Date().toISOString(),
      };

      if (format === "csv" && detectedType === KVType.LEAF_LIST) {
        const elements = parseLeafList(decodedValue);
        content = elements.join("\n");
      } else {
        let data: unknown;
        if (detectedType === KVType.LEAF_LIST) {
          const elements = parseLeafList(decodedValue);
          data = { elements };
        } else {
          try {
            data = JSON.parse(decodedValue);
          } catch {
            data = { value: decodedValue };
          }
        }

        switch (format) {
          case "json":
            content = exportAsJSON(data, metadata);
            break;
          case "yaml":
            content = exportAsYAML(data, metadata);
            break;
          case "properties":
            content = exportAsProperties(data as Record<string, unknown>, metadata);
            break;
          default:
            content = exportAsJSON(data, metadata);
        }
      }

      const filename = generateExportFilename(path, format, detectedType);
      const mimeType = getMimeType(format);
      downloadFile(content, filename, mimeType);
      toast.success(`Exported as ${format.toUpperCase()}`);
    } catch (err) {
      toast.error(`Failed to export as ${format}`);
    }
    setExportMenuAnchor(null);
  };

  const handleExportMenuClose = () => {
    setExportMenuAnchor(null);
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
      case KVType.LIST:
        return "secondary";
      case KVType.LEAF_LIST:
        return "warning";
      case KVType.FOLDER:
        return "info";
      default:
        return "default";
    }
  };

  // LEAF_LIST type: show Leaf List view (not a prefix, but special type)
  if (!editMode && detectedType === KVType.LEAF_LIST && entry) {
    return (
      <KVLeafListView
        serviceId={serviceId}
        path={path}
        onEdit={!isReadOnly ? () => setEditMode(true) : undefined}
        onRefresh={onRefresh}
        isReadOnly={isReadOnly}
        isLoading={isLoading}
      />
    );
  }

  // If this is a prefix and not in edit mode, route to appropriate view
  if (isActualPrefix && !editMode && detectedType !== KVType.LEAF && detectedType !== KVType.LEAF_LIST) {
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
                <Button
                  size="small"
                  variant={viewPrefixFormat === "json" ? "contained" : "outlined"}
                  onClick={() => setViewPrefixFormat("json")}
                >
                  JSON
                </Button>
                <Button
                  size="small"
                  variant={viewPrefixFormat === "yaml" ? "contained" : "outlined"}
                  onClick={() => setViewPrefixFormat("yaml")}
                >
                  YAML
                </Button>
                <Button
                  size="small"
                  variant={viewPrefixFormat === "properties" ? "contained" : "outlined"}
                  onClick={() => setViewPrefixFormat("properties")}
                >
                  Properties
                </Button>
                <Divider orientation="vertical" flexItem sx={{ mx: 1 }} />
                <Tooltip title="Copy Path">
                  <IconButton
                    size="small"
                    onClick={() => handleCopyPath("/")}
                    aria-label="Copy path"
                  >
                    <ContentCopyIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
                <Tooltip title="Copy as curl">
                  <IconButton
                    size="small"
                    onClick={handleCopyCurl}
                    aria-label="Copy as curl command"
                  >
                    <CodeIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
                <Tooltip title="Export">
                  <IconButton
                    size="small"
                    onClick={(e) => setExportMenuAnchor(e.currentTarget)}
                    aria-label="Export"
                  >
                    <FileDownloadIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
                <Menu
                  anchorEl={exportMenuAnchor}
                  open={Boolean(exportMenuAnchor)}
                  onClose={handleExportMenuClose}
                >
                  <MenuItem onClick={() => handleExport("json")}>
                    <ListItemIcon>
                      <FileDownloadIcon fontSize="small" />
                    </ListItemIcon>
                    <ListItemText>Export as JSON</ListItemText>
                  </MenuItem>
                  <MenuItem onClick={() => handleExport("yaml")}>
                    <ListItemIcon>
                      <FileDownloadIcon fontSize="small" />
                    </ListItemIcon>
                    <ListItemText>Export as YAML</ListItemText>
                  </MenuItem>
                  <MenuItem onClick={() => handleExport("properties")}>
                    <ListItemIcon>
                      <FileDownloadIcon fontSize="small" />
                    </ListItemIcon>
                    <ListItemText>Export as Properties</ListItemText>
                  </MenuItem>
                </Menu>
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
              initialFormat={viewPrefixFormat}
              isLoading={isLoading || prefixViewQuery.isLoading}
              content={prefixViewQuery.data}
              error={prefixViewQuery.error as Error | null | undefined}
            />
          </CardContent>
        </Card>
      );
    }
  }

  // Edit mode - route to appropriate editor
  if (editMode) {
    if (detectedType === KVType.LIST) {
      return (
        <KVListEditor
          serviceId={serviceId}
          prefix={path}
          initialItems={[]}
          initialManifest={undefined}
          onSave={handleEditList}
          onCancel={handleCancel}
          isReadOnly={isReadOnly}
          isSaving={isEditing}
        />
      );
    }

    if (detectedType === KVType.LEAF_LIST) {
      // Parse existing elements if available
      let initialElements: string[] = [];
      if (entry?.valueBase64) {
        try {
          const value = decodeBase64(entry.valueBase64);
          initialElements = parseLeafList(value);
        } catch (e) {
          console.error("Failed to parse LEAF_LIST for editor:", e);
        }
      }
      
      return (
        <KVLeafListEditor
          serviceId={serviceId}
          path={path}
          initialElements={initialElements}
          onSave={handleEdit}
          onCancel={handleCancel}
          isReadOnly={isReadOnly}
          isSaving={isEditing}
          isCreateMode={!entry}
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
            <Tooltip title="Copy Path">
              <IconButton
                size="small"
                onClick={() => handleCopyPath("/")}
                aria-label="Copy path"
              >
                <ContentCopyIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            {entry && (
              <>
                <Tooltip title="Copy Value">
                  <IconButton
                    size="small"
                    onClick={() => handleCopyValue()}
                    aria-label="Copy value"
                  >
                    <CodeIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
                <Tooltip title="Copy as curl">
                  <IconButton
                    size="small"
                    onClick={handleCopyCurl}
                    aria-label="Copy as curl command"
                  >
                    <CodeIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
              </>
            )}
            <Tooltip title="Export">
              <IconButton
                size="small"
                onClick={(e) => setExportMenuAnchor(e.currentTarget)}
                aria-label="Export"
              >
                <FileDownloadIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            <Menu
              anchorEl={exportMenuAnchor}
              open={Boolean(exportMenuAnchor)}
              onClose={handleExportMenuClose}
            >
              <MenuItem onClick={() => handleExport("json")}>
                <ListItemIcon>
                  <FileDownloadIcon fontSize="small" />
                </ListItemIcon>
                <ListItemText>Export as JSON</ListItemText>
              </MenuItem>
              <MenuItem onClick={() => handleExport("yaml")}>
                <ListItemIcon>
                  <FileDownloadIcon fontSize="small" />
                </ListItemIcon>
                <ListItemText>Export as YAML</ListItemText>
              </MenuItem>
              <MenuItem onClick={() => handleExport("properties")}>
                <ListItemIcon>
                  <FileDownloadIcon fontSize="small" />
                </ListItemIcon>
                <ListItemText>Export as Properties</ListItemText>
              </MenuItem>
              {detectedType === KVType.LEAF_LIST && (
                <MenuItem onClick={() => handleExport("csv")}>
                  <ListItemIcon>
                    <FileDownloadIcon fontSize="small" />
                  </ListItemIcon>
                  <ListItemText>Export as CSV</ListItemText>
                </MenuItem>
              )}
            </Menu>
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
                    {detectedType === KVType.LIST ? "Prefix" : "Key"}
                  </Typography>
                  <Typography variant="h6" sx={{ wordBreak: "break-word", mb: 0.5 }}>
                    {detectedType === KVType.LIST 
                      ? path 
                      : path.split("/").pop() || path}
                  </Typography>
                  {detectedType !== KVType.LIST && path.includes("/") && (
                    <Typography variant="caption" color="text.secondary" sx={{ fontFamily: "monospace" }}>
                      Full path: {path}
                    </Typography>
                  )}
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
