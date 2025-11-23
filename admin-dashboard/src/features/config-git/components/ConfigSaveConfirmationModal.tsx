import { useState, useMemo } from "react";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  Alert,
  Box,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  Collapse,
  IconButton,
  Chip,
  CircularProgress,
  Divider,
} from "@mui/material";
import Grid from "@mui/material/Grid";
import {
  ExpandMore as ExpandMoreIcon,
  ExpandLess as ExpandLessIcon,
  Warning as WarningIcon,
  CheckCircle as CheckCircleIcon,
} from "@mui/icons-material";
import { useFindAllServiceInstances } from "@lib/api/hooks";
import type { Profile } from "../types";
import type { ServiceInstanceResponse } from "@lib/api/models";

interface ConfigSaveConfirmationModalProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  serviceId: string;
  profile: Profile;
  isSaving?: boolean;
}

// Map profile to environment
const PROFILE_TO_ENV: Record<Profile, string> = {
  dev: "dev",
  prod: "prod",
  staging: "staging",
  test: "test",
};

export function ConfigSaveConfirmationModal({
  open,
  onClose,
  onConfirm,
  serviceId,
  profile,
  isSaving = false,
}: ConfigSaveConfirmationModalProps) {
  const [expandedEnvironments, setExpandedEnvironments] = useState<Set<string>>(
    new Set()
  );

  // Fetch all instances for this service
  const { data: instancesData, isLoading: instancesLoading } =
    useFindAllServiceInstances(
      {
        serviceId,
        page: 0,
        size: 1000, // Get all instances
      },
      {
        query: {
          enabled: open && !!serviceId,
          staleTime: 10_000,
        },
      }
    );

  // Group instances by environment
  const instancesByEnv = useMemo(() => {
    if (!instancesData?.items) return {};

    const grouped: Record<string, ServiceInstanceResponse[]> = {};
    instancesData.items.forEach((instance) => {
      if (instance.environment) {
        if (!grouped[instance.environment]) {
          grouped[instance.environment] = [];
        }
        grouped[instance.environment].push(instance);
      }
    });

    return grouped;
  }, [instancesData]);

  // Get instances for the current profile's environment
  const targetEnv = PROFILE_TO_ENV[profile];
  const targetInstances = instancesByEnv[targetEnv] || [];
  const totalInstances = Object.values(instancesByEnv).reduce(
    (sum, instances) => sum + instances.length,
    0
  );

  const handleToggleEnvironment = (env: string) => {
    setExpandedEnvironments((prev) => {
      const next = new Set(prev);
      if (next.has(env)) {
        next.delete(env);
      } else {
        next.add(env);
      }
      return next;
    });
  };

  const handleConfirm = () => {
    onConfirm();
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="md"
      fullWidth
      aria-labelledby="config-save-confirmation-title"
    >
      <DialogTitle id="config-save-confirmation-title">
        Confirm Config File Update
      </DialogTitle>
      <DialogContent>
        <Alert severity="warning" icon={<WarningIcon />} sx={{ mb: 3 }}>
          <Typography variant="body2" fontWeight={600} gutterBottom>
            This will update the configuration for the {profile.toUpperCase()}{" "}
            environment
          </Typography>
          <Typography variant="body2">
            All service instances in this environment will receive the updated
            configuration. Please ensure the changes are correct before
            proceeding.
          </Typography>
        </Alert>

        {instancesLoading ? (
          <Box display="flex" justifyContent="center" p={3}>
            <CircularProgress size={24} />
          </Box>
        ) : (
          <Box>
            <Typography variant="subtitle1" fontWeight={600} gutterBottom>
              Impact Summary
            </Typography>
            <Grid container spacing={2} sx={{ mb: 3 }}>
              <Grid size={{ xs: 12, sm: 6 }}>
                <Box
                  sx={{
                    p: 2,
                    border: 1,
                    borderColor: "divider",
                    borderRadius: 1,
                    bgcolor: targetInstances.length > 0 ? "warning.light" : "grey.50",
                  }}
                >
                  <Typography variant="caption" color="text.secondary">
                    {profile.toUpperCase()} Environment
                  </Typography>
                  <Typography variant="h6" fontWeight={600}>
                    {targetInstances.length} instance
                    {targetInstances.length !== 1 ? "s" : ""}
                  </Typography>
                </Box>
              </Grid>
              <Grid size={{ xs: 12, sm: 6 }}>
                <Box
                  sx={{
                    p: 2,
                    border: 1,
                    borderColor: "divider",
                    borderRadius: 1,
                  }}
                >
                  <Typography variant="caption" color="text.secondary">
                    Total Instances
                  </Typography>
                  <Typography variant="h6" fontWeight={600}>
                    {totalInstances} instance{totalInstances !== 1 ? "s" : ""}
                  </Typography>
                </Box>
              </Grid>
            </Grid>

            {targetInstances.length > 0 && (
              <Box>
                <Typography variant="subtitle2" gutterBottom>
                  Affected Instances ({targetInstances.length})
                </Typography>
                <List dense>
                  {targetInstances.slice(0, 5).map((instance) => (
                    <ListItem key={instance.instanceId}>
                      <ListItemIcon>
                        <CheckCircleIcon color="warning" fontSize="small" />
                      </ListItemIcon>
                      <ListItemText
                        primary={
                          <Typography variant="body2">
                            {instance.instanceId || "Unknown"}
                          </Typography>
                        }
                        secondary={
                          <Typography variant="caption" color="text.secondary">
                            {instance.host}:{instance.port} • Status:{" "}
                            {instance.status || "Unknown"}
                          </Typography>
                        }
                      />
                    </ListItem>
                  ))}
                  {targetInstances.length > 5 && (
                    <ListItem>
                      <ListItemText
                        primary={
                          <Typography variant="body2" color="text.secondary">
                            ... and {targetInstances.length - 5} more instance
                            {targetInstances.length - 5 !== 1 ? "s" : ""}
                          </Typography>
                        }
                      />
                    </ListItem>
                  )}
                </List>
              </Box>
            )}

            {Object.keys(instancesByEnv).length > 1 && (
              <Box sx={{ mt: 3 }}>
                <Typography variant="subtitle2" gutterBottom>
                  All Environments
                </Typography>
                <List>
                  {Object.entries(instancesByEnv).map(([env, instances]) => {
                    const isExpanded = expandedEnvironments.has(env);
                    const isTargetEnv = env === targetEnv;

                    return (
                      <Box key={env}>
                        <ListItem
                          component="button"
                          onClick={() => handleToggleEnvironment(env)}
                          sx={{
                            bgcolor: isTargetEnv ? "warning.light" : "transparent",
                            borderRadius: 1,
                            mb: 0.5,
                          }}
                        >
                          <ListItemText
                            primary={
                              <Box
                                sx={{
                                  display: "flex",
                                  alignItems: "center",
                                  gap: 1,
                                }}
                              >
                                <Typography variant="body2" fontWeight={500}>
                                  {env.toUpperCase()}
                                </Typography>
                                {isTargetEnv && (
                                  <Chip
                                    label="Target"
                                    size="small"
                                    color="warning"
                                  />
                                )}
                                <Chip
                                  label={`${instances.length} instance${
                                    instances.length !== 1 ? "s" : ""
                                  }`}
                                  size="small"
                                  variant="outlined"
                                />
                              </Box>
                            }
                          />
                          <IconButton size="small">
                            {isExpanded ? (
                              <ExpandLessIcon />
                            ) : (
                              <ExpandMoreIcon />
                            )}
                          </IconButton>
                        </ListItem>
                        <Collapse in={isExpanded} timeout="auto" unmountOnExit>
                          <List component="div" disablePadding dense>
                            {instances.map((instance) => (
                              <ListItem
                                key={instance.instanceId}
                                sx={{ pl: 4 }}
                              >
                                <ListItemText
                                  primary={
                                    <Typography variant="body2">
                                      {instance.instanceId || "Unknown"}
                                    </Typography>
                                  }
                                  secondary={
                                    <Typography
                                      variant="caption"
                                      color="text.secondary"
                                    >
                                      {instance.host}:{instance.port} •{" "}
                                      {instance.status || "Unknown"}
                                    </Typography>
                                  }
                                />
                              </ListItem>
                            ))}
                          </List>
                        </Collapse>
                        <Divider />
                      </Box>
                    );
                  })}
                </List>
              </Box>
            )}

            {targetInstances.length === 0 && (
              <Alert severity="info" sx={{ mt: 2 }}>
                No instances are currently running in the {profile.toUpperCase()}{" "}
                environment. The configuration will be applied when instances
                start.
              </Alert>
            )}
          </Box>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={isSaving}>
          Cancel
        </Button>
        <Button
          onClick={handleConfirm}
          variant="contained"
          color="primary"
          disabled={isSaving || instancesLoading}
          startIcon={isSaving ? <CircularProgress size={16} /> : null}
        >
          {isSaving ? "Saving..." : "Confirm & Save"}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

