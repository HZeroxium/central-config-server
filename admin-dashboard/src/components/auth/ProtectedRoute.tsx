import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '@lib/keycloak/useAuth';
import { CircularProgress, Box } from '@mui/material';

interface ProtectedRouteProps {
  children: React.ReactNode;
  requiredRoles?: string[];
  requiredTeams?: string[];
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
}) => {
  const { initialized, isAuthenticated, userInfo, hasRole } = useAuth();

  // Show loading while Keycloak is initializing
  if (!initialized) {
    return <LoadingComponent />;
  }

  // Redirect to login if not authenticated
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  // Check role requirements
  if (requiredRoles.length > 0) {
    const hasRequiredRole = requiredRoles.some(role => hasRole(role));
    if (!hasRequiredRole) {
      return <Navigate to="/unauthorized" replace />;
    }
  }

  // Check team requirements
  if (requiredTeams.length > 0 && userInfo) {
    const hasRequiredTeam = requiredTeams.some(team => 
      userInfo.teamIds.some(userTeam => userTeam.includes(team))
    );
    if (!hasRequiredTeam) {
      return <Navigate to="/unauthorized" replace />;
    }
  }

  return <>{children}</>;
};

export default ProtectedRoute;
