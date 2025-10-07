export type ApiResponse<T> = {
  status: string
  message?: string
  data: T
  timestamp?: number
  traceId?: string
  errors?: string[]
}

export type ConfigEnvironmentResponse = {
  name: string
  profiles: string[]
  label?: string
  version?: string
  state?: string
  propertySources: { name: string; source: Record<string, unknown> }[]
}

export type ConsulServicesMap = {
  services: Record<string, string[]>
}

export type ServiceInstanceSummary = {
  serviceName: string
  instanceId: string
  host?: string
  port?: number
  status?: string
  scheme?: string
  uri?: string
  healthy?: boolean
  lastSeenAt?: number
}


