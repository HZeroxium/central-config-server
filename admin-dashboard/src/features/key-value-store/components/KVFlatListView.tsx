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
  Code as ObjectIcon,
  List as ListIcon,
} from "@mui/icons-material";
import { normalizePath, isListPrefix, isObjectPrefix } from "../types";
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
}

export function KVFlatListView({
  keys,
  prefix,
  selectedPath,
  onSelect,
  isLoading = false,
}: KVFlatListViewProps) {
  const { folders, files } = getImmediateChildren(keys, prefix);

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

  if (folders.length === 0 && files.length === 0) {
    return (
      <Box sx={{ p: 3, textAlign: "center" }}>
        <Typography variant="body2" color="text.secondary">
          No items in this folder
        </Typography>
      </Box>
    );
  }

  // Helper to determine if a path is List/Object and get its type
  const getItemType = (path: string): "list" | "object" | "folder" | "file" => {
    if (isListPrefix(path, keys)) return "list";
    if (isObjectPrefix(path, keys)) return "object";
    // Check if it's in folders or files
    if (folders.includes(path)) return "folder";
    return "file";
  };

  // Separate List/Object from regular folders
  const listObjectItems = folders.filter(
    (path) => isListPrefix(path, keys) || isObjectPrefix(path, keys)
  );
  const regularFolders = folders.filter(
    (path) => !isListPrefix(path, keys) && !isObjectPrefix(path, keys)
  );

  return (
    <List component="nav" dense>
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

      {/* List/Object items (treated as single entities, not navigable) */}
      {listObjectItems.map((itemPath) => {
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
                : "Object structure - click to view/edit"
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
                  <ObjectIcon fontSize="small" color="primary" />
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
                    <Chip
                      label={isList ? "LIST" : "OBJECT"}
                      size="small"
                      color={isList ? "secondary" : "primary"}
                      sx={{ height: 18, fontSize: "0.65rem" }}
                    />
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
  );
}

