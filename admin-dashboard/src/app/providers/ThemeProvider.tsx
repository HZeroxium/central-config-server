import { createContext, useContext, useState, useEffect, ReactNode } from 'react'
import { ThemeProvider as MuiThemeProvider, createTheme } from '@mui/material/styles'

type ThemeMode = 'light' | 'dark'

interface ThemeContextType {
  mode: ThemeMode
  toggleMode: () => void
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined)

export const useTheme = () => {
  const context = useContext(ThemeContext)
  if (!context) {
    throw new Error('useTheme must be used within a ThemeProvider')
  }
  return context
}

const createAppTheme = (mode: ThemeMode) =>
  createTheme({
    palette: {
      mode,
      primary: { 
        main: '#2196f3',
        light: '#64b5f6',
        dark: '#1976d2',
        contrastText: '#ffffff',
      },
      secondary: {
        main: mode === 'dark' ? '#f5f5f5' : '#f5f5f5',
        light: mode === 'dark' ? '#ffffff' : '#ffffff',
        dark: mode === 'dark' ? '#e0e0e0' : '#e0e0e0',
        contrastText: mode === 'dark' ? '#333333' : '#333333',
      },
      background: {
        default: mode === 'dark' ? '#0a0e27' : '#fafafa',
        paper: mode === 'dark' ? '#1a1f3a' : '#ffffff',
      },
      text: {
        primary: mode === 'dark' ? '#e4e7eb' : '#1a1a1a',
        secondary: mode === 'dark' ? '#a0a8b9' : '#666666',
      },
      grey: {
        50: mode === 'dark' ? '#1a1f3a' : '#fafafa',
        100: mode === 'dark' ? '#2a2f4a' : '#f5f5f5',
        200: mode === 'dark' ? '#3a3f5a' : '#eeeeee',
        300: mode === 'dark' ? '#4a4f6a' : '#e0e0e0',
        400: mode === 'dark' ? '#5a5f7a' : '#bdbdbd',
        500: mode === 'dark' ? '#6a6f8a' : '#9e9e9e',
        600: mode === 'dark' ? '#7a7f9a' : '#757575',
        700: mode === 'dark' ? '#8a8faa' : '#616161',
        800: mode === 'dark' ? '#9a9fba' : '#424242',
        900: mode === 'dark' ? '#aaafca' : '#212121',
      },
    },
    shape: { 
      borderRadius: 8 
    },
    typography: {
      fontFamily: [
        'Inter',
        'system-ui',
        'Segoe UI',
        'Roboto',
        'Helvetica',
        'Arial',
        'sans-serif',
      ].join(','),
      h1: {
        fontSize: '2.5rem',
        fontWeight: 700,
        lineHeight: 1.2,
      },
      h2: {
        fontSize: '2rem',
        fontWeight: 600,
        lineHeight: 1.3,
      },
      h3: {
        fontSize: '1.75rem',
        fontWeight: 600,
        lineHeight: 1.3,
      },
      h4: {
        fontSize: '1.5rem',
        fontWeight: 600,
        lineHeight: 1.4,
      },
      h5: {
        fontSize: '1.25rem',
        fontWeight: 600,
        lineHeight: 1.4,
      },
      h6: {
        fontSize: '1.125rem',
        fontWeight: 600,
        lineHeight: 1.4,
      },
      body1: {
        fontSize: '1rem',
        lineHeight: 1.6,
      },
      body2: {
        fontSize: '0.875rem',
        lineHeight: 1.6,
      },
    },
    shadows: [
      'none',
      mode === 'dark' ? '0px 1px 3px rgba(0, 0, 0, 0.3)' : '0px 1px 3px rgba(0, 0, 0, 0.1)',
      mode === 'dark' ? '0px 1px 5px rgba(0, 0, 0, 0.3)' : '0px 1px 5px rgba(0, 0, 0, 0.1)',
      mode === 'dark' ? '0px 2px 8px rgba(0, 0, 0, 0.3)' : '0px 2px 8px rgba(0, 0, 0, 0.1)',
      mode === 'dark' ? '0px 3px 12px rgba(0, 0, 0, 0.3)' : '0px 3px 12px rgba(0, 0, 0, 0.1)',
      mode === 'dark' ? '0px 4px 16px rgba(0, 0, 0, 0.3)' : '0px 4px 16px rgba(0, 0, 0, 0.1)',
      mode === 'dark' ? '0px 6px 20px rgba(0, 0, 0, 0.3)' : '0px 6px 20px rgba(0, 0, 0, 0.1)',
      mode === 'dark' ? '0px 8px 24px rgba(0, 0, 0, 0.3)' : '0px 8px 24px rgba(0, 0, 0, 0.1)',
      mode === 'dark' ? '0px 10px 28px rgba(0, 0, 0, 0.3)' : '0px 10px 28px rgba(0, 0, 0, 0.1)',
      mode === 'dark' ? '0px 12px 32px rgba(0, 0, 0, 0.3)' : '0px 12px 32px rgba(0, 0, 0, 0.1)',
      mode === 'dark' ? '0px 16px 40px rgba(0, 0, 0, 0.3)' : '0px 16px 40px rgba(0, 0, 0, 0.1)',
      mode === 'dark' ? '0px 20px 48px rgba(0, 0, 0, 0.3)' : '0px 20px 48px rgba(0, 0, 0, 0.1)',
      mode === 'dark' ? '0px 24px 56px rgba(0, 0, 0, 0.3)' : '0px 24px 56px rgba(0, 0, 0, 0.1)',
      mode === 'dark' ? '0px 28px 64px rgba(0, 0, 0, 0.3)' : '0px 28px 64px rgba(0, 0, 0, 0.1)',
      mode === 'dark' ? '0px 32px 72px rgba(0, 0, 0, 0.3)' : '0px 32px 72px rgba(0, 0, 0, 0.1)',
      mode === 'dark' ? '0px 36px 80px rgba(0, 0, 0, 0.3)' : '0px 36px 80px rgba(0, 0, 0, 0.1)',
      mode === 'dark' ? '0px 40px 88px rgba(0, 0, 0, 0.3)' : '0px 40px 88px rgba(0, 0, 0, 0.1)',
      mode === 'dark' ? '0px 44px 96px rgba(0, 0, 0, 0.3)' : '0px 44px 96px rgba(0, 0, 0, 0.1)',
      mode === 'dark' ? '0px 48px 104px rgba(0, 0, 0, 0.3)' : '0px 48px 104px rgba(0, 0, 0, 0.1)',
      mode === 'dark' ? '0px 52px 112px rgba(0, 0, 0, 0.3)' : '0px 52px 112px rgba(0, 0, 0, 0.1)',
      mode === 'dark' ? '0px 56px 120px rgba(0, 0, 0, 0.3)' : '0px 56px 120px rgba(0, 0, 0, 0.1)',
      mode === 'dark' ? '0px 60px 128px rgba(0, 0, 0, 0.3)' : '0px 60px 128px rgba(0, 0, 0, 0.1)',
      mode === 'dark' ? '0px 64px 136px rgba(0, 0, 0, 0.3)' : '0px 64px 136px rgba(0, 0, 0, 0.1)',
      mode === 'dark' ? '0px 68px 144px rgba(0, 0, 0, 0.3)' : '0px 68px 144px rgba(0, 0, 0, 0.1)',
      mode === 'dark' ? '0px 72px 152px rgba(0, 0, 0, 0.3)' : '0px 72px 152px rgba(0, 0, 0, 0.1)',
    ],
    components: {
      MuiAppBar: {
        styleOverrides: {
          root: {
            backgroundColor: mode === 'dark' ? '#1a1f3a' : '#ffffff',
            color: mode === 'dark' ? '#e4e7eb' : '#1a1a1a',
            boxShadow: mode === 'dark' ? '0px 1px 3px rgba(0, 0, 0, 0.3)' : '0px 1px 3px rgba(0, 0, 0, 0.1)',
          },
        },
      },
      MuiDrawer: {
        styleOverrides: {
          paper: {
            backgroundColor: mode === 'dark' ? '#1a1f3a' : '#ffffff',
            borderRight: mode === 'dark' ? '1px solid #2a2f4a' : '1px solid #e0e0e0',
          },
        },
      },
      MuiCard: {
        styleOverrides: {
          root: {
            boxShadow: mode === 'dark' ? '0px 1px 3px rgba(0, 0, 0, 0.3)' : '0px 1px 3px rgba(0, 0, 0, 0.1)',
            border: mode === 'dark' ? '1px solid #2a2f4a' : '1px solid #e0e0e0',
            transition: 'all 0.2s ease-in-out',
            '&:hover': {
              boxShadow: mode === 'dark' ? '0px 4px 12px rgba(0, 0, 0, 0.4)' : '0px 4px 12px rgba(0, 0, 0, 0.15)',
            },
          },
        },
      },
      MuiButton: {
        styleOverrides: {
          root: {
            textTransform: 'none',
            fontWeight: 500,
            borderRadius: 8,
            transition: 'all 0.2s ease-in-out',
          },
          contained: {
            boxShadow: mode === 'dark' ? '0px 2px 4px rgba(0, 0, 0, 0.3)' : '0px 2px 4px rgba(0, 0, 0, 0.1)',
            '&:hover': {
              boxShadow: mode === 'dark' ? '0px 4px 8px rgba(0, 0, 0, 0.4)' : '0px 4px 8px rgba(0, 0, 0, 0.15)',
            },
          },
        },
      },
      MuiChip: {
        styleOverrides: {
          root: {
            borderRadius: 6,
            fontWeight: 500,
          },
        },
      },
      MuiTextField: {
        styleOverrides: {
          root: {
            '& .MuiOutlinedInput-root': {
              borderRadius: 8,
              transition: 'all 0.2s ease-in-out',
            },
          },
        },
      },
      MuiTableRow: {
        styleOverrides: {
          root: {
            transition: 'background-color 0.2s ease-in-out',
          },
        },
      },
    },
    transitions: {
      duration: {
        shortest: 150,
        shorter: 200,
        short: 250,
        standard: 300,
        complex: 375,
        enteringScreen: 225,
        leavingScreen: 195,
      },
      easing: {
        easeInOut: 'cubic-bezier(0.4, 0, 0.2, 1)',
        easeOut: 'cubic-bezier(0, 0, 0.2, 1)',
        easeIn: 'cubic-bezier(0.4, 0, 1, 1)',
        sharp: 'cubic-bezier(0.4, 0, 0.6, 1)',
      },
    },
  })

interface ThemeProviderProps {
  children: ReactNode
}

export default function ThemeProvider({ children }: ThemeProviderProps) {
  const [mode, setMode] = useState<ThemeMode>(() => {
    const saved = localStorage.getItem('theme-mode')
    return (saved as ThemeMode) || 'light'
  })

  useEffect(() => {
    localStorage.setItem('theme-mode', mode)
  }, [mode])

  const toggleMode = () => {
    setMode(prev => prev === 'light' ? 'dark' : 'light')
  }

  const theme = createAppTheme(mode)

  return (
    <ThemeContext.Provider value={{ mode, toggleMode }}>
      <MuiThemeProvider theme={theme}>
        {children}
      </MuiThemeProvider>
    </ThemeContext.Provider>
  )
}
