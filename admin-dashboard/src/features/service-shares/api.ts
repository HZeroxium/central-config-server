import { apiSlice } from '@lib/api/apiSlice';
import type {
  ServiceShare,
  CreateServiceShareRequest,
  ServiceShareQueryFilter,
} from './types';

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const serviceSharesApi = apiSlice.injectEndpoints({
  endpoints: (builder) => ({
    getServiceShares: builder.query<PageResponse<ServiceShare>, ServiceShareQueryFilter>({
      query: (filters) => ({
        url: '/service-shares',
        params: filters,
      }),
      providesTags: (result) =>
        result
          ? [
              ...result.content.map(({ id }) => ({ type: 'ServiceShares' as const, id })),
              { type: 'ServiceShares', id: 'LIST' },
            ]
          : [{ type: 'ServiceShares', id: 'LIST' }],
    }),
    getServiceShareById: builder.query<ServiceShare, string>({
      query: (id) => `/service-shares/${id}`,
      providesTags: (_result, _error, id) => [{ type: 'ServiceShares', id }],
    }),
    grantServiceShare: builder.mutation<ServiceShare, CreateServiceShareRequest>({
      query: (share) => ({
        url: '/service-shares',
        method: 'POST',
        body: share,
      }),
      invalidatesTags: [{ type: 'ServiceShares', id: 'LIST' }],
    }),
    revokeServiceShare: builder.mutation<void, string>({
      query: (id) => ({
        url: `/service-shares/${id}/revoke`,
        method: 'POST',
      }),
      invalidatesTags: (_result, _error, id) => [
        { type: 'ServiceShares', id },
        { type: 'ServiceShares', id: 'LIST' },
      ],
    }),
  }),
});

export const {
  useGetServiceSharesQuery,
  useGetServiceShareByIdQuery,
  useGrantServiceShareMutation,
  useRevokeServiceShareMutation,
} = serviceSharesApi;
