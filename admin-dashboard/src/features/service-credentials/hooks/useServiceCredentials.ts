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
      retry: false, // Don't retry if credentials don't exist
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

