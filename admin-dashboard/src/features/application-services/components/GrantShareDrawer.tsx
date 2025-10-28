import React, { useState } from "react";
import {
  Drawer,
  Box,
  Typography,
  Button,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  FormControlLabel,
  Checkbox,
  FormGroup,
  Chip,
  Alert,
  CircularProgress,
  Divider,
  IconButton,
} from "@mui/material";
import { Close as CloseIcon } from "@mui/icons-material";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useFindAllIamTeams, useFindAllIamUsers } from "@lib/api/hooks";
import { useGrantServiceShare } from "@lib/api/generated/service-shares/service-shares";
import type {
  IamUserResponse,
  ServiceShareCreateRequest,
} from "@lib/api/models";

const grantShareSchema = z.object({
  grantToType: z.enum(["USER", "TEAM"]),
  grantToId: z.string().min(1, "Please select a user or team"),
  permissions: z
    .array(z.string())
    .min(1, "Please select at least one permission"),
  environments: z.array(z.string()).optional(),
  expiresAt: z.string().optional(),
  note: z.string().optional(),
});

type GrantShareFormData = z.infer<typeof grantShareSchema>;

interface GrantShareDrawerProps {
  open: boolean;
  serviceId: string;
  onClose: () => void;
  onSuccess: () => void;
}

const PERMISSIONS = [
  { value: "VIEW_SERVICE", label: "View Service" },
  { value: "EDIT_SERVICE", label: "Edit Service" },
  { value: "VIEW_INSTANCE", label: "View Instances" },
  { value: "EDIT_INSTANCE", label: "Edit Instances" },
  { value: "VIEW_DRIFT", label: "View Drift Events" },
  { value: "RESTART_INSTANCE", label: "Restart Instances" },
];

const ENVIRONMENTS = ["dev", "staging", "prod"];

export const GrantShareDrawer: React.FC<GrantShareDrawerProps> = ({
  open,
  serviceId,
  onClose,
  onSuccess,
}) => {
  const [error, setError] = useState<string | null>(null);
  const [grantToType, setGrantToType] = useState<"USER" | "TEAM">("TEAM");

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
    watch,
    setValue,
  } = useForm<GrantShareFormData>({
    resolver: zodResolver(grantShareSchema),
    defaultValues: {
      grantToType: "TEAM",
      permissions: [],
      environments: [],
    },
  });

  // Fetch teams and users
  const { data: teamsData, isLoading: teamsLoading } = useFindAllIamTeams(
    undefined,
    { query: { enabled: open } }
  );

  const { data: usersData, isLoading: usersLoading } = useFindAllIamUsers(
    undefined,
    { query: { enabled: open && grantToType === "USER" } }
  );

  // Grant share mutation
  const grantShareMutation = useGrantServiceShare({
    mutation: {
      onSuccess: () => {
        reset();
        setError(null);
        onClose();
        onSuccess();
      },
      onError: (error) => {
        setError(error.detail || "Failed to grant service share");
      },
    },
  });

  const onSubmit = (data: GrantShareFormData) => {
    setError(null);

    const payload: ServiceShareCreateRequest = {
      serviceId,
      grantToType: data.grantToType,
      grantToId: data.grantToId,
      permissions: data.permissions,
      environments: data.environments?.length ? data.environments : undefined,
      expiresAt: data.expiresAt || undefined,
    };

    grantShareMutation.mutate({
      data: payload,
    });
  };

  const handleClose = () => {
    reset();
    setError(null);
    setGrantToType("TEAM");
    onClose();
  };

  const handlePermissionToggle = (permission: string) => {
    const currentPermissions = watch("permissions") || [];
    const newPermissions = currentPermissions.includes(permission)
      ? currentPermissions.filter((p) => p !== permission)
      : [...currentPermissions, permission];
    setValue("permissions", newPermissions);
  };

  const handleEnvironmentToggle = (environment: string) => {
    const currentEnvironments = watch("environments") || [];
    const newEnvironments = currentEnvironments.includes(environment)
      ? currentEnvironments.filter((e) => e !== environment)
      : [...currentEnvironments, environment];
    setValue("environments", newEnvironments);
  };

  const isLoading = grantShareMutation.isPending;

  return (
    <Drawer
      anchor="right"
      open={open}
      onClose={handleClose}
      slotProps={{
        paper: {
          sx: { width: { xs: "100%", sm: 600 } },
        },
      }}
    >
      <Box sx={{ p: 3 }}>
        <Box
          sx={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            mb: 3,
          }}
        >
          <Typography variant="h6" fontWeight="bold">
            Grant Service Share
          </Typography>
          <IconButton onClick={handleClose} size="small">
            <CloseIcon />
          </IconButton>
        </Box>

        <Divider sx={{ mb: 3 }} />

        <Box component="form" onSubmit={handleSubmit(onSubmit)}>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}

          {/* Grant To Type */}
          <FormControl fullWidth margin="normal" error={!!errors.grantToType}>
            <InputLabel>Grant To</InputLabel>
            <Select<"USER" | "TEAM">
              {...register("grantToType")}
              label="Grant To"
              value={grantToType}
              onChange={(e) => {
                const value = e.target.value;
                setGrantToType(value);
                setValue("grantToType", value);
                setValue("grantToId", ""); // Reset selection when type changes
              }}
              disabled={isLoading}
            >
              <MenuItem value="TEAM">Team</MenuItem>
              <MenuItem value="USER">User</MenuItem>
            </Select>
            {errors.grantToType && (
              <Typography
                variant="caption"
                color="error"
                sx={{ mt: 0.5, ml: 1.5 }}
              >
                {errors.grantToType.message}
              </Typography>
            )}
          </FormControl>

          {/* Grant To ID */}
          <FormControl fullWidth margin="normal" error={!!errors.grantToId}>
            <InputLabel>
              {grantToType === "TEAM" ? "Select Team" : "Select User"}
            </InputLabel>
            <Select
              {...register("grantToId")}
              label={grantToType === "TEAM" ? "Select Team" : "Select User"}
              disabled={
                isLoading ||
                (grantToType === "TEAM" ? teamsLoading : usersLoading)
              }
            >
              {grantToType === "TEAM"
                ? teamsData?.items?.map((team) => (
                    <MenuItem key={team.teamId} value={team.teamId}>
                      <Box>
                        <Typography variant="body1" fontWeight="medium">
                          {team.displayName}
                        </Typography>
                      </Box>
                    </MenuItem>
                  ))
                : usersData?.items?.map((user: IamUserResponse) => (
                    <MenuItem key={user.userId} value={user.userId}>
                      <Box>
                        <Typography variant="body1" fontWeight="medium">
                          {user.firstName} {user.lastName}
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                          {user.email}
                        </Typography>
                      </Box>
                    </MenuItem>
                  ))}
            </Select>
            {errors.grantToId && (
              <Typography
                variant="caption"
                color="error"
                sx={{ mt: 0.5, ml: 1.5 }}
              >
                {errors.grantToId.message}
              </Typography>
            )}
          </FormControl>

          {/* Permissions */}
          <Box sx={{ mt: 3 }}>
            <Typography variant="subtitle2" gutterBottom>
              Permissions *
            </Typography>
            <FormGroup>
              {PERMISSIONS.map((permission) => (
                <FormControlLabel
                  key={permission.value}
                  control={
                    <Checkbox
                      checked={
                        watch("permissions")?.includes(permission.value) ||
                        false
                      }
                      onChange={() => handlePermissionToggle(permission.value)}
                      disabled={isLoading}
                    />
                  }
                  label={permission.label}
                />
              ))}
            </FormGroup>
            {errors.permissions && (
              <Typography variant="caption" color="error" sx={{ mt: 0.5 }}>
                {errors.permissions.message}
              </Typography>
            )}
          </Box>

          {/* Environments */}
          <Box sx={{ mt: 3 }}>
            <Typography variant="subtitle2" gutterBottom>
              Environments (Optional)
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
              Leave empty to grant access to all environments
            </Typography>
            <Box sx={{ display: "flex", gap: 1, flexWrap: "wrap" }}>
              {ENVIRONMENTS.map((env) => (
                <Chip
                  key={env}
                  label={env.toUpperCase()}
                  onClick={() => handleEnvironmentToggle(env)}
                  color={
                    watch("environments")?.includes(env) ? "primary" : "default"
                  }
                  variant={
                    watch("environments")?.includes(env) ? "filled" : "outlined"
                  }
                  disabled={isLoading}
                />
              ))}
            </Box>
          </Box>

          {/* Expires At */}
          <TextField
            {...register("expiresAt")}
            label="Expires At (Optional)"
            type="datetime-local"
            fullWidth
            margin="normal"
            slotProps={{ inputLabel: { shrink: true } }}
            disabled={isLoading}
            error={!!errors.expiresAt}
            helperText={errors.expiresAt?.message}
          />

          {(teamsLoading || usersLoading) && (
            <Box display="flex" justifyContent="center" py={2}>
              <CircularProgress size={24} />
              <Typography variant="body2" sx={{ ml: 1 }}>
                Loading {grantToType === "TEAM" ? "teams" : "users"}...
              </Typography>
            </Box>
          )}
        </Box>

        <Divider sx={{ my: 3 }} />

        <Box sx={{ display: "flex", gap: 2, justifyContent: "flex-end" }}>
          <Button
            onClick={handleClose}
            disabled={isLoading}
            sx={{ textTransform: "none" }}
          >
            Cancel
          </Button>
          <Button
            onClick={handleSubmit(onSubmit)}
            variant="contained"
            disabled={isLoading || teamsLoading || usersLoading}
            sx={{ textTransform: "none" }}
          >
            {isLoading ? (
              <>
                <CircularProgress size={16} sx={{ mr: 1 }} />
                Granting...
              </>
            ) : (
              "Grant Share"
            )}
          </Button>
        </Box>
      </Box>
    </Drawer>
  );
};
