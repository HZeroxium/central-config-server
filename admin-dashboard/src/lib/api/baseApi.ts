import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react'
import type { ApiResponse } from '@lib/api/types'

const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api'

export const baseApi = createApi({
  reducerPath: 'api',
  baseQuery: fetchBaseQuery({ baseUrl }),
  tagTypes: ['Services', 'Configs'],
  endpoints: () => ({}),
  keepUnusedDataFor: 60,
  refetchOnFocus: false,
  refetchOnReconnect: true,
})

export function unwrap<T>(resp: ApiResponse<T> | T): T {
  if (resp && typeof resp === 'object' && 'status' in (resp as any) && 'data' in (resp as any)) {
    return (resp as ApiResponse<T>).data
  }
  return resp as T
}


