import { useMemo } from "react";
import { useQueryClient } from "@tanstack/react-query";
import {
  useFindApplicationServiceById,
  useFindApprovalRequestById,
  useFindDriftEventById,
  useFindByIdServiceShare,
} from "@lib/api/hooks";
import { getFindApplicationServiceByIdQueryKey } from "@lib/api/generated/application-services/application-services";
import { getFindApprovalRequestByIdQueryKey } from "@lib/api/generated/approval-requests/approval-requests";
import { getFindDriftEventByIdQueryKey } from "@lib/api/generated/drift-events/drift-events";
import { getFindByIdServiceShareQueryKey } from "@lib/api/generated/service-shares/service-shares";

/**
 * Hook to fetch dynamic breadcrumb label for detail pages
 */
export function useDynamicBreadcrumbLabel(
  routePattern: string,
  params: Record<string, string | undefined>
): { label: string | null; isLoading: boolean } {
  const queryClient = useQueryClient();

  // Application Service
  const applicationServiceQuery = useFindApplicationServiceById(
    params.id || "",
    {
      query: {
        enabled: routePattern === "application-services/:id" && !!params.id,
        staleTime: 5 * 60 * 1000, // 5 minutes cache
        gcTime: 10 * 60 * 1000, // 10 minutes garbage collection
      },
    }
  );

  // Approval Request
  const approvalRequestQuery = useFindApprovalRequestById(params.id || "", {
    query: {
      enabled: routePattern === "approvals/:id" && !!params.id,
      staleTime: 5 * 60 * 1000,
      gcTime: 10 * 60 * 1000,
    },
  });

  // Drift Event
  const driftEventQuery = useFindDriftEventById(params.id || "", {
    query: {
      enabled: routePattern === "drift-events/:id" && !!params.id,
      staleTime: 5 * 60 * 1000,
      gcTime: 10 * 60 * 1000,
    },
  });

  // Service Share
  const serviceShareQuery = useFindByIdServiceShare(params.id || "", {
    query: {
      enabled: routePattern === "service-shares/:id" && !!params.id,
      staleTime: 5 * 60 * 1000,
      gcTime: 10 * 60 * 1000,
    },
  });

  // Try to get label from cache first, then fetch if needed
  const label = useMemo(() => {
    const entityId = params.id || params.instanceId || params.serviceName;
    if (!entityId) return null;

    switch (routePattern) {
      case "application-services/:id": {
        // Try cache first
        const cached = queryClient.getQueryData(
          getFindApplicationServiceByIdQueryKey(params.id || "")
        ) as { displayName?: string } | undefined;
        if (cached?.displayName) return cached.displayName;
        if (applicationServiceQuery.data?.displayName)
          return applicationServiceQuery.data.displayName;
        return null;
      }
      case "approvals/:id": {
        const cached = queryClient.getQueryData(
          getFindApprovalRequestByIdQueryKey(params.id || "")
        ) as { target?: { serviceId?: string } } | undefined;
        if (cached?.target?.serviceId) {
          return `Approval: ${cached.target.serviceId}`;
        }
        if (approvalRequestQuery.data?.target?.serviceId) {
          return `Approval: ${approvalRequestQuery.data.target.serviceId}`;
        }
        return null;
      }
      case "drift-events/:id": {
        const cached = queryClient.getQueryData(
          getFindDriftEventByIdQueryKey(params.id || "")
        ) as { serviceName?: string } | undefined;
        if (cached?.serviceName) {
          return `Drift: ${cached.serviceName}`;
        }
        if (driftEventQuery.data?.serviceName) {
          return `Drift: ${driftEventQuery.data.serviceName}`;
        }
        return null;
      }
      case "service-shares/:id": {
        const cached = queryClient.getQueryData(
          getFindByIdServiceShareQueryKey(params.id || "")
        ) as { serviceId?: string } | undefined;
        if (cached?.serviceId) {
          return `Share: ${cached.serviceId}`;
        }
        if (serviceShareQuery.data?.serviceId) {
          return `Share: ${serviceShareQuery.data.serviceId}`;
        }
        return null;
      }
      case "service-instances/:serviceName/:instanceId": {
        // Use serviceName and instanceId directly
        return params.instanceId
          ? `${params.serviceName} / ${params.instanceId.substring(0, 8)}...`
          : params.serviceName || null;
      }
      case "configs/:application/:profile": {
        return `${params.application} / ${params.profile}`;
      }
      case "registry/:serviceName": {
        return params.serviceName || null;
      }
      default:
        return null;
    }
  }, [
    routePattern,
    params,
    applicationServiceQuery.data,
    approvalRequestQuery.data,
    driftEventQuery.data,
    serviceShareQuery.data,
    queryClient,
  ]);

  const isLoading =
    applicationServiceQuery.isLoading ||
    approvalRequestQuery.isLoading ||
    driftEventQuery.isLoading ||
    serviceShareQuery.isLoading;

  return { label, isLoading };
}
