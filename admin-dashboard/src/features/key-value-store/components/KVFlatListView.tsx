/**
 * Flat list view component for KV Store (shows immediate children of current prefix)
 */

import {
  Box,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Typography,
  CircularProgress,
  Divider,
  Chip,
  Tooltip,
} from "@mui/material";
import {
  Folder as FolderIcon,
  InsertDriveFile as FileIcon,
  List as ListIcon,
  ArrowUpward as ArrowUpIcon,
} from "@mui/icons-material";
import { normalizePath, isListPrefix, getParentPath } from "../types";
import { getImmediateChildren } from "../types";

export interface KVFlatListViewProps {
  /** List of all keys */
  keys: string[];
  /** Current prefix */
  prefix: string;
  /** Selected path */
  selectedPath?: string;
  /** Callback when item is clicked */
  onSelect: (path: string, isFolder: boolean) => void;
  /** Loading state */
  isLoading?: boolean;
  /** All keys (for detecting List/Object types) */
  allKeys?: string[];
  /** Callback for navigating to parent prefix */
  onNavigateUp?: (prefix: string) => void;
}

export function KVFlatListView({
  keys,
  prefix,
  selectedPath,
  onSelect,
  isLoading = false,
  allKeys,
  onNavigateUp,
}: KVFlatListViewProps) {
  // Use allKeys for List detection if provided, otherwise fall back to keys
  const keysForDetection = allKeys && allKeys.length > 0 ? allKeys : keys;
  const { folders, files } = getImmediateChildren(keys, prefix);
  const normalizedPrefix = normalizePath(prefix);
  const hasParent = normalizedPrefix.length > 0;
  const parentPath = hasParent ? getParentPath(normalizedPrefix) : "";

  const handleNavigateUp = () => {
    if (onNavigateUp && hasParent) {
      onNavigateUp(parentPath);
    }
  };

  if (isLoading) {
    return (
      <Box
        sx={{
          display: "flex",
          justifyContent: "center",
          alignItems: "center",
          p: 4,
        }}
      >
        <CircularProgress size={32} />
      </Box>
    );
  }

  if (folders.length === 0 && files.length === 0 && !hasParent) {
    return (
      <Box sx={{ p: 3, textAlign: "center" }}>
        <Typography variant="body2" color="text.secondary">
          No items in this folder
        </Typography>
      </Box>
    );
  }

  // Helper to determine if a path is List and get its type
  const getItemType = (path: string): "list" | "folder" | "file" => {
    if (isListPrefix(path, keysForDetection)) return "list";
    // Check if it's in folders or files
    if (folders.includes(path)) return "folder";
    return "file";
  };

  // Separate List from regular folders
  const listItems = folders.filter(
    (path) => isListPrefix(path, keysForDetection)
  );
  const regularFolders = folders.filter(
    (path) => !isListPrefix(path, keysForDetection)
  );

  return (
    <Box>
      <List component="nav" dense>
        {/* Parent directory entry */}
        {hasParent && (
          <ListItemButton
            onClick={handleNavigateUp}
            sx={{
              "&:hover": {
                backgroundColor: "action.hover",
              },
            }}
          >
            <ListItemIcon sx={{ minWidth: 40 }}>
              <ArrowUpIcon fontSize="small" color="action" />
            </ListItemIcon>
            <ListItemText
              primary={
                <Typography variant="body2" sx={{ fontStyle: "italic" }}>
                  ..
                </Typography>
              }
            />
          </ListItemButton>
        )}

        {hasParent && (regularFolders.length > 0 || listItems.length > 0 || files.length > 0) && (
          <Divider sx={{ my: 0.5 }} />
        )}

        {/* Regular folders */}
        {regularFolders.map((folderPath) => {
          const normalized = normalizePath(folderPath);
          const name = normalized.split("/").pop() || normalized;
          const isSelected = selectedPath === normalized;

          return (
            <ListItemButton
              key={folderPath}
              selected={isSelected}
              onClick={() => onSelect(normalized, true)}
              sx={{
                "&.Mui-selected": {
                  backgroundColor: "action.selected",
                  "&:hover": {
                    backgroundColor: "action.selected",
                  },
                },
              }}
            >
              <ListItemIcon sx={{ minWidth: 40 }}>
                <FolderIcon fontSize="small" color="primary" />
              </ListItemIcon>
              <ListItemText
                primary={
                  <Typography
                    variant="body2"
                    sx={{
                      fontWeight: isSelected ? 600 : 400,
                    }}
                  >
                    {name}
                  </Typography>
                }
              />
            </ListItemButton>
          );
        })}

        {/* List items (treated as single entities, not navigable) */}
        {listItems.map((itemPath) => {
          const normalized = normalizePath(itemPath);
          const name = normalized.split("/").pop() || normalized;
          const isSelected = selectedPath === normalized;
          const itemType = getItemType(normalized);
          const isList = itemType === "list";

          return (
            <Tooltip
              key={itemPath}
              title={
                isList
                  ? "List structure - click to view/edit"
                  : "Folder - click to navigate"
              }
              arrow
            >
              <ListItemButton
                selected={isSelected}
                onClick={() => onSelect(normalized, false)}
                sx={{
                  "&.Mui-selected": {
                    backgroundColor: "action.selected",
                    "&:hover": {
                      backgroundColor: "action.selected",
                    },
                  },
                }}
              >
                <ListItemIcon sx={{ minWidth: 40 }}>
                  {isList ? (
                    <ListIcon fontSize="small" color="secondary" />
                  ) : (
                    <FolderIcon fontSize="small" color="primary" />
                  )}
                </ListItemIcon>
                <ListItemText
                  primary={
                    <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                      <Typography
                        variant="body2"
                        sx={{
                          fontWeight: isSelected ? 600 : 400,
                        }}
                      >
                        {name}
                      </Typography>
                      {isList && (
                        <Chip
                          label="LIST"
                          size="small"
                          color="secondary"
                          sx={{ height: 18, fontSize: "0.65rem" }}
                        />
                      )}
                    </Box>
                  }
                />
              </ListItemButton>
            </Tooltip>
          );
        })}

        {folders.length > 0 && files.length > 0 && <Divider sx={{ my: 1 }} />}

        {/* Regular files */}
        {files.map((filePath) => {
          const normalized = normalizePath(filePath);
          const name = normalized.split("/").pop() || normalized;
          const isSelected = selectedPath === normalized;

          return (
            <ListItemButton
              key={filePath}
              selected={isSelected}
              onClick={() => onSelect(normalized, false)}
              sx={{
                "&.Mui-selected": {
                  backgroundColor: "action.selected",
                  "&:hover": {
                    backgroundColor: "action.selected",
                  },
                },
              }}
            >
              <ListItemIcon sx={{ minWidth: 40 }}>
                <FileIcon fontSize="small" color="action" />
              </ListItemIcon>
              <ListItemText
                primary={
                  <Typography
                    variant="body2"
                    sx={{
                      fontWeight: isSelected ? 600 : 400,
                    }}
                  >
                    {name}
                  </Typography>
                }
              />
            </ListItemButton>
          );
        })}
      </List>
    </Box>
  );
}

