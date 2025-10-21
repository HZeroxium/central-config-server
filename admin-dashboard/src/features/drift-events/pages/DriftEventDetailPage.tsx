import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Box, Button, Typography, Alert, Grid, Chip, Divider } from '@mui/material';
import { ArrowBack as BackIcon } from '@mui/icons-material';
import { PageHeader } from '@components/common/PageHeader';
import { SkeletonLoader } from '@components/common/SkeletonLoader';
import { DetailCard } from '@components/common/DetailCard';
import { DriftSeverityChip } from '../components/DriftSeverityChip';
import { ChipStatus } from '@components/common/ChipStatus';
import { useGetDriftEventByIdQuery } from '../api';

export const DriftEventDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  
  const { data: event, isLoading, error } = useGetDriftEventByIdQuery(id!, {
    skip: !id,
  });

  const handleBack = () => {
    navigate('/drift-events');
  };

  if (isLoading) {
    return <SkeletonLoader variant="page" />;
  }

  if (error || !event) {
    return (
      <Box>
        <PageHeader
          title="Drift Event Details"
          actions={
            <Button variant="outlined" startIcon={<BackIcon />} onClick={handleBack}>
              Back to Drift Events
            </Button>
          }
        />
        <Alert severity="error">
          Failed to load drift event. {error ? 'Please try again.' : 'Event not found.'}
        </Alert>
      </Box>
    );
  }

  const formatDateTime = (dateTime: string) => {
    return new Date(dateTime).toLocaleString();
  };

  const renderHashComparison = () => {
    if (!event.expectedHash || !event.appliedHash) {
      return (
        <Typography variant="body2" color="text.secondary">
          Hash information not available
        </Typography>
      );
    }

    return (
      <Box>
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 6 }}>
            <Box>
              <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                Expected Hash
              </Typography>
              <Typography 
                variant="body2" 
                fontFamily="monospace" 
                sx={{ 
                  backgroundColor: 'success.light', 
                  p: 1, 
                  borderRadius: 1,
                  wordBreak: 'break-all'
                }}
              >
                {event.expectedHash}
              </Typography>
            </Box>
          </Grid>
          <Grid size={{ xs: 12, md: 6 }}>
            <Box>
              <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                Applied Hash
              </Typography>
              <Typography 
                variant="body2" 
                fontFamily="monospace" 
                sx={{ 
                  backgroundColor: 'error.light', 
                  p: 1, 
                  borderRadius: 1,
                  wordBreak: 'break-all'
                }}
              >
                {event.appliedHash}
              </Typography>
            </Box>
          </Grid>
        </Grid>
      </Box>
    );
  };

  return (
    <Box>
      <PageHeader
        title={`Drift Event: ${event.id.substring(0, 8)}...`}
        actions={
          <Button variant="outlined" startIcon={<BackIcon />} onClick={handleBack}>
            Back to Drift Events
          </Button>
        }
      />
      
      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 8 }}>
          <DetailCard title="Event Information">
            <Grid container spacing={3}>
              <Grid size={{ xs: 12, sm: 6 }}>
                <Box>
                  <Typography variant="subtitle2" color="text.secondary">
                    Service Name
                  </Typography>
                  <Typography variant="h6" fontWeight={500}>
                    {event.serviceName}
                  </Typography>
                </Box>
              </Grid>
              
              <Grid size={{ xs: 12, sm: 6 }}>
                <Box>
                  <Typography variant="subtitle2" color="text.secondary">
                    Instance ID
                  </Typography>
                  <Typography variant="body1" fontFamily="monospace">
                    {event.instanceId}
                  </Typography>
                </Box>
              </Grid>

              <Grid size={{ xs: 12, sm: 6 }}>
                <Box>
                  <Typography variant="subtitle2" color="text.secondary">
                    Environment
                  </Typography>
                  {event.environment ? (
                    <Chip label={event.environment.toUpperCase()} variant="outlined" />
                  ) : (
                    <Typography variant="body2" color="text.secondary">N/A</Typography>
                  )}
                </Box>
              </Grid>

              <Grid size={{ xs: 12, sm: 6 }}>
                <Box>
                  <Typography variant="subtitle2" color="text.secondary">
                    Config Key
                  </Typography>
                  {event.configKey ? (
                    <Typography variant="body1" fontFamily="monospace">
                      {event.configKey}
                    </Typography>
                  ) : (
                    <Typography variant="body2" color="text.secondary">N/A</Typography>
                  )}
                </Box>
              </Grid>

              <Grid size={{ xs: 12, sm: 6 }}>
                <Box>
                  <Typography variant="subtitle2" color="text.secondary">
                    Severity
                  </Typography>
                  <DriftSeverityChip severity={event.severity} />
                </Box>
              </Grid>

              <Grid size={{ xs: 12, sm: 6 }}>
                <Box>
                  <Typography variant="subtitle2" color="text.secondary">
                    Status
                  </Typography>
                  <ChipStatus status={event.status} />
                </Box>
              </Grid>

              <Grid size={{ xs: 12, sm: 6 }}>
                <Box>
                  <Typography variant="subtitle2" color="text.secondary">
                    Detected At
                  </Typography>
                  <Typography variant="body1">
                    {formatDateTime(event.detectedAt)}
                  </Typography>
                </Box>
              </Grid>

              {event.resolvedAt && (
                <Grid size={{ xs: 12, sm: 6 }}>
                  <Box>
                    <Typography variant="subtitle2" color="text.secondary">
                      Resolved At
                    </Typography>
                    <Typography variant="body1">
                      {formatDateTime(event.resolvedAt)}
                    </Typography>
                  </Box>
                </Grid>
              )}

              {event.notes && (
                <Grid size={{ xs: 12 }}>
                  <Box>
                    <Typography variant="subtitle2" color="text.secondary">
                      Notes
                    </Typography>
                    <Typography variant="body1">
                      {event.notes}
                    </Typography>
                  </Box>
                </Grid>
              )}
            </Grid>
          </DetailCard>

          <DetailCard title="Configuration Hash Comparison">
            {renderHashComparison()}
          </DetailCard>
        </Grid>

        <Grid size={{ xs: 12, md: 4 }}>
          <DetailCard title="Resolution Information">
            <Box>
              <Typography variant="subtitle2" color="text.secondary">
                Resolved By
              </Typography>
              <Typography variant="body1">
                {event.resolvedBy || 'Not resolved yet'}
              </Typography>
            </Box>
            
            <Divider sx={{ my: 2 }} />
            
            <Box>
              <Typography variant="subtitle2" color="text.secondary">
                Created At
              </Typography>
              <Typography variant="body1">
                {formatDateTime(event.createdAt)}
              </Typography>
            </Box>
            
            <Box sx={{ mt: 2 }}>
              <Typography variant="subtitle2" color="text.secondary">
                Last Updated
              </Typography>
              <Typography variant="body1">
                {formatDateTime(event.updatedAt)}
              </Typography>
            </Box>
          </DetailCard>
        </Grid>
      </Grid>
    </Box>
  );
};
