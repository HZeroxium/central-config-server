import { Box, Typography, Button, TextField, InputAdornment, Card, CardContent, Grid, Chip, CircularProgress } from '@mui/material'
import { Search, Refresh, Storage, HealthAndSafety as HealthIcon, Warning as WarningIcon, CheckCircle as CheckCircleIcon } from '@mui/icons-material'
import { useNavigate } from 'react-router-dom'
import { useGetServiceRegistryService } from '@lib/api/hooks'
import Loading from '@components/common/Loading'
import ErrorFallback from '@components/common/ErrorFallback'
import ServiceListTable from '@features/services/components/ServiceListTable'
import EmptyState from '@components/common/EmptyState'
import { PageHeader } from '@components/common/PageHeader'
import { useState, useMemo } from 'react'

export default function ServiceListPage() {
  const { data: servicesResponse, isLoading, error, refetch } = useGetServiceRegistryService('services')
  const navigate = useNavigate()
  const [searchTerm, setSearchTerm] = useState('')
  const [showHealthyOnly, setShowHealthyOnly] = useState(false)

  const servicesData = servicesResponse
  const services = servicesData || {}

  const filteredServices = useMemo(() => {
    if (!services) return {}
    
    let filtered = services
    
    if (searchTerm) {
      filtered = Object.keys(services).reduce((acc, key) => {
        if (key.toLowerCase().includes(searchTerm.toLowerCase())) {
          acc[key] = services[key]
        }
        return acc
      }, {} as typeof services)
    }

    if (showHealthyOnly) {
      filtered = Object.keys(filtered).reduce((acc, key) => {
        const service = filtered[key]
        if (service && Array.isArray(service)) {
          const hasHealthyInstance = service.some((instance: any) => 
            instance.status === 'HEALTHY' || instance.status === 'UP'
          )
          if (hasHealthyInstance) {
            acc[key] = service
          }
        }
        return acc
      }, {} as typeof filtered)
    }
    
    return filtered
  }, [services, searchTerm, showHealthyOnly])

  // Calculate statistics
  const stats = useMemo(() => {
    const totalServices = Object.keys(services).length
    const totalInstances = Object.values(services).reduce((sum, service) => {
      return sum + (Array.isArray(service) ? service.length : 0)
    }, 0)
    
    const healthyInstances = Object.values(services).reduce((sum, service) => {
      if (Array.isArray(service)) {
        return sum + service.filter((instance: any) => 
          instance.status === 'HEALTHY' || instance.status === 'UP'
        ).length
      }
      return sum
    }, 0)

    const unhealthyInstances = totalInstances - healthyInstances

    return {
      totalServices,
      totalInstances,
      healthyInstances,
      unhealthyInstances,
    }
  }, [services])

  if (isLoading) return <Loading />
  if (error) return <ErrorFallback message={(error as any)?.error || 'Failed to load services'} onRetry={refetch} />

  const hasServices = Object.keys(filteredServices).length > 0

  return (
    <Box sx={{ p: { xs: 2, sm: 3, md: 4 } }}>
      <PageHeader
        title="Service Registry"
        subtitle="Monitor and manage registered services"
        actions={
          <Button
            variant="outlined"
            startIcon={<Refresh />}
            onClick={() => refetch()}
            disabled={isLoading}
          >
            Refresh
          </Button>
        }
      />

      {/* Statistics Cards */}
      <Box sx={{ mb: 3 }}>
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, sm: 6, md: 3 }}>
            <Card>
              <CardContent sx={{ textAlign: 'center' }}>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 1, mb: 1 }}>
                  <Storage color="primary" />
                  <Typography variant="h6">{stats.totalServices}</Typography>
                </Box>
                <Typography variant="body2" color="text.secondary">
                  Total Services
                </Typography>
              </CardContent>
            </Card>
          </Grid>

          <Grid size={{ xs: 12, sm: 6, md: 3 }}>
            <Card>
              <CardContent sx={{ textAlign: 'center' }}>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 1, mb: 1 }}>
                  <CheckCircleIcon color="success" />
                  <Typography variant="h6">{stats.healthyInstances}</Typography>
                </Box>
                <Typography variant="body2" color="text.secondary">
                  Healthy Instances
                </Typography>
              </CardContent>
            </Card>
          </Grid>

          <Grid size={{ xs: 12, sm: 6, md: 3 }}>
            <Card>
              <CardContent sx={{ textAlign: 'center' }}>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 1, mb: 1 }}>
                  <WarningIcon color="warning" />
                  <Typography variant="h6">{stats.unhealthyInstances}</Typography>
                </Box>
                <Typography variant="body2" color="text.secondary">
                  Unhealthy Instances
                </Typography>
              </CardContent>
            </Card>
          </Grid>

          <Grid size={{ xs: 12, sm: 6, md: 3 }}>
            <Card>
              <CardContent sx={{ textAlign: 'center' }}>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 1, mb: 1 }}>
                  <HealthIcon color="info" />
                  <Typography variant="h6">{stats.totalInstances}</Typography>
                </Box>
                <Typography variant="body2" color="text.secondary">
                  Total Instances
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </Box>

      {/* Search and Filters */}
      <Box sx={{ 
        display: 'flex', 
        flexDirection: { xs: 'column', sm: 'row' },
        alignItems: { xs: 'stretch', sm: 'center' },
        justifyContent: 'space-between',
        mb: { xs: 3, sm: 4 },
        gap: 2
      }}>
        <Box sx={{ 
          display: 'flex', 
          flexDirection: { xs: 'column', sm: 'row' },
          gap: 2,
          alignItems: { xs: 'stretch', sm: 'center' }
        }}>
          <TextField
            placeholder="Search services..."
            size="small"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <Search sx={{ color: 'text.secondary' }} />
                </InputAdornment>
              ),
            }}
            sx={{ minWidth: { xs: '100%', sm: 300 } }}
          />
          
          <Chip
            label={showHealthyOnly ? "Show Healthy Only" : "Show All"}
            onClick={() => setShowHealthyOnly(!showHealthyOnly)}
            color={showHealthyOnly ? "success" : "default"}
            variant={showHealthyOnly ? "filled" : "outlined"}
            sx={{ cursor: 'pointer' }}
          />
        </Box>
      </Box>

      {!hasServices ? (
        <EmptyState
          icon={<Storage sx={{ fontSize: '4rem', color: 'text.disabled' }} />}
          title={searchTerm ? "No services found" : "No services registered"}
          description={searchTerm ? `No services match "${searchTerm}"` : "No services are currently registered in the system."}
          action={searchTerm ? {
            label: "Clear search",
            onClick: () => setSearchTerm('')
          } : undefined}
        />
      ) : (
        <ServiceListTable 
          services={filteredServices} 
          onSelect={(name) => navigate(`/services/${encodeURIComponent(name)}`)} 
        />
      )}
    </Box>
  )
}


