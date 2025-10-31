import { IconButton, Toolbar, Tooltip } from "@mui/material";
import MenuIcon from "@mui/icons-material/Menu";

interface SidebarHeaderProps {
  /**
   * Whether sidebar is expanded
   */
  expanded: boolean;
  /**
   * Toggle sidebar handler
   */
  onToggle: () => void;
}

/**
 * Sidebar header component with hamburger menu button
 */
export function SidebarHeader({ expanded, onToggle }: SidebarHeaderProps) {
  return (
    <Toolbar
      sx={{
        minHeight: 64,
        display: "flex",
        alignItems: "center",
        justifyContent: expanded ? "flex-start" : "center",
        px: expanded ? 2 : 1,
        borderBottom: 1,
        borderColor: "divider",
      }}
    >
      <Tooltip title={expanded ? "Collapse sidebar" : "Expand sidebar"}>
        <IconButton
          onClick={onToggle}
          aria-label={expanded ? "Collapse sidebar" : "Expand sidebar"}
          sx={{
            color: "text.primary",
            "&:hover": {
              backgroundColor: "action.hover",
            },
          }}
        >
          <MenuIcon />
        </IconButton>
      </Tooltip>
    </Toolbar>
  );
}

export default SidebarHeader;

