import { useState, useEffect } from "react";
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
  Stack,
  Autocomplete,
  Drawer,
  FormHelperText,
  Checkbox,
  FormControlLabel,
  FormGroup,
  Alert,
} from "@mui/material";
import Grid from "@mui/material/Grid";
import { Save as SaveIcon, Close as CloseIcon } from "@mui/icons-material";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import {
  serviceShareCreateSchema,
  type ServiceShareCreateInput,
} from "@lib/forms/schemas";
import {
  useFindAllApplicationServices,
  useFindAllIamTeams,
  useFindAllIamUsers,
  useGrantServiceShare,
} from "@lib/api/hooks";
import { toast } from "@lib/toast/toast";
import { handleApiError } from "@lib/api/errorHandler";

interface ShareFormDrawerProps {
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

const PERMISSION_OPTIONS = [
  { value: "VIEW_SERVICE", label: "View Service" },
  { value: "VIEW_INSTANCE", label: "View Instance" },
  { value: "VIEW_DRIFT", label: "View Drift Events" },
  { value: "EDIT_SERVICE", label: "Edit Service" },
  { value: "EDIT_INSTANCE", label: "Edit Instance" },
  { value: "RESTART_INSTANCE", label: "Restart Instance" },
];

const ENVIRONMENT_OPTIONS = ["dev", "staging", "prod"];

export function ShareFormDrawer({
  open,
  onClose,
  onSuccess,
}: Readonly<ShareFormDrawerProps>) {
  const [selectedPermissions, setSelectedPermissions] = useState<string[]>([]);
  const [selectedEnvironments, setSelectedEnvironments] = useState<string[]>(
    []
  );

  const {
    control,
    handleSubmit,
    watch,
    reset,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<ServiceShareCreateInput>({
    resolver: zodResolver(serviceShareCreateSchema),
    defaultValues: {
      serviceId: "",
      grantToType: "TEAM",
      grantToId: "",
      permissions: [],
      environments: [],
    },
  });

  const grantToType = watch("grantToType");

  // Fetch data for autocompletes
  const { data: servicesData } = useFindAllApplicationServices(
    { page: 0, size: 100 },
    { query: { enabled: open, staleTime: 60_000 } }
  );

  const { data: teamsData } = useFindAllIamTeams(
    { page: 0, size: 100 },
    { query: { enabled: open && grantToType === "TEAM", staleTime: 60_000 } }
  );

  const { data: usersData } = useFindAllIamUsers(
    { page: 0, size: 100 },
    { query: { enabled: open && grantToType === "USER", staleTime: 60_000 } }
  );

  const grantShareMutation = useGrantServiceShare();

  const services = servicesData?.items || [];
  const teams = teamsData?.items || [];
  const users = usersData?.items || [];

  useEffect(() => {
    if (!open) {
      reset();
      setSelectedPermissions([]);
      setSelectedEnvironments([]);
    }
  }, [open, reset]);

  useEffect(() => {
    setValue("permissions", selectedPermissions);
  }, [selectedPermissions, setValue]);

  useEffect(() => {
    setValue("environments", selectedEnvironments);
  }, [selectedEnvironments, setValue]);

  const handlePermissionToggle = (permission: string) => {
    setSelectedPermissions((prev) =>
      prev.includes(permission)
        ? prev.filter((p) => p !== permission)
        : [...prev, permission]
    );
  };

  const handleEnvironmentToggle = (environment: string) => {
    setSelectedEnvironments((prev) =>
      prev.includes(environment)
        ? prev.filter((e) => e !== environment)
        : [...prev, environment]
    );
  };

  const onSubmit = async (data: ServiceShareCreateInput) => {
    try {
      await grantShareMutation.mutateAsync({
        data: {
          serviceId: data.serviceId,
          grantToType: data.grantToType,
          grantToId: data.grantToId,
          permissions: data.permissions,
          environments: data.environments?.length
            ? data.environments
            : undefined,
          expiresAt: data.expiresAt,
        },
      });
      toast.success("Service share created successfully");
      onSuccess();
    } catch (error) {
      handleApiError(error);
    }
  };

  return (
    <Drawer
      anchor="right"
      open={open}
      onClose={onClose}
      slotProps={{
        paper: {
          sx: { width: { xs: "100%", sm: 600 } },
        },
      }}
    >
      <Box
        component="form"
        onSubmit={handleSubmit(onSubmit)}
        sx={{ p: 3, height: "100%", display: "flex", flexDirection: "column" }}
      >
        <Typography variant="h5" gutterBottom>
          Create Service Share
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Grant service access to teams or users with fine-grained permissions
        </Typography>
        <Divider sx={{ mb: 3 }} />

        <Box sx={{ flex: 1, overflow: "auto", pr: 1 }}>
          <Stack spacing={3}>
            {/* Service Selection */}
            <Controller
              name="serviceId"
              control={control}
              render={({ field }) => (
                <Autocomplete
                  {...field}
                  options={services.map((s) => s.id || "")}
                  getOptionLabel={(option) => {
                    const service = services.find((s) => s.id === option);
                    return service
                      ? `${service.displayName} (${service.id})`
                      : option;
                  }}
                  onChange={(_, value) => field.onChange(value || "")}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      label="Service *"
                      error={!!errors.serviceId}
                      helperText={errors.serviceId?.message}
                    />
                  )}
                />
              )}
            />

            {/* Grant To Type */}
            <Controller
              name="grantToType"
              control={control}
              render={({ field }) => (
                <FormControl fullWidth error={!!errors.grantToType}>
                  <InputLabel>Grant To Type *</InputLabel>
                  <Select {...field} label="Grant To Type *">
                    <MenuItem value="TEAM">Team</MenuItem>
                    <MenuItem value="USER">User</MenuItem>
                  </Select>
                  {errors.grantToType && (
                    <FormHelperText>
                      {errors.grantToType.message}
                    </FormHelperText>
                  )}
                </FormControl>
              )}
            />

            {/* Grant To ID */}
            <Controller
              name="grantToId"
              control={control}
              render={({ field }) => (
                <Autocomplete
                  {...field}
                  options={
                    grantToType === "TEAM"
                      ? teams.map((t) => t.teamId || "")
                      : users.map((u) => u.userId || "")
                  }
                  getOptionLabel={(option) => {
                    if (grantToType === "TEAM") {
                      const team = teams.find((t) => t.teamId === option);
                      return team
                        ? `${team.displayName} (${team.teamId})`
                        : option;
                    } else {
                      const user = users.find((u) => u.userId === option);
                      return user ? `${user.username} (${user.email})` : option;
                    }
                  }}
                  onChange={(_, value) => field.onChange(value || "")}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      label={grantToType === "TEAM" ? "Team *" : "User *"}
                      error={!!errors.grantToId}
                      helperText={errors.grantToId?.message}
                    />
                  )}
                />
              )}
            />

            {/* Permissions */}
            <Box>
              <Typography variant="subtitle2" gutterBottom>
                Permissions *
              </Typography>
              <FormGroup>
                <Grid container spacing={1}>
                  {PERMISSION_OPTIONS.map((permission) => (
                    <Grid key={permission.value} size={{ xs: 12, sm: 6 }}>
                      <FormControlLabel
                        control={
                          <Checkbox
                            checked={selectedPermissions.includes(
                              permission.value
                            )}
                            onChange={() =>
                              handlePermissionToggle(permission.value)
                            }
                          />
                        }
                        label={permission.label}
                      />
                    </Grid>
                  ))}
                </Grid>
              </FormGroup>
              {errors.permissions && (
                <FormHelperText error>
                  {errors.permissions.message}
                </FormHelperText>
              )}
            </Box>

            {/* Environments (Optional) */}
            <Box>
              <Typography variant="subtitle2" gutterBottom>
                Environments (Optional)
              </Typography>
              <Typography
                variant="caption"
                color="text.secondary"
                sx={{ mb: 1, display: "block" }}
              >
                Leave empty to grant access to all environments
              </Typography>
              <Stack direction="row" spacing={1} flexWrap="wrap" gap={1}>
                {ENVIRONMENT_OPTIONS.map((env) => (
                  <Chip
                    key={env}
                    label={env.toUpperCase()}
                    onClick={() => handleEnvironmentToggle(env)}
                    color={
                      selectedEnvironments.includes(env) ? "primary" : "default"
                    }
                    variant={
                      selectedEnvironments.includes(env) ? "filled" : "outlined"
                    }
                  />
                ))}
              </Stack>
            </Box>

            {/* Expiration Date (Optional) */}
            <Controller
              name="expiresAt"
              control={control}
              render={({ field }) => (
                <TextField
                  {...field}
                  label="Expiration Date (Optional)"
                  type="datetime-local"
                  slotProps={{ inputLabel: { shrink: true } }}
                  error={!!errors.expiresAt}
                  helperText={
                    errors.expiresAt?.message || "Leave empty for no expiration"
                  }
                />
              )}
            />

            {grantShareMutation.isError && (
              <Alert severity="error">
                Failed to create service share. Please try again.
              </Alert>
            )}
          </Stack>
        </Box>

        {/* Actions */}
        <Box sx={{ mt: 3, pt: 2, borderTop: 1, borderColor: "divider" }}>
          <Stack direction="row" spacing={2} justifyContent="flex-end">
            <Button
              variant="outlined"
              onClick={onClose}
              startIcon={<CloseIcon />}
              disabled={isSubmitting}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="contained"
              startIcon={<SaveIcon />}
              disabled={isSubmitting}
            >
              {isSubmitting ? "Creating..." : "Create Share"}
            </Button>
          </Stack>
        </Box>
      </Box>
    </Drawer>
  );
}
