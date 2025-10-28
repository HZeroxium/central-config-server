import { useMemo, type ReactNode, useCallback } from "react";
import { ThemeProvider as MuiThemeProvider } from "@mui/material/styles";
import { createAppTheme } from "@theme/theme";
import { ColorModeContext } from "@theme/colorModeContext";
import { useAppDispatch, useAppSelector } from "@store/hooks";
import { toggleTheme } from "@store/uiSlice";

interface ThemeProviderProps {
  readonly children: ReactNode;
}

export default function ThemeProvider({ children }: ThemeProviderProps) {
  const dispatch = useAppDispatch();
  const mode = useAppSelector((state) => state.ui.themeMode);

  const toggleMode = useCallback(() => {
    dispatch(toggleTheme());
  }, [dispatch]);

  // Create theme based on current mode
  const theme = useMemo(() => createAppTheme(mode), [mode]);

  // Memoize context value to prevent unnecessary re-renders
  const contextValue = useMemo(
    () => ({ mode, toggleMode }),
    [mode, toggleMode]
  );

  return (
    <ColorModeContext.Provider value={contextValue}>
      <MuiThemeProvider theme={theme}>{children}</MuiThemeProvider>
    </ColorModeContext.Provider>
  );
}
