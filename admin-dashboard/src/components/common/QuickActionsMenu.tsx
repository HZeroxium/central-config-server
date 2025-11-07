import { useState, useEffect } from "react";
import {
  Fab,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
  Tooltip,
} from "@mui/material";
import {
  Add as AddIcon,
  Refresh as RefreshIcon,
  Search as SearchIcon,
  MoreVert as MoreIcon,
} from "@mui/icons-material";
import { useLocation, useNavigate } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import { useCommandPalette } from "@components/common/CommandPalette";

export interface QuickAction {
  id: string;
  label: string;
  icon: React.ReactNode;
  onClick: () => void;
  /**
   * Routes where this action should be visible
   */
  visibleOn?: string[];
  /**
   * Routes where this action should be hidden
   */
  hiddenOn?: string[];
}

interface QuickActionsMenuProps {
  /**
   * Custom actions to include
   */
  customActions?: QuickAction[];
  /**
   * Position of the FAB
   */
  position?: "bottom-right" | "bottom-left" | "top-right" | "top-left";
}

/**
 * Quick Actions Menu - Floating Action Button with context-aware actions
 */
export function QuickActionsMenu({
  customActions = [],
  position = "bottom-right",
}: QuickActionsMenuProps) {
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const [isScrolled, setIsScrolled] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { setOpen: setCommandPaletteOpen } = useCommandPalette();

  // Track scroll position
  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 100);
    };
    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  // Default actions
  const defaultActions: QuickAction[] = [
    {
      id: "refresh",
      label: "Refresh Page",
      icon: <RefreshIcon />,
      onClick: () => {
        // Invalidate all queries for current route
        queryClient.invalidateQueries();
        setAnchorEl(null);
      },
    },
    {
      id: "search",
      label: "Search",
      icon: <SearchIcon />,
      onClick: () => {
        setCommandPaletteOpen(true);
        setAnchorEl(null);
      },
    },
    {
      id: "create-service",
      label: "Create Service",
      icon: <AddIcon />,
      onClick: () => {
        navigate("/application-services");
        // TODO: Open create dialog
        setAnchorEl(null);
      },
      visibleOn: ["/application-services"],
    },
    {
      id: "create-share",
      label: "Grant Share",
      icon: <AddIcon />,
      onClick: () => {
        navigate("/service-shares");
        setAnchorEl(null);
      },
      visibleOn: ["/service-shares"],
    },
  ];

  // Merge default and custom actions
  const allActions = [...defaultActions, ...customActions];

  // Filter actions based on current route
  const visibleActions = allActions.filter((action) => {
    if (action.hiddenOn?.some((route) => location.pathname.startsWith(route))) {
      return false;
    }
    if (
      action.visibleOn &&
      !action.visibleOn.some((route) => location.pathname.startsWith(route))
    ) {
      return false;
    }
    return true;
  });

  const handleOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const open = Boolean(anchorEl);

  // Position styles
  const positionStyles = {
    "bottom-right": { bottom: 24, right: 24 },
    "bottom-left": { bottom: 24, left: 24 },
    "top-right": { top: 24, right: 24 },
    "top-left": { top: 24, left: 24 },
  };

  if (visibleActions.length === 0) {
    return null;
  }

  return (
    <>
      <Tooltip title="Quick Actions" placement="left">
        <Fab
          color="primary"
          aria-label="quick actions"
          onClick={handleOpen}
          sx={{
            position: "fixed",
            ...positionStyles[position],
            zIndex: 1000,
            transition: "transform 0.2s",
            transform: isScrolled ? "scale(1)" : "scale(1)",
          }}
        >
          <MoreIcon />
        </Fab>
      </Tooltip>

      <Menu
        anchorEl={anchorEl}
        open={open}
        onClose={handleClose}
        anchorOrigin={{
          vertical: position.includes("bottom") ? "top" : "bottom",
          horizontal: position.includes("right") ? "right" : "left",
        }}
        transformOrigin={{
          vertical: position.includes("bottom") ? "bottom" : "top",
          horizontal: position.includes("right") ? "right" : "left",
        }}
        PaperProps={{
          sx: {
            mt: position.includes("bottom") ? -1 : 1,
            minWidth: 200,
          },
        }}
      >
        {visibleActions.map((action) => (
          <MenuItem
            key={action.id}
            onClick={() => {
              action.onClick();
            }}
          >
            <ListItemIcon sx={{ minWidth: 40 }}>{action.icon}</ListItemIcon>
            <ListItemText primary={action.label} />
          </MenuItem>
        ))}
      </Menu>
    </>
  );
}

export default QuickActionsMenu;
