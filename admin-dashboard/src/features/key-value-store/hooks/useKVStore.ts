/**
 * Hook for KV Store operations
 */

import {
  useListKVEntries,
  useGetKVEntry,
  usePutKVEntry,
  useDeleteKVEntry,
} from "@lib/api/generated/key-value-store/key-value-store";
import type {
  ListKVEntriesParams,
  GetKVEntryParams,
  KVPutRequest,
  DeleteKVEntryParams,
} from "@lib/api/models";
import { toast } from "@lib/toast/toast";
import { handleApiError } from "@lib/api/errorHandler";

export interface UseKVStoreOptions {
  serviceId: string;
  /** Current prefix for navigation */
  prefix?: string;
  /** Selected entry path (for fetching entry value) */
  selectedPath?: string;
  /** List parameters (optional, defaults to keysOnly=true, recurse=false) */
  listParams?: ListKVEntriesParams;
  /** Get entry parameters */
  getParams?: GetKVEntryParams;
}

export function useKVStore(options: UseKVStoreOptions) {
  const {
    serviceId,
    prefix = "",
    selectedPath,
    listParams,
    getParams,
  } = options;

  // Optimized list query: keysOnly=true, recurse=true for proper List/Object detection
  // We need recurse=true to see all keys (including .manifest) for detecting List/Object prefixes
  // The filtering logic will handle showing only immediate children in the UI
  const optimizedListParams: ListKVEntriesParams = {
    prefix,
    keysOnly: true,
    recurse: true, // Changed to true to enable List/Object detection
    ...listParams, // Allow override if needed
  };

  // List keys (for navigation)
  const listQuery = useListKVEntries(
    serviceId,
    optimizedListParams,
    {
      query: {
        enabled: !!serviceId,
        staleTime: 10_000,
      },
    }
  );

  // Get single entry (only when a leaf node is selected)
  const getQuery = useGetKVEntry(
    serviceId,
    selectedPath || "",
    getParams,
    {
      query: {
        enabled: !!serviceId && !!selectedPath,
        staleTime: 10_000,
      },
    }
  );

  // Put mutation
  const putMutation = usePutKVEntry({
    mutation: {
      onSuccess: () => {
        toast.success("KV entry saved successfully");
        // Invalidate queries
        listQuery.refetch();
        if (selectedPath) {
          getQuery.refetch();
        }
      },
      onError: (error) => {
        if (error && typeof error === "object" && "status" in error) {
          const status = (error as { status?: number }).status;
          if (status === 409) {
            toast.error("CAS conflict: Entry was modified by another user. Please refresh and try again.");
          } else {
            handleApiError(error);
          }
        } else {
          handleApiError(error);
        }
      },
    },
  });

  // Delete mutation
  const deleteMutation = useDeleteKVEntry({
    mutation: {
      onSuccess: () => {
        toast.success("KV entry deleted successfully");
        // Invalidate queries
        listQuery.refetch();
        if (selectedPath) {
          getQuery.refetch();
        }
      },
      onError: (error: unknown) => {
        handleApiError(error, {});
      },
    },
  });

  const putEntry = async (
    entryPath: string,
    data: KVPutRequest
  ): Promise<void> => {
    return new Promise((resolve, reject) => {
      putMutation.mutate(
        {
          serviceId,
          path: entryPath,
          data,
        },
        {
          onSuccess: () => resolve(),
          onError: (error) => reject(error),
        }
      );
    });
  };

  const deleteEntry = async (
    entryPath: string,
    params?: DeleteKVEntryParams
  ): Promise<void> => {
    return new Promise((resolve, reject) => {
      deleteMutation.mutate(
        {
          serviceId,
          path: entryPath,
          params,
        },
        {
          onSuccess: () => resolve(),
          onError: (error) => reject(error),
        }
      );
    });
  };

  // Extract keys from response (handles both KeysResponse and ListResponse)
  const keys = listQuery.data
    ? "keys" in listQuery.data && Array.isArray(listQuery.data.keys)
      ? listQuery.data.keys
      : []
    : [];

  return {
    // List data (keys)
    keys,
    keysLoading: listQuery.isLoading,
    keysError: listQuery.error,
    refetchKeys: listQuery.refetch,
    // Legacy support
    entries: listQuery.data,
    entriesLoading: listQuery.isLoading,
    entriesError: listQuery.error,
    refetchEntries: listQuery.refetch,

    // Get data
    entry: getQuery.data,
    entryLoading: getQuery.isLoading,
    entryError: getQuery.error,
    refetchEntry: getQuery.refetch,

    // Mutations
    putEntry,
    deleteEntry,
    isPutting: putMutation.isPending,
    isDeleting: deleteMutation.isPending,
  };
}

