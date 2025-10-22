import { useAuth } from '../authContext';

/**
 * Hook for auth initialization state
 * This is now a simple wrapper around useAuth for backward compatibility
 */
export const useAuthInit = () => {
  const { initialized, isAuthenticated, permissionsLoading } = useAuth();

  return {
    initialized,
    isAuthenticated,
    isLoading: !initialized || permissionsLoading,
    error: null, // Error handling is now done internally in AuthContext
  };
};
