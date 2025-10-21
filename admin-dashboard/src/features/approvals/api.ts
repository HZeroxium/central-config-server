import { apiSlice } from '@lib/api/apiSlice';
import type {
  ApprovalRequest,
  CreateApprovalRequestRequest,
  DecisionRequest,
  ApprovalRequestQueryFilter,
} from './types';

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const approvalsApi = apiSlice.injectEndpoints({
  endpoints: (builder) => ({
    getApprovalRequests: builder.query<PageResponse<ApprovalRequest>, ApprovalRequestQueryFilter>({
      query: (filters) => ({
        url: '/approval-requests',
        params: filters,
      }),
      providesTags: (result) =>
        result
          ? [
              ...result.content.map(({ id }) => ({ type: 'ApprovalRequests' as const, id })),
              { type: 'ApprovalRequests', id: 'LIST' },
            ]
          : [{ type: 'ApprovalRequests', id: 'LIST' }],
    }),
    getApprovalRequestById: builder.query<ApprovalRequest, string>({
      query: (id) => `/approval-requests/${id}`,
      providesTags: (_result, _error, id) => [{ type: 'ApprovalRequests', id }],
    }),
    createApprovalRequest: builder.mutation<ApprovalRequest, CreateApprovalRequestRequest>({
      query: (request) => ({
        url: '/approval-requests',
        method: 'POST',
        body: request,
      }),
      invalidatesTags: [{ type: 'ApprovalRequests', id: 'LIST' }],
    }),
    submitDecision: builder.mutation<ApprovalRequest, { id: string; decision: DecisionRequest }>({
      query: ({ id, decision }) => ({
        url: `/approval-requests/${id}/decide`,
        method: 'POST',
        body: decision,
      }),
      invalidatesTags: (_result, _error, { id }) => [
        { type: 'ApprovalRequests', id },
        { type: 'ApprovalRequests', id: 'LIST' },
      ],
    }),
    cancelApprovalRequest: builder.mutation<void, string>({
      query: (id) => ({
        url: `/approval-requests/${id}/cancel`,
        method: 'POST',
      }),
      invalidatesTags: (_result, _error, id) => [
        { type: 'ApprovalRequests', id },
        { type: 'ApprovalRequests', id: 'LIST' },
      ],
    }),
  }),
});

export const {
  useGetApprovalRequestsQuery,
  useGetApprovalRequestByIdQuery,
  useCreateApprovalRequestMutation,
  useSubmitDecisionMutation,
  useCancelApprovalRequestMutation,
} = approvalsApi;
