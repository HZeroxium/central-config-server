/**
 * Hook for KV Tree operations
 */

import { useState, useMemo, useCallback } from "react";
import {
  buildKVTreeFromKeys,
  findNodeInTree,
  type KVTree,
  normalizePath,
} from "../types";

export interface UseKVTreeOptions {
  /** List of keys (for keys-only responses) */
  keys?: string[];
  /** Current prefix */
  prefix?: string;
  searchQuery?: string;
}

export function useKVTree(options: UseKVTreeOptions) {
  const { keys = [], prefix = "", searchQuery = "" } = options;
  const [expandedNodes, setExpandedNodes] = useState<Set<string>>(new Set());

  // Build tree from keys (keys-only response)
  const tree = useMemo(() => {
    if (!keys.length) return {};
    return buildKVTreeFromKeys(keys, prefix);
  }, [keys, prefix]);

  // Filter tree by search query
  const filteredTree = useMemo(() => {
    if (!searchQuery) return tree;

    const normalizedQuery = normalizePath(searchQuery.toLowerCase());
    const filtered: KVTree = {};

    function matchesSearch(path: string): boolean {
      return path.toLowerCase().includes(normalizedQuery);
    }

    function traverseAndFilter(
      sourceTree: KVTree,
      targetTree: KVTree,
      currentPath: string = ""
    ) {
      Object.entries(sourceTree).forEach(([name, node]) => {
        const nodePath = currentPath ? `${currentPath}/${name}` : name;

        // Check if node or any descendant matches search
        const nodeMatches = matchesSearch(nodePath);
        let hasMatchingDescendant = false;

        if (node.children) {
          const filteredChildren: Record<string, typeof node> = {};
          Object.entries(node.children).forEach(([childName, childNode]) => {
            const childPath = `${nodePath}/${childName}`;
            if (matchesSearch(childPath)) {
              filteredChildren[childName] = childNode;
              hasMatchingDescendant = true;
            } else if (childNode.children) {
              // Recursively check descendants
              const childFiltered: KVTree = {};
              traverseAndFilter(
                { [childName]: childNode },
                childFiltered,
                nodePath
              );
              if (Object.keys(childFiltered).length > 0) {
                filteredChildren[childName] = {
                  ...childNode,
                  children: childFiltered[childName]?.children,
                };
                hasMatchingDescendant = true;
              }
            }
          });

          if (hasMatchingDescendant || nodeMatches) {
            targetTree[name] = {
              ...node,
              children: Object.keys(filteredChildren).length > 0
                ? filteredChildren
                : node.children,
            };
          }
        } else if (nodeMatches) {
          targetTree[name] = node;
        }
      });
    }

    traverseAndFilter(tree, filtered);
    return filtered;
  }, [tree, searchQuery]);

  // Find node by path
  const findNode = useCallback(
    (path: string) => {
      return findNodeInTree(filteredTree, path);
    },
    [filteredTree]
  );

  // Toggle node expansion
  const toggleNode = useCallback((path: string) => {
    setExpandedNodes((prev) => {
      const next = new Set(prev);
      if (next.has(path)) {
        next.delete(path);
      } else {
        next.add(path);
      }
      return next;
    });
  }, []);

  // Expand node
  const expandNode = useCallback((path: string) => {
    setExpandedNodes((prev) => new Set(prev).add(path));
  }, []);

  // Collapse node
  const collapseNode = useCallback((path: string) => {
    setExpandedNodes((prev) => {
      const next = new Set(prev);
      next.delete(path);
      return next;
    });
  }, []);

  // Expand all
  const expandAll = useCallback(() => {
    const allPaths = new Set<string>();
    function collectPaths(t: KVTree, currentPath: string = "") {
      Object.entries(t).forEach(([name, node]) => {
        const nodePath = currentPath ? `${currentPath}/${name}` : name;
        if (!node.isLeaf) {
          allPaths.add(nodePath);
        }
        if (node.children) {
          collectPaths(node.children, nodePath);
        }
      });
    }
    collectPaths(filteredTree);
    setExpandedNodes(allPaths);
  }, [filteredTree]);

  // Collapse all
  const collapseAll = useCallback(() => {
    setExpandedNodes(new Set());
  }, []);

  // Check if node is expanded
  const isExpanded = useCallback(
    (path: string) => {
      return expandedNodes.has(path);
    },
    [expandedNodes]
  );

  return {
    tree: filteredTree,
    findNode,
    expandedNodes,
    toggleNode,
    expandNode,
    collapseNode,
    expandAll,
    collapseAll,
    isExpanded,
  };
}

