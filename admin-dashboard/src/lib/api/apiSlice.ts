import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import type { RootState } from '@app/store';

// Define tag types for cache invalidation
export const tagTypes = [
  'ApplicationServices',
  'ServiceInstances',
  'DriftEvents',
  'ApprovalRequests',
  'ServiceShares',
  'ConfigServer',
  'ServiceRegistry',
  'Users',
  'Teams',
  'KeycloakSync',
] as const;

export type TagType = typeof tagTypes[number];

// Base API slice with authentication
export const apiSlice = createApi({
  reducerPath: 'api',
  baseQuery: fetchBaseQuery({
    baseUrl: '/api',
    prepareHeaders: (headers, { getState }) => {
      // Get the token from the Redux state (we'll set this up in authSlice)
      const token = (getState() as RootState).auth?.token;
      
      if (token) {
        headers.set('authorization', `Bearer ${token}`);
      }
      
      headers.set('Content-Type', 'application/json');
      return headers;
    },
  }),
  tagTypes,
  endpoints: () => ({}),
});

export default apiSlice;
