import React from 'react';
import { useParams } from 'react-router-dom';
import { Box, Card, CardContent, Typography, Chip, Button, Grid, Alert } from '@mui/material';
import { Edit as EditIcon, Delete as DeleteIcon, ArrowBack as ArrowBackIcon } from '@mui/icons-material';
import { PageHeader } from '@components/common/PageHeader';
import { useFindByIdServiceShare, useRevokeServiceShare } from '@lib/api/hooks';
import { useErrorHandler } from '../../../hooks/useErrorHandler';
import { usePermissions } from '@features/auth/hooks/usePermissions';
import { PERMISSION_LABELS, ENVIRONMENT_LABELS, ENVIRONMENT_COLORS } from '../types';
import { format } from 'date-fns';

export const ServiceShareDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const { isSysAdmin, permissions } = usePermissions();
  const { handleError, showSuccess } = useErrorHandler();

  const {
    data: shareResponse,
    isLoading,
    error,
    refetch,
  } = useFindByIdServiceShare(id!, {
    query: {
      enabled: !!id,
    },
  });

  const revokeShareMutation = useRevokeServiceShare();

  const share = shareResponse as any; // TODO: Fix API type generation

  const handleRevoke = async () => {
    if (!share || !id) return;
    
    try {
      await revokeShareMutation.mutateAsync({ id });
      showSuccess('Service share revoked successfully');
      // Navigate back to list
      window.history.back();
    } catch (error) {
      handleError(error, 'Failed to revoke service share');
    }
  };

  const canManageShares = isSysAdmin;

  if (isLoading) {
    return (
      <Box>
        <PageHeader title="Loading..." />
        <Box sx={{ p: 3 }}>Loading service share details...</Box>
      </Box>
    );
  }

  if (error || !share) {
    return (
      <Box>
        <PageHeader title="Service Share Not Found" />
        <Alert severity="error" sx={{ m: 3 }}>
          The requested service share could not be found or you don't have permission to view it.
        </Alert>
      </Box>
    );
  }

  const isExpired = share.expiresAt ? new Date(share.expiresAt) < new Date() : false;

  return (
    <Box>
      <PageHeader
        title={`Service Share: ${share.serviceName}`}
        subtitle={`Access granted to ${share.grantedToName}`}
        actions={
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Button
              variant="outlined"
              startIcon={<ArrowBackIcon />}
              onClick={() => window.history.back()}
            >
              Back
            </Button>
            {canManageShares && (
              <>
                <Button
                  variant="outlined"
                  startIcon={<EditIcon />}
                  onClick={() => {
                    // Navigate to edit form
                    console.log('Edit share:', share.id);
                  }}
                >
                  Edit
                </Button>
                <Button
                  variant="contained"
                  color="error"
                  startIcon={<DeleteIcon />}
                  onClick={handleRevoke}
                  disabled={revokeShareMutation.isPending}
                >
                  Revoke Access
                </Button>
              </>
            )}
          </Box>
        }
      />

      <Grid container spacing={3} sx={{ p: 3 }}>
        {/* Basic Information */}
        <Grid size={{ xs: 12, md: 6 }}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Basic Information
              </Typography>
              
              <Box sx={{ mb: 2 }}>
                <Typography variant="body2" color="text.secondary">
                  Service
                </Typography>
                <Typography variant="body1" fontWeight="medium">
                  {share.serviceName}
                </Typography>
              </Box>

              <Box sx={{ mb: 2 }}>
                <Typography variant="body2" color="text.secondary">
                  Granted To
                </Typography>
                <Typography variant="body1" fontWeight="medium">
                  {share.grantedToName} ({share.grantedTo})
                </Typography>
              </Box>

              <Box sx={{ mb: 2 }}>
                <Typography variant="body2" color="text.secondary">
                  Status
                </Typography>
                <Chip
                  label={share.status}
                  color={share.status === 'ACTIVE' ? 'success' : share.status === 'EXPIRED' ? 'warning' : 'error'}
                  size="small"
                />
              </Box>

              <Box sx={{ mb: 2 }}>
                <Typography variant="body2" color="text.secondary">
                  Created
                </Typography>
                <Typography variant="body1">
                  {format(new Date(share.createdAt), 'PPP')}
                </Typography>
              </Box>

              <Box sx={{ mb: 2 }}>
                <Typography variant="body2" color="text.secondary">
                  Created By
                </Typography>
                <Typography variant="body1">
                  {share.createdBy}
                </Typography>
              </Box>

              {share.expiresAt && (
                <Box sx={{ mb: 2 }}>
                  <Typography variant="body2" color="text.secondary">
                    Expires
                  </Typography>
                  <Typography 
                    variant="body1" 
                    color={isExpired ? 'error' : 'text.primary'}
                    fontWeight={isExpired ? 'medium' : 'normal'}
                  >
                    {format(new Date(share.expiresAt), 'PPP')}
                    {isExpired && ' (Expired)'}
                  </Typography>
                </Box>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* Permissions */}
        <Grid size={{ xs: 12, md: 6 }}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Permissions
              </Typography>
              
              <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
                {share.permissions.map((permission) => (
                  <Chip
                    key={permission}
                    label={PERMISSION_LABELS[permission as keyof typeof PERMISSION_LABELS] || permission}
                    size="small"
                    variant="outlined"
                    sx={{ mb: 0.5 }}
                  />
                ))}
              </Box>

              {share.permissions.length === 0 && (
                <Typography variant="body2" color="text.secondary">
                  No permissions granted
                </Typography>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* Environments */}
        <Grid size={{ xs: 12 }}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Environments
              </Typography>
              
              <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
                {share.environments.map((environment) => (
                  <Chip
                    key={environment}
                    label={ENVIRONMENT_LABELS[environment as keyof typeof ENVIRONMENT_LABELS] || environment}
                    size="small"
                    sx={{
                      backgroundColor: ENVIRONMENT_COLORS[environment as keyof typeof ENVIRONMENT_COLORS] || 'grey.300',
                      color: 'white',
                      fontWeight: 'medium',
                    }}
                  />
                ))}
              </Box>

              {share.environments.length === 0 && (
                <Typography variant="body2" color="text.secondary">
                  No environments specified
                </Typography>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default ServiceShareDetailPage;