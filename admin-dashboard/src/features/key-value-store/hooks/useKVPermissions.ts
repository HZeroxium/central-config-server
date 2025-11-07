/**
 * Hook for KV Store permissions
 */

import { useMemo } from "react";
import { usePermissions } from "@features/auth/hooks/usePermissions";

export interface KVPermissions {
  canView: boolean;
  canEdit: boolean;
  canDelete: boolean;
  isReadOnly: boolean;
}

export function useKVPermissions(serviceId: string): KVPermissions {
  const { canViewService, canEditService } = usePermissions();

  return useMemo(() => {
    const canView = canViewService(serviceId) ?? false;
    const canEdit = canEditService(serviceId) ?? false;
    const canDelete = canEdit; // Same permission for delete

    return {
      canView,
      canEdit,
      canDelete,
      isReadOnly: canView && !canEdit,
    };
  }, [serviceId, canViewService, canEditService]);
}

