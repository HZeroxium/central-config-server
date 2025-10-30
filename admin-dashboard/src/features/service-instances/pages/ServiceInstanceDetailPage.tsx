import { useParams, useNavigate } from "react-router-dom";
import {
  Box,
  Button,
  Alert,
  Card,
  CardContent,
  Typography,
  Chip,
  Divider,
} from "@mui/material";
import Grid from "@mui/material/Grid";
import {
  ArrowBack as BackIcon,
  Delete as DeleteIcon,
} from "@mui/icons-material";
import { useState } from "react";
import PageHeader from "@components/common/PageHeader";
import { DetailPageSkeleton } from "@components/common/skeletons";
import ConfirmDialog from "@components/common/ConfirmDialog";
import {
  useFindByIdServiceInstance,
  useDeleteServiceInstance,
} from "@lib/api/hooks";
import { usePermissions } from "@features/auth/hooks/usePermissions";
import { toast } from "@lib/toast/toast";
import { handleApiError } from "@lib/api/errorHandler";
import InstanceStatusChip from "../components/InstanceStatusChip";
import DriftIndicator from "../components/DriftIndicator";
import { format } from "date-fns";

export default function ServiceInstanceDetailPage() {
  const { serviceName, instanceId } = useParams<{
    serviceName: string;
    instanceId: string;
  }>();
  const navigate = useNavigate();
  const { canDeleteInstance } = usePermissions();
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);

  const {
    data: instance,
    isLoading,
    error,
  } = useFindByIdServiceInstance(instanceId!, {
    query: {
      enabled: !!instanceId,
      staleTime: 10_000,
    },
  });

  const deleteMutation = useDeleteServiceInstance();

  const handleBack = () => {
    navigate("/service-instances");
  };

  const canDelete = serviceName ? canDeleteInstance(serviceName) : false;

  const handleDelete = async () => {
    if (!instanceId) return;

    deleteMutation.mutate(
      { instanceId },
      {
        onSuccess: () => {
          toast.success("Instance deleted successfully");
          navigate("/service-instances");
        },
        onError: (error) => {
          handleApiError(error);
        },
      }
    );
  };

  if (isLoading) {
    return <DetailPageSkeleton />;
  }

  if (error || !instance) {
    return (
      <Box>
        <PageHeader
          title="Service Instance Details"
          actions={
            <Button
              variant="outlined"
              startIcon={<BackIcon />}
              onClick={handleBack}
            >
              Back to Instances
            </Button>
          }
        />
        <Alert severity="error">
          Failed to load service instance.{" "}
          {error ? error.detail || "Please try again." : "Instance not found."}
        </Alert>
      </Box>
    );
  }

  return (
    <Box>
      <PageHeader
        title={`Instance: ${instance.instanceId}`}
        subtitle={`Service: ${instance.serviceId}`}
        actions={
          <>
            <Button
              variant="outlined"
              startIcon={<BackIcon />}
              onClick={handleBack}
            >
              Back
            </Button>
            {canDelete && (
              <Button
                variant="outlined"
                color="error"
                startIcon={<DeleteIcon />}
                onClick={() => setDeleteDialogOpen(true)}
              >
                Delete
              </Button>
            )}
          </>
        }
      />

      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Instance Information
          </Typography>
          <Divider sx={{ mb: 2 }} />

          <Grid container spacing={3}>
            <Grid size={{ xs: 12, md: 6 }}>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Service ID
              </Typography>
              <Typography variant="body1" fontWeight={600} gutterBottom>
                {instance.serviceId}
              </Typography>
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Instance ID
              </Typography>
              <Typography variant="body1" fontWeight={400} gutterBottom>
                {instance.instanceId}
              </Typography>
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Environment
              </Typography>
              <Chip
                label={instance.environment?.toUpperCase()}
                color={
                  instance.environment === "prod"
                    ? "error"
                    : instance.environment === "staging"
                    ? "warning"
                    : "info"
                }
                size="small"
              />
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Status
              </Typography>
              <InstanceStatusChip status={instance.status} />
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Drift Status
              </Typography>
              <DriftIndicator
                hasDrift={instance.hasDrift}
                driftDetectedAt={instance.driftDetectedAt}
              />
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Version
              </Typography>
              <Typography variant="body1">
                {instance.version || "N/A"}
              </Typography>
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Host
              </Typography>
              <Typography variant="body1">{instance.host}</Typography>
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Port
              </Typography>
              <Typography variant="body1">{instance.port}</Typography>
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Last Seen At
              </Typography>
              <Typography variant="body1">
                {instance.lastSeenAt
                  ? format(
                      new Date(instance.lastSeenAt),
                      "MMM dd, yyyy HH:mm:ss"
                    )
                  : "N/A"}
              </Typography>
            </Grid>

            {instance.driftDetectedAt && (
              <Grid size={{ xs: 12, md: 6 }}>
                <Typography
                  variant="subtitle2"
                  color="text.secondary"
                  gutterBottom
                >
                  Drift Detected At
                </Typography>
                <Typography variant="body1">
                  {format(
                    new Date(instance.driftDetectedAt),
                    "MMM dd, yyyy HH:mm:ss"
                  )}
                </Typography>
              </Grid>
            )}

            {instance.lastAppliedHash && (
              <Grid size={{ xs: 12 }}>
                <Typography
                  variant="subtitle2"
                  color="text.secondary"
                  gutterBottom
                >
                  Last Applied Hash
                </Typography>
                <Typography
                  variant="body2"
                  fontFamily="monospace"
                  sx={{ wordBreak: "break-all" }}
                >
                  {instance.lastAppliedHash}
                </Typography>
              </Grid>
            )}

            {instance.expectedHash && (
              <Grid size={{ xs: 12 }}>
                <Typography
                  variant="subtitle2"
                  color="text.secondary"
                  gutterBottom
                >
                  Expected Hash
                </Typography>
                <Typography
                  variant="body2"
                  fontFamily="monospace"
                  sx={{ wordBreak: "break-all" }}
                >
                  {instance.expectedHash}
                </Typography>
              </Grid>
            )}

            {instance.metadata && Object.keys(instance.metadata).length > 0 && (
              <Grid size={{ xs: 12 }}>
                <Typography
                  variant="subtitle2"
                  color="text.secondary"
                  gutterBottom
                >
                  Metadata
                </Typography>
                <Box
                  component="pre"
                  sx={(theme) => ({
                    bgcolor:
                      theme.palette.mode === "dark" ? "grey.900" : "grey.100",
                    color:
                      theme.palette.mode === "dark" ? "grey.100" : "grey.900",
                    p: 2,
                    borderRadius: 1,
                    overflow: "auto",
                    fontSize: "0.875rem",
                  })}
                >
                  {JSON.stringify(instance.metadata, null, 2)}
                </Box>
              </Grid>
            )}
          </Grid>
        </CardContent>
      </Card>

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        open={deleteDialogOpen}
        title="Delete Service Instance"
        message={`Are you sure you want to delete instance ${instance.instanceId}? This action cannot be undone.`}
        confirmText="Delete"
        cancelText="Cancel"
        onConfirm={handleDelete}
        onCancel={() => setDeleteDialogOpen(false)}
        loading={deleteMutation.isPending}
      />
    </Box>
  );
}
