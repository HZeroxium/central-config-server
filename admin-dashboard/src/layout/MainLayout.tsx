import { Outlet } from "react-router-dom";
import { Box, Toolbar, useMediaQuery, useTheme } from "@mui/material";
import Sidebar from "@components/layout/Sidebar";
import Header from "@components/layout/Header";
import QuickActionsMenu from "@components/common/QuickActionsMenu";
import CommandPalette from "@components/common/CommandPalette";
import KeyboardShortcutsDialog from "@components/common/KeyboardShortcutsDialog";
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
  const queryClient = useQueryClient();

  const dispatch = useAppDispatch();
  const open = useAppSelector((state) => state.ui.sidebarOpen);
  const { open: commandPaletteOpen, setOpen: setCommandPaletteOpen } =
    useCommandPalette();
  const [shortcutsDialogOpen, setShortcutsDialogOpen] = useState(false);

  // Header scroll behavior: hide on scroll down, show on scroll up
  const { isVisible: isHeaderVisible } = useScrollDirection({
    threshold: 10,
    initialVisible: true,
  });

  const handleToggleSidebar = useCallback(() => {
    dispatch(toggleSidebar());
  }, [dispatch]);

  // Drawer width calculation
  const drawerWidth = open ? 240 : 60;

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
        key: "ctrl+b",
        handler: (e: KeyboardEvent) => {
          e.preventDefault();
          handleToggleSidebar();
        },
        description: "Toggle sidebar",
      },
      {
        key: "ctrl+/",
        handler: (e: KeyboardEvent) => {
          e.preventDefault();
          setShortcutsDialogOpen(true);
        },
        description: "Show keyboard shortcuts",
      },
      {
        key: "ctrl+?",
        handler: (e: KeyboardEvent) => {
          e.preventDefault();
          setShortcutsDialogOpen(true);
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

  return (
    <Box sx={{ display: "flex", minHeight: "100vh" }}>
      {/* Sidebar with hamburger menu */}
      <Sidebar open={open} onToggle={handleToggleSidebar} />

      {/* Header with search, breadcrumbs, theme toggle, and user menu */}
      <Header
        isVisible={isHeaderVisible}
        drawerWidth={drawerWidth}
        isMobile={isMobile}
      />

      {/* Main content area */}
      <Box
        component="main"
        id="main-content"
        role="main"
        aria-label="Main content"
        tabIndex={-1}
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
          pt: { xs: 10, sm: 12 }, // Account for header height with breadcrumbs
        }}
      >
        <Toolbar /> {/* Spacer for fixed header */}
        <Outlet />
        {/* <QuickActionsMenu /> */}
        <CommandPalette
          open={commandPaletteOpen}
          onClose={() => setCommandPaletteOpen(false)}
        />
        <KeyboardShortcutsDialog
          open={shortcutsDialogOpen}
          onClose={() => setShortcutsDialogOpen(false)}
        />
      </Box>
    </Box>
  );
}
