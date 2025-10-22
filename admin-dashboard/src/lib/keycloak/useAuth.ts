import { keycloak } from './keycloakConfig';

// This file is deprecated - use features/auth/authContext instead
// Kept for backward compatibility

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

export const getAuthToken = (): string | undefined => {
  return keycloak?.token;
};

// Re-export the new useAuth hook from authContext
export { useAuth } from '@features/auth/authContext';
