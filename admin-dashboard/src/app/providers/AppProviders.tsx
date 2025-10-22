import { RouterProvider } from 'react-router-dom'
import { CssBaseline } from '@mui/material'
import { SnackbarProvider, useSnackbar } from 'notistack'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import router from '@app/router'
import ThemeProvider from './ThemeProvider'
import KeycloakProvider from '@lib/keycloak/KeycloakProvider'
import { AuthProvider } from '@features/auth/authContext'
import { getErrorMessage, handleApiError } from '@lib/api/errorHandler'

// Create QueryClient instance with default options and error handling
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutes
      gcTime: 10 * 60 * 1000, // 10 minutes (replaces cacheTime in React Query v5)
      retry: (failureCount, error: any) => {
        // Don't retry on 401, 403, 404
        if (error?.response?.status === 401 || 
            error?.response?.status === 403 || 
            error?.response?.status === 404) {
          return false;
        }
        return failureCount < 3;
      },
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: false,
      onError: (error: unknown) => {
        // Handle errors globally for mutations
        const handled = handleApiError(error);
        if (!handled) {
          // Show error toast via snackbar
          const message = getErrorMessage(error);
          console.error('Mutation error:', message);
        }
      },
    },
  },
});

export default function AppProviders() {
  return (
    <QueryClientProvider client={queryClient}>
      <KeycloakProvider>
        <AuthProvider>
          <ThemeProvider>
            <CssBaseline />
            <SnackbarProvider
              maxSnack={3}
              anchorOrigin={{
                vertical: 'top',
                horizontal: 'right',
              }}
              autoHideDuration={6000}
            >
              <RouterProvider router={router} />
              {/* Only show devtools in development */}
              {import.meta.env.DEV && <ReactQueryDevtools initialIsOpen={false} />}
            </SnackbarProvider>
          </ThemeProvider>
        </AuthProvider>
      </KeycloakProvider>
    </QueryClientProvider>
  )
}


