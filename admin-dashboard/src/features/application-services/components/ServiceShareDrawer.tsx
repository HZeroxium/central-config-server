import React from 'react';
import {
  Drawer,
  Box,
  Typography,
  TextField,
  Button,
  FormControl,
  FormLabel,
  FormGroup,
  FormControlLabel,
  Checkbox,
  Autocomplete,
  Divider,
  Stack,
} from '@mui/material';
import Grid from '@mui/material/Grid';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

const ServiceShareSchema = z.object({
  grantToType: z.enum(['TEAM', 'USER']),
  grantToId: z.string().min(1, 'Grant to ID is required'),
  permissions: z.array(z.string()).min(1, 'At least one permission must be selected'),
  environments: z.array(z.string()).optional(),
  expiresAt: z.date().optional(),
});

type ServiceShareFormData = z.infer<typeof ServiceShareSchema>;

interface ServiceShareDrawerProps {
  open: boolean;
  serviceId: string;
  onClose: () => void;
  onSubmit: (data: ServiceShareFormData) => void;
  loading?: boolean;
}

const PERMISSION_OPTIONS = [
  { value: 'VIEW', label: 'View' },
  { value: 'EDIT', label: 'Edit' },
  { value: 'DEPLOY', label: 'Deploy' },
  { value: 'MANAGE', label: 'Manage' },
];

const ENVIRONMENT_OPTIONS = ['dev', 'staging', 'prod', 'test'];

export const ServiceShareDrawer: React.FC<ServiceShareDrawerProps> = ({
  open,
  serviceId,
  onClose,
  onSubmit,
  loading = false,
}) => {
  const {
    control,
    handleSubmit,
    reset,
    watch,
    formState: { errors },
  } = useForm<ServiceShareFormData>({
    resolver: zodResolver(ServiceShareSchema),
    defaultValues: {
      grantToType: 'TEAM',
      grantToId: '',
      permissions: [],
      environments: [],
    },
  });

  const grantToType = watch('grantToType');

  React.useEffect(() => {
    if (open) {
      reset({
        grantToType: 'TEAM',
        grantToId: '',
        permissions: [],
        environments: [],
      });
    }
  }, [open, reset]);

  const handleFormSubmit = (data: ServiceShareFormData) => {
    onSubmit(data);
  };

  const handleClose = () => {
    reset();
    onClose();
  };

  return (
    <Drawer
      anchor="right"
      open={open}
      onClose={handleClose}
      PaperProps={{
        sx: { width: 400 },
      }}
    >
      <LocalizationProvider dateAdapter={AdapterDateFns}>
        <Box sx={{ p: 3, height: '100%', display: 'flex', flexDirection: 'column' }}>
          <Typography variant="h6" fontWeight={600} gutterBottom>
            Share Service
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            Grant permissions for service: <strong>{serviceId}</strong>
          </Typography>

          <form onSubmit={handleSubmit(handleFormSubmit)} style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
            <Box sx={{ flex: 1 }}>
              <Grid container spacing={3}>
                <Grid size={{ xs: 12 }}>
                  <Controller
                    name="grantToType"
                    control={control}
                    render={({ field }) => (
                      <Autocomplete
                        {...field}
                        options={['TEAM', 'USER']}
                        value={field.value}
                        onChange={(_, value) => field.onChange(value)}
                        renderInput={(params) => (
                          <TextField
                            {...params}
                            label="Grant to Type"
                            required
                            error={!!errors.grantToType}
                            helperText={errors.grantToType?.message}
                          />
                        )}
                      />
                    )}
                  />
                </Grid>

                <Grid size={{ xs: 12 }}>
                  <Controller
                    name="grantToId"
                    control={control}
                    render={({ field }) => (
                      <TextField
                        {...field}
                        label={`${grantToType} ID`}
                        fullWidth
                        required
                        error={!!errors.grantToId}
                        helperText={errors.grantToId?.message || `Enter the ${grantToType.toLowerCase()} ID`}
                        placeholder={`team-${grantToType.toLowerCase()}`}
                      />
                    )}
                  />
                </Grid>

                <Grid size={{ xs: 12 }}>
                  <Controller
                    name="permissions"
                    control={control}
                    render={({ field }) => (
                      <FormControl component="fieldset" error={!!errors.permissions} fullWidth>
                        <FormLabel component="legend">Permissions</FormLabel>
                        <FormGroup>
                          {PERMISSION_OPTIONS.map((option) => (
                            <FormControlLabel
                              key={option.value}
                              control={
                                <Checkbox
                                  checked={field.value?.includes(option.value)}
                                  onChange={(e) => {
                                    const value = field.value || [];
                                    if (e.target.checked) {
                                      field.onChange([...value, option.value]);
                                    } else {
                                      field.onChange(value.filter((v) => v !== option.value));
                                    }
                                  }}
                                />
                              }
                              label={option.label}
                            />
                          ))}
                        </FormGroup>
                        {errors.permissions && (
                          <Typography variant="caption" color="error" sx={{ mt: 1 }}>
                            {errors.permissions.message}
                          </Typography>
                        )}
                      </FormControl>
                    )}
                  />
                </Grid>

                <Grid size={{ xs: 12 }}>
                  <Controller
                    name="environments"
                    control={control}
                    render={({ field }) => (
                      <Autocomplete
                        {...field}
                        multiple
                        options={ENVIRONMENT_OPTIONS}
                        value={field.value || []}
                        onChange={(_, value) => field.onChange(value)}
                        renderInput={(params) => (
                          <TextField
                            {...params}
                            label="Environments (Optional)"
                            placeholder="Select specific environments"
                          />
                        )}
                      />
                    )}
                  />
                </Grid>

                <Grid size={{ xs: 12 }}>
                  <Controller
                    name="expiresAt"
                    control={control}
                    render={({ field }) => (
                      <DatePicker
                        {...field}
                        label="Expires At (Optional)"
                        value={field.value}
                        onChange={field.onChange}
                        slotProps={{
                          textField: {
                            fullWidth: true,
                            helperText: 'Leave empty for no expiration',
                          },
                        }}
                      />
                    )}
                  />
                </Grid>
              </Grid>
            </Box>

            <Divider sx={{ my: 3 }} />

            <Stack direction="row" spacing={2}>
              <Button
                onClick={handleClose}
                disabled={loading}
                sx={{ flex: 1 }}
              >
                Cancel
              </Button>
              <Button
                type="submit"
                variant="contained"
                disabled={loading}
                sx={{ flex: 1 }}
              >
                Grant Share
              </Button>
            </Stack>
          </form>
        </Box>
      </LocalizationProvider>
    </Drawer>
  );
};

export default ServiceShareDrawer;
