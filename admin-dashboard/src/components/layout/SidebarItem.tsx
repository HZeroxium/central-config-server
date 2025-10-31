import { NavLink, useLocation } from "react-router-dom";
import {
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Tooltip,
  Badge,
  Box,
} from "@mui/material";

interface SidebarItemProps {
  /**
   * Navigation path
   */
  path: string;
  /**
   * Display label
   */
  label: string;
  /**
   * Icon component
   */
  icon: React.ReactNode;
  /**
   * Badge content (optional)
   */
  badge?: number;
  /**
   * Whether sidebar is expanded
   */
  expanded: boolean;
}

/**
 * Individual sidebar navigation item component
 */
export function SidebarItem({
  path,
  label,
  icon,
  badge,
  expanded,
}: SidebarItemProps) {
  const location = useLocation();
  const isActive =
    location.pathname === path ||
    (path !== "/" && location.pathname.startsWith(path));

  return (
    <Tooltip
      title={label}
      placement="right"
      disableHoverListener={expanded}
      arrow
    >
      <ListItemButton
        component={NavLink}
        to={path}
        sx={{
          minHeight: 48,
          justifyContent: expanded ? "initial" : "center",
          px: 2.5,
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
          "&.active": {
            backgroundColor: "action.selected",
            "&::before": {
              width: 4,
            },
          },
        }}
      >
        <ListItemIcon
          sx={{
            minWidth: 0,
            mr: expanded ? 2 : "auto",
            justifyContent: "center",
            color: isActive ? "primary.main" : "text.secondary",
            transition: "color 0.2s ease-in-out",
          }}
        >
          {badge && badge > 0 ? (
            <Badge badgeContent={badge} color="warning" max={99}>
              {icon}
            </Badge>
          ) : (
            icon
          )}
        </ListItemIcon>
        {expanded && (
          <Box
            sx={{ display: "flex", alignItems: "center", flex: 1 }}
          >
            <ListItemText
              primary={label}
              sx={{
                opacity: expanded ? 1 : 0,
                transition: "opacity 0.3s",
                fontWeight: isActive ? 600 : 400,
                flex: 1,
              }}
            />
            {badge && badge > 0 && (
              <Badge
                badgeContent={badge}
                color="warning"
                max={99}
                sx={{ ml: 1 }}
              />
            )}
          </Box>
        )}
      </ListItemButton>
    </Tooltip>
  );
}

export default SidebarItem;

