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
} from "@mui/material";
import {
  Folder as FolderIcon,
  InsertDriveFile as FileIcon,
} from "@mui/icons-material";
import { normalizePath } from "../types";
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

  return (
    <List component="nav" dense>
      {folders.map((folderPath) => {
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

      {folders.length > 0 && files.length > 0 && <Divider sx={{ my: 1 }} />}

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

