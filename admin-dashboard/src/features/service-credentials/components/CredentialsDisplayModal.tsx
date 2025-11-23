import { useState } from "react";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Box,
  Typography,
  Alert,
  IconButton,
  InputAdornment,
  Stack,
  Divider,
} from "@mui/material";
import {
  Visibility,
  VisibilityOff,
  ContentCopy,
  Download,
  Warning,
} from "@mui/icons-material";
import type { CredentialsDisplayData } from "../types";
import { toast } from "@lib/toast/toast";

interface CredentialsDisplayModalProps {
  open: boolean;
  onClose: () => void;
  credentials: CredentialsDisplayData;
}

export function CredentialsDisplayModal({
  open,
  onClose,
  credentials,
}: CredentialsDisplayModalProps) {
  const [showSecret, setShowSecret] = useState(false);
  const [copied, setCopied] = useState(false);

  const handleCopySecret = async () => {
    try {
      await navigator.clipboard.writeText(credentials.clientSecret);
      setCopied(true);
      toast.success("Client secret copied to clipboard");
      setTimeout(() => setCopied(false), 2000);
    } catch (error) {
      toast.error("Failed to copy to clipboard");
    }
  };

  const handleCopyClientId = async () => {
    try {
      await navigator.clipboard.writeText(credentials.clientId);
      toast.success("Client ID copied to clipboard");
    } catch (error) {
      toast.error("Failed to copy to clipboard");
    }
  };

  const handleDownloadEnv = () => {
    const envContent = `# Service Credentials
# Generated: ${new Date().toISOString()}
# WARNING: Keep this file secure. Do not commit to version control.

KEYCLOAK_CLIENT_ID=${credentials.clientId}
KEYCLOAK_CLIENT_SECRET=${credentials.clientSecret}
KEYCLOAK_TOKEN_ENDPOINT=${credentials.tokenEndpoint}

# Add to your application.yml:
# zcm:
#   sdk:
#     ping:
#       client-credentials:
#         required: true
#         client-id: ${credentials.clientId}
#         client-secret: ${credentials.clientSecret}
#         token-endpoint: ${credentials.tokenEndpoint}
`;

    const blob = new Blob([envContent], { type: "text/plain" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `.env.${credentials.clientId}`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    toast.success("Credentials downloaded as .env file");
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="sm"
      fullWidth
      aria-labelledby="credentials-dialog-title"
    >
      <DialogTitle id="credentials-dialog-title">
        Service Credentials
      </DialogTitle>
      <DialogContent>
        <Alert severity="warning" icon={<Warning />} sx={{ mb: 3 }}>
          <Typography variant="body2" fontWeight={600} gutterBottom>
            One-time Display
          </Typography>
          <Typography variant="body2">
            These credentials are shown only once. Please save them immediately.
            The client secret cannot be retrieved again after this dialog is
            closed.
          </Typography>
        </Alert>

        <Stack spacing={3}>
          <Box>
            <Typography variant="subtitle2" color="text.secondary" gutterBottom>
              Client ID
            </Typography>
            <TextField
              fullWidth
              value={credentials.clientId}
              InputProps={{
                readOnly: true,
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton
                      onClick={handleCopyClientId}
                      edge="end"
                      size="small"
                      aria-label="Copy client ID"
                    >
                      <ContentCopy fontSize="small" />
                    </IconButton>
                  </InputAdornment>
                ),
              }}
              size="small"
            />
          </Box>

          <Box>
            <Typography variant="subtitle2" color="text.secondary" gutterBottom>
              Client Secret
            </Typography>
            <TextField
              fullWidth
              type={showSecret ? "text" : "password"}
              value={credentials.clientSecret}
              InputProps={{
                readOnly: true,
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton
                      onClick={() => setShowSecret(!showSecret)}
                      edge="end"
                      size="small"
                      aria-label={showSecret ? "Hide secret" : "Show secret"}
                    >
                      {showSecret ? (
                        <VisibilityOff fontSize="small" />
                      ) : (
                        <Visibility fontSize="small" />
                      )}
                    </IconButton>
                    <IconButton
                      onClick={handleCopySecret}
                      edge="end"
                      size="small"
                      aria-label="Copy client secret"
                      color={copied ? "success" : "default"}
                    >
                      <ContentCopy fontSize="small" />
                    </IconButton>
                  </InputAdornment>
                ),
              }}
              size="small"
            />
          </Box>

          {/* <Box>
            <Typography variant="subtitle2" color="text.secondary" gutterBottom>
              Token Endpoint
            </Typography>
            <TextField
              fullWidth
              value={credentials.tokenEndpoint}
              InputProps={{
                readOnly: true,
              }}
              size="small"
            />
          </Box> */}

          {credentials.expiresAt && (
            <Box>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Expires At
              </Typography>
              <Typography variant="body2">
                {new Date(credentials.expiresAt).toLocaleString()}
              </Typography>
            </Box>
          )}

          <Divider />

          <Box>
            <Button
              variant="outlined"
              startIcon={<Download />}
              onClick={handleDownloadEnv}
              fullWidth
            >
              Download as .env file
            </Button>
          </Box>
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} variant="contained">
          I've Saved the Credentials
        </Button>
      </DialogActions>
    </Dialog>
  );
}

