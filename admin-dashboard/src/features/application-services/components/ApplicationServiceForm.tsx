import { useEffect } from 'react';
import {
  Box,
  Button,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Chip,
  Typography,
  Divider,
  Autocomplete,
  Stack,
} from '@mui/material';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Save as SaveIcon, Close as CloseIcon } from '@mui/icons-material';
import {
  useCreateApplicationService,
  useUpdateApplicationService,
} from '@lib/api/hooks';
import { toast } from '@lib/toast/toast';
import { handleApiError } from '@lib/api/errorHandler';
import { applicationServiceCreateSchema, applicationServiceUpdateSchema, type ApplicationServiceCreateInput, type ApplicationServiceUpdateInput } from '@lib/forms/schemas';
import type { ApplicationServiceResponse } from '@lib/api/models';

interface ApplicationServiceFormProps {
  mode: 'create' | 'edit';
  initialData?: ApplicationServiceResponse;
  onSuccess: () => void;
  onCancel: () => void;
}

const ENVIRONMENT_OPTIONS = ['dev', 'staging', 'prod'];
const LIFECYCLE_OPTIONS = ['ACTIVE', 'DEPRECATED', 'RETIRED'];

export function ApplicationServiceForm({
  mode,
  initialData,
  onSuccess,
  onCancel,
}: ApplicationServiceFormProps) {
  const createMutation = useCreateApplicationService();
  const updateMutation = useUpdateApplicationService();

  const {
    control,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ApplicationServiceCreateInput | ApplicationServiceUpdateInput>({
    resolver: zodResolver(mode === 'create' ? applicationServiceCreateSchema : applicationServiceUpdateSchema),
    defaultValues: mode === 'create'
      ? {
          id: '',
          displayName: '',
          ownerTeamId: '',
          environments: ['dev'],
          tags: [],
          repoUrl: '',
          attributes: {},
        }
      : {
          displayName: initialData?.displayName || '',
          lifecycle: initialData?.lifecycle || 'ACTIVE',
          tags: initialData?.tags || [],
          repoUrl: initialData?.repoUrl || '',
          attributes: initialData?.attributes || {},
        },
  });

  useEffect(() => {
    if (initialData && mode === 'edit') {
      reset({
        displayName: initialData.displayName || '',
        lifecycle: initialData.lifecycle || 'ACTIVE',
        tags: initialData.tags || [],
        repoUrl: initialData.repoUrl || '',
        attributes: initialData.attributes || {},
      });
    }
  }, [initialData, mode, reset]);

  const onSubmit = async (data: ApplicationServiceCreateInput | ApplicationServiceUpdateInput) => {
    if (mode === 'create') {
      const createData = data as ApplicationServiceCreateInput;
      createMutation.mutate(
        { data: createData },
        {
          onSuccess: () => {
            toast.success('Service created successfully');
            onSuccess();
          },
          onError: (error) => {
            handleApiError(error);
          },
        }
      );
    } else {
      const updateData = data as ApplicationServiceUpdateInput;
      updateMutation.mutate(
        { id: initialData!.id!, data: updateData },
        {
          onSuccess: () => {
            toast.success('Service updated successfully');
            onSuccess();
          },
          onError: (error) => {
            handleApiError(error);
          },
        }
      );
    }
  };

  const isLoading = createMutation.isPending || updateMutation.isPending || isSubmitting;

  return (
    <Box sx={{ p: 3, height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Typography variant="h5" gutterBottom>
        {mode === 'create' ? 'Create Application Service' : 'Edit Application Service'}
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        {mode === 'create'
          ? 'Register a new application service in the system'
          : 'Update the application service details'}
      </Typography>
      <Divider sx={{ mb: 3 }} />

      <Box
        component="form"
        onSubmit={handleSubmit(onSubmit)}
        sx={{ flex: 1, overflow: 'auto' }}
      >
        <Stack spacing={3}>
          {/* Service ID - Only for create */}
          {mode === 'create' && (
            <Controller
              name="id"
              control={control}
              render={({ field }) => (
                <TextField
                  {...field}
                  label="Service ID *"
                  fullWidth
                  error={!!(errors as any).id}
                  helperText={(errors as any).id?.message || 'Unique identifier for the service (e.g., user-service)'}
                />
              )}
            />
          )}

          {/* Display Name */}
          <Controller
            name="displayName"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Display Name *"
                fullWidth
                error={!!errors.displayName}
                helperText={(errors as any).displayName?.message || 'Human-readable name for the service'}
              />
            )}
          />

          {/* Owner Team ID - Only for create */}
          {mode === 'create' && (
            <Controller
              name="ownerTeamId"
              control={control}
              render={({ field }) => (
                <TextField
                  {...field}
                  label="Owner Team ID *"
                  fullWidth
                  error={!!(errors as any).ownerTeamId}
                  helperText={(errors as any).ownerTeamId?.message || 'ID of the team that owns this service'}
                />
              )}
            />
          )}

          {/* Lifecycle - Only for edit */}
          {mode === 'edit' && (
            <Controller
              name="lifecycle"
              control={control}
              render={({ field }) => (
                <FormControl fullWidth error={!!(errors as any).lifecycle}>
                  <InputLabel>Lifecycle</InputLabel>
                  <Select {...field} label="Lifecycle">
                    {LIFECYCLE_OPTIONS.map((option) => (
                      <MenuItem key={option} value={option}>
                        {option}
                      </MenuItem>
                    ))}
                  </Select>
                  {(errors as any).lifecycle && (
                    <Typography variant="caption" color="error" sx={{ mt: 0.5, ml: 2 }}>
                      {(errors as any).lifecycle.message}
                    </Typography>
                  )}
                </FormControl>
              )}
            />
          )}

          {/* Environments - Only for create */}
          {mode === 'create' && (
            <Controller
              name="environments"
              control={control}
              render={({ field }) => (
                <Autocomplete
                  {...field}
                  multiple
                  options={ENVIRONMENT_OPTIONS}
                  value={field.value || []}
                  onChange={(_, newValue) => field.onChange(newValue)}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      label="Environments *"
                      error={!!(errors as any).environments}
                      helperText={(errors as any).environments?.message || 'Select deployment environments'}
                    />
                  )}
                  renderTags={(value, getTagProps) =>
                    value.map((option, index) => (
                      <Chip
                        {...getTagProps({ index })}
                        key={option}
                        label={option.toUpperCase()}
                        color={option === 'prod' ? 'error' : option === 'staging' ? 'warning' : 'info'}
                        size="small"
                      />
                    ))
                  }
                />
              )}
            />
          )}

          {/* Tags */}
          <Controller
            name="tags"
            control={control}
            render={({ field }) => (
              <Autocomplete
                {...field}
                multiple
                freeSolo
                options={[]}
                value={field.value || []}
                onChange={(_, newValue) => field.onChange(newValue)}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    label="Tags"
                    error={!!(errors as any).tags}
                    helperText={(errors as any).tags?.message || 'Add custom tags (press Enter to add)'}
                  />
                )}
                renderTags={(value, getTagProps) =>
                  value.map((option, index) => (
                    <Chip {...getTagProps({ index })} key={option} label={option} size="small" />
                  ))
                }
              />
            )}
          />

          {/* Repository URL */}
          <Controller
            name="repoUrl"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Repository URL"
                fullWidth
                error={!!(errors as any).repoUrl}
                helperText={(errors as any).repoUrl?.message || 'Git repository URL'}
              />
            )}
          />
        </Stack>
      </Box>

      {/* Actions */}
      <Box sx={{ mt: 3, pt: 2, borderTop: 1, borderColor: 'divider' }}>
        <Stack direction="row" spacing={2} justifyContent="flex-end">
          <Button variant="outlined" onClick={onCancel} disabled={isLoading} startIcon={<CloseIcon />}>
            Cancel
          </Button>
          <Button
            variant="contained"
            onClick={handleSubmit(onSubmit)}
            disabled={isLoading}
            startIcon={<SaveIcon />}
          >
            {mode === 'create' ? 'Create' : 'Update'}
          </Button>
        </Stack>
      </Box>
    </Box>
  );
}
