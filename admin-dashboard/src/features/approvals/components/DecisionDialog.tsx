import React from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  FormControl,
  FormLabel,
  RadioGroup,
  FormControlLabel,
  Radio,
  Box,
} from '@mui/material';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { DecisionRequestSchema } from '../types';
import type { DecisionRequest } from '../types';

interface DecisionDialogProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (decision: DecisionRequest) => void;
  loading?: boolean;
  requestTitle?: string;
}

export const DecisionDialog: React.FC<DecisionDialogProps> = ({
  open,
  onClose,
  onSubmit,
  loading = false,
  requestTitle = 'Approval Request',
}) => {
  const {
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<DecisionRequest>({
    resolver: zodResolver(DecisionRequestSchema),
    defaultValues: {
      decision: 'APPROVE',
      reason: '',
    },
  });

  const handleFormSubmit = (data: DecisionRequest) => {
    onSubmit(data);
  };

  const handleClose = () => {
    reset();
    onClose();
  };

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>Make Decision: {requestTitle}</DialogTitle>
      <DialogContent dividers>
        <Box component="form" onSubmit={handleSubmit(handleFormSubmit)} noValidate>
          <FormControl component="fieldset" sx={{ mb: 3 }}>
            <FormLabel component="legend">Decision</FormLabel>
            <Controller
              name="decision"
              control={control}
              render={({ field }) => (
                <RadioGroup {...field}>
                  <FormControlLabel
                    value="APPROVE"
                    control={<Radio />}
                    label="Approve"
                  />
                  <FormControlLabel
                    value="REJECT"
                    control={<Radio />}
                    label="Reject"
                  />
                </RadioGroup>
              )}
            />
          </FormControl>

          <Controller
            name="reason"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Reason (Optional)"
                fullWidth
                multiline
                rows={3}
                error={!!errors.reason}
                helperText={errors.reason?.message}
                placeholder="Provide a reason for your decision..."
              />
            )}
          />
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose} disabled={loading}>
          Cancel
        </Button>
        <Button
          onClick={handleSubmit(handleFormSubmit)}
          variant="contained"
          disabled={loading}
        >
          Submit Decision
        </Button>
      </DialogActions>
    </Dialog>
  );
};
