import { useMemo } from "react";
import { useAuth } from "../context";

export const usePermissions = () => {
  const { userInfo, permissions, isSysAdmin: isSysAdminFromAuth } = useAuth();

  const isSysAdmin = isSysAdminFromAuth;

  const isUser = useMemo(() => {
    return userInfo?.roles?.includes("USER") ?? false;
  }, [userInfo?.roles]);

  const canViewService = useMemo(() => {
    return (serviceId: string) => {
      if (isSysAdmin) return true;
      return (
        permissions?.ownedServiceIds?.includes(serviceId) ||
        permissions?.sharedServiceIds?.includes(serviceId)
      );
    };
  }, [isSysAdmin, permissions?.ownedServiceIds, permissions?.sharedServiceIds]);

  const canEditService = useMemo(() => {
    return (serviceId: string) => {
      if (isSysAdmin) return true;
      return (
        permissions?.ownedServiceIds?.includes(serviceId) ||
        (permissions?.sharedServiceIds?.includes(serviceId) &&
          permissions?.actions?.[serviceId]?.includes("EDIT_SERVICE"))
      );
    };
  }, [
    isSysAdmin,
    permissions?.ownedServiceIds,
    permissions?.sharedServiceIds,
    permissions?.actions,
  ]);

  const canDeleteService = useMemo(() => {
    return (serviceId: string) => {
      if (isSysAdmin) return true;
      return permissions?.ownedServiceIds?.includes(serviceId);
    };
  }, [isSysAdmin, permissions?.ownedServiceIds]);

  const canShareService = useMemo(() => {
    return (serviceId: string) => {
      if (isSysAdmin) return true;
      return permissions?.ownedServiceIds?.includes(serviceId);
    };
  }, [isSysAdmin, permissions?.ownedServiceIds]);

  const canApproveRequests = useMemo(() => {
    return isSysAdmin || permissions?.features?.canApproveRequests || false;
  }, [isSysAdmin, permissions?.features?.canApproveRequests]);

  const canManageAllServices = useMemo(() => {
    return isSysAdmin || permissions?.features?.canManageAllServices || false;
  }, [isSysAdmin, permissions?.features?.canManageAllServices]);

  const canManageApplicationServices = useMemo(() => {
    return permissions?.features?.canManageApplicationServices || isSysAdmin;
  }, [permissions?.features?.canManageApplicationServices, isSysAdmin]);

  const canViewDriftEvents = useMemo(() => {
    return permissions?.features?.canViewDriftEvents || isSysAdmin;
  }, [permissions?.features?.canViewDriftEvents, isSysAdmin]);

  const canEditInstance = useMemo(() => {
    return (serviceId: string) => {
      if (isSysAdmin) return true;
      return (
        permissions?.ownedServiceIds?.includes(serviceId) ||
        (permissions?.sharedServiceIds?.includes(serviceId) &&
          permissions?.actions?.[serviceId]?.includes("EDIT_INSTANCE"))
      );
    };
  }, [
    isSysAdmin,
    permissions?.ownedServiceIds,
    permissions?.sharedServiceIds,
    permissions?.actions,
  ]);

  const canDeleteInstance = useMemo(() => {
    return (serviceId: string) => {
      if (isSysAdmin) return true;
      return (
        permissions?.ownedServiceIds?.includes(serviceId) ||
        (permissions?.sharedServiceIds?.includes(serviceId) &&
          permissions?.actions?.[serviceId]?.includes("EDIT_INSTANCE"))
      );
    };
  }, [
    isSysAdmin,
    permissions?.ownedServiceIds,
    permissions?.sharedServiceIds,
    permissions?.actions,
  ]);

  const canAccessRoute = useMemo(() => {
    return (route: string) => {
      if (isSysAdmin) return true;
      return permissions?.allowedUiRoutes?.includes(route) ?? false;
    };
  }, [isSysAdmin, permissions?.allowedUiRoutes]);

  const canViewTeamServices = useMemo(() => {
    return (teamId: string) => {
      if (isSysAdmin) return true;
      return userInfo?.teamIds?.includes(teamId) ?? false;
    };
  }, [isSysAdmin, userInfo?.teamIds]);

  const canEditTeamServices = useMemo(() => {
    return (teamId: string) => {
      if (isSysAdmin) return true;
      return userInfo?.teamIds?.includes(teamId) ?? false;
    };
  }, [isSysAdmin, userInfo?.teamIds]);

  const ownedServiceIds = useMemo(() => {
    return permissions?.ownedServiceIds || [];
  }, [permissions?.ownedServiceIds]);

  const sharedServiceIds = useMemo(() => {
    return permissions?.sharedServiceIds || [];
  }, [permissions?.sharedServiceIds]);

  return {
    isSysAdmin,
    isUser,
    canViewService,
    canEditService,
    canDeleteService,
    canShareService,
    canApproveRequests,
    canManageAllServices,
    canManageApplicationServices,
    canViewDriftEvents,
    canEditInstance,
    canDeleteInstance,
    canAccessRoute,
    canViewTeamServices,
    canEditTeamServices,
    ownedServiceIds,
    sharedServiceIds,
  };
};

// Specific permission hooks for convenience
export const useIsSysAdmin = () => {
  const { isSysAdmin } = usePermissions();
  return isSysAdmin;
};

export const useCanApproveRequests = () => {
  const { canApproveRequests } = usePermissions();
  return canApproveRequests;
};

export const useCanManageAllServices = () => {
  const { canManageAllServices } = usePermissions();
  return canManageAllServices;
};
