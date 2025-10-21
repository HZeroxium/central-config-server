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
import { UpdateDriftEventSchema } from '../types';
import type { UpdateDriftEventRequest } from '../types';

interface ResolveDialogProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (update: UpdateDriftEventRequest) => void;
  loading?: boolean;
  eventTitle?: string;
}

export const ResolveDialog: React.FC<ResolveDialogProps> = ({
  open,
  onClose,
  onSubmit,
  loading = false,
  eventTitle = 'Drift Event',
}) => {
  const {
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<UpdateDriftEventRequest>({
    resolver: zodResolver(UpdateDriftEventSchema),
    defaultValues: {
      status: 'RESOLVED',
      notes: '',
    },
  });

  const handleFormSubmit = (data: UpdateDriftEventRequest) => {
    onSubmit(data);
  };

  const handleClose = () => {
    reset();
    onClose();
  };

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>Update Drift Event: {eventTitle}</DialogTitle>
      <DialogContent dividers>
        <Box component="form" onSubmit={handleSubmit(handleFormSubmit)} noValidate>
          <FormControl component="fieldset" sx={{ mb: 3 }}>
            <FormLabel component="legend">Status</FormLabel>
            <Controller
              name="status"
              control={control}
              render={({ field }) => (
                <RadioGroup {...field}>
                  <FormControlLabel
                    value="RESOLVED"
                    control={<Radio />}
                    label="Resolved"
                  />
                  <FormControlLabel
                    value="IGNORED"
                    control={<Radio />}
                    label="Ignored"
                  />
                  <FormControlLabel
                    value="OPEN"
                    control={<Radio />}
                    label="Reopen"
                  />
                </RadioGroup>
              )}
            />
          </FormControl>

          <Controller
            name="notes"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Notes (Optional)"
                fullWidth
                multiline
                rows={4}
                error={!!errors.notes}
                helperText={errors.notes?.message}
                placeholder="Add notes about the resolution or reason for ignoring..."
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
          Update Event
        </Button>
      </DialogActions>
    </Dialog>
  );
};
