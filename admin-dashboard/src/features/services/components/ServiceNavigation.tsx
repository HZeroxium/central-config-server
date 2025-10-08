import { Card, CardContent, Typography, Box, Chip, IconButton, Tooltip } from '@mui/material'
import { OpenInNew, Link as LinkIcon, Code, Storage } from '@mui/icons-material'
import { useMemo } from 'react'
import type { ServiceInstanceSummary } from '@lib/api/types'

interface ServiceNavigationProps {
  instances: ServiceInstanceSummary[]
  serviceName: string
}

interface NavigationLink {
  type: 'service' | 'endpoint'
  label: string
  url: string
  instances?: number
  status?: string
  parent?: string
}

export default function ServiceNavigation({ instances, serviceName }: ServiceNavigationProps) {
  const externalLinks = useMemo((): NavigationLink[] => {
    const links: NavigationLink[] = []
    
    // Group instances by host for easier navigation
    const instancesByHost = instances.reduce((acc, instance) => {
      if (instance.host && instance.port) {
        const key = `${instance.host}:${instance.port}`
        if (!acc[key]) {
          acc[key] = []
        }
        acc[key].push(instance)
      }
      return acc
    }, {} as Record<string, ServiceInstanceSummary[]>)

    // Create navigation links
    Object.entries(instancesByHost).forEach(([hostPort, hostInstances]) => {
      const [host, port] = hostPort.split(':')
      const protocol = hostInstances[0]?.scheme || 'http'
      const baseUrl = `${protocol}://${host}:${port}`
      
      links.push({
        type: 'service',
        label: `${serviceName} on ${host}:${port}`,
        url: baseUrl,
        instances: hostInstances.length,
        status: hostInstances[0]?.status || 'UNKNOWN'
      })

      // Add common endpoints
      const commonEndpoints = [
        { path: '/actuator/health', label: 'Health Check' },
        { path: '/actuator/info', label: 'Service Info' },
        { path: '/actuator', label: 'Actuator' }
      ]

      commonEndpoints.forEach(endpoint => {
        links.push({
          type: 'endpoint',
          label: endpoint.label,
          url: `${baseUrl}${endpoint.path}`,
          parent: `${host}:${port}`
        })
      })
    })

    return links
  }, [instances, serviceName])

  if (externalLinks.length === 0) {
    return null
  }

  return (
    <Card sx={{ border: 1, borderColor: 'divider', boxShadow: 1 }}>
      <CardContent sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
          <LinkIcon color="primary" />
          <Typography variant="h6" sx={{ fontWeight: 600, color: 'text.primary' }}>
            Service Navigation
          </Typography>
        </Box>

        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          {externalLinks.map((link, index) => (
            <Box
              key={index}
              sx={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                p: 2,
                border: 1,
                borderColor: 'divider',
                borderRadius: 2,
                bgcolor: 'background.paper',
                transition: 'all 0.2s',
                '&:hover': {
                  bgcolor: 'action.hover',
                  borderColor: 'primary.main'
                }
              }}
            >
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, flex: 1, minWidth: 0 }}>
                {link.type === 'service' ? (
                  <Storage color="primary" />
                ) : (
                  <Code color="secondary" />
                )}
                <Box sx={{ flex: 1, minWidth: 0 }}>
                  <Typography variant="body1" sx={{ fontWeight: 500, color: 'text.primary' }}>
                    {link.label}
                  </Typography>
                  {link.parent && (
                    <Typography variant="caption" sx={{ color: 'text.secondary' }}>
                      {link.parent}
                    </Typography>
                  )}
                  <Typography 
                    variant="caption" 
                    sx={{ 
                      color: 'text.secondary',
                      fontFamily: 'monospace',
                      display: 'block',
                      wordBreak: 'break-all'
                    }}
                  >
                    {link.url}
                  </Typography>
                </Box>
              </Box>

              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                {link.instances && (
                  <Chip 
                    label={`${link.instances} instance${link.instances > 1 ? 's' : ''}`}
                    size="small"
                    color="primary"
                    variant="outlined"
                  />
                )}
                {link.status && (
                  <Chip 
                    label={link.status}
                    size="small"
                    color={link.status === 'HEALTHY' ? 'success' : 'default'}
                  />
                )}
                <Tooltip title="Open in new tab">
                  <IconButton
                    size="small"
                    component="a"
                    href={link.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    sx={{ color: 'text.secondary' }}
                  >
                    <OpenInNew fontSize="small" />
                  </IconButton>
                </Tooltip>
              </Box>
            </Box>
          ))}
        </Box>

        <Box sx={{ mt: 2, p: 2, bgcolor: 'grey.50', borderRadius: 2 }}>
          <Typography variant="caption" sx={{ color: 'text.secondary' }}>
            💡 Tip: Click the external link icon to open service endpoints in new tabs. 
            Use these links to monitor service health and access actuator endpoints.
          </Typography>
        </Box>
      </CardContent>
    </Card>
  )
}
