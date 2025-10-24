import { Tooltip, Chip } from '@mui/material';
import { Warning as WarningIcon, CheckCircle as CheckIcon } from '@mui/icons-material';
import { format } from 'date-fns';

interface DriftIndicatorProps {
  hasDrift?: boolean;
  driftDetectedAt?: string;
}

export default function DriftIndicator({ hasDrift, driftDetectedAt }: DriftIndicatorProps) {
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
    ? `Drift detected at ${format(new Date(driftDetectedAt), 'MMM dd, yyyy HH:mm')}`
    : 'Configuration drift detected';

  return (
    <Tooltip title={tooltipTitle}>
      <Chip
        label="Drift"
        color="warning"
        size="small"
        icon={<WarningIcon />}
      />
    </Tooltip>
  );
}
