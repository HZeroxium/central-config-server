import React, { useMemo, useEffect } from "react";
import { useKeycloak } from "@react-keycloak/web";
import { useDispatch, useSelector } from "react-redux";
import {
  useFindCurrentUserPermissions,
  useFindCurrentUserInformation,
} from "@lib/api/hooks";
import type { MeResponse, PermissionsResponse } from "@lib/api/models";
import {
  setUserInfo,
  setPermissions,
  setInitialized,
  clearAuth,
} from "@store/authSlice";
import type { RootState } from "@store/index";
import AuthContext, {
  type UserInfo,
  type UserPermissions,
  type AuthContextType,
} from "./context";

/**
 * Parse Keycloak token to UserInfo
 */
function parseKeycloakToken(
  tokenParsed: Record<string, unknown> | undefined
): UserInfo | null {
  if (!tokenParsed) return null;

  return {
    userId: (tokenParsed.sub as string) || "",
    username: (tokenParsed.preferred_username as string) || "",
    email: (tokenParsed.email as string) || "",
    firstName: (tokenParsed.given_name as string) || "",
    lastName: (tokenParsed.family_name as string) || "",
    teamIds: (tokenParsed.groups as string[]) || [],
    roles:
      ((tokenParsed.realm_access as Record<string, unknown>)
        ?.roles as string[]) || [],
    managerId: tokenParsed.manager_id as string | undefined,
  };
}

/**
 * Parse MeResponse to UserInfo
 */
function parseMeResponse(meResponse: MeResponse | undefined): UserInfo | null {
  if (!meResponse) return null;

  return {
    userId: meResponse.userId || "",
    username: meResponse.username || "",
    email: meResponse.email || "",
    firstName: meResponse.firstName || "",
    lastName: meResponse.lastName || "",
    teamIds: meResponse.teamIds || [],
    roles: meResponse.roles || [],
    managerId: meResponse.managerId,
  };
}

/**
 * Parse PermissionsResponse to UserPermissions
 */
function parsePermissionsResponse(
  permissionsResponse: PermissionsResponse | undefined
): UserPermissions | null {
  if (!permissionsResponse) return null;

  return {
    allowedApiRoutes: permissionsResponse.allowedApiRoutes || [],
    allowedUiRoutes: permissionsResponse.allowedUiRoutes || [],
    roles: permissionsResponse.roles || [],
    teams: permissionsResponse.teams || [],
    features: permissionsResponse.features || {},
    actions: permissionsResponse.actions || {},
    ownedServiceIds: permissionsResponse.ownedServiceIds || [],
    sharedServiceIds: permissionsResponse.sharedServiceIds || [],
  };
}

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const { keycloak, initialized } = useKeycloak();
  const dispatch = useDispatch();
  const authState = useSelector((state: RootState) => state.auth);

  const isAuthenticated = keycloak?.authenticated ?? false;

  // Update Redux store when Keycloak initialization state changes
  useEffect(() => {
    dispatch(setInitialized(initialized));
  }, [initialized, dispatch]);

  // Clear auth data when user logs out
  useEffect(() => {
    if (!isAuthenticated && authState.userInfo) {
      dispatch(clearAuth());
    }
  }, [isAuthenticated, authState.userInfo, dispatch]);

  // Fetch user info from backend (more reliable than token parsing)
  const {
    data: meData,
    isLoading: meLoading,
    refetch: refetchUserInfo,
  } = useFindCurrentUserInformation({
    query: {
      enabled: isAuthenticated,
      staleTime: Infinity, // Cache indefinitely - user info rarely changes
      refetchOnWindowFocus: false,
    },
  });

  // Fetch user permissions from backend
  const {
    data: permissionsData,
    isLoading: permissionsLoading,
    refetch: refetchPermissions,
  } = useFindCurrentUserPermissions({
    query: {
      enabled: isAuthenticated,
      staleTime: Infinity, // Cache indefinitely - permissions change rarely, refetch manually when needed
      refetchOnWindowFocus: false,
    },
  });

  // Update Redux store when data changes
  useEffect(() => {
    if (meData) {
      const userInfo = parseMeResponse(meData);
      dispatch(setUserInfo(userInfo));
    }
  }, [meData, dispatch]);

  useEffect(() => {
    if (permissionsData) {
      const permissions = parsePermissionsResponse(permissionsData);
      dispatch(setPermissions(permissions));
    }
  }, [permissionsData, dispatch]);

  // Parse user info - prefer Redux store over token
  const userInfo = useMemo(() => {
    if (authState.userInfo) return authState.userInfo;

    // Fallback to token parsing if Redux data not available yet
    return parseKeycloakToken(keycloak?.tokenParsed);
  }, [authState.userInfo, keycloak?.tokenParsed]);

  // Parse permissions - prefer Redux store
  const permissions = useMemo((): UserPermissions | null => {
    if (authState.permissions) return authState.permissions;
    return null;
  }, [authState.permissions]);

  const hasRole = useMemo(
    () =>
      (role: string): boolean => {
        return keycloak?.hasRealmRole(role) ?? false;
      },
    [keycloak]
  );

  const isSysAdmin = useMemo(() => {
    return keycloak?.hasRealmRole("SYS_ADMIN") ?? false;
  }, [keycloak]);

  const login = useMemo(
    () => () => {
      keycloak?.login();
    },
    [keycloak]
  );

  const logout = useMemo(
    () => () => {
      keycloak?.logout();
    },
    [keycloak]
  );

  const handleRefetchUserInfo = useMemo(
    () => () => {
      refetchUserInfo();
    },
    [refetchUserInfo]
  );

  const handleRefetchPermissions = useMemo(
    () => () => {
      refetchPermissions();
    },
    [refetchPermissions]
  );

  // Memoize context value to prevent unnecessary re-renders
  const value: AuthContextType = useMemo(
    () => ({
      initialized,
      isAuthenticated,
      token: keycloak?.token,
      userInfo,
      permissions,
      permissionsLoading: permissionsLoading || meLoading,
      hasRole,
      isSysAdmin,
      login,
      logout,
      refetchUserInfo: handleRefetchUserInfo,
      refetchPermissions: handleRefetchPermissions,
    }),
    [
      initialized,
      isAuthenticated,
      keycloak?.token,
      userInfo,
      permissions,
      permissionsLoading,
      meLoading,
      hasRole,
      isSysAdmin,
      login,
      logout,
      handleRefetchUserInfo,
      handleRefetchPermissions,
    ]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};
