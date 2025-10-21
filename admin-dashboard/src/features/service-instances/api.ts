import { apiSlice } from '@lib/api/apiSlice';
import type {
  ServiceInstance,
  UpdateServiceInstanceRequest,
  ServiceInstanceQueryFilter,
} from './types';

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const serviceInstancesApi = apiSlice.injectEndpoints({
  endpoints: (builder) => ({
    getServiceInstances: builder.query<PageResponse<ServiceInstance>, ServiceInstanceQueryFilter>({
      query: (filters) => ({
        url: '/service-instances',
        params: filters,
      }),
      providesTags: (result) =>
        result
          ? [
              ...result.content.map(({ id }) => ({ type: 'ServiceInstances' as const, id })),
              { type: 'ServiceInstances', id: 'LIST' },
            ]
          : [{ type: 'ServiceInstances', id: 'LIST' }],
    }),
    getServiceInstanceById: builder.query<ServiceInstance, string>({
      query: (id) => `/service-instances/${id}`,
      providesTags: (_result, _error, id) => [{ type: 'ServiceInstances', id }],
    }),
    updateServiceInstance: builder.mutation<ServiceInstance, { id: string; updates: UpdateServiceInstanceRequest }>({
      query: ({ id, updates }) => ({
        url: `/service-instances/${id}`,
        method: 'PUT',
        body: updates,
      }),
      invalidatesTags: (_result, _error, { id }) => [
        { type: 'ServiceInstances', id },
        { type: 'ServiceInstances', id: 'LIST' },
      ],
    }),
    deleteServiceInstance: builder.mutation<void, string>({
      query: (id) => ({
        url: `/service-instances/${id}`,
        method: 'DELETE',
      }),
      invalidatesTags: (_result, _error, id) => [
        { type: 'ServiceInstances', id },
        { type: 'ServiceInstances', id: 'LIST' },
      ],
    }),
  }),
});

export const {
  useGetServiceInstancesQuery,
  useGetServiceInstanceByIdQuery,
  useUpdateServiceInstanceMutation,
  useDeleteServiceInstanceMutation,
} = serviceInstancesApi;
