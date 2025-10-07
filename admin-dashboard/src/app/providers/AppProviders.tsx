import { Provider } from 'react-redux'
import { RouterProvider } from 'react-router-dom'
import { CssBaseline } from '@mui/material'
import { store } from '@app/store'
import router from '@app/router'
import ThemeProvider from './ThemeProvider'

export default function AppProviders() {
  return (
    <ThemeProvider>
      <CssBaseline />
      <Provider store={store}>
        <RouterProvider router={router} />
      </Provider>
    </ThemeProvider>
  )
}


