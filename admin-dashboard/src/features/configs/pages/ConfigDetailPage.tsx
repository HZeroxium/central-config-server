import { Box } from '@mui/material'
import { useSearchParams, useParams } from 'react-router-dom'
import { useGetEnvironmentQuery } from '@features/configs/api'
import Loading from '@components/common/Loading'
import ErrorFallback from '@components/common/ErrorFallback'
import ConfigDetailCard from '@features/configs/components/ConfigDetailCard'

export default function ConfigDetailPage() {
  const { application = '', profile = '' } = useParams()
  const [sp] = useSearchParams()
  const label = sp.get('label') || undefined
  const { data, isLoading, error, refetch } = useGetEnvironmentQuery({ application, profile, label })

  if (isLoading) return <Loading />
  if (error || !data) return <ErrorFallback message={(error as any)?.error || 'Failed to load config'} onRetry={refetch} />

  return (
    <Box sx={{ p: { xs: 2, sm: 3, md: 4 } }}>
      <ConfigDetailCard env={data} />
    </Box>
  )
}


