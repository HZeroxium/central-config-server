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
  path?: string;
  listParams?: ListKVEntriesParams;
  getParams?: GetKVEntryParams;
}

export function useKVStore(options: UseKVStoreOptions) {
  const { serviceId, path, listParams, getParams } = options;

  // List entries
  const listQuery = useListKVEntries(
    serviceId,
    listParams,
    {
      query: {
        enabled: !!serviceId,
        staleTime: 10_000,
      },
    }
  );

  // Get single entry
  const getQuery = useGetKVEntry(
    serviceId,
    path || "",
    getParams,
    {
      query: {
        enabled: !!serviceId && !!path,
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
        if (path) {
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
        if (path) {
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

  return {
    // List data
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

