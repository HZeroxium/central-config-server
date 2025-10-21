import { useKeycloak } from '@react-keycloak/web';

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
  allowedRoutes: string[];
  roles: string[];
  teams: string[];
  features: Record<string, boolean>;
}

export const useAuth = () => {
  const { keycloak, initialized } = useKeycloak();

  const login = () => {
    keycloak?.login();
  };

  const logout = () => {
    keycloak?.logout();
  };

  const isAuthenticated = keycloak?.authenticated ?? false;

  const token = keycloak?.token;

  const userInfo: UserInfo | null = keycloak?.tokenParsed ? {
    userId: keycloak.tokenParsed.sub || '',
    username: keycloak.tokenParsed.preferred_username || '',
    email: keycloak.tokenParsed.email || '',
    firstName: keycloak.tokenParsed.given_name || '',
    lastName: keycloak.tokenParsed.family_name || '',
    teamIds: keycloak.tokenParsed.groups || [],
    roles: keycloak.tokenParsed.realm_access?.roles || [],
    managerId: keycloak.tokenParsed.manager_id,
  } : null;

  const hasRole = (role: string): boolean => {
    return keycloak?.hasRealmRole(role) ?? false;
  };

  const isSysAdmin = hasRole('SYS_ADMIN');
  const isUser = hasRole('USER');

  return {
    initialized,
    isAuthenticated,
    login,
    logout,
    token,
    userInfo,
    hasRole,
    isSysAdmin,
    isUser,
  };
};
