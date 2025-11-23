import { useQueryClient } from "@tanstack/react-query";
import {
  useGetServiceCredentials,
  useActivateServiceCredentials,
  useRevokeServiceCredentials,
} from "@lib/api/hooks";
import {
  getGetServiceCredentialsQueryKey,
} from "@lib/api/generated/service-credentials/service-credentials";
import { toast } from "@lib/toast/toast";
import { handleApiError } from "@lib/api/errorHandler";
import type { ErrorResponse } from "@lib/api/models";

export function useServiceCredentialsOperations(serviceId: string) {
  const queryClient = useQueryClient();

  const {
    data: credentials,
    isLoading,
    error,
    refetch,
  } = useGetServiceCredentials(serviceId, {
    query: {
      enabled: !!serviceId,
      staleTime: 10_000,
      retry: (failureCount, error) => {
        // Don't retry on client errors (4xx) - these are permanent
        if (
          error &&
          typeof error === "object" &&
          "status" in error &&
          typeof (error as ErrorResponse).status === "number"
        ) {
          const status = (error as ErrorResponse).status;
          if (status !== undefined && status >= 400 && status < 500) {
            return false; // Don't retry 4xx errors (404, 403, 400, etc.)
          }
        }
        // Retry on server errors (5xx) and network errors
        // Exponential backoff: 1s, 2s, 4s (max 3 retries)
        if (failureCount < 3) {
          return true;
        }
        return false;
      },
      retryDelay: (attemptIndex) => {
        // Exponential backoff: 1s, 2s, 4s
        return Math.min(1000 * Math.pow(2, attemptIndex), 4000);
      },
      refetchOnMount: true, // Always refetch when component mounts
      refetchOnWindowFocus: false, // Don't refetch on window focus
    },
  });

  const activateMutation = useActivateServiceCredentials();
  const revokeMutation = useRevokeServiceCredentials();

  const activate = async () => {
    activateMutation.mutate(
      { serviceId },
      {
        onSuccess: () => {
          toast.success("Credentials activated successfully");
          queryClient.invalidateQueries({
            queryKey: getGetServiceCredentialsQueryKey(serviceId),
          });
        },
        onError: (error) => {
          handleApiError(error);
        },
      }
    );
  };

  const revoke = async () => {
    revokeMutation.mutate(
      { serviceId },
      {
        onSuccess: () => {
          toast.success("Credentials revoked successfully");
          queryClient.invalidateQueries({
            queryKey: getGetServiceCredentialsQueryKey(serviceId),
          });
        },
        onError: (error) => {
          handleApiError(error);
        },
      }
    );
  };

  return {
    credentials,
    isLoading,
    error,
    refetch,
    activate,
    revoke,
    isActivating: activateMutation.isPending,
    isRevoking: revokeMutation.isPending,
  };
}

