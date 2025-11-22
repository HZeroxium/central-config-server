import { useState } from "react";
import {
  Box,
  Card,
  CardContent,
  Typography,
  Checkbox,
  FormControlLabel,
  Button,
  Stack,
  Alert,
} from "@mui/material";
import { CheckCircle, RadioButtonUnchecked } from "@mui/icons-material";

interface ActivateCredentialsChecklistProps {
  onActivate: () => void;
  disabled?: boolean;
}

export function ActivateCredentialsChecklist({
  onActivate,
  disabled = false,
}: ActivateCredentialsChecklistProps) {
  const [configFilesUpdated, setConfigFilesUpdated] = useState(false);
  const [sdkConfigured, setSdkConfigured] = useState(false);

  const allChecked = configFilesUpdated && sdkConfigured;

  return (
    <Card variant="outlined">
      <CardContent>
        <Typography variant="h6" gutterBottom>
          Activation Checklist
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Please confirm that you have completed the following steps before
          activating credentials:
        </Typography>

        <Stack spacing={2}>
          <FormControlLabel
            control={
              <Checkbox
                checked={configFilesUpdated}
                onChange={(e) => setConfigFilesUpdated(e.target.checked)}
                icon={<RadioButtonUnchecked />}
                checkedIcon={<CheckCircle />}
                color="primary"
              />
            }
            label={
              <Box>
                <Typography variant="body1" fontWeight={500}>
                  Config files updated
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Configuration files have been created/updated in the Git
                  repository
                </Typography>
              </Box>
            }
          />

          <FormControlLabel
            control={
              <Checkbox
                checked={sdkConfigured}
                onChange={(e) => setSdkConfigured(e.target.checked)}
                icon={<RadioButtonUnchecked />}
                checkedIcon={<CheckCircle />}
                color="primary"
              />
            }
            label={
              <Box>
                <Typography variant="body1" fontWeight={500}>
                  SDK configured with credentials
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Client credentials have been added to your service
                  configuration
                </Typography>
              </Box>
            }
          />
        </Stack>

        {allChecked && (
          <Alert severity="success" sx={{ mt: 2 }}>
            All steps completed. You can now activate the credentials.
          </Alert>
        )}

        <Box sx={{ mt: 3, display: "flex", justifyContent: "flex-end" }}>
          <Button
            variant="contained"
            onClick={onActivate}
            disabled={!allChecked || disabled}
            size="large"
          >
            Activate Credentials
          </Button>
        </Box>
      </CardContent>
    </Card>
  );
}

