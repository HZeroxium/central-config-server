import { useState, useEffect, useMemo } from "react";
import {
  Dialog,
  DialogContent,
  TextField,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Typography,
  Box,
  Chip,
  Divider,
  InputAdornment,
} from "@mui/material";
import {
  Search as SearchIcon,
  Dashboard as DashboardIcon,
  Apps as AppsIcon,
  Memory as MemoryIcon,
  CheckCircle as ApprovalIcon,
  Gavel as DecisionIcon,
  TrendingUp as DriftIcon,
  Share as ShareIcon,
  Settings as ConfigIcon,
  Dns as RegistryIcon,
  Refresh as RefreshIcon,
  ArrowForward as ArrowIcon,
} from "@mui/icons-material";
import { useNavigate } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import {
  useKeyboardShortcuts,
  getKeyDisplay,
} from "@hooks/useKeyboardShortcuts";

export interface CommandPaletteItem {
  id: string;
  label: string;
  description?: string;
  icon: React.ReactNode;
  action: () => void;
  keywords?: string[];
  category: "navigation" | "action" | "search";
}

interface CommandPaletteProps {
  open: boolean;
  onClose: () => void;
}

/**
 * Command Palette - Global search and quick actions (Cmd/Ctrl+K)
 */
export function CommandPalette({ open, onClose }: CommandPaletteProps) {
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedIndex, setSelectedIndex] = useState(0);
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  // Navigation commands
  const navigationCommands: CommandPaletteItem[] = useMemo(
    () => [
      {
        id: "dashboard",
        label: "Go to Dashboard",
        description: "View system overview",
        icon: <DashboardIcon />,
        action: () => {
          navigate("/dashboard");
          onClose();
        },
        keywords: ["dashboard", "home", "overview"],
        category: "navigation",
      },
      {
        id: "services",
        label: "Application Services",
        description: "Manage application services",
        icon: <AppsIcon />,
        action: () => {
          navigate("/application-services");
          onClose();
        },
        keywords: ["services", "apps", "applications"],
        category: "navigation",
      },
      {
        id: "instances",
        label: "Service Instances",
        description: "View service instances",
        icon: <MemoryIcon />,
        action: () => {
          navigate("/service-instances");
          onClose();
        },
        keywords: ["instances", "runtime"],
        category: "navigation",
      },
      {
        id: "approvals",
        label: "Approval Requests",
        description: "Manage approval requests",
        icon: <ApprovalIcon />,
        action: () => {
          navigate("/approvals");
          onClose();
        },
        keywords: ["approvals", "requests"],
        category: "navigation",
      },
      {
        id: "decisions",
        label: "Approval Decisions",
        description: "View approval decisions",
        icon: <DecisionIcon />,
        action: () => {
          navigate("/approval-decisions");
          onClose();
        },
        keywords: ["decisions", "history"],
        category: "navigation",
      },
      {
        id: "drift",
        label: "Drift Events",
        description: "Monitor configuration drift",
        icon: <DriftIcon />,
        action: () => {
          navigate("/drift-events");
          onClose();
        },
        keywords: ["drift", "config", "changes"],
        category: "navigation",
      },
      {
        id: "shares",
        label: "Service Shares",
        description: "Manage service shares",
        icon: <ShareIcon />,
        action: () => {
          navigate("/service-shares");
          onClose();
        },
        keywords: ["shares", "access"],
        category: "navigation",
      },
      {
        id: "configs",
        label: "Configuration",
        description: "View configuration server",
        icon: <ConfigIcon />,
        action: () => {
          navigate("/configs");
          onClose();
        },
        keywords: ["config", "configuration"],
        category: "navigation",
      },
      {
        id: "registry",
        label: "Service Registry",
        description: "View service registry",
        icon: <RegistryIcon />,
        action: () => {
          navigate("/registry");
          onClose();
        },
        keywords: ["registry", "consul"],
        category: "navigation",
      },
    ],
    [navigate, onClose]
  );

  // Action commands
  const actionCommands: CommandPaletteItem[] = useMemo(
    () => [
      {
        id: "refresh",
        label: "Refresh Page Data",
        description: "Invalidate all queries and refetch",
        icon: <RefreshIcon />,
        action: () => {
          queryClient.invalidateQueries();
          onClose();
        },
        keywords: ["refresh", "reload", "update"],
        category: "action",
      },
    ],
    [queryClient, onClose]
  );

  // Combine all commands
  const allCommands = useMemo(() => {
    return [...navigationCommands, ...actionCommands];
  }, [navigationCommands, actionCommands]);

  // Filter commands based on search query
  const filteredCommands = useMemo(() => {
    if (!searchQuery.trim()) {
      return allCommands;
    }

    const query = searchQuery.toLowerCase();
    return allCommands.filter((cmd) => {
      const matchesLabel = cmd.label.toLowerCase().includes(query);
      const matchesDescription = cmd.description?.toLowerCase().includes(query);
      const matchesKeywords = cmd.keywords?.some((kw) =>
        kw.toLowerCase().includes(query)
      );
      return matchesLabel || matchesDescription || matchesKeywords;
    });
  }, [searchQuery, allCommands]);

  // Reset selection when search changes
  useEffect(() => {
    setSelectedIndex(0);
  }, [searchQuery]);

  // Handle keyboard navigation
  useEffect(() => {
    if (!open) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "ArrowDown") {
        e.preventDefault();
        setSelectedIndex((prev) =>
          prev < filteredCommands.length - 1 ? prev + 1 : prev
        );
      } else if (e.key === "ArrowUp") {
        e.preventDefault();
        setSelectedIndex((prev) => (prev > 0 ? prev - 1 : prev));
      } else if (e.key === "Enter") {
        e.preventDefault();
        if (filteredCommands[selectedIndex]) {
          filteredCommands[selectedIndex].action();
        }
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [open, filteredCommands, selectedIndex]);

  const handleClose = () => {
    setSearchQuery("");
    setSelectedIndex(0);
    onClose();
  };

  return (
    <Dialog
      open={open}
      onClose={handleClose}
      maxWidth="sm"
      fullWidth
      PaperProps={{
        sx: {
          mt: 8,
          maxHeight: "70vh",
        },
      }}
    >
      <DialogContent sx={{ p: 0 }}>
        <Box sx={{ p: 2, borderBottom: 1, borderColor: "divider" }}>
          <TextField
            fullWidth
            placeholder="Type to search commands..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            autoFocus
            variant="standard"
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon />
                </InputAdornment>
              ),
              endAdornment: searchQuery && (
                <InputAdornment position="end">
                  <Chip
                    label={getKeyDisplay("escape")}
                    size="small"
                    variant="outlined"
                  />
                </InputAdornment>
              ),
            }}
            sx={{ mb: 1 }}
          />
          <Typography variant="caption" color="text.secondary">
            {filteredCommands.length} result
            {filteredCommands.length !== 1 ? "s" : ""}
          </Typography>
        </Box>

        <List sx={{ maxHeight: "50vh", overflow: "auto", p: 0 }}>
          {filteredCommands.length === 0 ? (
            <ListItem>
              <ListItemText
                primary="No commands found"
                secondary="Try a different search term"
              />
            </ListItem>
          ) : (
            filteredCommands.map((cmd, index) => (
              <ListItemButton
                key={cmd.id}
                selected={index === selectedIndex}
                onClick={cmd.action}
                sx={{
                  "&:hover": {
                    backgroundColor: "action.hover",
                  },
                }}
              >
                <ListItemIcon>{cmd.icon}</ListItemIcon>
                <ListItemText primary={cmd.label} secondary={cmd.description} />
                <ArrowIcon fontSize="small" sx={{ opacity: 0.5 }} />
              </ListItemButton>
            ))
          )}
        </List>

        <Divider />
        <Box sx={{ p: 1, bgcolor: "background.default" }}>
          <Typography variant="caption" color="text.secondary">
            Use ↑↓ to navigate, Enter to select, Esc to close
          </Typography>
        </Box>
      </DialogContent>
    </Dialog>
  );
}

/**
 * Hook to manage command palette state
 */
export function useCommandPalette() {
  const [open, setOpen] = useState(false);

  useKeyboardShortcuts({
    shortcuts: [
      {
        key: "ctrl+k",
        handler: () => {
          setOpen(true);
        },
        description: "Open command palette",
      },
      {
        key: "escape",
        handler: () => {
          setOpen(false);
        },
        description: "Close command palette",
        enabled: open,
      },
    ],
  });

  return {
    open,
    setOpen,
  };
}

export default CommandPalette;
