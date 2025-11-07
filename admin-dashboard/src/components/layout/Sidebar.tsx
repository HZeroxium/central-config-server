import { useState, useMemo } from "react";
import { useLocation, NavLink } from "react-router-dom";
import {
  Drawer,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Collapse,
  Tooltip,
  useMediaQuery,
  useTheme,
} from "@mui/material";
import DashboardIcon from "@mui/icons-material/Dashboard";
import AppsIcon from "@mui/icons-material/Apps";
import MemoryIcon from "@mui/icons-material/Memory";
import DnsIcon from "@mui/icons-material/Dns";
import SettingsIcon from "@mui/icons-material/Settings";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import GavelIcon from "@mui/icons-material/Gavel";
import TrendingUpIcon from "@mui/icons-material/TrendingUp";
import ShareIcon from "@mui/icons-material/Share";
import PeopleIcon from "@mui/icons-material/People";
import PersonIcon from "@mui/icons-material/Person";
import GroupIcon from "@mui/icons-material/Group";
import ExpandLessIcon from "@mui/icons-material/ExpandLess";
import ExpandMoreIcon from "@mui/icons-material/ExpandMore";
import StorageIcon from "@mui/icons-material/Storage";
import SidebarHeader from "./SidebarHeader";
import SidebarItem from "./SidebarItem";
import { usePermissions } from "@features/auth/hooks/usePermissions";
import { usePendingApprovalCount } from "@features/approvals/hooks/usePendingApprovalCount";

interface SidebarProps {
  /**
   * Whether sidebar is open/expanded
   */
  open: boolean;
  /**
   * Toggle sidebar handler
   */
  onToggle: () => void;
}

/**
 * Main sidebar navigation component
 */
export function Sidebar({ open, onToggle }: SidebarProps) {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down("sm"));
  const { isSysAdmin } = usePermissions();
  const pendingApprovalCount = usePendingApprovalCount();
  const location = useLocation();
  const [iamMenuOpen, setIamMenuOpen] = useState(false);

  const drawerWidth = open ? 240 : 60;

  const navigationItems = useMemo(
    () => [
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
        path: "/kv",
        label: "Key-Value Store",
        icon: <StorageIcon />,
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
    ],
    [pendingApprovalCount]
  );

  const iamItems = useMemo(
    () =>
      isSysAdmin
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
        : [],
    [isSysAdmin]
  );

  return (
    <Drawer
      variant={isMobile ? "temporary" : "permanent"}
      open={open}
      onClose={onToggle}
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
      aria-label="Main navigation"
    >
      <SidebarHeader expanded={open} onToggle={onToggle} />
      <List disablePadding component="nav" aria-label="Navigation menu">
        {navigationItems.map((item) => (
          <SidebarItem
            key={item.path}
            path={item.path}
            label={item.label}
            icon={item.icon}
            badge={item.badge}
            expanded={open}
          />
        ))}

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
                  position: "relative",
                  "&:hover": {
                    backgroundColor: "action.hover",
                  },
                }}
                aria-expanded={iamMenuOpen}
                aria-controls="iam-menu"
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

            <Collapse
              in={iamMenuOpen && open}
              timeout="auto"
              unmountOnExit
              id="iam-menu"
            >
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
                          position: "relative",
                          backgroundColor: isActive
                            ? "action.selected"
                            : "transparent",
                          "&::before": {
                            content: '""',
                            position: "absolute",
                            left: 0,
                            top: 0,
                            bottom: 0,
                            width: isActive ? 4 : 0,
                            backgroundColor: "primary.main",
                            transition: "width 0.2s ease-in-out",
                          },
                          "&:hover": {
                            backgroundColor: "action.hover",
                          },
                        }}
                      >
                        <ListItemIcon
                          sx={{
                            minWidth: 0,
                            mr: 2,
                            justifyContent: "center",
                            color: isActive
                              ? "primary.main"
                              : "text.secondary",
                          }}
                        >
                          {item.icon}
                        </ListItemIcon>
                        <ListItemText
                          primary={item.label}
                          sx={{
                            fontWeight: isActive ? 600 : 400,
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
  );
}

export default Sidebar;

