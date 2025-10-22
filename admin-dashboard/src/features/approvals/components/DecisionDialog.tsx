import React, { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Box,
  Typography,
  Alert,
  Chip,
  Divider,
} from '@mui/material';
import { CheckCircle as ApproveIcon, Cancel as RejectIcon } from '@mui/icons-material';
import type { ApprovalRequest, ApprovalDecision } from '../types';
import { GATE_LABELS } from '../types';

interface DecisionDialogProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (decision: ApprovalDecision) => void;
  request?: ApprovalRequest | null;
}

export const DecisionDialog: React.FC<DecisionDialogProps> = ({
  open,
  onClose,
  onSubmit,
  request,
}) => {
  const [decision, setDecision] = useState<'APPROVED' | 'REJECTED'>('APPROVED');
  const [gate, setGate] = useState<string>('');
  const [note, setNote] = useState('');

  const handleSubmit = () => {
    if (!request || !gate) return;

    onSubmit({
      requestId: request.id,
      gate,
      decision,
      note: note.trim() || undefined,
    });

    // Reset form
    setDecision('APPROVED');
    setGate('');
    setNote('');
  };

  const handleClose = () => {
    onClose();
    // Reset form
    setDecision('APPROVED');
    setGate('');
    setNote('');
  };

  if (!request) return null;

  // Get available gates for this request
  const availableGates = request.requiredGates.filter(gate => {
    const currentApprovals = request.currentApprovals.filter(approval => approval.gate === gate.gate);
    return currentApprovals.length < gate.minApprovals;
  });

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="md" fullWidth>
      <DialogTitle>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Typography variant="h6">
            Submit Approval Decision
          </Typography>
          <Chip
            label={request.serviceName}
            size="small"
            color="primary"
            variant="outlined"
          />
        </Box>
      </DialogTitle>

      <DialogContent>
        {/* Request Details */}
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle1" gutterBottom>
            Request Details
          </Typography>
          <Box sx={{ p: 2, bgcolor: 'grey.50', borderRadius: 1 }}>
            <Typography variant="body2" color="text.secondary" gutterBottom>
              <strong>Type:</strong> {request.requestType}
            </Typography>
            <Typography variant="body2" color="text.secondary" gutterBottom>
              <strong>Requester:</strong> {request.requestedBy} ({request.requestedByEmail})
            </Typography>
            <Typography variant="body2" color="text.secondary" gutterBottom>
              <strong>Description:</strong> {request.description}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              <strong>Priority:</strong> {request.priority}
            </Typography>
          </Box>
        </Box>

        <Divider sx={{ my: 3 }} />

        {/* Approval Gates Status */}
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle1" gutterBottom>
            Approval Gates Status
          </Typography>
          {request.requiredGates.map((gate) => {
            const currentApprovals = request.currentApprovals.filter(approval => approval.gate === gate.gate);
            const isComplete = currentApprovals.length >= gate.minApprovals;
            
            return (
              <Box key={gate.gate} sx={{ mb: 2, p: 2, border: 1, borderColor: 'divider', borderRadius: 1 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                  <Typography variant="body2" fontWeight="medium">
                    {GATE_LABELS[gate.gate as keyof typeof GATE_LABELS] || gate.gate}
                  </Typography>
                  <Chip
                    label={`${currentApprovals.length}/${gate.minApprovals}`}
                    color={isComplete ? 'success' : 'default'}
                    size="small"
                  />
                </Box>
                <Typography variant="caption" color="text.secondary">
                  {gate.description}
                </Typography>
                {currentApprovals.length > 0 && (
                  <Box sx={{ mt: 1 }}>
                    <Typography variant="caption" color="text.secondary">
                      Approved by: {currentApprovals.map(approval => approval.approverName).join(', ')}
                    </Typography>
                  </Box>
                )}
              </Box>
            );
          })}
        </Box>

        <Divider sx={{ my: 3 }} />

        {/* Decision Form */}
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle1" gutterBottom>
            Your Decision
          </Typography>
          
          <FormControl fullWidth sx={{ mb: 2 }}>
            <InputLabel>Approval Gate *</InputLabel>
            <Select
              value={gate}
              onChange={(e) => setGate(e.target.value)}
              label="Approval Gate *"
              required
            >
              {availableGates.map((gateOption) => (
                <MenuItem key={gateOption.gate} value={gateOption.gate}>
                  {GATE_LABELS[gateOption.gate as keyof typeof GATE_LABELS] || gateOption.gate}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <Box sx={{ display: 'flex', gap: 1, mb: 2 }}>
            <Button
              variant={decision === 'APPROVED' ? 'contained' : 'outlined'}
              color="success"
              startIcon={<ApproveIcon />}
              onClick={() => setDecision('APPROVED')}
              fullWidth
            >
              Approve
            </Button>
            <Button
              variant={decision === 'REJECTED' ? 'contained' : 'outlined'}
              color="error"
              startIcon={<RejectIcon />}
              onClick={() => setDecision('REJECTED')}
              fullWidth
            >
              Reject
            </Button>
          </Box>

          <TextField
            label="Note (Optional)"
            multiline
            rows={3}
            value={note}
            onChange={(e) => setNote(e.target.value)}
            fullWidth
            placeholder="Add a note explaining your decision..."
          />
        </Box>

        {decision === 'REJECTED' && (
          <Alert severity="warning" sx={{ mb: 2 }}>
            Rejecting this request will prevent it from being approved. This action cannot be undone.
          </Alert>
        )}
      </DialogContent>

      <DialogActions>
        <Button onClick={handleClose}>
          Cancel
        </Button>
        <Button
          onClick={handleSubmit}
          variant="contained"
          color={decision === 'APPROVED' ? 'success' : 'error'}
          disabled={!gate}
        >
          Submit Decision
        </Button>
      </DialogActions>
    </Dialog>
  );
};