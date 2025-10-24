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
  useMediaQuery,
  useTheme,
  Badge
} from '@mui/material'
import MenuIcon from '@mui/icons-material/Menu'
import SettingsIcon from '@mui/icons-material/Settings'
import DnsIcon from '@mui/icons-material/Dns'
import DashboardIcon from '@mui/icons-material/Dashboard'
import AppsIcon from '@mui/icons-material/Apps'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import TrendingUpIcon from '@mui/icons-material/TrendingUp'
import ShareIcon from '@mui/icons-material/Share'
import PeopleIcon from '@mui/icons-material/People'
import MemoryIcon from '@mui/icons-material/Memory'
import LightModeIcon from '@mui/icons-material/LightMode'
import DarkModeIcon from '@mui/icons-material/DarkMode'
import Breadcrumbs from '@components/common/Breadcrumbs'
import { useTheme as useCustomTheme } from '@app/providers/ThemeProvider'
import UserMenu from '@features/auth/components/UserMenu'
import { usePermissions } from '@features/auth/hooks/usePermissions'
import { usePendingApprovalCount } from '@features/approvals/hooks/usePendingApprovalCount'
import { useAppDispatch, useAppSelector } from '@store/hooks'
import { toggleSidebar } from '@store/uiSlice'
import { useCallback } from 'react'

export default function MainLayout() {
  const theme = useTheme()
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'))
  const { mode, toggleMode } = useCustomTheme()
  const { isSysAdmin } = usePermissions()
  const pendingApprovalCount = usePendingApprovalCount()
  
  const dispatch = useAppDispatch()
  const open = useAppSelector((state) => state.ui.sidebarOpen)
  const location = useLocation()
  
  const handleToggleSidebar = useCallback(() => {
    dispatch(toggleSidebar())
  }, [dispatch])

  // Kích thước khi mở / khi đóng
  const openWidth = 240
  const closedWidth = 60

  const navigationItems = [
    { path: '/dashboard', label: 'Dashboard', icon: <DashboardIcon />, badge: undefined },
    { path: '/application-services', label: 'Application Services', icon: <AppsIcon />, badge: undefined },
    { path: '/service-instances', label: 'Service Instances', icon: <MemoryIcon />, badge: undefined },
    { path: '/registry', label: 'Service Registry', icon: <DnsIcon />, badge: undefined },
    { path: '/configs', label: 'Config Server', icon: <SettingsIcon />, badge: undefined },
    { path: '/approvals', label: 'Approvals', icon: <CheckCircleIcon />, badge: pendingApprovalCount > 0 ? pendingApprovalCount : undefined },
    { path: '/drift-events', label: 'Drift Events', icon: <TrendingUpIcon />, badge: undefined },
    { path: '/service-shares', label: 'Service Shares', icon: <ShareIcon />, badge: undefined },
  ]

  // Add admin-only items
  if (isSysAdmin) {
    navigationItems.push(
      { path: '/iam/users', label: 'Users', icon: <PeopleIcon />, badge: undefined },
      { path: '/iam/teams', label: 'Teams', icon: <PeopleIcon />, badge: undefined }
    )
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
              onClick={handleToggleSidebar}
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
            <UserMenu />
          </Box>
        </Toolbar>
      </AppBar>

      <Drawer
        variant={isMobile ? 'temporary' : 'permanent'}
        open={open}
        onClose={handleToggleSidebar}
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
                  {item.badge ? (
                    <Badge badgeContent={item.badge} color="warning" max={99}>
                      {item.icon}
                    </Badge>
                  ) : (
                    item.icon
                  )}
                </ListItemIcon>
                {open && (
                  <Box sx={{ display: 'flex', alignItems: 'center', flex: 1 }}>
                    <ListItemText
                      primary={item.label}
                      sx={{
                        opacity: open ? 1 : 0,
                        transition: 'opacity 0.3s',
                        fontWeight: isActive ? 500 : 400,
                        flex: 1,
                      }}
                    />
                    {item.badge && (
                      <Badge badgeContent={item.badge} color="warning" max={99} />
                    )}
                  </Box>
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
