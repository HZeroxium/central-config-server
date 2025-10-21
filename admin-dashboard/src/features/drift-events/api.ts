import { apiSlice } from '@lib/api/apiSlice';
import type {
  DriftEvent,
  UpdateDriftEventRequest,
  DriftEventQueryFilter,
} from './types';

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const driftEventsApi = apiSlice.injectEndpoints({
  endpoints: (builder) => ({
    getDriftEvents: builder.query<PageResponse<DriftEvent>, DriftEventQueryFilter>({
      query: (filters) => ({
        url: '/drift-events',
        params: filters,
      }),
      providesTags: (result) =>
        result
          ? [
              ...result.content.map(({ id }) => ({ type: 'DriftEvents' as const, id })),
              { type: 'DriftEvents', id: 'LIST' },
            ]
          : [{ type: 'DriftEvents', id: 'LIST' }],
    }),
    getDriftEventById: builder.query<DriftEvent, string>({
      query: (id) => `/drift-events/${id}`,
      providesTags: (_result, _error, id) => [{ type: 'DriftEvents', id }],
    }),
    updateDriftEvent: builder.mutation<DriftEvent, { id: string; updates: UpdateDriftEventRequest }>({
      query: ({ id, updates }) => ({
        url: `/drift-events/${id}`,
        method: 'PUT',
        body: updates,
      }),
      invalidatesTags: (_result, _error, { id }) => [
        { type: 'DriftEvents', id },
        { type: 'DriftEvents', id: 'LIST' },
      ],
    }),
  }),
});

export const {
  useGetDriftEventsQuery,
  useGetDriftEventByIdQuery,
  useUpdateDriftEventMutation,
} = driftEventsApi;
