import { Chip, type ChipProps } from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import ErrorIcon from '@mui/icons-material/Error';
import WarningIcon from '@mui/icons-material/Warning';
import HelpIcon from '@mui/icons-material/Help';
import type { ServiceInstanceResponseStatus } from '@lib/api/models';

interface InstanceStatusChipProps {
  status?: ServiceInstanceResponseStatus | string;
  size?: 'small' | 'medium';
}

const getStatusConfig = (
  status?: ServiceInstanceResponseStatus | string
): { label: string; color: ChipProps['color']; icon: React.ReactElement } => {
  switch (status) {
    case 'HEALTHY':
      return {
        label: 'Healthy',
        color: 'success',
        icon: <CheckCircleIcon />,
      };
    case 'UNHEALTHY':
      return {
        label: 'Unhealthy',
        color: 'error',
        icon: <ErrorIcon />,
      };
    case 'DRIFT':
      return {
        label: 'Drift',
        color: 'warning',
        icon: <WarningIcon />,
      };
    case 'UNKNOWN':
    default:
      return {
        label: 'Unknown',
        color: 'default',
        icon: <HelpIcon />,
      };
  }
};

export default function InstanceStatusChip({ status, size = 'small' }: InstanceStatusChipProps) {
  const config = getStatusConfig(status);

  return (
    <Chip
      label={config.label}
      color={config.color}
      icon={config.icon}
      size={size}
      variant="outlined"
    />
  );
}
