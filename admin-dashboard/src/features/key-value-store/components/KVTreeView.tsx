/**
 * Tree view component for KV Store navigation
 */

import { useMemo } from "react";
import {
  Box,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Typography,
  IconButton,
  CircularProgress,
  Collapse,
  Chip,
} from "@mui/material";
import {
  Folder as FolderIcon,
  FolderOpen as FolderOpenIcon,
  InsertDriveFile as FileIcon,
  ExpandMore as ExpandMoreIcon,
  ChevronRight as ChevronRightIcon,
  List as ListIcon,
  ArrowUpward as ArrowUpwardIcon,
} from "@mui/icons-material";
import { Button, Breadcrumbs, Link } from "@mui/material";
import type { KVTree, KVTreeNode } from "../types";
import { isListPrefix, isFolderPrefix } from "../types";

export interface KVTreeViewProps {
  tree: KVTree;
  selectedPath?: string;
  onSelect: (path: string, isFolder: boolean) => void;
  expandedNodes: Set<string>;
  onToggleNode: (path: string) => void;
  isLoading?: boolean;
  searchQuery?: string;
  onFolderNavigate?: (path: string) => void;
  /** All keys (for detecting List/Object types) */
  allKeys?: string[];
  /** Current prefix for parent navigation */
  currentPrefix?: string;
}

function TreeNode({
  node,
  level = 0,
  selectedPath,
  onSelect,
  expandedNodes,
  onToggleNode,
  searchQuery,
  onFolderNavigate,
  allKeys,
}: {
  node: KVTreeNode;
  level?: number;
  selectedPath?: string;
  onSelect: (path: string, isFolder: boolean) => void;
  expandedNodes: Set<string>;
  onToggleNode: (path: string) => void;
  searchQuery?: string;
  onFolderNavigate?: (path: string) => void;
  allKeys?: string[];
}) {
  const isExpanded = expandedNodes.has(node.fullPath);
  const isSelected = selectedPath === node.fullPath;
  const hasChildren = node.children && Object.keys(node.children).length > 0;
  
  // Detect List/Folder types
  const isList = allKeys ? isListPrefix(node.fullPath, allKeys) : false;
  const isFolder = allKeys ? isFolderPrefix(node.fullPath, allKeys) : false;

  const handleClick = () => {
    if (isList) {
      // List: select to view/edit (don't navigate into internal structure)
      onSelect(node.fullPath, false);
    } else if (isFolder) {
      // Folder: navigate into it
      if (onFolderNavigate) {
        onFolderNavigate(node.fullPath);
      } else {
        // Fallback: toggle expansion
        onToggleNode(node.fullPath);
      }
    } else if (node.isLeaf || node.nodeType === "file") {
      // File/leaf: select to view/edit
      onSelect(node.fullPath, false);
    } else {
      // Regular folder (fallback): navigate into it
      if (onFolderNavigate) {
        onFolderNavigate(node.fullPath);
      } else {
        // Fallback: toggle expansion
        onToggleNode(node.fullPath);
      }
    }
  };

  const handleToggle = (e: React.MouseEvent) => {
    e.stopPropagation();
    onToggleNode(node.fullPath);
  };

  const children = hasChildren ? Object.values(node.children!) : [];

  return (
    <>
      <ListItemButton
        selected={isSelected}
        onClick={handleClick}
        sx={{
          pl: 2 + level * 2,
          py: 0.5,
          "&.Mui-selected": {
            backgroundColor: "action.selected",
            "&:hover": {
              backgroundColor: "action.selected",
            },
          },
        }}
      >
        {hasChildren && (
          <IconButton
            size="small"
            onClick={handleToggle}
            sx={{ mr: 1, p: 0.5 }}
            aria-label={isExpanded ? "Collapse" : "Expand"}
          >
            {isExpanded ? (
              <ExpandMoreIcon fontSize="small" />
            ) : (
              <ChevronRightIcon fontSize="small" />
            )}
          </IconButton>
        )}
        {!hasChildren && !isList && <Box sx={{ width: 32 }} />}
        {isList && <Box sx={{ width: 32 }} />}
        <ListItemIcon sx={{ minWidth: 32 }}>
          {isList ? (
            <ListIcon fontSize="small" color="secondary" />
          ) : node.isLeaf || node.nodeType === "file" ? (
            <FileIcon fontSize="small" color="action" />
          ) : isExpanded ? (
            <FolderOpenIcon fontSize="small" color="primary" />
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
                  wordBreak: "break-word",
                }}
              >
                {node.name}
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
      {hasChildren && !isList && (
        <Collapse in={isExpanded} timeout="auto" unmountOnExit>
          <List component="div" disablePadding>
            {children.map((child) => (
              <TreeNode
                key={child.fullPath}
                node={child}
                level={level + 1}
                selectedPath={selectedPath}
                onSelect={onSelect}
                expandedNodes={expandedNodes}
                onToggleNode={onToggleNode}
                searchQuery={searchQuery}
                onFolderNavigate={onFolderNavigate}
                allKeys={allKeys}
              />
            ))}
          </List>
        </Collapse>
      )}
    </>
  );
}

export function KVTreeView({
  tree,
  selectedPath,
  onSelect,
  expandedNodes,
  onToggleNode,
  isLoading,
  searchQuery,
  onFolderNavigate,
  allKeys,
  currentPrefix = "",
}: KVTreeViewProps) {
  const rootNodes = useMemo(() => {
    return Object.values(tree) as KVTreeNode[];
  }, [tree]);

  // Get parent path for navigation
  const getParentPath = (path: string): string | null => {
    if (!path || path === "") return null;
    const parts = path.split("/").filter(Boolean);
    if (parts.length === 0) return null;
    parts.pop();
    return parts.length > 0 ? parts.join("/") : "";
  };

  const parentPath = currentPrefix ? getParentPath(currentPrefix) : null;

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

  if (rootNodes.length === 0) {
    return (
      <Box sx={{ p: 3, textAlign: "center" }}>
        <Typography variant="body2" color="text.secondary">
          {searchQuery ? "No matching entries found" : "No entries"}
        </Typography>
      </Box>
    );
  }

  const handleParentClick = () => {
    if (parentPath !== null && onFolderNavigate) {
      onFolderNavigate(parentPath);
    }
  };

  return (
    <Box>
      {/* Breadcrumb-style parent navigation */}
      {parentPath !== null && (
        <Box sx={{ px: 2, py: 1, borderBottom: 1, borderColor: "divider" }}>
          <Breadcrumbs separator="›" aria-label="breadcrumb">
            <Link
              component="button"
              variant="body2"
              onClick={handleParentClick}
              sx={{
                cursor: "pointer",
                textDecoration: "none",
                "&:hover": { textDecoration: "underline" },
              }}
            >
              {parentPath === "" ? "Root" : parentPath}
            </Link>
            <Typography variant="body2" color="text.primary">
              {currentPrefix.split("/").pop() || "Current"}
            </Typography>
          </Breadcrumbs>
        </Box>
      )}

      {/* Parent entry button */}
      {parentPath !== null && (
        <Box sx={{ px: 2, py: 1 }}>
          <Button
            fullWidth
            startIcon={<ArrowUpwardIcon />}
            onClick={handleParentClick}
            variant="outlined"
            size="small"
            sx={{
              justifyContent: "flex-start",
              textTransform: "none",
              fontStyle: "italic",
            }}
            aria-label="Navigate to parent folder"
          >
            .. (Parent: {parentPath === "" ? "Root" : parentPath})
          </Button>
        </Box>
      )}

      <List component="nav" dense sx={{ py: 1 }}>
        {/* Parent entry at top of tree */}
        {parentPath !== null && (
          <ListItemButton
            onClick={handleParentClick}
            sx={{
              fontStyle: "italic",
              color: "text.secondary",
              "&:hover": {
                bgcolor: "action.hover",
              },
            }}
            aria-label="Navigate to parent folder"
          >
            <ListItemIcon sx={{ minWidth: 36 }}>
              <ArrowUpwardIcon fontSize="small" />
            </ListItemIcon>
            <ListItemText
              primary=".."
              secondary={parentPath === "" ? "Root" : parentPath}
            />
          </ListItemButton>
        )}

        {rootNodes.map((node) => (
          <TreeNode
            key={node.fullPath}
            node={node}
            selectedPath={selectedPath}
            onSelect={onSelect}
            expandedNodes={expandedNodes}
            onToggleNode={onToggleNode}
            searchQuery={searchQuery}
            onFolderNavigate={onFolderNavigate}
            allKeys={allKeys}
          />
        ))}
      </List>
    </Box>
  );
}

