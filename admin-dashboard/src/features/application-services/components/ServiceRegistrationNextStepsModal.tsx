import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Box,
  Typography,
  Stepper,
  Step,
  StepLabel,
  StepContent,
  Alert,
  Link,
} from "@mui/material";
import {
  CheckCircle as CheckCircleIcon,
  VpnKey as KeyIcon,
  Code as CodeIcon,
  PlayArrow as ActivateIcon,
} from "@mui/icons-material";
import { useNavigate } from "react-router-dom";

interface ServiceRegistrationNextStepsModalProps {
  open: boolean;
  onClose: () => void;
  serviceId: string;
  approvalRequestId: string;
}

const steps = [
  {
    label: "Waiting for Admin Approval",
    description:
      "Your service registration request has been submitted and is pending admin approval.",
    icon: <CheckCircleIcon />,
  },
  {
    label: "Get Credentials",
    description:
      "After approval, navigate to the Credentials tab to retrieve your client credentials.",
    icon: <KeyIcon />,
  },
  {
    label: "Update Config Files",
    description:
      "Add the credentials to your service configuration files in the Config Files tab.",
    icon: <CodeIcon />,
  },
  {
    label: "Activate Credentials",
    description:
      "Once config files are ready, activate your credentials in the Credentials tab.",
    icon: <ActivateIcon />,
  },
];

export function ServiceRegistrationNextStepsModal({
  open,
  onClose,
  serviceId,
  approvalRequestId,
}: ServiceRegistrationNextStepsModalProps) {
  const navigate = useNavigate();

  const handleViewApproval = () => {
    if (approvalRequestId) {
      navigate(`/approvals/${approvalRequestId}`);
      onClose();
    }
  };

  const handleViewService = () => {
    if (serviceId) {
      navigate(`/application-services/${serviceId}`);
      onClose();
    }
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="md"
      fullWidth
      aria-labelledby="next-steps-dialog-title"
    >
      <DialogTitle id="next-steps-dialog-title">
        Service Registration Submitted
      </DialogTitle>
      <DialogContent>
        <Alert severity="success" sx={{ mb: 3 }}>
          Your service registration request has been submitted successfully and
          is waiting for admin approval.
        </Alert>

        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Follow these steps to complete the setup:
        </Typography>

        <Stepper orientation="vertical">
          {steps.map((step, index) => (
            <Step key={step.label} active={index === 0} completed={false}>
              <StepLabel
                StepIconComponent={() => (
                  <Box
                    sx={{
                      color: index === 0 ? "primary.main" : "text.disabled",
                    }}
                  >
                    {step.icon}
                  </Box>
                )}
              >
                <Typography variant="subtitle1" fontWeight={600}>
                  {step.label}
                </Typography>
              </StepLabel>
              <StepContent>
                <Typography variant="body2" color="text.secondary">
                  {step.description}
                </Typography>
              </StepContent>
            </Step>
          ))}
        </Stepper>

        <Box sx={{ mt: 3, p: 2, bgcolor: "background.default", borderRadius: 1 }}>
          <Typography variant="subtitle2" gutterBottom>
            Quick Links:
          </Typography>
          <Box sx={{ display: "flex", flexDirection: "column", gap: 1 }}>
            {approvalRequestId && (
              <Link
                component="button"
                variant="body2"
                onClick={handleViewApproval}
                sx={{ textAlign: "left", cursor: "pointer" }}
              >
                View Approval Request
              </Link>
            )}
            {serviceId && (
              <Link
                component="button"
                variant="body2"
                onClick={handleViewService}
                sx={{ textAlign: "left", cursor: "pointer" }}
              >
                View Service Details
              </Link>
            )}
          </Box>
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Close</Button>
        {approvalRequestId && (
          <Button variant="contained" onClick={handleViewApproval}>
            View Approval Request
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
}

