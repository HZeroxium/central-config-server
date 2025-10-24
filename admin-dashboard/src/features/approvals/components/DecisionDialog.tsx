import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  TextField,
  Stack,
} from '@mui/material';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

const decisionSchema = z.object({
  decision: z.enum(['APPROVE', 'REJECT']),
  note: z.string().max(500, 'Note must not exceed 500 characters').optional(),
});

type DecisionFormData = z.infer<typeof decisionSchema>;

interface DecisionDialogProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (data: DecisionFormData) => void;
  loading?: boolean;
}

export function DecisionDialog({ open, onClose, onSubmit, loading = false }: DecisionDialogProps) {
  const {
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<DecisionFormData>({
    resolver: zodResolver(decisionSchema),
    defaultValues: {
      decision: 'APPROVE',
      note: '',
    },
  });

  const handleClose = () => {
    if (!loading) {
      reset();
      onClose();
    }
  };

  const handleFormSubmit = (data: DecisionFormData) => {
    onSubmit(data);
  };

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>Make Approval Decision</DialogTitle>
      <DialogContent>
        <Stack spacing={3} sx={{ mt: 1 }}>
          <Controller
            name="decision"
            control={control}
            render={({ field }) => (
              <FormControl fullWidth error={!!errors.decision}>
                <InputLabel>Decision *</InputLabel>
                <Select {...field} label="Decision *">
                  <MenuItem value="APPROVE">Approve</MenuItem>
                  <MenuItem value="REJECT">Reject</MenuItem>
                </Select>
              </FormControl>
            )}
          />

          <Controller
            name="note"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Note (Optional)"
                multiline
                rows={4}
                fullWidth
                error={!!errors.note}
                helperText={errors.note?.message || 'Provide additional context for your decision'}
              />
            )}
          />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose} disabled={loading}>
          Cancel
        </Button>
        <Button onClick={handleSubmit(handleFormSubmit)} variant="contained" disabled={loading}>
          Submit Decision
        </Button>
      </DialogActions>
    </Dialog>
  );
}
