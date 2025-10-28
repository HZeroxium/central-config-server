import { configureStore } from "@reduxjs/toolkit";
import uiReducer from "./uiSlice";
import authReducer from "./authSlice";

export const store = configureStore({
  reducer: {
    ui: uiReducer,
    auth: authReducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      serializableCheck: {
        // Ignore these action types
        ignoredActions: ["ui/addNotification"],
        // Ignore these field paths in all actions
        ignoredActionPaths: ["payload.timestamp"],
        // Ignore these paths in the state
        ignoredPaths: ["ui.notifications"],
      },
    }),
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
