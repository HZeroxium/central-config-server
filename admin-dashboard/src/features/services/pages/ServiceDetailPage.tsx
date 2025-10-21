import { Box, Card, CardContent, Typography } from '@mui/material'
import { useParams } from 'react-router-dom'
import { useListInstancesQuery } from '@features/services/api'
import Loading from '@components/common/Loading'
import ErrorFallback from '@components/common/ErrorFallback'
import InstanceListTable from '@features/services/components/InstanceListTable'

export default function ServiceDetailPage() {
  const { serviceName = '' } = useParams()
  const { data, isLoading, error, refetch } = useListInstancesQuery(serviceName)

  if (!serviceName) return <ErrorFallback message="Missing service name" />
  if (isLoading) return <Loading />
  if (error) return <ErrorFallback message={(error as any)?.error || 'Failed to load instances'} onRetry={refetch} />

  return (
    <Box sx={{ p: { xs: 2, sm: 3, md: 4 } }}>
      <Typography variant="h4" sx={{ mb: { xs: 3, sm: 4 }, fontWeight: 700 }}>
        Service Details
      </Typography>
      
          <Box sx={{ display: 'grid', gap: { xs: 2, sm: 3 } }}>
            <Card sx={{ border: 1, borderColor: 'divider', boxShadow: 1 }}>
              <CardContent sx={{ p: 3 }}>
                <Typography variant="h5" sx={{ mb: 2, fontWeight: 600, color: 'primary.main' }}>
                  {serviceName}
                </Typography>
                <Typography variant="body1" sx={{ color: 'text.secondary', display: 'flex', alignItems: 'center', gap: 1 }}>
                  <strong>Total Instances:</strong> {data?.length || 0}
                </Typography>
              </CardContent>
            </Card>
            
            {/* <ServiceNavigation instances={data || []} serviceName={serviceName} /> */}
            
            <InstanceListTable instances={data || []} />
          </Box>
    </Box>
  )
}


