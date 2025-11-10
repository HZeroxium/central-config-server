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
 * Node type: folder or file
 */
export type KVNodeType = "folder" | "file";

/**
 * Path validation result
 */
export interface PathValidationResult {
  isValid: boolean;
  error?: string;
  warning?: string;
}

/**
 * Navigation state for KV store
 */
export interface KVNavigationState {
  currentPrefix: string;
  selectedPath?: string;
}

/**
 * Tree node structure for hierarchical display
 */
export interface KVTreeNode {
  /** Node name (folder or file name) */
  name: string;
  /** Full path from root */
  fullPath: string;
  /** Node type: folder or file */
  nodeType: KVNodeType;
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
 * Check if a key represents a folder (has trailing slash or is a prefix of other keys)
 */
export function isFolderKey(key: string, allKeys: string[]): boolean {
  // If key ends with /, it's a folder
  if (key.endsWith("/")) {
    return true;
  }
  
  // If there are other keys that start with this key + "/", it's a folder
  const normalizedKey = normalizePath(key);
  return allKeys.some(
    (k) => k !== key && normalizePath(k).startsWith(normalizedKey + "/")
  );
}

/**
 * Get immediate children of a prefix from keys list
 */
export function getImmediateChildren(
  keys: string[],
  prefix: string = ""
): { folders: string[]; files: string[] } {
  const normalizedPrefix = normalizePath(prefix);
  const folders = new Set<string>();
  const files = new Set<string>();

  keys.forEach((key) => {
    const normalizedKey = normalizePath(key);
    
    // Skip if key doesn't match prefix
    if (normalizedPrefix) {
      if (!normalizedKey.startsWith(normalizedPrefix + "/")) {
        return;
      }
    }

    // Get relative path
    const relativePath = normalizedPrefix
      ? normalizedKey.substring(normalizedPrefix.length + 1)
      : normalizedKey;

    if (!relativePath) return;

    const segments = relativePath.split("/").filter(Boolean);
    
    // Get the first segment (immediate child)
    if (segments.length > 0) {
      const firstSegment = segments[0];
      const childPath = normalizedPrefix
        ? `${normalizedPrefix}/${firstSegment}`
        : firstSegment;

      // Check if it's a folder (has more segments) or a file (exact match)
      if (segments.length > 1 || isFolderKey(key, keys)) {
        folders.add(childPath);
      } else {
        files.add(childPath);
      }
    }
  });

  return {
    folders: Array.from(folders).sort(),
    files: Array.from(files).sort(),
  };
}

/**
 * Build tree from keys list (for keys-only responses)
 */
export function buildKVTreeFromKeys(
  keys: string[],
  prefix: string = ""
): KVTree {
  const tree: KVTree = {};
  const normalizedPrefix = normalizePath(prefix);

  // Process each key
  keys.forEach((key) => {
    const keyPath = normalizePath(key);
    if (!keyPath) return;

    // Skip if key doesn't match prefix
    if (normalizedPrefix && !keyPath.startsWith(normalizedPrefix + "/")) {
      return;
    }

    // Get relative path from prefix
    const relativePath = normalizedPrefix
      ? keyPath.substring(normalizedPrefix.length + 1)
      : keyPath;

    if (!relativePath) return;

    const segments = relativePath.split("/").filter(Boolean);
    let current = tree;

    segments.forEach((segment, index) => {
      const isLast = index === segments.length - 1;

      if (!current[segment]) {
        // Build full path
        const parentSegments = segments.slice(0, index);
        const fullPath = normalizedPrefix
          ? `${normalizedPrefix}/${[...parentSegments, segment].join("/")}`
          : [...parentSegments, segment].join("/");

        current[segment] = {
          name: segment,
          fullPath: normalizePath(fullPath),
          nodeType: isLast ? "file" : "folder",
          isLeaf: isLast,
          children: isLast ? undefined : {},
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
 * Transform flat key list into hierarchical tree structure
 * (For entries with values)
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

    const segments = relativePath.split("/").filter(Boolean);
    let current = tree;

    segments.forEach((segment, index) => {
      const isLast = index === segments.length - 1;

      if (!current[segment]) {
        const parentSegments = segments.slice(0, index);
        const fullPath = normalizedPrefix
          ? `${normalizedPrefix}/${[...parentSegments, segment].join("/")}`
          : [...parentSegments, segment].join("/");

        current[segment] = {
          name: segment,
          fullPath: normalizePath(fullPath),
          nodeType: isLast ? "file" : "folder",
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
      Object.values(node.children).forEach((child) => traverse(child));
    }
  }

  Object.values(tree).forEach((node) => traverse(node));
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
 * Validate KV path with detailed error messages
 */
export function validateKVPath(
  path: string,
  strict: boolean = false
): PathValidationResult {
  if (!path) {
    return {
      isValid: false,
      error: strict ? "Path is required" : undefined,
      warning: !strict ? "Path cannot be empty" : undefined,
    };
  }

  const normalized = normalizePath(path);

  // Check for empty segments (double slashes)
  if (path.includes("//")) {
    return {
      isValid: false,
      error: strict ? "Path contains invalid double slashes" : undefined,
      warning: !strict ? "Path contains double slashes" : undefined,
    };
  }

  const segments = normalized.split("/").filter(Boolean);

  // Check for empty segments
  if (segments.some((seg) => seg.length === 0)) {
    return {
      isValid: false,
      error: strict ? "Path contains empty segments" : undefined,
      warning: !strict ? "Path should not contain empty segments" : undefined,
    };
  }

  // Check for invalid characters (basic check)
  const invalidChars = /[<>:"|?*\x00-\x1f]/;
  for (const segment of segments) {
    if (invalidChars.test(segment)) {
      return {
        isValid: false,
        error: strict
          ? `Invalid character in path segment: ${segment}`
          : undefined,
        warning: !strict
          ? `Path segment contains invalid characters: ${segment}`
          : undefined,
      };
    }
  }

  // Check for reserved names (Windows-style, but we'll be conservative)
  const reservedNames = ["CON", "PRN", "AUX", "NUL"];
  const upperSegments = segments.map((s) => s.toUpperCase());
  for (const reserved of reservedNames) {
    if (upperSegments.includes(reserved)) {
      return {
        isValid: false,
        error: strict ? `Reserved name not allowed: ${reserved}` : undefined,
        warning: !strict
          ? `Path contains reserved name: ${reserved}`
          : undefined,
      };
    }
  }

  // Check path length (reasonable limit)
  if (normalized.length > 512) {
    return {
      isValid: false,
      error: strict ? "Path is too long (max 512 characters)" : undefined,
      warning: !strict ? "Path is very long" : undefined,
    };
  }

  return { isValid: true };
}

/**
 * Check if path is a valid KV path (simple boolean check)
 */
export function isValidKVPath(path: string): boolean {
  return validateKVPath(path, true).isValid;
}

