/**
 * Hook for KV Store permissions
 */

import { useMemo } from "react";
import { usePermissions } from "@features/auth/hooks/usePermissions";
import { useAuth } from "@features/auth/context";

export interface KVPermissions {
  canView: boolean;
  canEdit: boolean;
  canDelete: boolean;
  isReadOnly: boolean;
  isLoading: boolean;
}

export function useKVPermissions(serviceId: string): KVPermissions {
  const { canViewService, canEditService } = usePermissions();
  const { permissionsLoading } = useAuth();

  return useMemo(() => {
    // While permissions are loading, allow access (optimistic)
    // Only block when permissions are loaded and explicitly denied
    const canViewResult = canViewService(serviceId);
    const canView = permissionsLoading 
      ? true // Allow access while loading
      : (canViewResult ?? false); // Block only if explicitly false after loading
    
    const canEditResult = canEditService(serviceId);
    const canEdit = permissionsLoading
      ? false // Don't allow edit while loading
      : (canEditResult ?? false);
    
    const canDelete = canEdit; // Same permission for delete

    return {
      canView,
      canEdit,
      canDelete,
      isReadOnly: canView && !canEdit,
      isLoading: permissionsLoading,
    };
  }, [serviceId, canViewService, canEditService, permissionsLoading]);
}

