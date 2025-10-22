import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '@features/auth/authContext';
import { CircularProgress, Box, Alert } from '@mui/material';

interface ProtectedRouteProps {
  children: React.ReactNode;
  requiredRoles?: string[];
  requiredTeams?: string[];
  requiredPermissions?: string[];
  requiredRoute?: string; // Check against allowedUiRoutes from backend
}

const LoadingComponent: React.FC = () => (
  <Box
    display="flex"
    justifyContent="center"
    alignItems="center"
    minHeight="100vh"
  >
    <CircularProgress />
  </Box>
);

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
  children,
  requiredRoles = [],
  requiredTeams = [],
  requiredPermissions = [],
  requiredRoute,
}) => {
  const { 
    initialized, 
    isAuthenticated, 
    permissions, 
    permissionsLoading,
    userInfo,
    isSysAdmin 
  } = useAuth();

  // Show loading while initializing or fetching permissions
  if (!initialized || permissionsLoading) {
    return <LoadingComponent />;
  }

  // Redirect to login if not authenticated
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  // Wait for permissions to load
  if (!permissions && !isSysAdmin) {
    return <LoadingComponent />;
  }

  // SysAdmin bypasses all permission checks
  if (isSysAdmin) {
    return <>{children}</>;
  }

  // Check route permission against backend-provided allowedUiRoutes
  if (requiredRoute && permissions) {
    const hasRouteAccess = permissions.allowedUiRoutes.includes(requiredRoute);
    if (!hasRouteAccess) {
      return <Navigate to="/unauthorized" replace />;
    }
  }

  // Check role requirements
  if (requiredRoles.length > 0 && userInfo) {
    const hasRequiredRole = requiredRoles.some(role => 
      userInfo.roles.includes(role)
    );
    if (!hasRequiredRole) {
      return <Navigate to="/unauthorized" replace />;
    }
  }

  // Check team requirements
  if (requiredTeams.length > 0 && userInfo) {
    const hasRequiredTeam = requiredTeams.some(team => 
      userInfo.teamIds.includes(team)
    );
    if (!hasRequiredTeam) {
      return <Navigate to="/unauthorized" replace />;
    }
  }

  // Check granular permissions (actions)
  if (requiredPermissions.length > 0 && permissions) {
    const allActions = Object.values(permissions.actions).flat();
    const hasAllPermissions = requiredPermissions.every(perm =>
      allActions.includes(perm)
    );
    if (!hasAllPermissions) {
      return <Navigate to="/unauthorized" replace />;
    }
  }

  return <>{children}</>;
};

export default ProtectedRoute;
