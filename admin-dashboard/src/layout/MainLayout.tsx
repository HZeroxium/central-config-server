import { Outlet, NavLink, useLocation } from "react-router-dom";
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
  Badge,
  Collapse,
  Tooltip,
} from "@mui/material";
import MenuIcon from "@mui/icons-material/Menu";
import SettingsIcon from "@mui/icons-material/Settings";
import DnsIcon from "@mui/icons-material/Dns";
import DashboardIcon from "@mui/icons-material/Dashboard";
import AppsIcon from "@mui/icons-material/Apps";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import GavelIcon from "@mui/icons-material/Gavel";
import TrendingUpIcon from "@mui/icons-material/TrendingUp";
import ShareIcon from "@mui/icons-material/Share";
import PeopleIcon from "@mui/icons-material/People";
import PersonIcon from "@mui/icons-material/Person";
import GroupIcon from "@mui/icons-material/Group";
import MemoryIcon from "@mui/icons-material/Memory";
import LightModeIcon from "@mui/icons-material/LightMode";
import DarkModeIcon from "@mui/icons-material/DarkMode";
import ExpandLessIcon from "@mui/icons-material/ExpandLess";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import Breadcrumbs from "@components/common/Breadcrumbs";
import QuickActionsMenu from "@components/common/QuickActionsMenu";
import CommandPalette from "@components/common/CommandPalette";
import { useColorMode } from "@theme/colorModeContext";
import UserMenu from "@features/auth/components/UserMenu";
import { usePermissions } from "@features/auth/hooks/usePermissions";
import { usePendingApprovalCount } from "@features/approvals/hooks/usePendingApprovalCount";
import { useAppDispatch, useAppSelector } from "@store/hooks";
import { toggleSidebar } from "@store/uiSlice";
import { useCallback, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { useKeyboardShortcuts } from "@hooks/useKeyboardShortcuts";
import { useCommandPalette } from "@components/common/CommandPalette";
import { useScrollDirection } from "@hooks/useScrollDirection";

export default function MainLayout() {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down("sm"));
  const { mode, toggleMode } = useColorMode();
  const { isSysAdmin } = usePermissions();
  const pendingApprovalCount = usePendingApprovalCount();
  const queryClient = useQueryClient();

  const dispatch = useAppDispatch();
  const open = useAppSelector((state) => state.ui.sidebarOpen);
  const location = useLocation();
  const [iamMenuOpen, setIamMenuOpen] = useState(false);
  const { open: commandPaletteOpen, setOpen: setCommandPaletteOpen } =
    useCommandPalette();

  // Header scroll behavior: hide on scroll down, show on scroll up
  const { isVisible: isHeaderVisible } = useScrollDirection({
    threshold: 10,
    initialVisible: true,
  });

  const handleToggleSidebar = useCallback(() => {
    dispatch(toggleSidebar());
  }, [dispatch]);

  // Global keyboard shortcuts
  useKeyboardShortcuts({
    shortcuts: [
      {
        key: "ctrl+k",
        handler: () => {
          setCommandPaletteOpen(true);
        },
        description: "Open command palette",
      },
      {
        key: "ctrl+/",
        handler: () => {
          // TODO: Show keyboard shortcuts help dialog
          console.log("Keyboard shortcuts help - to be implemented");
        },
        description: "Show keyboard shortcuts",
      },
      {
        key: "ctrl+r",
        handler: (e: KeyboardEvent) => {
          e.preventDefault();
          // Refresh current page queries
          queryClient.invalidateQueries();
        },
        description: "Refresh page data",
      },
    ],
  });

  // Kích thước khi mở / khi đóng
  const drawerWidth = open ? 240 : 60;

  const navigationItems = [
    {
      path: "/dashboard",
      label: "Dashboard",
      icon: <DashboardIcon />,
      badge: undefined,
    },
    {
      path: "/application-services",
      label: "Application Services",
      icon: <AppsIcon />,
      badge: undefined,
    },
    {
      path: "/service-instances",
      label: "Service Instances",
      icon: <MemoryIcon />,
      badge: undefined,
    },
    {
      path: "/registry",
      label: "Service Registry",
      icon: <DnsIcon />,
      badge: undefined,
    },
    {
      path: "/configs",
      label: "Config Server",
      icon: <SettingsIcon />,
      badge: undefined,
    },
    {
      path: "/approvals",
      label: "Approvals",
      icon: <CheckCircleIcon />,
      badge: pendingApprovalCount > 0 ? pendingApprovalCount : undefined,
    },
    {
      path: "/approval-decisions",
      label: "Approval Decisions",
      icon: <GavelIcon />,
      badge: undefined,
    },
    {
      path: "/drift-events",
      label: "Drift Events",
      icon: <TrendingUpIcon />,
      badge: undefined,
    },
    {
      path: "/service-shares",
      label: "Service Shares",
      icon: <ShareIcon />,
      badge: undefined,
    },
  ];

  // Add admin-only IAM section
  const iamItems = isSysAdmin
    ? [
        {
          path: "/iam/users",
          label: "Users",
          icon: <PersonIcon />,
          badge: undefined,
        },
        {
          path: "/iam/teams",
          label: "Teams",
          icon: <GroupIcon />,
          badge: undefined,
        },
      ]
    : [];

  return (
    <Box sx={{ display: "flex", minHeight: "100vh" }}>
      <AppBar
        position="fixed"
        color="transparent"
        elevation={0}
        sx={{
          borderBottom: 1,
          borderColor: "divider",
          backdropFilter: "blur(6px)",
          zIndex: (theme) => theme.zIndex.drawer + 1,
          transition: "margin 0.3s, width 0.3s, transform 0.3s ease-in-out",
          width: {
            sm: `calc(100% - ${drawerWidth}px)`,
          },
          ml: {
            sm: `${drawerWidth}px`,
          },
          transform: isHeaderVisible ? "translateY(0)" : "translateY(-100%)",
        }}
      >
        <Toolbar sx={{ justifyContent: "space-between" }}>
          <Box sx={{ display: "flex", alignItems: "center" }}>
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
          <Box sx={{ display: "flex", alignItems: "center" }}>
            <IconButton onClick={toggleMode} sx={{ mr: 1 }}>
              {mode === "dark" ? <LightModeIcon /> : <DarkModeIcon />}
            </IconButton>
            <UserMenu />
          </Box>
        </Toolbar>
      </AppBar>

      <Drawer
        variant={isMobile ? "temporary" : "permanent"}
        open={open}
        onClose={handleToggleSidebar}
        PaperProps={{
          sx: {
            width: drawerWidth,
            boxSizing: "border-box",
            overflowX: "hidden",
            transition: "width 0.3s ease-in-out",
            borderRight: 1,
            borderColor: "divider",
          },
        }}
        sx={{
          flexShrink: 0,
          whiteSpace: "nowrap",
          width: drawerWidth,
          "& .MuiBackdrop-root": {
            display: "none", // Hide backdrop for permanent drawer
          },
        }}
      >
        <Toolbar />
        <List disablePadding>
          {navigationItems.map((item) => {
            const isActive =
              location.pathname === item.path ||
              (item.path !== "/" && location.pathname.startsWith(item.path));

            return (
              <Tooltip
                key={item.path}
                title={item.label}
                placement="right"
                disableHoverListener={open}
                arrow
              >
                <ListItemButton
                  component={NavLink}
                  to={item.path}
                  sx={{
                    minHeight: 48,
                    justifyContent: open ? "initial" : "center",
                    px: 2.5,
                    "&.Mui-selected": {}, // nếu muốn style thêm khi active
                  }}
                >
                  <ListItemIcon
                    sx={{
                      minWidth: 0,
                      mr: open ? 2 : "auto",
                      justifyContent: "center",
                      color: isActive ? "primary.600" : "text.secondary",
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
                    <Box
                      sx={{ display: "flex", alignItems: "center", flex: 1 }}
                    >
                      <ListItemText
                        primary={item.label}
                        sx={{
                          opacity: open ? 1 : 0,
                          transition: "opacity 0.3s",
                          fontWeight: isActive ? 500 : 400,
                          flex: 1,
                        }}
                      />
                      {item.badge && (
                        <Badge
                          badgeContent={item.badge}
                          color="warning"
                          max={99}
                        />
                      )}
                    </Box>
                  )}
                </ListItemButton>
              </Tooltip>
            );
          })}

          {/* IAM Section */}
          {isSysAdmin && (
            <>
              <Tooltip
                title="IAM"
                placement="right"
                disableHoverListener={open}
                arrow
              >
                <ListItemButton
                  onClick={() => setIamMenuOpen(!iamMenuOpen)}
                  sx={{
                    minHeight: 48,
                    justifyContent: open ? "initial" : "center",
                    px: 2.5,
                  }}
                >
                  <ListItemIcon
                    sx={{
                      minWidth: 0,
                      mr: open ? 2 : "auto",
                      justifyContent: "center",
                      color: "text.secondary",
                    }}
                  >
                    <PeopleIcon />
                  </ListItemIcon>
                  {open && (
                    <>
                      <ListItemText
                        primary="IAM"
                        sx={{
                          opacity: open ? 1 : 0,
                          transition: "opacity 0.3s",
                          fontWeight: 500,
                        }}
                      />
                      {iamMenuOpen ? <ExpandLessIcon /> : <ExpandMoreIcon />}
                    </>
                  )}
                </ListItemButton>
              </Tooltip>

              <Collapse in={iamMenuOpen && open} timeout="auto" unmountOnExit>
                <List component="div" disablePadding>
                  {iamItems.map((item) => {
                    const isActive =
                      location.pathname === item.path ||
                      (item.path !== "/" &&
                        location.pathname.startsWith(item.path));

                    return (
                      <Tooltip
                        key={item.path}
                        title={item.label}
                        placement="right"
                        disableHoverListener={open}
                        arrow
                      >
                        <ListItemButton
                          component={NavLink}
                          to={item.path}
                          sx={{
                            minHeight: 40,
                            pl: 6,
                            pr: 2.5,
                            "&.Mui-selected": {},
                          }}
                        >
                          <ListItemIcon
                            sx={{
                              minWidth: 0,
                              mr: 2,
                              justifyContent: "center",
                              color: isActive
                                ? "primary.600"
                                : "text.secondary",
                            }}
                          >
                            {item.icon}
                          </ListItemIcon>
                          <ListItemText
                            primary={item.label}
                            sx={{
                              fontWeight: isActive ? 500 : 400,
                            }}
                          />
                        </ListItemButton>
                      </Tooltip>
                    );
                  })}
                </List>
              </Collapse>
            </>
          )}
        </List>
      </Drawer>

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: { xs: 2, sm: 3 },
          width: {
            sm: `calc(100% - ${drawerWidth}px)`,
          },
          ml: {
            sm: `${drawerWidth}px`,
          },
          transition: "margin 0.3s, width 0.3s",
        }}
      >
        <Toolbar />
        <Breadcrumbs enableDynamicLabels />
        <Outlet />
        <QuickActionsMenu />
        <CommandPalette
          open={commandPaletteOpen}
          onClose={() => setCommandPaletteOpen(false)}
        />
      </Box>
    </Box>
  );
}
