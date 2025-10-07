import { Outlet, NavLink, useLocation } from 'react-router-dom'
import {
  AppBar,
  Box,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Typography,
  Avatar,
  Menu,
  MenuItem,
  useMediaQuery,
  useTheme
} from '@mui/material'
import MenuIcon from '@mui/icons-material/Menu'
import SettingsIcon from '@mui/icons-material/Settings'
import StorageIcon from '@mui/icons-material/Storage'
import DashboardIcon from '@mui/icons-material/Dashboard'
import AccountCircleIcon from '@mui/icons-material/AccountCircle'
import LightModeIcon from '@mui/icons-material/LightMode'
import DarkModeIcon from '@mui/icons-material/DarkMode'
import { useState } from 'react'
import Breadcrumbs from '@components/common/Breadcrumbs'
import { useTheme as useCustomTheme } from '@app/providers/ThemeProvider'

export default function MainLayout() {
  const theme = useTheme()
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'))

  const [open, setOpen] = useState(true)
  const [userMenuAnchor, setUserMenuAnchor] = useState<null | HTMLElement>(null)
  const location = useLocation()
  const { mode, toggleMode } = useCustomTheme()

  // Kích thước khi mở / khi đóng
  const openWidth = 240
  const closedWidth = 60

  const navigationItems = [
    { path: '/', label: 'Dashboard', icon: <DashboardIcon /> },
    { path: '/services', label: 'Services', icon: <StorageIcon /> },
    { path: '/configs', label: 'Configs', icon: <SettingsIcon /> },
  ]

  const handleUserMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setUserMenuAnchor(event.currentTarget)
  }

  const handleUserMenuClose = () => {
    setUserMenuAnchor(null)
  }

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <AppBar
        position="fixed"
        color="transparent"
        elevation={0}
        sx={{
          borderBottom: 1,
          borderColor: 'divider',
          backdropFilter: 'blur(6px)',
          zIndex: (theme) => theme.zIndex.drawer + 1,
          transition: 'margin 0.3s, width 0.3s',
          width: {
            sm: `calc(100% - ${open ? openWidth : closedWidth}px)`,
          },
          ml: {
            sm: `${open ? openWidth : closedWidth}px`,
          }
        }}
      >
        <Toolbar sx={{ justifyContent: 'space-between' }}>
          <Box sx={{ display: 'flex', alignItems: 'center' }}>
            <IconButton
              edge="start"
              color="inherit"
              onClick={() => setOpen(!open)}
              sx={{ mr: 2 }}
            >
              <MenuIcon />
            </IconButton>
            <Typography variant="h6" color="primary" sx={{ fontWeight: 700 }}>
              Config Control Dashboard
            </Typography>
          </Box>
          <Box sx={{ display: 'flex', alignItems: 'center' }}>
            <IconButton onClick={toggleMode} sx={{ mr: 1 }}>
              {mode === 'dark' ? <LightModeIcon /> : <DarkModeIcon />}
            </IconButton>
            <IconButton onClick={handleUserMenuOpen} sx={{ ml: 2 }}>
              <Avatar sx={{ width: 32, height: 32, bgcolor: 'primary.main' }}>
                <AccountCircleIcon />
              </Avatar>
            </IconButton>
            <Menu
              anchorEl={userMenuAnchor}
              open={Boolean(userMenuAnchor)}
              onClose={handleUserMenuClose}
            >
              <MenuItem onClick={handleUserMenuClose}>Profile</MenuItem>
              <MenuItem onClick={handleUserMenuClose}>Settings</MenuItem>
              <MenuItem onClick={handleUserMenuClose}>Logout</MenuItem>
            </Menu>
          </Box>
        </Toolbar>
      </AppBar>

      <Drawer
        variant={isMobile ? 'temporary' : 'permanent'}
        open={open}
        onClose={() => setOpen(false)}
        sx={{
          width: open ? openWidth : closedWidth,
          flexShrink: 0,
          whiteSpace: 'nowrap',
          '& .MuiDrawer-paper': {
            width: open ? openWidth : closedWidth,
            boxSizing: 'border-box',
            overflowX: 'hidden',
            transition: 'width 0.3s',
            borderRight: 1,
            borderColor: 'divider',
          },
        }}
      >
        <Toolbar />
        <List disablePadding>
          {navigationItems.map((item) => {
            const isActive =
              location.pathname === item.path ||
              (item.path !== '/' && location.pathname.startsWith(item.path))

            return (
              <ListItemButton
                key={item.path}
                component={NavLink}
                to={item.path}
                sx={{
                  minHeight: 48,
                  justifyContent: open ? 'initial' : 'center',
                  px: 2.5,
                  '&.Mui-selected': {}, // nếu muốn style thêm khi active
                }}
              >
                <ListItemIcon
                  sx={{
                    minWidth: 0,
                    mr: open ? 2 : 'auto',
                    justifyContent: 'center',
                    color: isActive ? 'primary.600' : 'text.secondary',
                  }}
                >
                  {item.icon}
                </ListItemIcon>
                {open && (
                  <ListItemText
                    primary={item.label}
                    sx={{
                      opacity: open ? 1 : 0,
                      transition: 'opacity 0.3s',
                      fontWeight: isActive ? 500 : 400,
                    }}
                  />
                )}
              </ListItemButton>
            )
          })}
        </List>
      </Drawer>

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: { xs: 2, sm: 3 },
          width: {
            sm: `calc(100% - ${open ? openWidth : closedWidth}px)`,
          },
          ml: {
            sm: `${open ? openWidth : closedWidth}px`,
          },
          transition: 'margin 0.3s, width 0.3s',
        }}
      >
        <Toolbar />
        <Breadcrumbs />
        <Outlet />
      </Box>
    </Box>
  )
}
