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
  CircularProgress,
} from '@mui/material';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useFindAllIamTeams } from '@lib/api/generated/iam-teams/iam-teams';
import { useCreateApprovalRequest } from '@lib/api/generated/approval-requests/approval-requests';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@features/auth/authContext';
import type { IamTeamResponse } from '@lib/api/models';

const claimOwnershipSchema = z.object({
  targetTeamId: z.string().min(1, 'Please select a team'),
  note: z.string().optional(),
});

type ClaimOwnershipFormData = z.infer<typeof claimOwnershipSchema>;

interface ClaimOwnershipDialogProps {
  open: boolean;
  serviceId: string;
  serviceName: string;
  onClose: () => void;
  onSuccess?: (requestId: string) => void;
}

export const ClaimOwnershipDialog: React.FC<ClaimOwnershipDialogProps> = ({
  open,
  serviceId,
  serviceName,
  onClose,
  onSuccess,
}) => {
  const navigate = useNavigate();
  const { userInfo } = useAuth();
  const [error, setError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
    watch,
  } = useForm<ClaimOwnershipFormData>({
    resolver: zodResolver(claimOwnershipSchema),
  });

  // Fetch teams that user belongs to
  const { data: teamsData, isLoading: teamsLoading } = useFindAllIamTeams(undefined, {
    query: {
      enabled: open && !!userInfo?.teamIds?.length,
    }
  });

  // Filter teams to only show user's teams
  const userTeams = teamsData?.items?.filter((team: IamTeamResponse) => 
    userInfo?.teamIds?.includes(team.teamId || '')
  ) || [];

  // Create approval request mutation
  const createApprovalRequestMutation = useCreateApprovalRequest({
    mutation: {
      onSuccess: (data) => {
        const requestId = data.id || '';
        reset();
        setError(null);
        onClose();
        
        if (onSuccess && requestId) {
          onSuccess(requestId);
        } else if (requestId) {
          // Navigate to approval detail page
          navigate(`/approvals/${requestId}`);
        }
      },
      onError: (error) => {
        setError(error.detail || 'Failed to create ownership request');
      },
    },
  });

  const onSubmit = (data: ClaimOwnershipFormData) => {
    setError(null);
    
    createApprovalRequestMutation.mutate({
      serviceId: serviceId || '',
      data: {
        serviceId: serviceId || '',
        targetTeamId: data.targetTeamId,
        note: data.note || `Request ownership of service "${serviceName}"`,
      },
    });
  };

  const handleClose = () => {
    reset();
    setError(null);
    onClose();
  };

  const isLoading = createApprovalRequestMutation.isPending;

  return (
    <Dialog 
      open={open} 
      onClose={handleClose}
      maxWidth="sm"
      fullWidth
      PaperProps={{
        sx: { borderRadius: 2 }
      }}
    >
      <DialogTitle>
        <Typography variant="h6" component="div" fontWeight="bold">
          Claim Service Ownership
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
          Request ownership of "{serviceName}"
        </Typography>
      </DialogTitle>

      <DialogContent>
        <Box component="form" onSubmit={handleSubmit(onSubmit)} sx={{ mt: 2 }}>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}

          <FormControl fullWidth margin="normal" error={!!errors.targetTeamId}>
            <InputLabel>Select Team</InputLabel>
            <Select
              {...register('targetTeamId')}
              label="Select Team"
              disabled={teamsLoading || isLoading}
            >
              {userTeams.map((team) => (
                <MenuItem key={team.teamId} value={team.teamId}>
                  <Box>
                    <Typography variant="body1" fontWeight="medium">
                      {team.displayName}
                    </Typography>
                  </Box>
                </MenuItem>
              ))}
            </Select>
            {errors.targetTeamId && (
              <Typography variant="caption" color="error" sx={{ mt: 0.5, ml: 1.5 }}>
                {errors.targetTeamId.message}
              </Typography>
            )}
          </FormControl>

          <TextField
            {...register('note')}
            label="Note (Optional)"
            multiline
            rows={3}
            fullWidth
            margin="normal"
            placeholder="Add any additional information about this ownership request..."
            disabled={isLoading}
            error={!!errors.note}
            helperText={errors.note?.message}
          />

          {teamsLoading && (
            <Box display="flex" justifyContent="center" py={2}>
              <CircularProgress size={24} />
              <Typography variant="body2" sx={{ ml: 1 }}>
                Loading teams...
              </Typography>
            </Box>
          )}
        </Box>
      </DialogContent>

      <DialogActions sx={{ p: 3, pt: 1 }}>
        <Button 
          onClick={handleClose}
          disabled={isLoading}
          sx={{ textTransform: 'none' }}
        >
          Cancel
        </Button>
        <Button
          onClick={handleSubmit(onSubmit)}
          variant="contained"
          disabled={isLoading || teamsLoading || !watch('targetTeamId')}
          sx={{ textTransform: 'none' }}
        >
          {isLoading ? (
            <>
              <CircularProgress size={16} sx={{ mr: 1 }} />
              Submitting...
            </>
          ) : (
            'Submit Request'
          )}
        </Button>
      </DialogActions>
    </Dialog>
  );
};
