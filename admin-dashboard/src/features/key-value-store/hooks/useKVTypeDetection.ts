/**
 * Hook for detecting KV entry type (LEAF, LIST, FOLDER)
 */

import { useMemo } from "react";
import type { KVEntry } from "../types";
import { isListPrefix, isFolderPrefix } from "../types";

export const KVType = {
  LEAF: "LEAF",
  LIST: "LIST",
  FOLDER: "FOLDER",
} as const;

export type KVType = (typeof KVType)[keyof typeof KVType];

export interface UseKVTypeDetectionOptions {
  entry?: KVEntry;
  /** List of child keys (for structure-based detection) */
  childKeys?: string[];
  /** Whether this is a prefix (has children) */
  isPrefix?: boolean;
  /** All keys (for detecting List prefixes) */
  allKeys?: string[];
  /** Path of the entry (for detecting List prefixes) */
  path?: string;
}

/**
 * Detects the type of a KV entry based on flags and structure.
 * 
 * Type detection priority (flags-first approach):
 * 1. Check entry flags (if entry exists): flags=2 → LIST, flags=0 → LEAF
 * 2. Check for .manifest key → LIST
 * 3. Check if prefix has children → FOLDER (if not LIST)
 * 4. Default → LEAF
 * 
 * Notes:
 * - Flags are authoritative when available
 * - LIST can be detected from .manifest key even without flags
 * - FOLDER is the default for prefixes with children that aren't LIST
 * - Flag value 1 (previously OBJECT) is reserved and falls back to LEAF/FOLDER
 */
export function useKVTypeDetection(
  options: UseKVTypeDetectionOptions
): KVType {
  const { entry, childKeys = [], isPrefix = false, allKeys = [], path } = options;

  return useMemo(() => {
    // 1. Check flags first (most authoritative)
    // Flags: 0=LEAF, 2=LIST (flag=1 is reserved, falls back to LEAF/FOLDER)
    if (entry?.flags !== undefined && entry.flags !== null) {
      if (entry.flags === 2) {
        return KVType.LIST;
      }
      // flags === 0 explicitly means LEAF (or flag=1 reserved, treat as LEAF)
      if (entry.flags === 0 || entry.flags === 1) {
        // But check if it's actually a prefix with children (should be FOLDER)
        const keysForDetection = allKeys.length > 0 ? allKeys : childKeys;
        const pathToCheck = path || entry?.path || "";
        if (pathToCheck && keysForDetection.length > 0) {
          // Check if this path has children (it's a prefix)
          if (isFolderPrefix(pathToCheck, keysForDetection)) {
            return KVType.FOLDER;
          }
        }
        return KVType.LEAF;
      }
    }

    // 2. Structure-based detection (fallback when flags not available)
    // Use allKeys if available for better detection
    const keysForDetection = allKeys.length > 0 ? allKeys : childKeys;
    const pathToCheck = path || entry?.path || "";

    if (pathToCheck && keysForDetection.length > 0) {
      // Check if this path is a List prefix (has .manifest)
      if (isListPrefix(pathToCheck, keysForDetection)) {
        return KVType.LIST;
      }
      
      // Check if this path is a Folder prefix (has children but not List)
      if (isFolderPrefix(pathToCheck, keysForDetection)) {
        return KVType.FOLDER;
      }
    }

    // 3. Legacy detection using childKeys and isPrefix
    if (isPrefix && childKeys.length > 0) {
      // Check if there's a manifest key (indicates LIST)
      const hasManifest = childKeys.some(
        (key) => key.endsWith("/.manifest") || key === ".manifest"
      );
      if (hasManifest) {
        return KVType.LIST;
      }

      // If has children but no manifest, treat as FOLDER
      return KVType.FOLDER;
    }

    // 4. Default to LEAF
    return KVType.LEAF;
  }, [entry?.flags, entry?.path, childKeys, isPrefix, allKeys, path]);
}

