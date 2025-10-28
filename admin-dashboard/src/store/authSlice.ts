import { createSlice, type PayloadAction } from '@reduxjs/toolkit';
import type { UserInfo, UserPermissions } from '@features/auth/authContext';

interface AuthState {
  userInfo: UserInfo | null;
  permissions: UserPermissions | null;
  initialized: boolean;
}

const initialState: AuthState = {
  userInfo: null,
  permissions: null,
  initialized: false,
};

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setUserInfo: (state, action: PayloadAction<UserInfo | null>) => {
      state.userInfo = action.payload;
    },
    setPermissions: (state, action: PayloadAction<UserPermissions | null>) => {
      state.permissions = action.payload;
    },
    setInitialized: (state, action: PayloadAction<boolean>) => {
      state.initialized = action.payload;
    },
    clearAuth: (state) => {
      state.userInfo = null;
      state.permissions = null;
    },
  },
});

export const { setUserInfo, setPermissions, setInitialized, clearAuth } = authSlice.actions;
export default authSlice.reducer;
