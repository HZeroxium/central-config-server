import { createSlice, type PayloadAction } from "@reduxjs/toolkit";

export type ThemeMode = "light" | "dark";

interface UIState {
  sidebarOpen: boolean;
  themeMode: ThemeMode;
  notifications: Array<{
    id: string;
    type: "success" | "error" | "warning" | "info";
    message: string;
    timestamp: number;
  }>;
}

// Load initial state from localStorage
const loadInitialState = (): UIState => {
  try {
    const savedTheme = localStorage.getItem("themeMode") as ThemeMode | null;
    const savedSidebar = localStorage.getItem("sidebarOpen");

    // If no saved preference, detect system preference
    let themeMode: ThemeMode = "light";
    if (savedTheme) {
      themeMode = savedTheme;
    } else if (globalThis.window?.matchMedia) {
      // Detect system preference
      themeMode = globalThis.window.matchMedia("(prefers-color-scheme: dark)")
        .matches
        ? "dark"
        : "light";
    }

    return {
      sidebarOpen: savedSidebar === null || savedSidebar === "true",
      themeMode,
      notifications: [],
    };
  } catch (error) {
    console.error("Failed to load UI state from localStorage:", error);
    return {
      sidebarOpen: true,
      themeMode: "light",
      notifications: [],
    };
  }
};

const initialState: UIState = loadInitialState();

const uiSlice = createSlice({
  name: "ui",
  initialState,
  reducers: {
    toggleSidebar: (state) => {
      state.sidebarOpen = !state.sidebarOpen;
      try {
        localStorage.setItem("sidebarOpen", String(state.sidebarOpen));
      } catch (error) {
        console.error("Failed to save sidebar state:", error);
      }
    },
    setSidebarOpen: (state, action: PayloadAction<boolean>) => {
      state.sidebarOpen = action.payload;
      try {
        localStorage.setItem("sidebarOpen", String(state.sidebarOpen));
      } catch (error) {
        console.error("Failed to save sidebar state:", error);
      }
    },
    toggleTheme: (state) => {
      state.themeMode = state.themeMode === "light" ? "dark" : "light";
      try {
        localStorage.setItem("themeMode", state.themeMode);
      } catch (error) {
        console.error("Failed to save theme mode:", error);
      }
    },
    setThemeMode: (state, action: PayloadAction<ThemeMode>) => {
      state.themeMode = action.payload;
      try {
        localStorage.setItem("themeMode", state.themeMode);
      } catch (error) {
        console.error("Failed to save theme mode:", error);
      }
    },
    addNotification: (
      state,
      action: PayloadAction<
        Omit<UIState["notifications"][0], "id" | "timestamp">
      >
    ) => {
      state.notifications.push({
        ...action.payload,
        id: `notif-${Date.now()}-${Math.random()}`,
        timestamp: Date.now(),
      });
      // Keep only last 50 notifications
      if (state.notifications.length > 50) {
        state.notifications = state.notifications.slice(-50);
      }
    },
    removeNotification: (state, action: PayloadAction<string>) => {
      state.notifications = state.notifications.filter(
        (n) => n.id !== action.payload
      );
    },
    clearNotifications: (state) => {
      state.notifications = [];
    },
  },
});

export const {
  toggleSidebar,
  setSidebarOpen,
  toggleTheme,
  setThemeMode,
  addNotification,
  removeNotification,
  clearNotifications,
} = uiSlice.actions;

export default uiSlice.reducer;
