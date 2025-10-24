import { Stepper, Step, StepLabel, StepContent, Typography, Box, Chip, Avatar } from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorIcon from '@mui/icons-material/Error';
import PendingIcon from '@mui/icons-material/Pending';
// Using inline types since they're not exported from @lib/api/models
interface ApprovalGate {
  name: string;
  description?: string;
  minApprovals: number;
  allowedApprovers?: string[];
}

interface GateDecision {
  gateName: string;
  decision: 'APPROVE' | 'REJECT';
  approverUserId: string;
  timestamp?: string;
  notes?: string;
}

interface ApprovalStepperProps {
  gates: ApprovalGate[];
  decisions?: GateDecision[];
  currentStep?: number;
}

export default function ApprovalStepper({ gates, decisions = [], currentStep = 0 }: ApprovalStepperProps) {
  const getGateStatus = (gate: ApprovalGate): 'pending' | 'approved' | 'rejected' => {
    const gateDecisions = decisions.filter(d => d.gateName === gate.name);
    const approvedCount = gateDecisions.filter(d => d.decision === 'APPROVE').length;
    const rejectedCount = gateDecisions.filter(d => d.decision === 'REJECT').length;

    if (rejectedCount > 0) return 'rejected';
    if (approvedCount >= gate.minApprovals) return 'approved';
    return 'pending';
  };

  const getApprovalCount = (gateName: string): { approved: number; required: number } => {
    const gate = gates.find(g => g.name === gateName);
    if (!gate) return { approved: 0, required: 0 };
    
    const approvedCount = decisions.filter(
      d => d.gateName === gateName && d.decision === 'APPROVE'
    ).length;

    return { approved: approvedCount, required: gate.minApprovals };
  };

  const getStepIcon = (status: 'pending' | 'approved' | 'rejected') => {
    switch (status) {
      case 'approved':
        return (
          <Avatar sx={{ bgcolor: 'success.main', width: 32, height: 32 }}>
            <CheckCircleIcon fontSize="small" />
          </Avatar>
        );
      case 'rejected':
        return (
          <Avatar sx={{ bgcolor: 'error.main', width: 32, height: 32 }}>
            <ErrorIcon fontSize="small" />
          </Avatar>
        );
      default:
        return (
          <Avatar sx={{ bgcolor: 'grey.400', width: 32, height: 32 }}>
            <PendingIcon fontSize="small" />
          </Avatar>
        );
    }
  };

  return (
    <Stepper activeStep={currentStep} orientation="vertical">
      {gates.map((gate) => {
        const status = getGateStatus(gate);
        const { approved, required } = getApprovalCount(gate.name);
        const gateDecisions = decisions.filter(d => d.gateName === gate.name);

        return (
          <Step key={gate.name} completed={status === 'approved'}>
            <StepLabel
              StepIconComponent={() => getStepIcon(status)}
              error={status === 'rejected'}
            >
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Typography variant="subtitle1" fontWeight="medium">
                  {gate.name}
                </Typography>
                <Chip 
                  label={`${approved}/${required} approvals`}
                  size="small"
                  color={status === 'approved' ? 'success' : status === 'rejected' ? 'error' : 'default'}
                />
              </Box>
            </StepLabel>
            <StepContent>
              {gate.description && (
                <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                  {gate.description}
                </Typography>
              )}

              {gate.allowedApprovers && gate.allowedApprovers.length > 0 && (
                <Box sx={{ mb: 2 }}>
                  <Typography variant="caption" color="text.secondary" display="block" gutterBottom>
                    Allowed Approvers:
                  </Typography>
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                    {gate.allowedApprovers.map((approver: string) => (
                      <Chip key={approver} label={approver} size="small" variant="outlined" />
                    ))}
                  </Box>
                </Box>
              )}

              {gateDecisions.length > 0 && (
                <Box>
                  <Typography variant="caption" color="text.secondary" display="block" gutterBottom>
                    Decisions ({gateDecisions.length}):
                  </Typography>
                  {gateDecisions.map((decision, idx) => (
                    <Box 
                      key={idx} 
                      sx={{ 
                        display: 'flex', 
                        alignItems: 'center', 
                        gap: 1, 
                        mb: 1,
                        p: 1,
                        bgcolor: 'background.paper',
                        borderRadius: 1,
                        border: '1px solid',
                        borderColor: 'divider'
                      }}
                    >
                      <Chip 
                        label={decision.decision}
                        size="small"
                        color={decision.decision === 'APPROVE' ? 'success' : 'error'}
                      />
                      <Typography variant="body2">
                        by <strong>{decision.approverUserId}</strong>
                      </Typography>
                      {decision.notes && (
                        <Typography variant="caption" color="text.secondary" sx={{ ml: 'auto' }}>
                          "{decision.notes}"
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
  );
}

