import { createContext, useContext } from 'react';

export type ThemeMode = 'light' | 'dark';

export interface ColorModeContextType {
  mode: ThemeMode;
  toggleMode: () => void;
}

export const ColorModeContext = createContext<ColorModeContextType | undefined>(undefined);

export const useColorMode = () => {
  const context = useContext(ColorModeContext);
  if (!context) {
    throw new Error('useColorMode must be used within a ThemeProvider');
  }
  return context;
};
