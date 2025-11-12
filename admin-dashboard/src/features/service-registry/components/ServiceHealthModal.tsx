import React from "react";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  Box,
  Grid,
  Divider,
  Stack,
} from "@mui/material";
import {
  HealthAndSafety as HealthIcon,
  Close as CloseIcon,
} from "@mui/icons-material";
import { format } from "date-fns";
import type { ServiceInstanceSummary } from "@lib/api/models";
import ConsulHealthBadge from "./ConsulHealthBadge";

interface ServiceHealthModalProps {
  open: boolean;
  onClose: () => void;
  serviceName: string;
  instances: ServiceInstanceSummary[];
}

export const ServiceHealthModal: React.FC<ServiceHealthModalProps> = ({
  open,
  onClose,
  serviceName,
  instances,
}) => {
  const healthyCount = instances.filter((i) => i.healthy).length;
  const totalCount = instances.length;

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="md"
      fullWidth
      aria-labelledby="health-modal-title"
      aria-describedby="health-modal-description"
    >
      <DialogTitle
        id="health-modal-title"
        sx={{ display: "flex", alignItems: "center", gap: 1 }}
      >
        <HealthIcon color="primary" />
        <Box sx={{ flex: 1 }}>
          <Typography variant="h6">Service Health: {serviceName}</Typography>
          <Typography variant="body2" color="text.secondary">
            {healthyCount} of {totalCount} instances healthy
          </Typography>
        </Box>
      </DialogTitle>
      <DialogContent dividers>
        <Box id="health-modal-description">
          {instances.length === 0 ? (
            <Box sx={{ textAlign: "center", py: 4 }}>
              <Typography variant="body2" color="text.secondary">
                No instances found for this service
              </Typography>
            </Box>
          ) : (
            <Stack spacing={2}>
              {instances.map((instance, index) => (
                <Box key={instance.instanceId || index}>
                  <Grid container spacing={2} sx={{ py: 1 }}>
                    <Grid size={{ xs: 12, md: 3 }}>
                      <Typography variant="body2" color="text.secondary">
                        Instance ID
                      </Typography>
                      <Typography
                        variant="body1"
                        sx={{ fontFamily: "monospace", fontSize: "0.875rem" }}
                      >
                        {instance.instanceId || "N/A"}
                      </Typography>
                    </Grid>
                    <Grid size={{ xs: 12, md: 2 }}>
                      <Typography variant="body2" color="text.secondary">
                        Host
                      </Typography>
                      <Typography variant="body1">{instance.host || "N/A"}</Typography>
                    </Grid>
                    <Grid size={{ xs: 12, md: 1 }}>
                      <Typography variant="body2" color="text.secondary">
                        Port
                      </Typography>
                      <Typography variant="body1">{instance.port || "N/A"}</Typography>
                    </Grid>
                    <Grid size={{ xs: 12, md: 2 }}>
                      <Typography variant="body2" color="text.secondary">
                        Status
                      </Typography>
                      <ConsulHealthBadge
                        status={
                          instance.healthy
                            ? "passing"
                            : ("critical" as "passing" | "warning" | "critical")
                        }
                      />
                    </Grid>
                    <Grid size={{ xs: 12, md: 4 }}>
                      <Typography variant="body2" color="text.secondary">
                        URI
                      </Typography>
                      <Typography
                        variant="body1"
                        sx={{ wordBreak: "break-all", fontSize: "0.875rem" }}
                      >
                        {instance.uri || "N/A"}
                      </Typography>
                    </Grid>
                    {instance.lastSeenAt && (
                      <Grid size={{ xs: 12 }}>
                        <Typography variant="body2" color="text.secondary">
                          Last Seen
                        </Typography>
                        <Typography variant="body2">
                          {format(
                            new Date(instance.lastSeenAt),
                            "MMM dd, yyyy HH:mm:ss"
                          )}
                        </Typography>
                      </Grid>
                    )}
                  </Grid>
                  {index < instances.length - 1 && <Divider sx={{ mt: 2 }} />}
                </Box>
              ))}
            </Stack>
          )}
        </Box>
      </DialogContent>
      <DialogActions>
        <Button
          onClick={onClose}
          startIcon={<CloseIcon />}
          aria-label="Close health modal"
        >
          Close
        </Button>
      </DialogActions>
    </Dialog>
  );
};

