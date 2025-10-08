import { Card, CardContent, Typography, Box, Chip } from '@mui/material'
import { Info, ArrowDownward } from '@mui/icons-material'
import type { ConfigEnvironmentResponse } from '@lib/api/types'

interface ConfigMergeInfoProps {
  env: ConfigEnvironmentResponse
}

export default function ConfigMergeInfo({ env }: ConfigMergeInfoProps) {
  if (!env.propertySources || env.propertySources.length === 0) {
    return null
  }

  // Calculate merge precedence
  const sources = env.propertySources.map((ps, index) => ({
    name: ps.name.split('/').pop() || ps.name,
    priority: env.propertySources!.length - index, // Highest priority = index 0
    properties: Object.keys(ps.source || {}).length,
    isGlobal: ps.name.includes('/application.yml') && !ps.name.includes('/application-'),
    isProfile: ps.name.includes('/application-') && !ps.name.includes('/application.yml'),
    isService: !ps.name.includes('/application.yml') && !ps.name.includes('/application-')
  }))

  return (
    <Card sx={{ border: 1, borderColor: 'divider', boxShadow: 1 }}>
      <CardContent sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
          <Info color="info" />
          <Typography variant="h6" sx={{ fontWeight: 600, color: 'text.primary' }}>
            Merge Precedence
          </Typography>
        </Box>

        <Typography variant="body2" sx={{ color: 'text.secondary', mb: 3 }}>
          Properties are merged with the following precedence (highest to lowest priority):
        </Typography>

        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          {sources.map((source, index) => (
            <Box key={source.name} sx={{ position: 'relative' }}>
              <Box sx={{ 
                display: 'flex', 
                alignItems: 'center', 
                justifyContent: 'space-between',
                p: 2,
                border: 1,
                borderColor: 'divider',
                borderRadius: 2,
                bgcolor: index === 0 ? 'primary.50' : 'background.paper',
                transition: 'all 0.2s'
              }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                  <Chip 
                    label={`#${source.priority}`} 
                    size="small" 
                    color={index === 0 ? 'primary' : 'default'}
                    sx={{ fontWeight: 600, minWidth: 40 }}
                  />
                  <Box>
                    <Typography variant="body1" sx={{ fontWeight: 500, color: 'text.primary' }}>
                      {source.name}
                    </Typography>
                    <Box sx={{ display: 'flex', gap: 1, mt: 0.5 }}>
                      {source.isGlobal && (
                        <Chip label="Global" size="small" variant="outlined" color="info" />
                      )}
                      {source.isProfile && (
                        <Chip label="Profile" size="small" variant="outlined" color="warning" />
                      )}
                      {source.isService && (
                        <Chip label="Service" size="small" variant="outlined" color="success" />
                      )}
                      <Chip 
                        label={`${source.properties} properties`} 
                        size="small" 
                        variant="outlined" 
                        color="default" 
                      />
                    </Box>
                  </Box>
                </Box>
                {index === 0 && (
                  <Chip 
                    label="Highest Priority" 
                    size="small" 
                    color="primary" 
                    sx={{ fontWeight: 600 }}
                  />
                )}
              </Box>
              
              {index < sources.length - 1 && (
                <Box sx={{ display: 'flex', justifyContent: 'center', mt: 1 }}>
                  <ArrowDownward sx={{ color: 'text.secondary', fontSize: 20 }} />
                </Box>
              )}
            </Box>
          ))}
        </Box>

        <Box sx={{ mt: 3, p: 2, bgcolor: 'grey.50', borderRadius: 2 }}>
          <Typography variant="body2" sx={{ color: 'text.secondary', mb: 1, fontWeight: 500 }}>
            Merge Logic:
          </Typography>
          <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block' }}>
            • Properties from higher priority sources override lower priority ones<br/>
            • Missing properties are inherited from lower priority sources<br/>
            • Final configuration combines all non-overridden properties
          </Typography>
        </Box>
      </CardContent>
    </Card>
  )
}
