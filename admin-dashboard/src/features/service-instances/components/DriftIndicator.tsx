import { Tooltip, Chip, IconButton } from "@mui/material";
import {
  Warning as WarningIcon,
  CheckCircle as CheckIcon,
} from "@mui/icons-material";
import { useNavigate } from "react-router-dom";
import { format } from "date-fns";

interface DriftIndicatorProps {
  hasDrift?: boolean;
  driftDetectedAt?: string;
  serviceId?: string;
  instanceId?: string;
}

export default function DriftIndicator({
  hasDrift,
  driftDetectedAt,
  serviceId,
  instanceId,
}: Readonly<DriftIndicatorProps>) {
  const navigate = useNavigate();

  const handleDriftClick = () => {
    if (serviceId && instanceId) {
      // Navigate to drift events filtered by this instance
      navigate(`/drift-events?serviceId=${serviceId}&instanceId=${instanceId}`);
    } else {
      // Navigate to all drift events
      navigate("/drift-events");
    }
  };

  if (!hasDrift) {
    return (
      <Chip
        label="No Drift"
        color="success"
        size="small"
        icon={<CheckIcon />}
        variant="outlined"
      />
    );
  }

  const tooltipTitle = driftDetectedAt
    ? `Drift detected at ${format(
        new Date(driftDetectedAt),
        "MMM dd, yyyy HH:mm"
      )}. Click to view drift events.`
    : "Configuration drift detected. Click to view drift events.";

  return (
    <Tooltip title={tooltipTitle}>
      <IconButton size="small" onClick={handleDriftClick} sx={{ p: 0 }}>
        <Chip
          label="Drift"
          color="warning"
          size="small"
          icon={<WarningIcon />}
          clickable
        />
      </IconButton>
    </Tooltip>
  );
}
