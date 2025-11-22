import { useQueryClient } from "@tanstack/react-query";
import {
  useCreateApplicationService,
  useCreateApprovalRequest,
} from "@lib/api/hooks";
import {
  getFindAllApplicationServicesQueryKey,
} from "@lib/api/generated/application-services/application-services";
import { getFindAllApprovalRequestsQueryKey } from "@lib/api/generated/approval-requests/approval-requests";
import { toast } from "@lib/toast/toast";
import { handleApiError } from "@lib/api/errorHandler";
import type {
  ApplicationServiceCreateRequest,
  ApprovalRequestCreateRequest,
} from "@lib/api/models";

interface UseServiceRegistrationOptions {
  onSuccess?: (approvalRequestId: string) => void;
}

export function useServiceRegistration(
  options?: UseServiceRegistrationOptions
) {
  const queryClient = useQueryClient();

  const createServiceMutation = useCreateApplicationService();
  const createApprovalMutation = useCreateApprovalRequest();

  const register = async (
    serviceId: string,
    displayName: string,
    targetTeamId: string
  ) => {
    return new Promise<{ serviceId: string; approvalRequestId: string }>(
      (resolve, reject) => {
        // Step 1: Create orphan service (ownerTeamId=null)
        const serviceRequest: ApplicationServiceCreateRequest = {
          id: serviceId,
          displayName,
          ownerTeamId: undefined, // null/undefined creates orphan service
          environments: ["dev", "staging", "prod"], // Default environments
        };

        createServiceMutation.mutate(
          { data: serviceRequest },
          {
            onSuccess: (serviceResponse) => {
              const createdServiceId = serviceResponse.id || serviceId;

              // Step 2: Create approval request
              const approvalRequest: ApprovalRequestCreateRequest = {
                serviceId: createdServiceId,
                targetTeamId,
                note: `Request ownership of service "${displayName}"`,
              };

              createApprovalMutation.mutate(
                { data: approvalRequest },
                {
                  onSuccess: (approvalResponse) => {
                    const approvalRequestId = approvalResponse.id || "";

                    // Invalidate queries to refresh data
                    queryClient.invalidateQueries({
                      queryKey: getFindAllApplicationServicesQueryKey(),
                    });
                    queryClient.invalidateQueries({
                      queryKey: getFindAllApprovalRequestsQueryKey(),
                    });

                    toast.success("Service registration submitted successfully");

                    if (options?.onSuccess) {
                      options.onSuccess(approvalRequestId);
                    }

                    resolve({
                      serviceId: createdServiceId,
                      approvalRequestId,
                    });
                  },
                  onError: (error) => {
                    // Service was created but approval request failed
                    toast.error(
                      "Service created but approval request failed. Please create approval request manually."
                    );
                    handleApiError(error);
                    reject(error);
                  },
                }
              );
            },
            onError: (error) => {
              handleApiError(error);
              reject(error);
            },
          }
        );
      }
    );
  };

  return {
    register,
    isRegistering:
      createServiceMutation.isPending || createApprovalMutation.isPending,
    error: createServiceMutation.error || createApprovalMutation.error,
  };
}

