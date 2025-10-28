import React, { createContext, useContext, useMemo, useEffect } from 'react';
import { useKeycloak } from '@react-keycloak/web';
import { useDispatch, useSelector } from 'react-redux';
import { useFindCurrentUserPermissions, useFindCurrentUserInformation } from '@lib/api/hooks';
import type { MeResponse, PermissionsResponse } from '@lib/api/models';
import { setUserInfo, setPermissions, setInitialized, clearAuth } from '@store/authSlice';
import type { RootState } from '@store/index';

export interface UserInfo {
  userId: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  teamIds: string[];
  roles: string[];
  managerId?: string;
}

export interface UserPermissions {
  allowedApiRoutes: string[];
  allowedUiRoutes: string[];
  roles: string[];
  teams: string[];
  features: Record<string, boolean>;
  actions: Record<string, string[]>;
  ownedServiceIds: string[];
  sharedServiceIds: string[];
}

interface AuthContextType {
  initialized: boolean;
  isAuthenticated: boolean;
  token: string | undefined;
  userInfo: UserInfo | null;
  permissions: UserPermissions | null;
  permissionsLoading: boolean;
  hasRole: (role: string) => boolean;
  isSysAdmin: boolean;
  login: () => void;
  logout: () => void;
  refetchUserInfo: () => void;
  refetchPermissions: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

/**
 * Parse Keycloak token to UserInfo
 */
function parseKeycloakToken(tokenParsed: any): UserInfo | null {
  if (!tokenParsed) return null;
  
  return {
    userId: tokenParsed.sub || '',
    username: tokenParsed.preferred_username || '',
    email: tokenParsed.email || '',
    firstName: tokenParsed.given_name || '',
    lastName: tokenParsed.family_name || '',
    teamIds: tokenParsed.groups || [],
    roles: tokenParsed.realm_access?.roles || [],
    managerId: tokenParsed.manager_id,
  };
}

/**
 * Parse MeResponse to UserInfo
 */
function parseMeResponse(meResponse: MeResponse | undefined): UserInfo | null {
  if (!meResponse) return null;
  
  return {
    userId: meResponse.userId || '',
    username: meResponse.username || '',
    email: meResponse.email || '',
    firstName: meResponse.firstName || '',
    lastName: meResponse.lastName || '',
    teamIds: meResponse.teamIds || [],
    roles: meResponse.roles || [],
    managerId: meResponse.managerId,
  };
}

/**
 * Parse PermissionsResponse to UserPermissions
 */
function parsePermissionsResponse(permissionsResponse: PermissionsResponse | undefined): UserPermissions | null {
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

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
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
  const { data: meData, isLoading: meLoading, refetch: refetchUserInfo } = useFindCurrentUserInformation({
    query: {
      enabled: isAuthenticated,
      staleTime: Infinity, // Cache indefinitely - user info rarely changes
      refetchOnWindowFocus: false,
    }
  });

  // Fetch user permissions from backend
  const { data: permissionsData, isLoading: permissionsLoading, refetch: refetchPermissions } = useFindCurrentUserPermissions({
    query: {
      enabled: isAuthenticated,
      staleTime: Infinity, // Cache indefinitely - permissions change rarely, refetch manually when needed
      refetchOnWindowFocus: false,
    }
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

  const hasRole = (role: string): boolean => {
    return keycloak?.hasRealmRole(role) ?? false;
  };

  const isSysAdmin = useMemo(() => {
    return hasRole('SYS_ADMIN');
  }, [keycloak?.authenticated]); // eslint-disable-line react-hooks/exhaustive-deps

  const login = () => {
    keycloak?.login();
  };

  const logout = () => {
    keycloak?.logout();
  };

  const value: AuthContextType = {
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
    refetchUserInfo: () => { refetchUserInfo(); },
    refetchPermissions: () => { refetchPermissions(); },
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

