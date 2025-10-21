import { apiSlice } from '@lib/api/apiSlice';

export interface UserMeResponse {
  userId: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  teamIds: string[];
  roles: string[];
  managerId?: string;
}

export interface UserPermissionsResponse {
  allowedRoutes: string[];
  roles: string[];
  teams: string[];
  features: Record<string, boolean>;
}

export const authApi = apiSlice.injectEndpoints({
  endpoints: (builder) => ({
    getCurrentUser: builder.query<UserMeResponse, void>({
      query: () => '/user/whoami',
      providesTags: ['Users'],
    }),
    getUserPermissions: builder.query<UserPermissionsResponse, void>({
      query: () => '/user/me/permissions',
      providesTags: ['Users'],
    }),
  }),
});

export const {
  useGetCurrentUserQuery,
  useGetUserPermissionsQuery,
} = authApi;
