import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Box, Button, Typography, Alert, Grid, Chip, Divider } from '@mui/material';
import { ArrowBack as BackIcon } from '@mui/icons-material';
import { PageHeader } from '@components/common/PageHeader';
import { SkeletonLoader } from '@components/common/SkeletonLoader';
import { DetailCard } from '@components/common/DetailCard';
import { PermissionChips } from '../components/PermissionChips';
import { useGetServiceShareByIdQuery } from '../api';

const ServiceShareDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  
  const { data: share, isLoading, error } = useGetServiceShareByIdQuery(id!, {
    skip: !id,
  });

  const handleBack = () => {
    navigate('/service-shares');
  };

  if (isLoading) {
    return <SkeletonLoader variant="page" />;
  }

  if (error || !share) {
    return (
      <Box>
        <PageHeader
          title="Service Share Details"
          actions={
            <Button variant="outlined" startIcon={<BackIcon />} onClick={handleBack}>
              Back to Shares
            </Button>
          }
        />
        <Alert severity="error">
          Failed to load service share. {error ? 'Please try again.' : 'Share not found.'}
        </Alert>
      </Box>
    );
  }

  const formatDateTime = (dateTime: string) => {
    return new Date(dateTime).toLocaleString();
  };

  return (
    <Box>
      <PageHeader
        title={`Service Share: ${share.id.substring(0, 8)}...`}
        actions={
          <Button variant="outlined" startIcon={<BackIcon />} onClick={handleBack}>
            Back to Shares
          </Button>
        }
      />
      
      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 8 }}>
          <DetailCard title="Share Information">
            <Grid container spacing={3}>
              <Grid size={{ xs: 12, sm: 6 }}>
                <Box>
                  <Typography variant="subtitle2" color="text.secondary">
                    Service ID
                  </Typography>
                  <Typography variant="h6" fontWeight={500}>
                    {share.serviceId}
                  </Typography>
                </Box>
              </Grid>
              
              <Grid size={{ xs: 12, sm: 6 }}>
                <Box>
                  <Typography variant="subtitle2" color="text.secondary">
                    Grant To Type
                  </Typography>
                  <Chip 
                    label={share.grantToType} 
                    variant="outlined"
                    sx={{ textTransform: 'capitalize' }}
                  />
                </Box>
              </Grid>

              <Grid size={{ xs: 12, sm: 6 }}>
                <Box>
                  <Typography variant="subtitle2" color="text.secondary">
                    Grant To ID
                  </Typography>
                  <Typography variant="body1" fontWeight={500}>
                    {share.grantToId}
                  </Typography>
                </Box>
              </Grid>

              <Grid size={{ xs: 12, sm: 6 }}>
                <Box>
                  <Typography variant="subtitle2" color="text.secondary">
                    Status
                  </Typography>
                  <Chip 
                    label={share.isActive ? 'Active' : 'Inactive'} 
                    color={share.isActive ? 'success' : 'default'}
                    variant="outlined"
                  />
                </Box>
              </Grid>

              <Grid size={{ xs: 12 }}>
                <Box>
                  <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                    Permissions
                  </Typography>
                  <PermissionChips permissions={share.permissions} />
                </Box>
              </Grid>

              <Grid size={{ xs: 12 }}>
                <Box>
                  <Typography variant="subtitle2" color="text.secondary">
                    Environments
                  </Typography>
                  {share.environments && share.environments.length > 0 ? (
                    <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mt: 1 }}>
                      {share.environments.map((env) => (
                        <Chip key={env} label={env.toUpperCase()} variant="outlined" />
                      ))}
                    </Box>
                  ) : (
                    <Typography variant="body2" color="text.secondary">
                      All environments
                    </Typography>
                  )}
                </Box>
              </Grid>

              {share.expiresAt && (
                <Grid size={{ xs: 12, sm: 6 }}>
                  <Box>
                    <Typography variant="subtitle2" color="text.secondary">
                      Expires At
                    </Typography>
                    <Typography variant="body1">
                      {formatDateTime(share.expiresAt)}
                    </Typography>
                  </Box>
                </Grid>
              )}
            </Grid>
          </DetailCard>
        </Grid>

        <Grid size={{ xs: 12, md: 4 }}>
          <DetailCard title="Share History">
            <Box>
              <Typography variant="subtitle2" color="text.secondary">
                Granted By
              </Typography>
              <Typography variant="body1">
                {share.grantedBy}
              </Typography>
            </Box>
            
            <Divider sx={{ my: 2 }} />
            
            <Box>
              <Typography variant="subtitle2" color="text.secondary">
                Granted At
              </Typography>
              <Typography variant="body1">
                {formatDateTime(share.grantedAt)}
              </Typography>
            </Box>
            
            {share.revokedAt && (
              <>
                <Divider sx={{ my: 2 }} />
                <Box>
                  <Typography variant="subtitle2" color="text.secondary">
                    Revoked By
                  </Typography>
                  <Typography variant="body1">
                    {share.revokedBy}
                  </Typography>
                </Box>
                
                <Box sx={{ mt: 2 }}>
                  <Typography variant="subtitle2" color="text.secondary">
                    Revoked At
                  </Typography>
                  <Typography variant="body1">
                    {formatDateTime(share.revokedAt)}
                  </Typography>
                </Box>
              </>
            )}
          </DetailCard>
        </Grid>
      </Grid>
    </Box>
  );
};

export default ServiceShareDetailPage;
