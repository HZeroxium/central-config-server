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
} from "@mui/material";
import {
  Folder as FolderIcon,
  FolderOpen as FolderOpenIcon,
  InsertDriveFile as FileIcon,
  ExpandMore as ExpandMoreIcon,
  ChevronRight as ChevronRightIcon,
} from "@mui/icons-material";
import type { KVTree, KVTreeNode } from "../types";

export interface KVTreeViewProps {
  tree: KVTree;
  selectedPath?: string;
  onSelect: (path: string) => void;
  expandedNodes: Set<string>;
  onToggleNode: (path: string) => void;
  isLoading?: boolean;
  searchQuery?: string;
}

function TreeNode({
  node,
  level = 0,
  selectedPath,
  onSelect,
  expandedNodes,
  onToggleNode,
  searchQuery,
}: {
  node: KVTreeNode;
  level?: number;
  selectedPath?: string;
  onSelect: (path: string) => void;
  expandedNodes: Set<string>;
  onToggleNode: (path: string) => void;
  searchQuery?: string;
}) {
  const isExpanded = expandedNodes.has(node.fullPath);
  const isSelected = selectedPath === node.fullPath;
  const hasChildren = node.children && Object.keys(node.children).length > 0;

  const handleClick = () => {
    if (node.isLeaf) {
      onSelect(node.fullPath);
    } else {
      onToggleNode(node.fullPath);
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
        {!hasChildren && <Box sx={{ width: 32 }} />}
        <ListItemIcon sx={{ minWidth: 32 }}>
          {node.isLeaf ? (
            <FileIcon fontSize="small" color="action" />
          ) : isExpanded ? (
            <FolderOpenIcon fontSize="small" color="primary" />
          ) : (
            <FolderIcon fontSize="small" color="action" />
          )}
        </ListItemIcon>
        <ListItemText
          primary={
            <Typography
              variant="body2"
              sx={{
                fontWeight: isSelected ? 600 : 400,
                wordBreak: "break-word",
              }}
            >
              {node.name}
            </Typography>
          }
        />
      </ListItemButton>
      {hasChildren && (
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
}: KVTreeViewProps) {
  const rootNodes = useMemo(() => {
    return Object.values(tree);
  }, [tree]);

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

  return (
    <List component="nav" dense sx={{ py: 1 }}>
      {rootNodes.map((node) => (
        <TreeNode
          key={node.fullPath}
          node={node}
          selectedPath={selectedPath}
          onSelect={onSelect}
          expandedNodes={expandedNodes}
          onToggleNode={onToggleNode}
          searchQuery={searchQuery}
        />
      ))}
    </List>
  );
}

