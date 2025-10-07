import { Box, Typography} from '@mui/material'
import Grid from '@mui/material/Grid'
import { useListServicesQuery, useListAllInstancesQuery } from '@features/services/api'
import StatCard from '@components/common/StatCard'
import { Storage, Settings, CheckCircle, Error } from '@mui/icons-material'
import { useMemo } from 'react'

export default function DashboardPage() {
  const { data: services, isLoading: servicesLoading } = useListServicesQuery()
  const { data: instanceCounts, isLoading: instancesLoading } = useListAllInstancesQuery()
  
  const stats = useMemo(() => {
    if (!services || !instanceCounts) return null
    
    const serviceCount = Object.keys(services).length
    const totalInstances = Object.values(instanceCounts).reduce((acc, count) => acc + count, 0)
    
    return {
      serviceCount,
      totalInstances,
      healthyServices: Math.floor(serviceCount * 0.85), // Mock data
      unhealthyServices: Math.floor(serviceCount * 0.15) // Mock data
    }
  }, [services, instanceCounts])

  if (servicesLoading || instancesLoading) {
    return (
      <Box sx={{ p: { xs: 2, sm: 3, md: 4 } }}>
        <Grid container spacing={{ xs: 2, sm: 3 }}>
          {Array.from({ length: 4 }).map((_, i) => (
            <Grid key={i} size={{ xs: 12, sm: 6, lg: 3 }}>
              <Box sx={{ 
                height: 140, 
                bgcolor: 'action.hover', 
                borderRadius: 2,
                animation: 'pulse 1.5s ease-in-out infinite',
                '@keyframes pulse': {
                  '0%, 100%': { opacity: 1 },
                  '50%': { opacity: 0.5 }
                }
              }} />
            </Grid>
          ))}
        </Grid>
      </Box>
    )
  }

  return (
    <Box sx={{ p: { xs: 2, sm: 3, md: 4 } }}>
      <Typography variant="h4" sx={{ mb: { xs: 3, sm: 4 }, fontWeight: 700 }}>
        Dashboard Overview
      </Typography>
      
      <Grid container spacing={{ xs: 2, sm: 3 }} sx={{ mb: { xs: 4, sm: 6 } }}>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <StatCard
            title="Total Services"
            value={stats?.serviceCount || 0}
            icon={<Storage />}
            color="primary"
            subtitle="Registered services"
          />
        </Grid>
        
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <StatCard
            title="Total Instances"
            value={stats?.totalInstances || 0}
            icon={<Settings />}
            color="info"
            subtitle="Service instances"
          />
        </Grid>
        
        {/* <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <StatCard
            title="Healthy Services"
            value={stats?.healthyServices || 0}
            icon={<CheckCircle />}
            color="success"
            subtitle="Running normally"
            trend={{ value: 5, isPositive: true }}
          />
        </Grid>
        
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <StatCard
            title="Unhealthy Services"
            value={stats?.unhealthyServices || 0}
            icon={<Error />}
            color="error"
            subtitle="Need attention"
          />
        </Grid> */}
      </Grid>
      
      <Grid container spacing={{ xs: 2, sm: 3 }}>
        <Grid size={{ xs: 12, lg: 8 }}>
          <Box sx={{ 
            p: { xs: 2, sm: 3 }, 
            border: 1, 
            borderColor: 'divider', 
            borderRadius: 2,
            bgcolor: 'background.paper',
            boxShadow: 1
          }}>
            <Typography variant="h6" sx={{ mb: 3, fontWeight: 600 }}>
              Recent Activity
            </Typography>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              {[
                { action: 'Service registered', service: 'user-service', time: '2 minutes ago' },
                { action: 'Config updated', service: 'sample-service', time: '5 minutes ago' },
                { action: 'Health check passed', service: 'config-server', time: '8 minutes ago' },
                { action: 'Instance started', service: 'user-watcher', time: '12 minutes ago' }
              ].map((activity, index) => (
                <Box key={index} sx={{ 
                  display: 'flex', 
                  alignItems: 'center', 
                  justifyContent: 'space-between',
                  py: 2,
                  borderBottom: index < 3 ? 1 : 0,
                  borderColor: 'divider'
                }}>
                  <Box>
                    <Typography variant="body2" sx={{ fontWeight: 500 }}>
                      {activity.action}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      {activity.service}
                    </Typography>
                  </Box>
                  <Typography variant="caption" color="text.secondary">
                    {activity.time}
                  </Typography>
                </Box>
              ))}
            </Box>
          </Box>
        </Grid>
        
        <Grid size={{ xs: 12, lg: 4 }}>
          <Box sx={{ 
            p: { xs: 2, sm: 3 }, 
            border: 1, 
            borderColor: 'divider', 
            borderRadius: 2,
            bgcolor: 'background.paper',
            boxShadow: 1
          }}>
            <Typography variant="h6" sx={{ mb: 3, fontWeight: 600 }}>
              Quick Actions
            </Typography>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <Box sx={{ 
                p: 2, 
                border: 1, 
                borderColor: 'divider', 
                borderRadius: 1,
                cursor: 'pointer',
                transition: 'all 0.2s',
                '&:hover': {
                  bgcolor: 'action.hover',
                  borderColor: 'primary.main'
                }
              }}>
                <Typography variant="body2" sx={{ fontWeight: 500, mb: 0.5 }}>
                  View All Services
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  Browse registered services
                </Typography>
              </Box>
              
              <Box sx={{ 
                p: 2, 
                border: 1, 
                borderColor: 'divider', 
                borderRadius: 1,
                cursor: 'pointer',
                transition: 'all 0.2s',
                '&:hover': {
                  bgcolor: 'action.hover',
                  borderColor: 'primary.main'
                }
              }}>
                <Typography variant="body2" sx={{ fontWeight: 500, mb: 0.5 }}>
                  Check Configs
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  View configuration details
                </Typography>
              </Box>
              
              <Box sx={{ 
                p: 2, 
                border: 1, 
                borderColor: 'divider', 
                borderRadius: 1,
                cursor: 'pointer',
                transition: 'all 0.2s',
                '&:hover': {
                  bgcolor: 'action.hover',
                  borderColor: 'primary.main'
                }
              }}>
                <Typography variant="body2" sx={{ fontWeight: 500, mb: 0.5 }}>
                  System Health
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  Monitor system status
                </Typography>
              </Box>
            </Box>
          </Box>
        </Grid>
      </Grid>
    </Box>
  )
}
