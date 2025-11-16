import { useState, useEffect } from "react";
import {
  AppBar,
  Toolbar,
  Box,
  Typography,
  IconButton,
} from "@mui/material";
import LightModeIcon from "@mui/icons-material/LightMode";
import DarkModeIcon from "@mui/icons-material/DarkMode";
import Breadcrumbs from "@components/common/Breadcrumbs";
import UserMenu from "@features/auth/components/UserMenu";
import { useColorMode } from "@theme/colorModeContext";
import { useCommandPalette } from "@components/common/CommandPalette";

interface HeaderProps {
  /**
   * Whether header should be visible (for scroll behavior)
   */
  isVisible: boolean;
  /**
   * Drawer width (for margin calculation)
   */
  drawerWidth: number;
  /**
   * Whether mobile view
   */
  isMobile: boolean;
}

/**
 * Main header component with search, breadcrumbs, theme toggle, and user menu
 */
export function Header({ isVisible, drawerWidth, isMobile }: HeaderProps) {
  const { mode, toggleMode } = useColorMode();
  const { setOpen: setCommandPaletteOpen } = useCommandPalette();
  const [searchFocused, setSearchFocused] = useState(false);

  // Open command palette when search is focused
  useEffect(() => {
    if (searchFocused) {
      setCommandPaletteOpen(true);
      // Reset focus state after opening
      setTimeout(() => setSearchFocused(false), 100);
    }
  }, [searchFocused, setCommandPaletteOpen]);
  
  // Suppress unused variable warning - isMobile may be used for future responsive features
  void isMobile;

  return (
    <>
      {/* Skip to main content link for accessibility */}
      {/* <Box
        component="a"
        href="#main-content"
        sx={{
          position: "absolute",
          top: -40,
          left: 0,
          zIndex: 10000,
          padding: 1.5,
          backgroundColor: "primary.main",
          color: "white",
          textDecoration: "none",
          borderRadius: 1,
          fontWeight: 500,
          fontSize: "0.875rem",
          "&:focus": {
            top: 8,
            outline: "2px solid",
            outlineOffset: "2px",
          },
          "&:focus-visible": {
            top: 8,
          },
        }}
        onClick={(e) => {
          e.preventDefault();
          const mainContent = document.getElementById("main-content");
          if (mainContent) {
            mainContent.focus();
            mainContent.scrollIntoView({ behavior: "smooth" });
          }
        }}
      >
        Skip to main content
      </Box> */}

      <AppBar
        position="fixed"
        color="transparent"
        elevation={0}
        role="banner"
        sx={{
          borderBottom: 1,
          borderColor: "divider",
          backdropFilter: "blur(6px)",
          zIndex: (theme) => theme.zIndex.appBar,
          transition: "margin 0.3s, width 0.3s, transform 0.3s ease-in-out",
          width: {
            sm: `calc(100% - ${drawerWidth}px)`,
          },
          ml: {
            sm: `${drawerWidth}px`,
          },
          transform: isVisible ? "translateY(0)" : "translateY(-100%)",
        }}
      >
        <Toolbar
          sx={{
            justifyContent: "space-between",
            flexDirection: "row",
            alignItems: "center",
            flexWrap: "wrap",
            py: 1,
            gap: 1,
            minHeight: { xs: 56, sm: 64 },
          }}
        >
          {/* Left side: Title + Breadcrumb */}
          <Box
            sx={{
              display: "flex",
              alignItems: "center",
              flexWrap: "wrap",
              gap: 1.5,
              flex: 1,
              minWidth: 0, // Allow shrinking
            }}
          >
            <Typography
              variant="h6"
              color="primary"
              sx={{ 
                fontWeight: 700,
                flexShrink: 0,
                whiteSpace: "nowrap",
              }}
              component="h1"
            >
              Config Control Dashboard
            </Typography>

            {/* Breadcrumb inline after title */}
            <Box
              sx={{
                display: { xs: "none", sm: "flex" },
                flex: 1,
                minWidth: 0,
                "& .MuiBreadcrumbs-root": {
                  mb: 0,
                },
                "& .MuiBreadcrumbs-ol": {
                  flexWrap: "wrap",
                },
              }}
            >
              <Breadcrumbs enableDynamicLabels />
            </Box>
          </Box>

          {/* Right side: Actions */}
          <Box 
            sx={{ 
              display: "flex", 
              alignItems: "center", 
              gap: 1,
              flexShrink: 0,
            }}
          >
            {/* Theme toggle */}
            <IconButton
              onClick={toggleMode}
              aria-label={`Switch to ${mode === "dark" ? "light" : "dark"} mode`}
            >
              {mode === "dark" ? <LightModeIcon /> : <DarkModeIcon />}
            </IconButton>

            {/* User menu */}
            <UserMenu />
          </Box>

          {/* Breadcrumb on second row for small screens */}
          <Box
            sx={{
              display: { xs: "flex", sm: "none" },
              width: "100%",
              "& .MuiBreadcrumbs-root": {
                mb: 0,
              },
            }}
          >
            <Breadcrumbs enableDynamicLabels />
          </Box>
        </Toolbar>
      </AppBar>
    </>
  );
}

export default Header;

