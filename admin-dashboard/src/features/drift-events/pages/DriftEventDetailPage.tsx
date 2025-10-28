import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  Box,
  Button,
  Card,
  CardContent,
  Typography,
  Chip,
  Divider,
  TextField,
  Alert,
  Paper,
} from "@mui/material";
import Grid from "@mui/material/Grid";
import {
  ArrowBack as BackIcon,
  CheckCircle as ResolveIcon,
  Warning as WarningIcon,
  Code as CodeIcon,
} from "@mui/icons-material";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import PageHeader from "@components/common/PageHeader";
import Loading from "@components/common/Loading";
import { useFindDriftEventById, useUpdateDriftEvent } from "@lib/api/hooks";
import { useAuth } from "@features/auth/context";
import { toast } from "@lib/toast/toast";
import { handleApiError } from "@lib/api/errorHandler";
import { format } from "date-fns";

const resolveSchema = z.object({
  note: z.string().min(1, "Resolution note is required"),
});

type ResolveFormData = z.infer<typeof resolveSchema>;

export default function DriftEventDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { isSysAdmin, permissions } = useAuth();
  const [resolveDialogOpen, setResolveDialogOpen] = useState(false);

  const {
    data: driftEvent,
    isLoading,
    error,
    refetch,
  } = useFindDriftEventById(id!, {
    query: {
      enabled: !!id,
      staleTime: 10_000,
    },
  });

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<ResolveFormData>({
    resolver: zodResolver(resolveSchema),
  });

  const updateDriftEventMutation = useUpdateDriftEvent({
    mutation: {
      onSuccess: () => {
        toast.success("Drift event resolved successfully");
        setResolveDialogOpen(false);
        reset();
        refetch();
      },
      onError: (error) => {
        handleApiError(error);
      },
    },
  });

  const canResolve =
    isSysAdmin || permissions?.actions?.["DRIFT_EVENT"]?.includes("UPDATE");

  const handleBack = () => {
    navigate("/drift-events");
  };

  const handleResolve = () => {
    setResolveDialogOpen(true);
  };

  const onSubmitResolve = (data: ResolveFormData) => {
    if (!id) return;

    updateDriftEventMutation.mutate({
      id,
      data: {
        status: "RESOLVED",
        notes: data.note,
      },
    });
  };

  const getSeverityColor = (severity: string | undefined) => {
    switch (severity?.toUpperCase()) {
      case "CRITICAL":
        return "error";
      case "HIGH":
        return "warning";
      case "MEDIUM":
        return "info";
      case "LOW":
        return "default";
      default:
        return "default";
    }
  };

  const getStatusColor = (status: string | undefined) => {
    switch (status?.toUpperCase()) {
      case "DETECTED":
        return "warning";
      case "RESOLVED":
        return "success";
      case "IGNORED":
        return "default";
      default:
        return "default";
    }
  };

  if (isLoading) {
    return <Loading />;
  }

  if (error || !driftEvent) {
    const errorMessage = error
      ? error.detail || "Drift event not found or access denied."
      : "Drift event not found or access denied.";

    return (
      <Box>
        <PageHeader
          title="Drift Event Details"
          actions={
            <Button
              variant="outlined"
              startIcon={<BackIcon />}
              onClick={handleBack}
            >
              Back to Drift Events
            </Button>
          }
        />
        <Alert
          severity="error"
          action={
            <Button color="inherit" size="small" onClick={handleBack}>
              Go Back
            </Button>
          }
        >
          <strong>Drift Event Not Found</strong>
          <br />
          {errorMessage}
        </Alert>
      </Box>
    );
  }

  return (
    <Box>
      <PageHeader
        title={`Drift Event: ${driftEvent.instanceId}`}
        subtitle={`${driftEvent.severity} severity - ${driftEvent.status}`}
        actions={
          <Box sx={{ display: "flex", gap: 1 }}>
            <Button
              variant="outlined"
              startIcon={<BackIcon />}
              onClick={handleBack}
            >
              Back
            </Button>
            {canResolve && driftEvent.status === "DETECTED" && (
              <Button
                variant="contained"
                color="success"
                startIcon={<ResolveIcon />}
                onClick={handleResolve}
              >
                Resolve
              </Button>
            )}
          </Box>
        }
      />

      {/* Event Information */}
      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Event Information
          </Typography>
          <Divider sx={{ mb: 2 }} />

          <Grid container spacing={3}>
            <Grid size={{ xs: 12, md: 6 }}>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Event ID
              </Typography>
              <Typography variant="body1" fontFamily="monospace">
                {driftEvent.id}
              </Typography>
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Service ID
              </Typography>
              <Typography variant="body1" fontFamily="monospace">
                {driftEvent.instanceId}
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
              <Typography variant="body1" fontFamily="monospace">
                {driftEvent.instanceId}
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
                label={driftEvent.environment?.toUpperCase() || "N/A"}
                color={
                  driftEvent.environment === "prod"
                    ? "error"
                    : driftEvent.environment === "staging"
                    ? "warning"
                    : "info"
                }
              />
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Severity
              </Typography>
              <Chip
                label={driftEvent.severity}
                color={getSeverityColor(driftEvent.severity)}
                icon={<WarningIcon />}
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
              <Chip
                label={driftEvent.status}
                color={getStatusColor(driftEvent.status)}
              />
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Detected At
              </Typography>
              <Typography variant="body1">
                {driftEvent.detectedAt
                  ? format(
                      new Date(driftEvent.detectedAt),
                      "MMM dd, yyyy HH:mm:ss"
                    )
                  : "N/A"}
              </Typography>
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Resolved At
              </Typography>
              <Typography variant="body1">
                {driftEvent.resolvedAt
                  ? format(
                      new Date(driftEvent.resolvedAt),
                      "MMM dd, yyyy HH:mm:ss"
                    )
                  : "Not resolved"}
              </Typography>
            </Grid>

            {driftEvent.notes && (
              <Grid size={{ xs: 12 }}>
                <Typography
                  variant="subtitle2"
                  color="text.secondary"
                  gutterBottom
                >
                  Notes
                </Typography>
                <Typography variant="body1">{driftEvent.notes}</Typography>
              </Grid>
            )}
          </Grid>
        </CardContent>
      </Card>

      {/* Hash Comparison */}
      <Card sx={{ mt: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Configuration Hash Comparison
          </Typography>
          <Divider sx={{ mb: 2 }} />

          <Grid container spacing={3}>
            <Grid size={{ xs: 12, md: 6 }}>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Expected Hash
              </Typography>
              <Paper
                variant="outlined"
                sx={{
                  p: 2,
                  backgroundColor: "success.light",
                }}
              >
                <Typography
                  variant="body2"
                  fontFamily="monospace"
                  sx={{ wordBreak: "break-all" }}
                >
                  {driftEvent.expectedHash || "N/A"}
                </Typography>
              </Paper>
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Applied Hash
              </Typography>
              <Paper
                variant="outlined"
                sx={{
                  p: 2,
                  backgroundColor: "error.light",
                }}
              >
                <Typography
                  variant="body2"
                  fontFamily="monospace"
                  sx={{ wordBreak: "break-all" }}
                >
                  {driftEvent.appliedHash || "N/A"}
                </Typography>
              </Paper>
            </Grid>
          </Grid>

          {driftEvent.expectedHash && driftEvent.appliedHash && (
            <Box sx={{ mt: 2 }}>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Hash Difference
              </Typography>
              <Alert severity="warning" icon={<CodeIcon />}>
                Configuration hashes do not match. This indicates a
                configuration drift.
              </Alert>
            </Box>
          )}
        </CardContent>
      </Card>

      {/* Resolution Form Dialog */}
      {resolveDialogOpen && (
        <Card sx={{ mt: 3 }}>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Resolve Drift Event
            </Typography>
            <Divider sx={{ mb: 2 }} />

            <Box component="form" onSubmit={handleSubmit(onSubmitResolve)}>
              <TextField
                {...register("note")}
                label="Resolution Note"
                multiline
                rows={4}
                fullWidth
                margin="normal"
                placeholder="Describe how this drift was resolved..."
                error={!!errors.note}
                helperText={errors.note?.message}
                disabled={updateDriftEventMutation.isPending}
              />

              <Box
                sx={{
                  display: "flex",
                  gap: 2,
                  justifyContent: "flex-end",
                  mt: 2,
                }}
              >
                <Button
                  onClick={() => setResolveDialogOpen(false)}
                  disabled={updateDriftEventMutation.isPending}
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  variant="contained"
                  color="success"
                  disabled={updateDriftEventMutation.isPending}
                >
                  {updateDriftEventMutation.isPending
                    ? "Resolving..."
                    : "Mark as Resolved"}
                </Button>
              </Box>
            </Box>
          </CardContent>
        </Card>
      )}
    </Box>
  );
}
