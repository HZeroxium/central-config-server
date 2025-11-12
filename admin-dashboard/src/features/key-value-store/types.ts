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
 * Filters out List internal keys but preserves List prefixes
 * 
 * @param keys List of all keys
 * @param prefix Current prefix (default: "")
 */
export function getImmediateChildren(
  keys: string[],
  prefix: string = ""
): { folders: string[]; files: string[] } {
  // Get filtered keys (includes List prefixes)
  const filteredKeys = filterListKeys(keys);
  const normalizedPrefix = normalizePath(prefix);
  const folders = new Set<string>();
  const files = new Set<string>();

  filteredKeys.forEach((key) => {
    const normalizedKey = normalizePath(key);
    
    // Skip if key doesn't match prefix
    if (normalizedPrefix) {
      if (!normalizedKey.startsWith(normalizedPrefix + "/")) {
        return;
      }
    } else {
      // Root level - skip keys that are nested (they'll be shown when navigating into folders)
      if (normalizedKey.includes("/")) {
        return;
      }
    }

    // Get relative path
    const relativePath = normalizedPrefix
      ? normalizedKey.substring(normalizedPrefix.length + 1)
      : normalizedKey;

    // Skip empty relative path (shouldn't happen, but safeguard)
    if (!relativePath || relativePath === normalizedPrefix) return;

    const segments = relativePath.split("/").filter(Boolean);
    
    // Get the first segment (immediate child)
    if (segments.length > 0) {
      const firstSegment = segments[0];
      const childPath = normalizedPrefix
        ? `${normalizedPrefix}/${firstSegment}`
        : firstSegment;

      // Use ORIGINAL keys array for detection (not filtered)
      // This ensures we correctly identify List prefixes even if their children are filtered
      const isList = isListPrefix(childPath, keys);
      
      if (isList) {
        // List prefixes are treated as folders (single entity, not navigable)
        folders.add(childPath);
      } else if (segments.length > 1 || isFolderKey(key, filteredKeys)) {
        // Regular folder (has children or is a prefix of other keys)
        folders.add(childPath);
      } else {
        // Regular file/leaf entry
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
 * Filters out List internal keys and marks List nodes appropriately
 * 
 * @param keys List of all keys
 * @param prefix Current prefix (default: "")
 */
export function buildKVTreeFromKeys(
  keys: string[],
  prefix: string = ""
): KVTree {
  // Get filtered keys (includes List prefixes)
  const filteredKeys = filterListKeys(keys);
  const tree: KVTree = {};
  const normalizedPrefix = normalizePath(prefix);

  // Process each filtered key
  filteredKeys.forEach((key) => {
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

      // Build full path
      const parentSegments = segments.slice(0, index);
      const fullPath = normalizedPrefix
        ? `${normalizedPrefix}/${[...parentSegments, segment].join("/")}`
        : [...parentSegments, segment].join("/");
      const normalizedFullPath = normalizePath(fullPath);

      // Use ORIGINAL keys array for detection (not filtered)
      const isList = isListPrefix(normalizedFullPath, keys);

      if (!current[segment]) {
        current[segment] = {
          name: segment,
          fullPath: normalizedFullPath,
          // List prefixes should be treated as folders (single entity)
          nodeType: isLast && !isList ? "file" : "folder",
          isLeaf: isLast && !isList,
          children: isLast && !isList ? undefined : {},
        };
      }

      // If this is a List prefix, don't navigate deeper (stop processing children)
      if (isList) {
        return;
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
 * Allows '/' characters in paths (for nested paths)
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

  // Check for empty segments (double slashes) - but allow single slashes
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

  // Check for invalid characters (basic check) - but allow '/' in the path itself
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

/**
 * Check if a prefix represents a List structure (has .manifest key)
 */
export function isListPrefix(prefix: string, keys: string[]): boolean {
  const normalizedPrefix = normalizePath(prefix);
  return keys.some((key) => {
    const normalizedKey = normalizePath(key);
    return (
      normalizedKey === `${normalizedPrefix}/.manifest` ||
      (normalizedKey === ".manifest" && normalizedPrefix === "")
    );
  });
}

/**
 * Check if a prefix represents a Folder structure (has children but not List)
 * Folder has children and is used for navigation/organization
 * 
 * This is the default type for prefixes with children that are not explicitly LIST
 */
export function isFolderPrefix(prefix: string, keys: string[]): boolean {
  // Not a List prefix
  if (isListPrefix(prefix, keys)) {
    return false;
  }
  
  const normalizedPrefix = normalizePath(prefix);
  
  // Get direct children of this prefix
  const directChildren = keys.filter((key) => {
    const normalizedKey = normalizePath(key);
    if (!normalizedKey.startsWith(normalizedPrefix + "/")) {
      return false;
    }
    // Skip .manifest keys (they indicate LIST)
    if (normalizedKey === `${normalizedPrefix}/.manifest`) {
      return false;
    }
    // Check if it's a direct child (no intermediate segments)
    const relativePath = normalizedKey.substring(normalizedPrefix.length + 1);
    return !relativePath.includes("/");
  });
  
  // Folder has children (for navigation/organization)
  return directChildren.length > 0;
}

/**
 * Get all List prefixes from keys
 * 
 * LIST prefixes can be reliably detected from keys alone (via .manifest).
 */
export function getListPrefixes(keys: string[]): Set<string> {
  const prefixes = new Set<string>();
  const processedPrefixes = new Set<string>();

  // Detect LIST prefixes (have .manifest key)
  keys.forEach((key) => {
    const normalizedKey = normalizePath(key);
    
    // Check if this is a .manifest key
    if (normalizedKey.endsWith("/.manifest")) {
      const prefix = normalizedKey.slice(0, -"/.manifest".length);
      if (prefix && !processedPrefixes.has(prefix)) {
        prefixes.add(prefix);
        processedPrefixes.add(prefix);
      }
    } else if (normalizedKey === ".manifest") {
      // Root level list
      if (!processedPrefixes.has("")) {
        prefixes.add("");
        processedPrefixes.add("");
      }
    }
  });

  return prefixes;
}

/**
 * Filter out List internal keys from keys list
 * Removes .manifest keys and keys that are children of List prefixes
 * BUT preserves List prefixes themselves so they can be displayed
 * 
 * @param keys List of all keys
 */
export function filterListKeys(keys: string[]): string[] {
  const listPrefixes = getListPrefixes(keys);
  const filtered = new Set<string>();
  
  // First, add all List prefixes (they should be visible)
  listPrefixes.forEach(prefix => {
    filtered.add(normalizePath(prefix));
  });
  
  // Then, add regular keys that are NOT internal to List
  keys.forEach((key) => {
    const normalizedKey = normalizePath(key);
    
    // Skip .manifest keys
    if (normalizedKey.endsWith("/.manifest") || normalizedKey === ".manifest") {
      return;
    }
    
    // Skip keys that are children of List prefixes
    let isInternal = false;
    for (const prefix of listPrefixes) {
      const normalizedPrefix = normalizePath(prefix);
      if (normalizedPrefix) {
        // Check if key is a child of this prefix (but not the prefix itself)
        if (normalizedKey.startsWith(normalizedPrefix + "/") && 
            normalizedKey !== normalizedPrefix) {
          isInternal = true;
          break;
        }
      } else {
        // Root prefix case - check if key is a direct child of root List
        const segments = normalizedKey.split("/");
        if (segments.length === 1) {
          // Top-level key - check if it's a List prefix
          if (listPrefixes.has(segments[0])) {
            // This is the prefix itself, already added above
            return;
          }
        } else if (segments.length > 1) {
          // Nested key - check if first segment is a List prefix
          const firstSegment = segments[0];
          if (listPrefixes.has(firstSegment)) {
            // This is a child of a root-level List prefix
            isInternal = true;
            break;
          }
        }
      }
    }
    
    if (!isInternal) {
      filtered.add(normalizedKey);
    }
  });
  
  return Array.from(filtered);
}

