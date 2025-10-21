import React, { useEffect } from 'react';
import {
  Drawer,
  Box,
  Typography,
  TextField,
  Button,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
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
import { CreateServiceShareSchema } from '../types';
import type { CreateServiceShareRequest } from '../types';

interface ShareFormDrawerProps {
  open: boolean;
  onClose: () => void;
  serviceId: string;
  initialData?: CreateServiceShareRequest;
  onSubmit: (data: CreateServiceShareRequest) => void;
  loading?: boolean;
}

const GRANT_TO_TYPE_OPTIONS = [
  { value: 'TEAM', label: 'Team' },
  { value: 'USER', label: 'User' },
];

const PERMISSION_OPTIONS = [
  { value: 'VIEW', label: 'View' },
  { value: 'EDIT', label: 'Edit' },
  { value: 'DELETE', label: 'Delete' },
];

const ENVIRONMENT_OPTIONS = ['dev', 'staging', 'prod', 'test'];

// Mock data - in real app, this would come from API
const TEAM_OPTIONS = ['team-alpha', 'team-beta', 'team-gamma', 'team-delta'];
const USER_OPTIONS = ['user1', 'user2', 'user3', 'user4'];

export const ShareFormDrawer: React.FC<ShareFormDrawerProps> = ({
  open,
  onClose,
  serviceId,
  initialData,
  onSubmit,
  loading = false,
}) => {
  const {
    control,
    handleSubmit,
    reset,
    watch,
    formState: { errors },
  } = useForm<CreateServiceShareRequest>({
    resolver: zodResolver(CreateServiceShareSchema),
    defaultValues: initialData || {
      serviceId,
      grantToType: 'TEAM',
      grantToId: '',
      permissions: [],
      environments: [],
      expiresAt: undefined,
    },
  });

  const grantToType = watch('grantToType');

  useEffect(() => {
    if (open) {
      reset(initialData || {
        serviceId,
        grantToType: 'TEAM',
        grantToId: '',
        permissions: [],
        environments: [],
        expiresAt: undefined,
      });
    }
  }, [open, initialData, serviceId, reset]);

  const handleFormSubmit = (data: CreateServiceShareRequest) => {
    onSubmit(data);
  };

  const getGrantToOptions = () => {
    return grantToType === 'TEAM' ? TEAM_OPTIONS : USER_OPTIONS;
  };

  return (
    <Drawer anchor="right" open={open} onClose={onClose} PaperProps={{ sx: { width: { xs: '100%', sm: 600 } } }}>
      <Box sx={{ p: 3 }}>
        <Typography variant="h5" gutterBottom>
          Share Service: {serviceId}
        </Typography>
        <Divider sx={{ my: 2 }} />
        
        <Box component="form" onSubmit={handleSubmit(handleFormSubmit)} noValidate>
          <Grid container spacing={3}>
            <Grid size={{ xs: 12 }}>
              <Controller
                name="grantToType"
                control={control}
                render={({ field }) => (
                  <FormControl fullWidth error={!!errors.grantToType}>
                    <InputLabel>Grant To Type</InputLabel>
                    <Select
                      {...field}
                      label="Grant To Type"
                    >
                      {GRANT_TO_TYPE_OPTIONS.map((option) => (
                        <MenuItem key={option.value} value={option.value}>
                          {option.label}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                )}
              />
            </Grid>

            <Grid size={{ xs: 12 }}>
              <Controller
                name="grantToId"
                control={control}
                render={({ field }) => (
                  <Autocomplete
                    {...field}
                    options={getGrantToOptions()}
                    onChange={(_, value) => field.onChange(value)}
                    renderInput={(params) => (
                      <TextField
                        {...params}
                        label={`Grant To ${grantToType}`}
                        fullWidth
                        required
                        error={!!errors.grantToId}
                        helperText={errors.grantToId?.message}
                        placeholder={`Select ${grantToType.toLowerCase()}`}
                      />
                    )}
                  />
                )}
              />
            </Grid>

            <Grid size={{ xs: 12 }}>
              <Typography variant="subtitle1" gutterBottom>
                Permissions
              </Typography>
              <Controller
                name="permissions"
                control={control}
                render={({ field }) => (
                  <Box>
                    {PERMISSION_OPTIONS.map((option) => (
                      <FormControlLabel
                        key={option.value}
                        control={
                          <Checkbox
                            checked={field.value.includes(option.value as any)}
                            onChange={(e) => {
                              const newPermissions = e.target.checked
                                ? [...field.value, option.value]
                                : field.value.filter((p: string) => p !== option.value);
                              field.onChange(newPermissions);
                            }}
                          />
                        }
                        label={option.label}
                      />
                    ))}
                    {errors.permissions && (
                      <Typography color="error" variant="caption" display="block">
                        {errors.permissions.message}
                      </Typography>
                    )}
                  </Box>
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
                    freeSolo
                    onChange={(_, value) => field.onChange(value)}
                    renderInput={(params) => (
                      <TextField
                        {...params}
                        label="Environments (Optional)"
                        fullWidth
                        error={!!errors.environments}
                        helperText={errors.environments?.message || "Leave empty to grant access to all environments"}
                        placeholder="Select environments"
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
                  <LocalizationProvider dateAdapter={AdapterDateFns}>
                    <DatePicker
                      label="Expires At (Optional)"
                      value={field.value ? new Date(field.value) : null}
                      onChange={(date) => field.onChange(date)}
                      slotProps={{
                        textField: {
                          fullWidth: true,
                          error: !!errors.expiresAt,
                          helperText: errors.expiresAt?.message || "Leave empty for no expiration"
                        }
                      }}
                    />
                  </LocalizationProvider>
                )}
              />
            </Grid>

            <Grid size={{ xs: 12 }}>
              <Stack direction="row" spacing={2} sx={{ mt: 3 }}>
                <Button onClick={onClose} disabled={loading} variant="outlined">
                  Cancel
                </Button>
                <Button type="submit" variant="contained" color="primary" disabled={loading}>
                  Grant Share
                </Button>
              </Stack>
            </Grid>
          </Grid>
        </Box>
      </Box>
    </Drawer>
  );
};
