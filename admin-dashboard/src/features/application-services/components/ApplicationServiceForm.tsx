import React from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Autocomplete,
  Typography,
} from '@mui/material';
import Grid from '@mui/material/Grid';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { CreateApplicationServiceSchema, UpdateApplicationServiceSchema } from '../types';
import type { CreateApplicationServiceFormData, UpdateApplicationServiceFormData } from '../types';
interface ApplicationServiceFormProps {
  open: boolean;
  mode: 'create' | 'edit';
  initialData?: UpdateApplicationServiceFormData;
  onSubmit: (data: CreateApplicationServiceFormData | UpdateApplicationServiceFormData) => void;
  onClose: () => void;
  loading?: boolean;
}

const ENVIRONMENT_OPTIONS = ['dev', 'staging', 'prod', 'test'];
const LIFECYCLE_OPTIONS = ['development', 'staging', 'production', 'maintenance', 'deprecated'];

export const ApplicationServiceForm: React.FC<ApplicationServiceFormProps> = ({
  open,
  mode,
  initialData,
  onSubmit,
  onClose,
  loading = false,
}) => {
  const schema = mode === 'create' ? CreateApplicationServiceSchema : UpdateApplicationServiceSchema;
  
  const {
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CreateApplicationServiceFormData | UpdateApplicationServiceFormData>({
    resolver: zodResolver(schema),
    defaultValues: initialData || {
      id: '',
      displayName: '',
      ownerTeamId: '',
      environments: [],
      tags: [],
      repoUrl: '',
      lifecycle: '',
      attributes: {},
    },
  });

  React.useEffect(() => {
    if (open) {
      reset(initialData || {
        id: '',
        displayName: '',
        ownerTeamId: '',
        environments: [],
        tags: [],
        repoUrl: '',
        lifecycle: '',
        attributes: {},
      });
    }
  }, [open, initialData, reset]);

  const handleFormSubmit = (data: CreateApplicationServiceFormData | UpdateApplicationServiceFormData) => {
    onSubmit(data);
  };

  const handleClose = () => {
    reset();
    onClose();
  };

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="md" fullWidth>
      <DialogTitle>
        <Typography variant="h6" fontWeight={600}>
          {mode === 'create' ? 'Create Application Service' : 'Edit Application Service'}
        </Typography>
      </DialogTitle>
      
      <form onSubmit={handleSubmit(handleFormSubmit)}>
        <DialogContent>
          <Grid container spacing={3}>
            {mode === 'create' && (
              <Grid size={{ xs: 12, sm: 6 }} component="div">
                <Controller
                  name="id"
                  control={control}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      label="Service ID"
                      fullWidth
                      required
                      error={!!(errors as any).id}
                      helperText={(errors as any).id?.message}
                      placeholder="my-service-id"
                    />
                  )}
                />
              </Grid>
            )}
            
            <Grid size={{ xs: 12, sm: mode === 'create' ? 6 : 12 }}>
              <Controller
                name="displayName"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    label="Display Name"
                    fullWidth
                    required={mode === 'create'}
                      error={!!(errors as any).displayName}
                      helperText={(errors as any).displayName?.message}
                    placeholder="My Service Display Name"
                  />
                )}
              />
            </Grid>

            {mode === 'create' && (
              <Grid size={{ xs: 12, sm: 6 }}>
                <Controller
                  name="ownerTeamId"
                  control={control}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      label="Owner Team ID"
                      fullWidth
                      required
                      error={!!(errors as any).ownerTeamId}
                      helperText={(errors as any).ownerTeamId?.message}
                      placeholder="team-core"
                    />
                  )}
                />
              </Grid>
            )}

            <Grid size={{ xs: 12, sm: mode === 'create' ? 6 : 12 }}>
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
                        label="Environments"
                        required={mode === 'create'}
                        error={!!(errors as any).environments}
                        helperText={(errors as any).environments?.message}
                        placeholder="Select environments"
                      />
                    )}
                  />
                )}
              />
            </Grid>

            <Grid size={{ xs: 12, sm: 6 }}>
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
                    onChange={(_, value) => field.onChange(value)}
                    renderInput={(params) => (
                      <TextField
                        {...params}
                        label="Tags"
                        placeholder="Add tags"
                      />
                    )}
                  />
                )}
              />
            </Grid>

            <Grid size={{ xs: 12, sm: 6 }}>
              <Controller
                name="lifecycle"
                control={control}
                render={({ field }) => (
                  <Autocomplete
                    {...field}
                    options={LIFECYCLE_OPTIONS}
                    value={field.value || ''}
                    onChange={(_, value) => field.onChange(value)}
                    renderInput={(params) => (
                      <TextField
                        {...params}
                        label="Lifecycle"
                        placeholder="Select lifecycle"
                      />
                    )}
                  />
                )}
              />
            </Grid>

            <Grid size={{ xs: 12 }}>
              <Controller
                name="repoUrl"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    label="Repository URL"
                    fullWidth
                    error={!!(errors as any).repoUrl}
                    helperText={(errors as any).repoUrl?.message}
                    placeholder="https://github.com/org/repo"
                  />
                )}
              />
            </Grid>
          </Grid>
        </DialogContent>

        <DialogActions sx={{ p: 3, pt: 1 }}>
          <Button onClick={handleClose} disabled={loading}>
            Cancel
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={loading}
          >
            {mode === 'create' ? 'Create' : 'Update'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
};

export default ApplicationServiceForm;
