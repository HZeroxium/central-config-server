import { Chip } from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import WarningIcon from '@mui/icons-material/Warning';
import ErrorIcon from '@mui/icons-material/Error';
import HelpIcon from '@mui/icons-material/Help';

interface ConsulHealthBadgeProps {
  status: 'passing' | 'warning' | 'critical' | 'unknown';
  size?: 'small' | 'medium';
}

export default function ConsulHealthBadge({ status, size = 'small' }: ConsulHealthBadgeProps) {
  const getConfig = () => {
    switch (status) {
      case 'passing':
        return {
          label: 'Passing',
          color: 'success' as const,
          icon: <CheckCircleIcon />,
        };
      case 'warning':
        return {
          label: 'Warning',
          color: 'warning' as const,
          icon: <WarningIcon />,
        };
      case 'critical':
        return {
          label: 'Critical',
          color: 'error' as const,
          icon: <ErrorIcon />,
        };
      default:
        return {
          label: 'Unknown',
          color: 'default' as const,
          icon: <HelpIcon />,
        };
    }
  };

  const config = getConfig();

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

