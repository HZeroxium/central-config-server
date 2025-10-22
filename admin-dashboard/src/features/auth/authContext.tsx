import React, { createContext, useContext, useMemo } from 'react';
import { useKeycloak } from '@react-keycloak/web';
import { useFindCurrentUserPermissions, useFindCurrentUserInformation } from '@lib/api/hooks';
import type { MeResponse } from '@lib/api/models';

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

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { keycloak, initialized } = useKeycloak();

  const isAuthenticated = keycloak?.authenticated ?? false;

  // Fetch user info from backend (more reliable than token parsing)
  const { data: meData, isLoading: meLoading } = useFindCurrentUserInformation({
    query: {
      enabled: isAuthenticated,
      staleTime: 5 * 60 * 1000, // 5 minutes
      refetchOnWindowFocus: false,
    }
  });

  // Fetch user permissions from backend
  const { data: permissionsData, isLoading: permissionsLoading } = useFindCurrentUserPermissions({
    query: {
      enabled: isAuthenticated,
      staleTime: 5 * 60 * 1000, // 5 minutes
      refetchOnWindowFocus: true, // Refetch on window focus to ensure fresh permissions
    }
  });

  // Parse user info - prefer backend data over token
  const userInfo = useMemo(() => {
    const backendUserInfo = parseMeResponse(meData);
    if (backendUserInfo) return backendUserInfo;
    
    // Fallback to token parsing if backend data not available yet
    return parseKeycloakToken(keycloak?.tokenParsed);
  }, [meData, keycloak?.tokenParsed]);

  // Parse permissions
  const permissions = useMemo((): UserPermissions | null => {
    if (!permissionsData) return null;
    
    const unwrapped = permissionsData;
    if (!unwrapped) return null;
    
    return {
      allowedApiRoutes: (unwrapped as any).allowedApiRoutes || [],
      allowedUiRoutes: (unwrapped as any).allowedUiRoutes || [],
      roles: (unwrapped as any).roles || [],
      teams: (unwrapped as any).teams || [],
      features: (unwrapped as any).features || {},
      actions: (unwrapped as any).actions || {},
      ownedServiceIds: (unwrapped as any).ownedServiceIds || [],
      sharedServiceIds: (unwrapped as any).sharedServiceIds || [],
    };
  }, [permissionsData]);

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

