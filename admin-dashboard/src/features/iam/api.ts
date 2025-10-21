import { apiSlice } from '@lib/api/apiSlice';
import type {
  IamUser,
  IamTeam,
  IamUserQueryFilter,
  IamTeamQueryFilter,
} from './types';

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const iamUsersApi = apiSlice.injectEndpoints({
  endpoints: (builder) => ({
    getIamUsers: builder.query<PageResponse<IamUser>, IamUserQueryFilter>({
      query: (filters) => ({
        url: '/iam/users',
        params: filters,
      }),
      providesTags: (result) =>
        result
          ? [
              ...result.content.map(({ id }) => ({ type: 'Users' as const, id })),
              { type: 'Users', id: 'LIST' },
            ]
          : [{ type: 'Users', id: 'LIST' }],
    }),
    getIamUserById: builder.query<IamUser, string>({
      query: (id) => `/iam/users/${id}`,
      providesTags: (_result, _error, id) => [{ type: 'Users', id }],
    }),
  }),
});

export const iamTeamsApi = apiSlice.injectEndpoints({
  endpoints: (builder) => ({
    getIamTeams: builder.query<PageResponse<IamTeam>, IamTeamQueryFilter>({
      query: (filters) => ({
        url: '/iam/teams',
        params: filters,
      }),
      providesTags: (result) =>
        result
          ? [
              ...result.content.map(({ id }) => ({ type: 'Teams' as const, id })),
              { type: 'Teams', id: 'LIST' },
            ]
          : [{ type: 'Teams', id: 'LIST' }],
    }),
    getIamTeamById: builder.query<IamTeam, string>({
      query: (id) => `/iam/teams/${id}`,
      providesTags: (_result, _error, id) => [{ type: 'Teams', id }],
    }),
  }),
});

export const {
  useGetIamUsersQuery,
  useGetIamUserByIdQuery,
} = iamUsersApi;

export const {
  useGetIamTeamsQuery,
  useGetIamTeamByIdQuery,
} = iamTeamsApi;
