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
          zIndex: (theme) => theme.zIndex.drawer + 1,
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
            flexDirection: "column",
            alignItems: "stretch",
            py: 1,
          }}
        >
          {/* Top row: Title, Search, Actions */}
          <Box
            sx={{
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
              width: "100%",
            }}
          >
            <Typography
              variant="h6"
              color="primary"
              sx={{ fontWeight: 700 }}
              component="h1"
            >
              Config Control Dashboard
            </Typography>

            <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
              {/* Search bar */}
              {/* <TextField
                ref={searchInputRef}
                placeholder="Search... (Ctrl+K)"
                size="small"
                onClick={handleSearchClick}
                onFocus={handleSearchFocus}
                sx={{
                  width: isMobile ? 150 : 250,
                  "& .MuiOutlinedInput-root": {
                    backgroundColor: "background.paper",
                  },
                }}
                slotProps={{
                  input: {
                    startAdornment: (
                      <InputAdornment position="start">
                        <SearchIcon fontSize="small" />
                      </InputAdornment>
                    ),
                  },
                }}
                aria-label="Search commands and navigate"
              /> */}

              {/* Theme toggle */}
              <IconButton
                onClick={toggleMode}
                aria-label={`Switch to ${mode === "dark" ? "light" : "dark"} mode`}
                sx={{ mr: 1 }}
              >
                {mode === "dark" ? <LightModeIcon /> : <DarkModeIcon />}
              </IconButton>

              {/* User menu */}
              <UserMenu />
            </Box>
          </Box>

          {/* Bottom row: Breadcrumbs */}
          <Box sx={{ width: "100%", mt: 0.5 }}>
            <Breadcrumbs enableDynamicLabels />
          </Box>
        </Toolbar>
      </AppBar>
    </>
  );
}

export default Header;

