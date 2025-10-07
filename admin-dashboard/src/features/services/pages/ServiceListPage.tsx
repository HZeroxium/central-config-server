import { Box, Typography, Button, TextField, InputAdornment } from '@mui/material'
import { Search, Refresh, Storage } from '@mui/icons-material'
import { useNavigate } from 'react-router-dom'
import { useListServicesQuery } from '@features/services/api'
import Loading from '@components/common/Loading'
import ErrorFallback from '@components/common/ErrorFallback'
import ServiceListTable from '@features/services/components/ServiceListTable'
import EmptyState from '@components/common/EmptyState'
import { useState, useMemo } from 'react'

export default function ServiceListPage() {
  const { data, isLoading, error, refetch } = useListServicesQuery()
  const navigate = useNavigate()
  const [searchTerm, setSearchTerm] = useState('')

  const filteredServices = useMemo(() => {
    if (!data) return {}
    
    if (!searchTerm) return data
    
    const filtered = Object.keys(data).reduce((acc, key) => {
      if (key.toLowerCase().includes(searchTerm.toLowerCase())) {
        acc[key] = data[key]
      }
      return acc
    }, {} as typeof data)
    
    return filtered
  }, [data, searchTerm])

  if (isLoading) return <Loading />
  if (error) return <ErrorFallback message={(error as any)?.error || 'Failed to load services'} onRetry={refetch} />

  const hasServices = Object.keys(filteredServices).length > 0

  return (
    <Box sx={{ p: { xs: 2, sm: 3, md: 4 } }}>
      <Box sx={{ 
        display: 'flex', 
        flexDirection: { xs: 'column', sm: 'row' },
        alignItems: { xs: 'stretch', sm: 'center' },
        justifyContent: 'space-between',
        mb: { xs: 3, sm: 4 },
        gap: 2
      }}>
        <Typography variant="h4" sx={{ fontWeight: 700 }}>
          Services
        </Typography>
        
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
          
          <Button
            variant="outlined"
            startIcon={<Refresh />}
            onClick={() => refetch()}
            className="btn-secondary"
          >
            Refresh
          </Button>
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


