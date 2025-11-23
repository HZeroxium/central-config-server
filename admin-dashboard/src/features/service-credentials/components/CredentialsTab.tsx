import { useState } from "react";
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Stack,
  Alert,
  Divider,
  CircularProgress,
} from "@mui/material";
import Grid from "@mui/material/Grid";
import {
  VpnKey as KeyIcon,
  GetApp as GetCredentialsIcon,
  CheckCircle as ActivateIcon,
  Block as RevokeIcon,
  Refresh as RefreshIcon,
} from "@mui/icons-material";
import { CredentialsStatusBadge } from "./CredentialsStatusBadge";
import { CredentialsDisplayModal } from "./CredentialsDisplayModal";
import { ActivateCredentialsChecklist } from "./ActivateCredentialsChecklist";
import { useServiceCredentialsOperations } from "../hooks/useServiceCredentials";
import ConfirmDialog from "@components/common/ConfirmDialog";
import { format } from "date-fns";

interface CredentialsTabProps {
  serviceId: string;
}

export function CredentialsTab({ serviceId }: CredentialsTabProps) {
  const [showCredentialsModal, setShowCredentialsModal] = useState(false);
  const [showActivateChecklist, setShowActivateChecklist] = useState(false);
  const [showRevokeDialog, setShowRevokeDialog] = useState(false);

  const {
    credentials,
    isLoading,
    error,
    refetch,
    activate,
    revoke,
    isActivating,
    isRevoking,
  } = useServiceCredentialsOperations(serviceId);

  const handleGetCredentials = () => {
    refetch().then(() => {
      if (credentials) {
        setShowCredentialsModal(true);
      }
    });
  };

  const handleActivate = () => {
    activate();
    setShowActivateChecklist(false);
  };

  const handleRevoke = () => {
    revoke();
    setShowRevokeDialog(false);
  };

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" p={4}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    // If 404, credentials don't exist yet (not an error)
    if (error.status === 404) {
      return (
        <Card>
          <CardContent>
            <Alert severity="info" icon={<KeyIcon />}>
              <Typography variant="body1" fontWeight={600} gutterBottom>
                No Credentials Found
              </Typography>
              <Typography variant="body2" sx={{ mb: 2 }}>
                Credentials will be automatically created after your service
                ownership request is approved. Once approved, you can retrieve
                your credentials here.
              </Typography>
              <Button
                variant="outlined"
                startIcon={<RefreshIcon />}
                onClick={() => refetch()}
              >
                Refresh
              </Button>
            </Alert>
          </CardContent>
        </Card>
      );
    }

    // For other errors (5xx, network errors), show error with retry button
    return (
      <Card>
        <CardContent>
          <Alert 
            severity="error"
            action={
              <Button
                color="inherit"
                size="small"
                startIcon={<RefreshIcon />}
                onClick={() => refetch()}
              >
                Retry
              </Button>
            }
          >
            <Typography variant="body1" fontWeight={600} gutterBottom>
              Failed to load credentials
            </Typography>
            <Typography variant="body2">
              {error.detail || "Unknown error occurred"}
            </Typography>
          </Alert>
        </CardContent>
      </Card>
    );
  }

  if (!credentials) {
    return (
      <Card>
        <CardContent>
          <Alert severity="info" icon={<KeyIcon />}>
            <Typography variant="body1" fontWeight={600} gutterBottom>
              No Credentials Found
            </Typography>
            <Typography variant="body2" sx={{ mb: 2 }}>
              Credentials will be automatically created after your service
              ownership request is approved.
            </Typography>
            <Button
              variant="outlined"
              startIcon={<RefreshIcon />}
              onClick={() => refetch()}
            >
              Refresh
            </Button>
          </Alert>
        </CardContent>
      </Card>
    );
  }

  return (
    <Box>
      <Card>
        <CardContent>
          <Stack spacing={3}>
            <Box>
              <Typography variant="h6" gutterBottom>
                Service Credentials
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Manage M2M (Machine-to-Machine) authentication credentials for
                this service
              </Typography>
            </Box>

            <Divider />

            <Grid container spacing={3}>
              <Grid size={{ xs: 12, md: 6 }}>
                <Typography
                  variant="subtitle2"
                  color="text.secondary"
                  gutterBottom
                >
                  Status
                </Typography>
                <CredentialsStatusBadge status={credentials.status} />
              </Grid>

              <Grid size={{ xs: 12, md: 6 }}>
                <Typography
                  variant="subtitle2"
                  color="text.secondary"
                  gutterBottom
                >
                  Client ID
                </Typography>
                <Typography variant="body1" fontFamily="monospace">
                  {credentials.clientId}
                </Typography>
              </Grid>

              {credentials.expiresAt && (
                <Grid size={{ xs: 12, md: 6 }}>
                  <Typography
                    variant="subtitle2"
                    color="text.secondary"
                    gutterBottom
                  >
                    Expires At
                  </Typography>
                  <Typography variant="body1">
                    {format(new Date(credentials.expiresAt), "PPpp")}
                  </Typography>
                </Grid>
              )}
            </Grid>

            <Divider />

            <Stack direction="row" spacing={2} flexWrap="wrap">
              {credentials.status === "PENDING" && (
                <>
                  <Button
                    variant="outlined"
                    startIcon={<GetCredentialsIcon />}
                    onClick={handleGetCredentials}
                  >
                    Get Credentials
                  </Button>
                  <Button
                    variant="contained"
                    startIcon={<ActivateIcon />}
                    onClick={() => setShowActivateChecklist(true)}
                    disabled={isActivating}
                  >
                    Activate Credentials
                  </Button>
                </>
              )}

              {credentials.status === "ACTIVE" && (
                <>
                  <Button
                    variant="outlined"
                    startIcon={<GetCredentialsIcon />}
                    onClick={handleGetCredentials}
                  >
                    View Credentials
                  </Button>
                  <Button
                    variant="outlined"
                    color="error"
                    startIcon={<RevokeIcon />}
                    onClick={() => setShowRevokeDialog(true)}
                    disabled={isRevoking}
                  >
                    Revoke Credentials
                  </Button>
                </>
              )}

              {credentials.status === "REVOKED" && (
                <Alert severity="warning">
                  These credentials have been revoked. New tokens cannot be
                  issued. Contact an administrator if you need to restore access.
                </Alert>
              )}
            </Stack>
          </Stack>
        </CardContent>
      </Card>

      {showActivateChecklist && (
        <Box sx={{ mt: 3 }}>
          <ActivateCredentialsChecklist
            onActivate={handleActivate}
            disabled={isActivating}
          />
        </Box>
      )}

      {showCredentialsModal && credentials && (
        <CredentialsDisplayModal
          open={showCredentialsModal}
          onClose={() => setShowCredentialsModal(false)}
          credentials={{
            clientId: credentials.clientId,
            clientSecret: credentials.clientSecret,
            tokenEndpoint: credentials.tokenEndpoint,
            status: credentials.status,
            expiresAt: credentials.expiresAt,
          }}
        />
      )}

      <ConfirmDialog
        open={showRevokeDialog}
        onCancel={() => setShowRevokeDialog(false)}  
        onConfirm={handleRevoke}
        title="Revoke Credentials"
        message="Are you sure you want to revoke these credentials? This will disable the Keycloak client and prevent new tokens from being issued. Existing tokens may still be valid until expiration."
        confirmText="Revoke"
        confirmColor="error"
        loading={isRevoking}
      />
    </Box>
  );
}

