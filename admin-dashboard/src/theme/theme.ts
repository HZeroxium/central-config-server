import { createTheme } from '@mui/material/styles'

export const appTheme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#2196f3',
      light: '#64b5f6',
      dark: '#1976d2',
      contrastText: '#ffffff',
    },
    secondary: {
      main: '#f5f5f5',
      light: '#ffffff',
      dark: '#e0e0e0',
      contrastText: '#333333',
    },
    background: {
      default: '#fafafa',
      paper: '#ffffff',
    },
    text: {
      primary: '#1a1a1a',
      secondary: '#666666',
    },
    grey: {
      50: '#fafafa',
      100: '#f5f5f5',
      200: '#eeeeee',
      300: '#e0e0e0',
      400: '#bdbdbd',
      500: '#9e9e9e',
      600: '#757575',
      700: '#616161',
      800: '#424242',
      900: '#212121',
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
    '0px 1px 3px rgba(0, 0, 0, 0.1)',
    '0px 1px 5px rgba(0, 0, 0, 0.1)',
    '0px 2px 8px rgba(0, 0, 0, 0.1)',
    '0px 3px 12px rgba(0, 0, 0, 0.1)',
    '0px 4px 16px rgba(0, 0, 0, 0.1)',
    '0px 6px 20px rgba(0, 0, 0, 0.1)',
    '0px 8px 24px rgba(0, 0, 0, 0.1)',
    '0px 10px 28px rgba(0, 0, 0, 0.1)',
    '0px 12px 32px rgba(0, 0, 0, 0.1)',
    '0px 16px 40px rgba(0, 0, 0, 0.1)',
    '0px 20px 48px rgba(0, 0, 0, 0.1)',
    '0px 24px 56px rgba(0, 0, 0, 0.1)',
    '0px 28px 64px rgba(0, 0, 0, 0.1)',
    '0px 32px 72px rgba(0, 0, 0, 0.1)',
    '0px 36px 80px rgba(0, 0, 0, 0.1)',
    '0px 40px 88px rgba(0, 0, 0, 0.1)',
    '0px 44px 96px rgba(0, 0, 0, 0.1)',
    '0px 48px 104px rgba(0, 0, 0, 0.1)',
    '0px 52px 112px rgba(0, 0, 0, 0.1)',
    '0px 56px 120px rgba(0, 0, 0, 0.1)',
    '0px 60px 128px rgba(0, 0, 0, 0.1)',
    '0px 64px 136px rgba(0, 0, 0, 0.1)',
    '0px 68px 144px rgba(0, 0, 0, 0.1)',
    '0px 72px 152px rgba(0, 0, 0, 0.1)',
  ],
  components: {
    MuiAppBar: {
      styleOverrides: {
        root: {
          backgroundColor: '#ffffff',
          color: '#1a1a1a',
          boxShadow: '0px 1px 3px rgba(0, 0, 0, 0.1)',
        },
      },
    },
    MuiDrawer: {
      styleOverrides: {
        paper: {
          backgroundColor: '#ffffff',
          borderRight: '1px solid #e0e0e0',
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          boxShadow: '0px 1px 3px rgba(0, 0, 0, 0.1)',
          border: '1px solid #e0e0e0',
          transition: 'all 0.2s ease-in-out',
          '&:hover': {
            boxShadow: '0px 4px 12px rgba(0, 0, 0, 0.15)',
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
          boxShadow: '0px 2px 4px rgba(0, 0, 0, 0.1)',
          '&:hover': {
            boxShadow: '0px 4px 8px rgba(0, 0, 0, 0.15)',
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


