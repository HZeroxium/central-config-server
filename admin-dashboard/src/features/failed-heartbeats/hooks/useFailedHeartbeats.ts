import { useQueryClient } from "@tanstack/react-query";
import {
  useFindAllFailedHeartbeats,
  useFindFailedHeartbeatById,
  useRedriveFailedHeartbeat,
  useUpdateFailedHeartbeatStatus,
  useBulkUpdateFailedHeartbeatStatus,
} from "@lib/api/hooks";
import {
  getFindAllFailedHeartbeatsQueryKey,
  getFindFailedHeartbeatByIdQueryKey,
} from "@lib/api/generated/failed-heartbeats/failed-heartbeats";
import { toast } from "@lib/toast/toast";
import { handleApiError } from "@lib/api/errorHandler";
import type {
  FindAllFailedHeartbeatsParams,
  UpdateStatusRequest,
  RedriveRequest,
  BulkUpdateStatusRequest,
} from "@lib/api/models";

export function useFailedHeartbeatsList(params?: FindAllFailedHeartbeatsParams) {
  const {
    data,
    isLoading,
    error,
    refetch,
  } = useFindAllFailedHeartbeats(params, {
    query: {
      staleTime: 10_000,
      refetchInterval: 30_000, // Auto-refresh every 30s
    },
  });

  return {
    failedHeartbeats: data?.items || [],
    metadata: data?.metadata,
    isLoading,
    error,
    refetch,
  };
}

export function useFailedHeartbeatDetail(id: string) {
  const {
    data: failedHeartbeat,
    isLoading,
    error,
    refetch,
  } = useFindFailedHeartbeatById(id, {
    query: {
      enabled: !!id,
      staleTime: 10_000,
    },
  });

  return {
    failedHeartbeat,
    isLoading,
    error,
    refetch,
  };
}

export function useFailedHeartbeatOperations() {
  const queryClient = useQueryClient();

  const redriveMutation = useRedriveFailedHeartbeat();
  const updateStatusMutation = useUpdateFailedHeartbeatStatus();
  const bulkUpdateMutation = useBulkUpdateFailedHeartbeatStatus();

  const redrive = async (id: string, force = false) => {
    const request: RedriveRequest = { force };
    redriveMutation.mutate(
      { id, data: request },
      {
        onSuccess: () => {
          toast.success("Failed heartbeat re-driven successfully");
          queryClient.invalidateQueries({
            queryKey: getFindAllFailedHeartbeatsQueryKey(),
          });
          queryClient.invalidateQueries({
            queryKey: getFindFailedHeartbeatByIdQueryKey(id),
          });
        },
        onError: (error) => {
          handleApiError(error);
        },
      }
    );
  };

  const updateStatus = async (
    id: string,
    status: UpdateStatusRequest["status"],
    notes?: string
  ) => {
    const request: UpdateStatusRequest = { status, notes };
    updateStatusMutation.mutate(
      { id, data: request },
      {
        onSuccess: () => {
          toast.success("Status updated successfully");
          queryClient.invalidateQueries({
            queryKey: getFindAllFailedHeartbeatsQueryKey(),
          });
          queryClient.invalidateQueries({
            queryKey: getFindFailedHeartbeatByIdQueryKey(id),
          });
        },
        onError: (error) => {
          handleApiError(error);
        },
      }
    );
  };

  const bulkUpdateStatus = async (
    ids: string[],
    status: UpdateStatusRequest["status"],
  ) => {
    const request: BulkUpdateStatusRequest = {
      ids,
      status,
    };
    bulkUpdateMutation.mutate(
      { data: request },
      {
        onSuccess: (count) => {
          toast.success(`Updated ${count} failed heartbeat(s)`);
          queryClient.invalidateQueries({
            queryKey: getFindAllFailedHeartbeatsQueryKey(),
          });
        },
        onError: (error) => {
          handleApiError(error);
        },
      }
    );
  };

  return {
    redrive,
    updateStatus,
    bulkUpdateStatus,
    isRedriving: redriveMutation.isPending,
    isUpdating: updateStatusMutation.isPending,
    isBulkUpdating: bulkUpdateMutation.isPending,
  };
}

