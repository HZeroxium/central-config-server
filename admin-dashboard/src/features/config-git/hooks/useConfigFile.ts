import { useQueryClient } from "@tanstack/react-query";
import {
  useGetConfigFile,
  useUpdateConfigFile,
} from "@lib/api/hooks";
import {
  getGetConfigFileQueryKey,
} from "@lib/api/generated/config-git/config-git";
import { toast } from "@lib/toast/toast";
import { handleApiError } from "@lib/api/errorHandler";
import type { UpdateConfigFileRequest } from "@lib/api/models";

export function useConfigFileOperations(
  serviceId: string,
  profile: string
) {
  const queryClient = useQueryClient();

  const {
    data: configFile,
    isLoading,
    error,
    refetch,
  } = useGetConfigFile(serviceId, profile, {
    query: {
      enabled: !!serviceId && !!profile,
      staleTime: 10_000,
    },
  });

  const updateMutation = useUpdateConfigFile();

  const update = async (
    content: string,
    customMessage?: string,
    expectedSha?: string
  ) => {
    const request: UpdateConfigFileRequest = {
      content,
      customMessage,
      expectedSha,
    };

    updateMutation.mutate(
      { serviceId, profile, data: request },
      {
        onSuccess: () => {
          toast.success("Config file updated successfully");
          queryClient.invalidateQueries({
            queryKey: getGetConfigFileQueryKey(serviceId, profile),
          });
        },
        onError: (error) => {
          handleApiError(error);
        },
      }
    );
  };

  return {
    configFile,
    isLoading,
    error,
    refetch,
    update,
    isUpdating: updateMutation.isPending,
  };
}

