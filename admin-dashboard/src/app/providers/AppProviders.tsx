import { RouterProvider } from 'react-router-dom'
import { CssBaseline } from '@mui/material'
import { Toaster } from 'sonner'
import { QueryClientProvider } from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import { Provider as ReduxProvider } from 'react-redux'
import router from '@app/router'
import ThemeProvider from './ThemeProvider'
import KeycloakProvider from '@lib/keycloak/KeycloakProvider'
import { AuthProvider } from '@features/auth/authContext'
import { queryClient } from '@lib/query/queryClient'
import { store } from '@store/index'

export default function AppProviders() {
  return (
    <ReduxProvider store={store}>
      <QueryClientProvider client={queryClient}>
        <KeycloakProvider>
          <AuthProvider>
            <ThemeProvider>
              <CssBaseline />
              <Toaster 
                position="top-right" 
                richColors 
                closeButton
                duration={6000}
              />
              <RouterProvider router={router} />
              {/* Only show devtools in development */}
              {import.meta.env.DEV && <ReactQueryDevtools initialIsOpen={false} />}
            </ThemeProvider>
          </AuthProvider>
        </KeycloakProvider>
      </QueryClientProvider>
    </ReduxProvider>
  )
}


