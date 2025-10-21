import { useSelector } from 'react-redux';
import { type RootState } from '@app/store';
import { useMemo } from 'react';

export const usePermissions = () => {
  const { userInfo, permissions } = useSelector((state: RootState) => state.auth);

  const isSysAdmin = useMemo(() => {
    return userInfo?.roles?.includes('SYS_ADMIN') ?? false;
  }, [userInfo?.roles]);

  const isUser = useMemo(() => {
    return userInfo?.roles?.includes('USER') ?? false;
  }, [userInfo?.roles]);

  const canViewService = useMemo(() => {
    return isSysAdmin || isUser;
  }, [isSysAdmin, isUser]);

  const canEditService = useMemo(() => {
    return isSysAdmin;
  }, [isSysAdmin]);

  const canDeleteService = useMemo(() => {
    return isSysAdmin;
  }, [isSysAdmin]);

  const canApproveRequests = useMemo(() => {
    return isSysAdmin;
  }, [isSysAdmin]);

  const canManageAllServices = useMemo(() => {
    return isSysAdmin;
  }, [isSysAdmin]);

  const canManageApplicationServices = useMemo(() => {
    return permissions?.features?.canManageApplicationServices || isSysAdmin;
  }, [permissions?.features?.canManageApplicationServices, isSysAdmin]);

  const canViewDriftEvents = useMemo(() => {
    return permissions?.features?.canViewDriftEvents || isSysAdmin;
  }, [permissions?.features?.canViewDriftEvents, isSysAdmin]);

  const canAccessRoute = useMemo(() => {
    return (route: string) => {
      if (isSysAdmin) return true;
      return permissions?.allowedRoutes?.includes(route) ?? false;
    };
  }, [isSysAdmin, permissions?.allowedRoutes]);

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
    // This would be populated from API based on user's teams
    return [];
  }, []);

  return {
    isSysAdmin,
    isUser,
    canViewService,
    canEditService,
    canDeleteService,
    canApproveRequests,
    canManageAllServices,
    canManageApplicationServices,
    canViewDriftEvents,
    canAccessRoute,
    canViewTeamServices,
    canEditTeamServices,
    ownedServiceIds,
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
