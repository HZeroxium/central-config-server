import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Box, Button, Typography, Alert, Chip } from '@mui/material';
import { Timeline, TimelineItem, TimelineSeparator, TimelineConnector, TimelineContent, TimelineDot } from '@mui/lab';
import Grid from '@mui/material/Grid';
import { ArrowBack as BackIcon } from '@mui/icons-material';
import { PageHeader } from '@components/common/PageHeader';
import { SkeletonLoader } from '@components/common/SkeletonLoader';
import { DetailCard } from '@components/common/DetailCard';
import { ApprovalBadge } from '../components/ApprovalBadge';
import { useGetApprovalRequestByIdQuery } from '../api';

const ApprovalDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  
  const { data: request, isLoading, error } = useGetApprovalRequestByIdQuery(id!, {
    skip: !id,
  });

  const handleBack = () => {
    navigate('/approvals');
  };

  if (isLoading) {
    return <SkeletonLoader variant="page" />;
  }

  if (error || !request) {
    return (
      <Box>
        <PageHeader
          title="Approval Request Details"
          actions={
            <Button variant="outlined" startIcon={<BackIcon />} onClick={handleBack}>
              Back to Approvals
            </Button>
          }
        />
        <Alert severity="error">
          Failed to load approval request. {error ? 'Please try again.' : 'Request not found.'}
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
        title={`Approval Request: ${request.id.substring(0, 8)}...`}
        actions={
          <Button variant="outlined" startIcon={<BackIcon />} onClick={handleBack}>
            Back to Approvals
          </Button>
        }
      />
      
      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 8 }}>
          <DetailCard title="Request Information">
            <Grid container spacing={3}>
              <Grid size={{ xs: 12, sm: 6 }}>
                <Box>
                  <Typography variant="subtitle2" color="text.secondary">
                    Request Type
                  </Typography>
                  <Chip 
                    label={request.requestType.replace(/_/g, ' ')} 
                    variant="outlined"
                    sx={{ textTransform: 'capitalize' }}
                  />
                </Box>
              </Grid>
              
              <Grid size={{ xs: 12, sm: 6 }}>
                <Box>
                  <Typography variant="subtitle2" color="text.secondary">
                    Status
                  </Typography>
                  <ApprovalBadge status={request.status} />
                </Box>
              </Grid>

              <Grid size={{ xs: 12, sm: 6 }}>
                <Box>
                  <Typography variant="subtitle2" color="text.secondary">
                    Requester
                  </Typography>
                  <Typography variant="body1" fontWeight={500}>
                    {request.requesterUsername}
                  </Typography>
                </Box>
              </Grid>

              <Grid size={{ xs: 12, sm: 6 }}>
                <Box>
                  <Typography variant="subtitle2" color="text.secondary">
                    Created At
                  </Typography>
                  <Typography variant="body1">
                    {formatDateTime(request.createdAt)}
                  </Typography>
                </Box>
              </Grid>

              <Grid size={{ xs: 12 }}>
                <Box>
                  <Typography variant="subtitle2" color="text.secondary">
                    Request Reason
                  </Typography>
                  <Typography variant="body1">
                    {request.requestReason || 'No reason provided'}
                  </Typography>
                </Box>
              </Grid>
            </Grid>
          </DetailCard>

          {request.target && (
            <DetailCard title="Target Information">
              <Grid container spacing={3}>
                {request.target.serviceName && (
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <Box>
                      <Typography variant="subtitle2" color="text.secondary">
                        Service Name
                      </Typography>
                      <Typography variant="body1" fontWeight={500}>
                        {request.target.serviceName}
                      </Typography>
                    </Box>
                  </Grid>
                )}
                
                {request.target.serviceId && (
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <Box>
                      <Typography variant="subtitle2" color="text.secondary">
                        Service ID
                      </Typography>
                      <Typography variant="body1" fontFamily="monospace">
                        {request.target.serviceId}
                      </Typography>
                    </Box>
                  </Grid>
                )}

                {request.target.environment && (
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <Box>
                      <Typography variant="subtitle2" color="text.secondary">
                        Environment
                      </Typography>
                      <Chip 
                        label={request.target.environment.toUpperCase()} 
                        variant="outlined"
                      />
                    </Box>
                  </Grid>
                )}

                {request.target.configKey && (
                  <Grid size={{ xs: 12, sm: 6 }}>
                    <Box>
                      <Typography variant="subtitle2" color="text.secondary">
                        Config Key
                      </Typography>
                      <Typography variant="body1" fontFamily="monospace">
                        {request.target.configKey}
                      </Typography>
                    </Box>
                  </Grid>
                )}
              </Grid>
            </DetailCard>
          )}
        </Grid>

        <Grid size={{ xs: 12, md: 4 }}>
          <DetailCard title="Decision History">
            {request.decisions && request.decisions.length > 0 ? (
              <Timeline>
                {request.decisions.map((decision, index) => (
                  <TimelineItem key={decision.id}>
                    <TimelineSeparator>
                      <TimelineDot 
                        color={decision.decision === 'APPROVE' ? 'success' : 'error'}
                      />
                      {index < request.decisions!.length - 1 && <TimelineConnector />}
                    </TimelineSeparator>
                    <TimelineContent>
                      <Box>
                        <Typography variant="subtitle2" fontWeight={500}>
                          {decision.decision} by {decision.decidedBy}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {formatDateTime(decision.decidedAt)}
                        </Typography>
                        {decision.reason && (
                          <Typography variant="body2" sx={{ mt: 1 }}>
                            {decision.reason}
                          </Typography>
                        )}
                      </Box>
                    </TimelineContent>
                  </TimelineItem>
                ))}
              </Timeline>
            ) : (
              <Typography variant="body2" color="text.secondary">
                No decisions yet
              </Typography>
            )}
          </DetailCard>
        </Grid>
      </Grid>
    </Box>
  );
};

export default ApprovalDetailPage;
