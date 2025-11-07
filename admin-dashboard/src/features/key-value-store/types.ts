/**
 * Type definitions and utilities for Key-Value Store feature
 */

import type {
  KVEntryResponse,
  KVListResponse,
  KVKeysResponse,
  KVPutRequest,
  KVPutRequestEncoding,
} from "@lib/api/models";

/**
 * Type alias for KV entry
 */
export type KVEntry = KVEntryResponse;

/**
 * Type alias for KV list response
 */
export type KVList = KVListResponse;

/**
 * Type alias for KV keys response
 */
export type KVKeys = KVKeysResponse;

/**
 * Type alias for KV put request
 */
export type KVPut = KVPutRequest;

/**
 * Type alias for encoding
 */
export type KVEncoding = KVPutRequestEncoding;

/**
 * Tree node structure for hierarchical display
 */
export interface KVTreeNode {
  /** Node name (folder or file name) */
  name: string;
  /** Full path from root */
  fullPath: string;
  /** Whether this is a leaf node (file) */
  isLeaf: boolean;
  /** Child nodes */
  children?: Record<string, KVTreeNode>;
  /** Entry data if this is a leaf */
  entry?: KVEntry;
}

/**
 * Tree structure root
 */
export type KVTree = Record<string, KVTreeNode>;

/**
 * Normalize path by removing leading/trailing slashes and empty segments
 */
export function normalizePath(path: string): string {
  if (!path) return "";
  // Remove leading slash
  let normalized = path.startsWith("/") ? path.substring(1) : path;
  // Remove trailing slash
  normalized = normalized.endsWith("/") ? normalized.slice(0, -1) : normalized;
  return normalized;
}

/**
 * Encode path for URL (handle special characters)
 */
export function encodePath(path: string): string {
  return encodeURIComponent(path);
}

/**
 * Decode path from URL
 */
export function decodePath(encodedPath: string): string {
  return decodeURIComponent(encodedPath);
}

/**
 * Decode base64 value to string
 */
export function decodeBase64(base64: string): string {
  try {
    return atob(base64);
  } catch (e) {
    return base64; // Return as-is if not valid base64
  }
}

/**
 * Encode string to base64
 */
export function encodeBase64(str: string): string {
  try {
    return btoa(str);
  } catch (e) {
    return str; // Return as-is if encoding fails
  }
}

/**
 * Get parent path from a given path
 */
export function getParentPath(path: string): string {
  const normalized = normalizePath(path);
  if (!normalized) return "";
  const parts = normalized.split("/");
  parts.pop();
  return parts.join("/");
}

/**
 * Get path segments
 */
export function getPathSegments(path: string): string[] {
  const normalized = normalizePath(path);
  if (!normalized) return [];
  return normalized.split("/").filter(Boolean);
}

/**
 * Transform flat key list into hierarchical tree structure
 */
export function buildKVTree(
  entries: KVEntry[],
  prefix: string = ""
): KVTree {
  const tree: KVTree = {};
  const normalizedPrefix = normalizePath(prefix);

  entries.forEach((entry) => {
    const entryPath = normalizePath(entry.path || "");
    if (!entryPath) return;

    // Skip if entry doesn't match prefix
    if (normalizedPrefix && !entryPath.startsWith(normalizedPrefix + "/")) {
      return;
    }

    // Get relative path from prefix
    const relativePath = normalizedPrefix
      ? entryPath.substring(normalizedPrefix.length + 1)
      : entryPath;

    if (!relativePath) return;

    const segments = relativePath.split("/");
    let current = tree;

    segments.forEach((segment, index) => {
      const isLast = index === segments.length - 1;

      if (!current[segment]) {
        const parentPath = segments.slice(0, index).join("/");
        const fullPath = normalizedPrefix
          ? `${normalizedPrefix}/${parentPath ? `${parentPath}/` : ""}${segment}`
          : segment;

        current[segment] = {
          name: segment,
          fullPath: normalizePath(fullPath),
          isLeaf: isLast,
          children: isLast ? undefined : {},
          entry: isLast ? entry : undefined,
        };
      }

      if (!isLast && current[segment].children) {
        current = current[segment].children!;
      }
    });
  });

  return tree;
}

/**
 * Flatten tree structure to array of entries
 */
export function flattenKVTree(tree: KVTree): KVEntry[] {
  const entries: KVEntry[] = [];

  function traverse(node: KVTreeNode) {
    if (node.isLeaf && node.entry) {
      entries.push(node.entry);
    }
    if (node.children) {
      Object.values(node.children).forEach(traverse);
    }
  }

  Object.values(tree).forEach(traverse);
  return entries;
}

/**
 * Find node in tree by path
 */
export function findNodeInTree(
  tree: KVTree,
  path: string
): KVTreeNode | null {
  const normalizedPath = normalizePath(path);
  if (!normalizedPath) return null;

  const segments = getPathSegments(normalizedPath);
  let current: KVTree | KVTreeNode | undefined = tree;

  for (const segment of segments) {
    if (!current || typeof current !== "object" || !("name" in current)) {
      if (typeof current === "object" && current && segment in current) {
        current = (current as KVTree)[segment];
      } else {
        return null;
      }
    } else {
      const node = current as KVTreeNode;
      if (!node.children || !node.children[segment]) {
        return null;
      }
      current = node.children[segment];
    }
  }

  return current as KVTreeNode | null;
}

/**
 * Check if path is a valid KV path
 */
export function isValidKVPath(path: string): boolean {
  if (!path) return false;
  // Basic validation: no empty segments, no double slashes
  const normalized = normalizePath(path);
  return normalized.split("/").every((segment) => segment.length > 0);
}

