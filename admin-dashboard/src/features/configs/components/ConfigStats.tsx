import { Card, CardContent, Typography, Box, Chip, Grid } from '@mui/material'
import { Settings, Storage, Code, Info } from '@mui/icons-material'
import { useMemo } from 'react'
import type { ConfigEnvironmentResponse } from '@lib/api/types'

interface ConfigStatsProps {
  env: ConfigEnvironmentResponse
}

export default function ConfigStats({ env }: ConfigStatsProps) {
  const stats = useMemo(() => {
    if (!env.propertySources) {
      return {
        totalSources: 0,
        totalProperties: 0,
        globalSources: 0,
        profileSources: 0,
        serviceSources: 0,
        uniqueProperties: 0
      }
    }

    const sources = env.propertySources
    const totalSources = sources.length
    const totalProperties = sources.reduce((acc, source) => acc + Object.keys(source.source || {}).length, 0)
    
    const globalSources = sources.filter(s => s.name.includes('/application.yml') && !s.name.includes('/application-')).length
    const profileSources = sources.filter(s => s.name.includes('/application-') && !s.name.includes('/application.yml')).length
    const serviceSources = sources.filter(s => !s.name.includes('/application.yml') && !s.name.includes('/application-')).length

    // Tính số properties unique (sau khi merge)
    const uniqueProperties = Object.keys(
      sources.reduce((acc, source) => ({ ...acc, ...source.source }), {})
    ).length

    return {
      totalSources,
      totalProperties,
      globalSources,
      profileSources,
      serviceSources,
      uniqueProperties
    }
  }, [env.propertySources])

  return (
    <Card sx={{ border: 1, borderColor: 'divider', boxShadow: 1 }}>
      <CardContent sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
          <Settings color="primary" />
          <Typography variant="h6" sx={{ fontWeight: 600, color: 'text.primary' }}>
            Configuration Statistics
          </Typography>
        </Box>

        <Grid container spacing={2}>
          <Grid size={{ xs: 12, sm: 6, md: 4 }}>
            <Box sx={{ 
              p: 2, 
              border: 1, 
              borderColor: 'divider', 
              borderRadius: 2,
              textAlign: 'center',
              bgcolor: 'primary.50'
            }}>
              <Storage color="primary" sx={{ fontSize: 32, mb: 1 }} />
              <Typography variant="h4" sx={{ fontWeight: 700, color: 'primary.main', mb: 0.5 }}>
                {stats.totalSources}
              </Typography>
              <Typography variant="body2" sx={{ color: 'text.secondary', fontWeight: 500 }}>
                Property Sources
              </Typography>
            </Box>
          </Grid>

          <Grid size={{ xs: 12, sm: 6, md: 4 }}>
            <Box sx={{ 
              p: 2, 
              border: 1, 
              borderColor: 'divider', 
              borderRadius: 2,
              textAlign: 'center',
              bgcolor: 'success.50'
            }}>
              <Code color="success" sx={{ fontSize: 32, mb: 1 }} />
              <Typography variant="h4" sx={{ fontWeight: 700, color: 'success.main', mb: 0.5 }}>
                {stats.totalProperties}
              </Typography>
              <Typography variant="body2" sx={{ color: 'text.secondary', fontWeight: 500 }}>
                Total Properties
              </Typography>
            </Box>
          </Grid>

          <Grid size={{ xs: 12, sm: 6, md: 4 }}>
            <Box sx={{ 
              p: 2, 
              border: 1, 
              borderColor: 'divider', 
              borderRadius: 2,
              textAlign: 'center',
              bgcolor: 'info.50'
            }}>
              <Info color="info" sx={{ fontSize: 32, mb: 1 }} />
              <Typography variant="h4" sx={{ fontWeight: 700, color: 'info.main', mb: 0.5 }}>
                {stats.uniqueProperties}
              </Typography>
              <Typography variant="body2" sx={{ color: 'text.secondary', fontWeight: 500 }}>
                Unique Properties
              </Typography>
            </Box>
          </Grid>
        </Grid>

        <Box sx={{ mt: 3 }}>
          <Typography variant="body2" sx={{ color: 'text.secondary', mb: 2, fontWeight: 500 }}>
            Source Distribution:
          </Typography>
          <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
            <Chip 
              icon={<Storage />}
              label={`${stats.globalSources} Global`} 
              color="primary" 
              variant="outlined"
            />
            <Chip 
              icon={<Code />}
              label={`${stats.profileSources} Profile`} 
              color="warning" 
              variant="outlined"
            />
            <Chip 
              icon={<Settings />}
              label={`${stats.serviceSources} Service`} 
              color="success" 
              variant="outlined"
            />
          </Box>
        </Box>
      </CardContent>
    </Card>
  )
}
