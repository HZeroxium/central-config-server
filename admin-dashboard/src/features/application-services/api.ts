import { apiSlice } from '@lib/api/apiSlice';
import type {
  ApplicationService,
  CreateApplicationServiceRequest,
  UpdateApplicationServiceRequest,
  ApplicationServiceQueryFilter,
} from './types';

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export const applicationServicesApi = apiSlice.injectEndpoints({
  endpoints: (builder) => ({
    getApplicationServices: builder.query<PageResponse<ApplicationService>, {
      filter?: ApplicationServiceQueryFilter;
      page?: number;
      size?: number;
    }>({
      query: ({ filter = {}, page = 0, size = 10 }) => ({
        url: '/application-services',
        params: {
          ...filter,
          page,
          size,
        },
      }),
      providesTags: ['ApplicationServices'],
    }),
    getApplicationServiceById: builder.query<ApplicationService, string>({
      query: (id) => `/application-services/${id}`,
      providesTags: (_result, _error, id) => [{ type: 'ApplicationServices', id }],
    }),
    createApplicationService: builder.mutation<ApplicationService, CreateApplicationServiceRequest>({
      query: (service) => ({
        url: '/application-services',
        method: 'POST',
        body: service,
      }),
      invalidatesTags: ['ApplicationServices'],
    }),
    updateApplicationService: builder.mutation<ApplicationService, {
      id: string;
      service: UpdateApplicationServiceRequest;
    }>({
      query: ({ id, service }) => ({
        url: `/application-services/${id}`,
        method: 'PUT',
        body: service,
      }),
      invalidatesTags: (_result, _error, { id }) => [
        { type: 'ApplicationServices', id },
        'ApplicationServices',
      ],
    }),
    deleteApplicationService: builder.mutation<void, string>({
      query: (id) => ({
        url: `/application-services/${id}`,
        method: 'DELETE',
      }),
      invalidatesTags: ['ApplicationServices'],
    }),
  }),
});

export const {
  useGetApplicationServicesQuery,
  useGetApplicationServiceByIdQuery,
  useCreateApplicationServiceMutation,
  useUpdateApplicationServiceMutation,
  useDeleteApplicationServiceMutation,
} = applicationServicesApi;
