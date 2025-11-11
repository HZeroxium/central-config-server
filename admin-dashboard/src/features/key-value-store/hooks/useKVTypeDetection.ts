/**
 * Hook for detecting KV entry type (LEAF, OBJECT, LIST)
 */

import { useMemo } from "react";
import type { KVEntry } from "../types";

export const KVType = {
  LEAF: "LEAF",
  OBJECT: "OBJECT",
  LIST: "LIST",
} as const;

export type KVType = (typeof KVType)[keyof typeof KVType];

export interface UseKVTypeDetectionOptions {
  entry?: KVEntry;
  /** List of child keys (for structure-based detection) */
  childKeys?: string[];
  /** Whether this is a prefix (has children) */
  isPrefix?: boolean;
}

/**
 * Detects the type of a KV entry based on flags and structure.
 * 
 * Type detection logic:
 * 1. Check flags: 0=LEAF, 1=OBJECT, 2=LIST
 * 2. Fallback to structure-based detection:
 *    - If has .manifest key → LIST
 *    - If has children → OBJECT
 *    - Otherwise → LEAF
 */
export function useKVTypeDetection(
  options: UseKVTypeDetectionOptions
): KVType {
  const { entry, childKeys = [], isPrefix = false } = options;

  return useMemo(() => {
    // 1. Check flags first (most authoritative)
    if (entry?.flags !== undefined && entry.flags !== null) {
      if (entry.flags === 1) {
        return KVType.OBJECT;
      }
      if (entry.flags === 2) {
        return KVType.LIST;
      }
      // flags === 0 or other values default to LEAF
    }

    // 2. Structure-based detection (fallback)
    if (isPrefix && childKeys.length > 0) {
      // Check if there's a manifest key (indicates LIST)
      const hasManifest = childKeys.some(
        (key) => key.endsWith("/.manifest") || key === ".manifest"
      );
      if (hasManifest) {
        return KVType.LIST;
      }

      // If has children but no manifest, assume OBJECT
      return KVType.OBJECT;
    }

    // Default to LEAF
    return KVType.LEAF;
  }, [entry?.flags, childKeys, isPrefix]);
}

