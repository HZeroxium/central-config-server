import { useParams, useNavigate } from "react-router-dom";
import {
  Box,
  Card,
  CardContent,
  Typography,
  Chip,
  Button,
  Alert,
  Divider,
} from "@mui/material";
import Grid from "@mui/material/Grid";
import {
  Delete as DeleteIcon,
  ArrowBack as ArrowBackIcon,
} from "@mui/icons-material";
import PageHeader from "@components/common/PageHeader";
import { DetailPageSkeleton } from "@components/common/skeletons";
import ConfirmDialog from "@components/common/ConfirmDialog";
import { useFindByIdServiceShare, useRevokeServiceShare } from "@lib/api/hooks";
import { useAuth } from "@/features/auth/context";
import { toast } from "@lib/toast/toast";
import { handleApiError } from "@lib/api/errorHandler";
import { format } from "date-fns";
import { useState } from "react";

export default function ServiceShareDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { isSysAdmin } = useAuth();
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);

  const {
    data: share,
    isLoading,
    error,
  } = useFindByIdServiceShare(id!, {
    query: {
      enabled: !!id,
      staleTime: 10_000,
    },
  });

  const revokeShareMutation = useRevokeServiceShare();

  const handleRevoke = async () => {
    if (!id) return;

    revokeShareMutation.mutate(
      { id },
      {
        onSuccess: () => {
          toast.success("Service share revoked successfully");
          navigate("/service-shares");
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

  if (error || !share) {
    return (
      <Box>
        <PageHeader
          title="Service Share Not Found"
          actions={
            <Button
              variant="outlined"
              startIcon={<ArrowBackIcon />}
              onClick={() => navigate("/service-shares")}
            >
              Back
            </Button>
          }
        />
        <Alert severity="error" sx={{ m: 3 }}>
          {error
            ? `Failed to load service share: ${error.detail || "Unknown error"}`
            : "The requested service share could not be found."}
        </Alert>
      </Box>
    );
  }

  return (
    <Box>
      <PageHeader
        title={`Service Share: ${share.serviceId || "N/A"}`}
        subtitle={`Granted to ${share.grantToType}: ${share.grantToId}`}
        actions={
          <>
            <Button
              variant="outlined"
              startIcon={<ArrowBackIcon />}
              onClick={() => navigate("/service-shares")}
            >
              Back
            </Button>
            {isSysAdmin && (
              <Button
                variant="outlined"
                color="error"
                startIcon={<DeleteIcon />}
                onClick={() => setDeleteDialogOpen(true)}
              >
                Revoke
              </Button>
            )}
          </>
        }
      />

      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Share Information
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
              <Typography
                variant="body1"
                fontFamily="monospace"
                fontWeight={600}
              >
                {share.serviceId}
              </Typography>
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Grant To Type
              </Typography>
              <Chip
                label={share.grantToType}
                color={share.grantToType === "TEAM" ? "primary" : "default"}
              />
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Grant To ID
              </Typography>
              <Typography variant="body1">{share.grantToId}</Typography>
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Permissions
              </Typography>
              <Box sx={{ display: "flex", gap: 1, flexWrap: "wrap" }}>
                {(share.permissions || []).map((perm: string) => (
                  <Chip
                    key={perm}
                    label={perm}
                    size="small"
                    variant="outlined"
                  />
                ))}
              </Box>
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Environments
              </Typography>
              <Box sx={{ display: "flex", gap: 1, flexWrap: "wrap" }}>
                {(share.environments || []).length > 0 ? (
                  (share.environments || []).map((env: string) => (
                    <Chip
                      key={env}
                      label={env.toUpperCase()}
                      size="small"
                      variant="filled"
                    />
                  ))
                ) : (
                  <Typography variant="body2" color="text.secondary">
                    All environments
                  </Typography>
                )}
              </Box>
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Created At
              </Typography>
              <Typography variant="body1">
                {share.createdAt
                  ? format(new Date(share.createdAt), "MMM dd, yyyy HH:mm:ss")
                  : "N/A"}
              </Typography>
            </Grid>

            {share.expiresAt && (
              <Grid size={{ xs: 12, md: 6 }}>
                <Typography
                  variant="subtitle2"
                  color="text.secondary"
                  gutterBottom
                >
                  Expires At
                </Typography>
                <Typography variant="body1">
                  {format(new Date(share.expiresAt), "MMM dd, yyyy HH:mm:ss")}
                </Typography>
              </Grid>
            )}
          </Grid>
        </CardContent>
      </Card>

      {/* Revoke Confirmation Dialog */}
      <ConfirmDialog
        open={deleteDialogOpen}
        title="Revoke Service Share"
        message={`Are you sure you want to revoke this service share? The ${share.grantToType?.toLowerCase()} will lose access immediately.`}
        confirmText="Revoke"
        cancelText="Cancel"
        onConfirm={handleRevoke}
        onCancel={() => setDeleteDialogOpen(false)}
        loading={revokeShareMutation.isPending}
      />
    </Box>
  );
}
