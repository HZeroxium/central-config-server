export { useKVStore } from "./useKVStore";
export { useKVPermissions } from "./useKVPermissions";
export { useKVTree } from "./useKVTree";
export { useKVTypeDetection, KVType } from "./useKVTypeDetection";
export { useKVPrefixView } from "./useKVPrefixView";

// Export generated hooks for KV List and Object operations
export {
  useGetKVList,
  usePutKVList,
  useGetKVObject,
  usePutKVObject,
} from "@lib/api/generated/key-value-store/key-value-store";

// Export generated types
export type {
  KVListResponseV2,
  KVListWriteRequest,
  KVListItem,
  KVObjectResponse,
  KVObjectWriteRequest,
  KVTransactionResponse,
  KVTransactionResult,
  GetKVListParams,
  PutKVListParams,
  GetKVObjectParams,
  PutKVObjectParams,
} from "@lib/api/models";

export type {
  KVListManifest,
  KVListItemData,
  KVObjectResponseData,
  KVObjectWriteRequestData,
} from "@lib/api/models";

// Export custom hook types
export type { UseKVStoreOptions } from "./useKVStore";
export type { KVPermissions } from "./useKVPermissions";
export type { UseKVTreeOptions } from "./useKVTree";
export type { UseKVTypeDetectionOptions } from "./useKVTypeDetection";
export type { KVStructuredFormat, UseKVPrefixViewOptions } from "./useKVPrefixView";

