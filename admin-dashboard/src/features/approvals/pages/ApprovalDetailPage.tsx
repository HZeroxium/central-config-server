import React, { useState } from 'react';
import { useParams } from 'react-router-dom';
import { Box, Card, CardContent, Typography, Chip, Button, Grid, Alert, Stepper, Step, StepLabel, StepContent } from '@mui/material';
import { ArrowBack as ArrowBackIcon, CheckCircle as ApproveIcon, Cancel as RejectIcon } from '@mui/icons-material';
import { PageHeader } from '@components/common/PageHeader';
import { useFindApprovalRequestById, useSubmitApprovalDecision } from '@lib/api/hooks';
import { useErrorHandler } from '../../../hooks/useErrorHandler';
import { usePermissions } from '@features/auth/hooks/usePermissions';
import { DecisionDialog } from '../components/DecisionDialog';
import { 
  REQUEST_TYPE_LABELS, 
  STATUS_LABELS, 
  PRIORITY_LABELS, 
  STATUS_COLORS, 
  PRIORITY_COLORS,
  GATE_LABELS 
} from '../types';
import { format } from 'date-fns';

export const ApprovalDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const { isSysAdmin } = usePermissions();
  const { handleError, showSuccess } = useErrorHandler();

  const {
    data: requestResponse,
    isLoading,
    error,
    refetch,
  } = useFindApprovalRequestById(id!, {
    query: {
      enabled: !!id,
    },
  });

  const submitDecisionMutation = useSubmitApprovalDecision();

  const request = requestResponse as any; // TODO: Fix API type generation
  const [decisionDialogOpen, setDecisionDialogOpen] = useState(false);

  const handleSubmitDecision = async (decision: any) => {
    if (!request || !id) return;
    
    try {
      await submitDecisionMutation.mutateAsync({
        id: id,
        data: {
          decision: decision.decision,
          note: decision.note,
        }
      });
      setDecisionDialogOpen(false);
      showSuccess('Decision submitted successfully');
      refetch();
    } catch (error) {
      handleError(error, 'Failed to submit decision');
    }
  };

  const canApprove = isSysAdmin;

  if (isLoading) {
    return (
      <Box>
        <PageHeader title="Loading..." />
        <Box sx={{ p: 3 }}>Loading approval request details...</Box>
      </Box>
    );
  }

  if (error || !request) {
    return (
      <Box>
        <PageHeader title="Approval Request Not Found" />
        <Alert severity="error" sx={{ m: 3 }}>
          The requested approval could not be found or you don't have permission to view it.
        </Alert>
      </Box>
    );
  }

  const isExpired = request.expiresAt ? new Date(request.expiresAt) < new Date() : false;

  return (
    <Box>
      <PageHeader
        title={`Approval Request: ${request.serviceName}`}
        subtitle={`${REQUEST_TYPE_LABELS[request.requestType as keyof typeof REQUEST_TYPE_LABELS]} - ${request.status}`}
        actions={
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Button
              variant="outlined"
              startIcon={<ArrowBackIcon />}
              onClick={() => window.history.back()}
            >
              Back
            </Button>
            {canApprove && request.status === 'PENDING' && (
              <Button
                variant="contained"
                startIcon={<ApproveIcon />}
                onClick={() => setDecisionDialogOpen(true)}
              >
                Make Decision
              </Button>
            )}
          </Box>
        }
      />

      <Grid container spacing={3} sx={{ p: 3 }}>
        {/* Request Information */}
        <Grid size={{ xs: 12, md: 6 }}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Request Information
              </Typography>
              
              <Box sx={{ mb: 2 }}>
                <Typography variant="body2" color="text.secondary">
                  Service
                </Typography>
                <Typography variant="body1" fontWeight="medium">
                  {request.serviceName}
                </Typography>
              </Box>

              <Box sx={{ mb: 2 }}>
                <Typography variant="body2" color="text.secondary">
                  Type
                </Typography>
                <Chip
                  label={REQUEST_TYPE_LABELS[request.requestType as keyof typeof REQUEST_TYPE_LABELS] || request.requestType}
                  size="small"
                  variant="outlined"
                />
              </Box>

              <Box sx={{ mb: 2 }}>
                <Typography variant="body2" color="text.secondary">
                  Status
                </Typography>
                <Chip
                  label={STATUS_LABELS[request.status as keyof typeof STATUS_LABELS] || request.status}
                  color={STATUS_COLORS[request.status as keyof typeof STATUS_COLORS] as any}
                  size="small"
                />
              </Box>

              <Box sx={{ mb: 2 }}>
                <Typography variant="body2" color="text.secondary">
                  Priority
                </Typography>
                <Chip
                  label={PRIORITY_LABELS[request.priority as keyof typeof PRIORITY_LABELS] || request.priority}
                  color={PRIORITY_COLORS[request.priority as keyof typeof PRIORITY_COLORS] as any}
                  size="small"
                />
              </Box>

              <Box sx={{ mb: 2 }}>
                <Typography variant="body2" color="text.secondary">
                  Requester
                </Typography>
                <Typography variant="body1">
                  {request.requestedBy} ({request.requestedByEmail})
                </Typography>
              </Box>

              <Box sx={{ mb: 2 }}>
                <Typography variant="body2" color="text.secondary">
                  Created
                </Typography>
                <Typography variant="body1">
                  {format(new Date(request.createdAt), 'PPP')}
                </Typography>
              </Box>

              {request.expiresAt && (
                <Box sx={{ mb: 2 }}>
                  <Typography variant="body2" color="text.secondary">
                    Expires
                  </Typography>
                  <Typography 
                    variant="body1" 
                    color={isExpired ? 'error' : 'text.primary'}
                    fontWeight={isExpired ? 'medium' : 'normal'}
                  >
                    {format(new Date(request.expiresAt), 'PPP')}
                    {isExpired && ' (Expired)'}
                  </Typography>
                </Box>
              )}
            </CardContent>
          </Card>
        </Grid>

        {/* Description */}
        <Grid size={{ xs: 12, md: 6 }}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Description
              </Typography>
              <Typography variant="body1">
                {request.description}
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        {/* Approval Timeline */}
        <Grid size={{ xs: 12 }}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Approval Timeline
              </Typography>
              
              <Stepper orientation="vertical">
                {request.requiredGates.map((gate, index) => {
                  const currentApprovals = request.currentApprovals.filter(approval => approval.gate === gate.gate);
                  const isComplete = currentApprovals.length >= gate.minApprovals;
                  const isActive = !isComplete && request.status === 'PENDING';
                  
                  return (
                    <Step key={gate.gate} completed={isComplete} active={isActive}>
                      <StepLabel>
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <Typography variant="body1" fontWeight="medium">
                            {GATE_LABELS[gate.gate as keyof typeof GATE_LABELS] || gate.gate}
                          </Typography>
                          <Chip
                            label={`${currentApprovals.length}/${gate.minApprovals}`}
                            color={isComplete ? 'success' : isActive ? 'primary' : 'default'}
                            size="small"
                          />
                        </Box>
                      </StepLabel>
                      <StepContent>
                        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                          {gate.description}
                        </Typography>
                        
                        {currentApprovals.length > 0 && (
                          <Box sx={{ mb: 2 }}>
                            <Typography variant="body2" fontWeight="medium" gutterBottom>
                              Approvals:
                            </Typography>
                            {currentApprovals.map((approval) => (
                              <Box key={approval.id} sx={{ ml: 2, mb: 1 }}>
                                <Typography variant="body2">
                                  <strong>{approval.approverName}</strong> - {approval.decision}
                                </Typography>
                                <Typography variant="caption" color="text.secondary">
                                  {format(new Date(approval.createdAt), 'PPP')}
                                </Typography>
                                {approval.note && (
                                  <Typography variant="caption" color="text.secondary" display="block">
                                    Note: {approval.note}
                                  </Typography>
                                )}
                              </Box>
                            ))}
                          </Box>
                        )}
                      </StepContent>
                    </Step>
                  );
                })}
              </Stepper>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <DecisionDialog
        open={decisionDialogOpen}
        onClose={() => setDecisionDialogOpen(false)}
        onSubmit={handleSubmitDecision}
        request={request}
      />
    </Box>
  );
};

export default ApprovalDetailPage;