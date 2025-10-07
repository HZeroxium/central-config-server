import { baseApi, unwrap } from '@lib/api/baseApi'
import type { ConfigEnvironmentResponse } from '@lib/api/types'

export const configsApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    getEnvironment: build.query<ConfigEnvironmentResponse, { application: string; profile: string; label?: string }>(
      {
        query: ({ application, profile, label }) => ({
          url: `/config-server/environment/${encodeURIComponent(application)}/${encodeURIComponent(profile)}`,
          params: label ? { label } : undefined,
        }),
        transformResponse: (resp: any) => unwrap<ConfigEnvironmentResponse>(resp),
        providesTags: (_r, _e, arg) => [{ type: 'Configs', id: `${arg.application}:${arg.profile}` } as any],
      }
    ),
  }),
  overrideExisting: false,
})

export const { useGetEnvironmentQuery } = configsApi


