import { createContext, useContext } from "react";

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

export interface AuthContextType {
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

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
};

export default AuthContext;
