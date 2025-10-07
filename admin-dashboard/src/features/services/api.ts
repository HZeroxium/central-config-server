import { baseApi, unwrap } from '@lib/api/baseApi'
import type { ServiceInstanceSummary } from '@lib/api/types'

export type ServicesMap = Record<string, string[]>

export const servicesApi = baseApi.injectEndpoints({
  endpoints: (build) => ({
    listServices: build.query<ServicesMap, void>({
      query: () => ({ url: '/registry/services' }),
      transformResponse: (resp: any) => {
        // backend returns ApiResponse<{ services: Record<string,string[]> }> or raw map
        const data = unwrap<{ services: ServicesMap } | ServicesMap>(resp)
        return 'services' in data ? (data as any).services : (data as ServicesMap)
      },
      providesTags: ['Services'],
    }),
    listInstances: build.query<ServiceInstanceSummary[], string>({
      query: (serviceName) => ({ url: `/registry/services/${encodeURIComponent(serviceName)}/instances` }),
      transformResponse: (resp: any) => unwrap<ServiceInstanceSummary[]>(resp),
      providesTags: (_r, _e, name) => [{ type: 'Services', id: name } as any],
    }),
    listAllInstances: build.query<Record<string, number>, void>({
      query: () => ({ url: '/registry/services' }),
      transformResponse: async (resp: any) => {
        const data = unwrap<{ services: ServicesMap } | ServicesMap>(resp)
        const services = 'services' in data ? (data as any).services : (data as ServicesMap)
        const serviceCounts: Record<string, number> = {}
        
        // Fetch instances for each service
        for (const serviceName of Object.keys(services)) {
          try {
            const instancesResp = await fetch(`${import.meta.env.VITE_API_BASE_URL || '/api'}/registry/services/${encodeURIComponent(serviceName)}/instances`)
            const instancesData = await instancesResp.json()
            const instances = unwrap<ServiceInstanceSummary[]>(instancesData)
            serviceCounts[serviceName] = instances.length
          } catch (error) {
            console.warn(`Failed to fetch instances for ${serviceName}:`, error)
            serviceCounts[serviceName] = 0
          }
        }
        return serviceCounts
      },
      providesTags: ['Services'],
    }),
  }),
  overrideExisting: false,
})

export const { useListServicesQuery, useListInstancesQuery, useListAllInstancesQuery } = servicesApi


