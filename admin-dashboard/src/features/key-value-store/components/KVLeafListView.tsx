/**
 * Read-only view component for KV Leaf List (comma-separated list)
 */

import { useMemo, useState } from "react";
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Stack,
  Alert,
  CircularProgress,
  IconButton,
  Tooltip,
  Chip,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
} from "@mui/material";
import {
  Edit as EditIcon,
  Refresh as RefreshIcon,
  FormatListBulleted as LeafListIcon,
  FileDownload as ExportIcon,
  ContentCopy as CopyIcon,
  Code as CodeIcon,
} from "@mui/icons-material";
import { useGetKVEntry } from "@lib/api/generated/key-value-store/key-value-store";
import { decodeBase64 } from "../types";
import { parseLeafList } from "../utils/leafListParser";
import { toast } from "@lib/toast/toast";
import { copyPath, copyLeafListElements, copyAsCurl, copyToClipboard } from "../utils/copyUtils";
import {
  exportAsJSON,
  exportAsYAML,
  exportAsProperties,
  exportAsCSV,
  downloadFile,
  generateExportFilename,
  getMimeType,
} from "../utils/exportUtils";

export interface KVLeafListViewProps {
  serviceId: string;
  path: string;
  /** Callback to switch to edit mode */
  onEdit?: () => void;
  /** Callback to refresh data */
  onRefresh?: () => void;
  /** Whether this is read-only */
  isReadOnly?: boolean;
  /** Loading state override */
  isLoading?: boolean;
  /** Error state override */
  error?: Error | null;
}

export function KVLeafListView({
  serviceId,
  path,
  onEdit,
  onRefresh,
  isReadOnly = false,
  isLoading: providedLoading,
  error: providedError,
}: KVLeafListViewProps) {
  const [exportMenuAnchor, setExportMenuAnchor] = useState<null | HTMLElement>(null);
  
  const {
    data: entryData,
    isLoading: isLoadingEntry,
    error: entryError,
    refetch,
  } = useGetKVEntry(
    serviceId,
    path,
    {},
    {
      query: {
        enabled: !!serviceId && !!path,
        staleTime: 10_000,
      },
    }
  );

  const isLoading = providedLoading !== undefined ? providedLoading : isLoadingEntry;
  const error = providedError || entryError;

  // Parse the comma-separated value into a list
  const elements = useMemo(() => {
    // Check if entryData is a Blob (raw response) or KVEntryResponse
    if (!entryData || entryData instanceof Blob) return [];
    if (!entryData.valueBase64) return [];
    try {
      const value = decodeBase64(entryData.valueBase64);
      return parseLeafList(value);
    } catch (e) {
      console.error("Failed to parse LEAF_LIST value:", e);
      return [];
    }
  }, [entryData]);

  const handleRefresh = () => {
    refetch();
    onRefresh?.();
  };

  const handleCopyPath = async (separator: "/" | "." = "/") => {
    try {
      const pathToCopy = copyPath(path, separator);
      await copyToClipboard(pathToCopy);
      toast.success(`Path copied (${separator === "/" ? "path" : "nested"} format)`);
    } catch (err) {
      toast.error("Failed to copy path");
    }
  };

  const handleCopyElements = async (separator: "," = ",") => {
    try {
      const elementsToCopy = copyLeafListElements(elements, separator);
      await copyToClipboard(elementsToCopy);
      toast.success(`Elements copied (${separator} separator)`);
    } catch (err) {
      toast.error("Failed to copy elements");
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

  const handleExport = (format: "json" | "yaml" | "properties" | "csv") => {
    try {
      const metadata = {
        path,
        type: "LEAF_LIST",
        count: elements.length,
        timestamp: new Date().toISOString(),
      };

      let content: string;
      if (format === "csv") {
        content = exportAsCSV(elements);
      } else {
        const data = { elements, count: elements.length };
        switch (format) {
          case "json":
            content = exportAsJSON(data, metadata);
            break;
          case "yaml":
            content = exportAsYAML(data, metadata);
            break;
          case "properties":
            content = exportAsProperties({ elements: elements.join(",") }, metadata);
            break;
          default:
            content = exportAsJSON(data, metadata);
        }
      }

      const filename = generateExportFilename(path, format, "LEAF_LIST");
      const mimeType = getMimeType(format);
      downloadFile(content, filename, mimeType);
      toast.success(`Exported as ${format.toUpperCase()}`);
    } catch (err) {
      toast.error(`Failed to export as ${format}`);
    }
  };

  // Loading state
  if (isLoading) {
    return (
      <Card>
        <CardContent>
          <Box sx={{ display: "flex", justifyContent: "center", alignItems: "center", py: 4 }}>
            <CircularProgress size={32} />
          </Box>
        </CardContent>
      </Card>
    );
  }

  // Error state
  if (error) {
    const errorMessage = error && typeof error === "object" && "detail" in error
      ? (error.detail as string)
      : error instanceof Error
      ? error.message
      : "Unknown error";
    return (
      <Card>
        <CardContent>
          <Alert severity="error" sx={{ mb: 2 }}>
            Failed to load LEAF_LIST: {errorMessage}
          </Alert>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={handleRefresh}
            size="small"
          >
            Retry
          </Button>
        </CardContent>
      </Card>
    );
  }

  // Empty state or Blob response (shouldn't happen for LEAF_LIST)
  if (!entryData || entryData instanceof Blob) {
    return (
      <Card>
        <CardContent>
          <Alert severity="info">LEAF_LIST entry not found</Alert>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardContent>
        {/* Header */}
        <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 3 }}>
          <Stack direction="row" spacing={2} alignItems="center">
            <LeafListIcon color="primary" />
            <Typography variant="h6">Leaf List</Typography>
            <Chip
              label={`${elements.length} element${elements.length !== 1 ? "s" : ""}`}
              size="small"
              color="primary"
              variant="outlined"
            />
          </Stack>
          <Stack direction="row" spacing={1}>
            <Tooltip title="Copy Path">
              <IconButton
                size="small"
                onClick={() => handleCopyPath("/")}
                aria-label="Copy path"
              >
                <CopyIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            <Tooltip title="Copy Elements">
              <IconButton
                size="small"
                onClick={() => handleCopyElements(",")}
                disabled={elements.length === 0}
                aria-label="Copy elements"
              >
                <CopyIcon fontSize="small" />
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
                disabled={elements.length === 0}
                aria-label="Export"
              >
                <ExportIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            <Menu
              anchorEl={exportMenuAnchor}
              open={Boolean(exportMenuAnchor)}
              onClose={() => setExportMenuAnchor(null)}
            >
              <MenuItem onClick={() => { handleExport("json"); setExportMenuAnchor(null); }}>
                <ListItemIcon>
                  <ExportIcon fontSize="small" />
                </ListItemIcon>
                <ListItemText>Export as JSON</ListItemText>
              </MenuItem>
              <MenuItem onClick={() => { handleExport("yaml"); setExportMenuAnchor(null); }}>
                <ListItemIcon>
                  <ExportIcon fontSize="small" />
                </ListItemIcon>
                <ListItemText>Export as YAML</ListItemText>
              </MenuItem>
              <MenuItem onClick={() => { handleExport("properties"); setExportMenuAnchor(null); }}>
                <ListItemIcon>
                  <ExportIcon fontSize="small" />
                </ListItemIcon>
                <ListItemText>Export as Properties</ListItemText>
              </MenuItem>
              <MenuItem onClick={() => { handleExport("csv"); setExportMenuAnchor(null); }}>
                <ListItemIcon>
                  <ExportIcon fontSize="small" />
                </ListItemIcon>
                <ListItemText>Export as CSV</ListItemText>
              </MenuItem>
            </Menu>
            <Tooltip title="Refresh">
              <IconButton
                size="small"
                onClick={handleRefresh}
                aria-label="Refresh leaf list"
              >
                <RefreshIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            {!isReadOnly && onEdit && (
              <Button
                variant="contained"
                startIcon={<EditIcon />}
                onClick={onEdit}
                size="small"
              >
                Edit
              </Button>
            )}
          </Stack>
        </Box>

        {/* Metadata */}
        <Box sx={{ display: "flex", gap: 4, mb: 3, flexWrap: "wrap" }}>
          <Box>
            <Typography variant="caption" color="text.secondary">
              Key
            </Typography>
            <Typography variant="h6" sx={{ mb: 0.5 }}>
              {path.split("/").pop() || path}
            </Typography>
            {path.includes("/") && (
              <Typography variant="caption" color="text.secondary" sx={{ fontFamily: "monospace" }}>
                Full path: {path}
              </Typography>
            )}
          </Box>
          {entryData.modifyIndex && (
            <Box>
              <Typography variant="caption" color="text.secondary">
                Modify Index
              </Typography>
              <Typography variant="body2">{entryData.modifyIndex}</Typography>
            </Box>
          )}
        </Box>

        {/* Elements display */}
        {elements.length === 0 ? (
          <Alert severity="info">This LEAF_LIST is empty</Alert>
        ) : (
          <Box>
            <Typography variant="subtitle2" gutterBottom>
              Elements
            </Typography>
            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
              {elements.map((element, index) => (
                <Chip
                  key={index}
                  label={element}
                  size="small"
                  variant="outlined"
                  aria-label={`Element ${index + 1}: ${element}`}
                />
              ))}
            </Stack>
          </Box>
        )}
      </CardContent>
    </Card>
  );
}

