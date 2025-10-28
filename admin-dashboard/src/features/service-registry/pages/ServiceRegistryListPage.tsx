import { Box, Card, CardContent, Typography, Alert, TextField, InputAdornment } from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import { useListServiceRegistryServices } from '@lib/api/generated/service-registry/service-registry';
import PageHeader from '@components/common/PageHeader';
import Loading from '@components/common/Loading';
import ConsulServiceTable from '../components/ConsulServiceTable';
import { getErrorMessage } from '@lib/api/errorHandler';
import { useState } from 'react';

export default function ServiceRegistryListPage() {
  const [searchTerm, setSearchTerm] = useState('');
  
  const { data, isLoading, error } = useListServiceRegistryServices({
    query: {
      staleTime: 30_000, // 30 seconds
      refetchInterval: 30000, // Refresh every 30 seconds
      refetchIntervalInBackground: false,
    },
  });

  // Filter services by search term
  const filteredServices = data?.services 
    ? Object.entries(data.services).filter(([serviceName]) =>
        serviceName.toLowerCase().includes(searchTerm.toLowerCase())
      ).reduce((acc, [key, value]) => ({ ...acc, [key]: value }), {})
    : {};

  const serviceCount = Object.keys(filteredServices).length;
  const totalServiceCount = Object.keys(data?.services || {}).length;

  return (
    <Box>
      <PageHeader
        title="Service Registry (Consul)"
        subtitle="View services registered in Consul service discovery"
      />

      <Card>
        <CardContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              Failed to load services from Consul: {getErrorMessage(error)}
            </Alert>
          )}

          {!error && (
            <>
              <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography variant="body2" color="text.secondary">
                  {searchTerm ? `Showing ${serviceCount} of ${totalServiceCount} services` : `Total Services: ${totalServiceCount}`}
                </Typography>
                <TextField
                  placeholder="Search services..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  size="small"
                  sx={{ minWidth: 300 }}
                  InputProps={{
                    startAdornment: (
                      <InputAdornment position="start">
                        <SearchIcon />
                      </InputAdornment>
                    ),
                  }}
                />
              </Box>

              {isLoading && <Loading />}

              {!isLoading && data && (
                <ConsulServiceTable 
                  servicesData={{ services: filteredServices }} 
                  loading={isLoading} 
                />
              )}
            </>
          )}
        </CardContent>
      </Card>
    </Box>
  );
}

