import { Provider } from 'react-redux'
import { RouterProvider } from 'react-router-dom'
import { CssBaseline } from '@mui/material'
import { SnackbarProvider } from 'notistack'
import { store } from '@app/store'
import router from '@app/router'
import ThemeProvider from './ThemeProvider'

export default function AppProviders() {
  return (
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
        <Provider store={store}>
          <RouterProvider router={router} />
        </Provider>
      </SnackbarProvider>
    </ThemeProvider>
  )
}


