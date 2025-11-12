/**
 * Type adapters for KV List data structures
 * 
 * Note: The generated types have a nested structure `{[key: string]: { [key: string]: unknown }}`
 * but the Java API accepts `Map<String, Object>` (equivalent to `Record<string, any>`).
 * These adapters convert between UI representation and generated types.
 */

import type { KVListItem } from "@lib/api/models/kVListItem";
import type { KVListItemData} from "@lib/api/models/kVListItemData";

/**
 * Convert UI representation (Record<string, any>) to KVListItemData
 * The generated type expects nested objects, but we'll flatten it
 */
export function toKVListItemData(data: Record<string, unknown>): KVListItemData {
  // The API accepts any object structure, so we'll just cast it
  // At runtime, the API will handle it correctly as Map<String, Object>
  return data as unknown as KVListItemData;
}

/**
 * Convert KVListItemData to UI representation (Record<string, any>)
 */
export function fromKVListItemData(data?: KVListItemData): Record<string, unknown> {
  if (!data) {
    return {};
  }
  // The data structure is actually flat at runtime, so we can safely cast
  return data as unknown as Record<string, unknown>;
}

/**
 * UI representation of list item (with Record<string, unknown> data)
 */
export type UIListItem = {
  id: string;
  data: Record<string, unknown>;
};

/**
 * Convert UI KVListItem (with Record<string, unknown> data) to generated KVListItem
 */
export function toGeneratedKVListItem(item: UIListItem): KVListItem {
  return {
    id: item.id,
    data: toKVListItemData(item.data),
  };
}

/**
 * Convert generated KVListItem to UI representation
 */
export function fromGeneratedKVListItem(item?: KVListItem): UIListItem | undefined {
  if (!item || !item.id) {
    return undefined;
  }
  return {
    id: item.id,
    data: fromKVListItemData(item.data),
  };
}

/**
 * Convert array of UI KVListItems to generated KVListItem array
 */
export function toGeneratedKVListItemArray(items: UIListItem[]): KVListItem[] {
  return items.map(toGeneratedKVListItem);
}

/**
 * Convert array of generated KVListItems to UI representation
 */
export function fromGeneratedKVListItemArray(items?: KVListItem[]): UIListItem[] {
  if (!items) {
    return [];
  }
  return items.map(fromGeneratedKVListItem).filter((item): item is UIListItem => item !== undefined);
}

/**
 * Convert metadata from UI representation to KVListManifestMetadata
 * Note: The generated type has nested structure, but API accepts Map<String, Object>
 */
export function toKVListManifestMetadata(metadata?: Record<string, unknown>): {[key: string]: { [key: string]: unknown }} | undefined {
  if (!metadata) {
    return undefined;
  }
  // At runtime, the API accepts Map<String, Object>, so we can safely cast
  return metadata as unknown as {[key: string]: { [key: string]: unknown }};
}

/**
 * Convert metadata from KVListManifestMetadata to UI representation
 */
export function fromKVListManifestMetadata(metadata?: {[key: string]: { [key: string]: unknown }}): Record<string, unknown> {
  if (!metadata) {
    return {};
  }
  // The data structure is actually flat at runtime, so we can safely cast
  return metadata as unknown as Record<string, unknown>;
}

