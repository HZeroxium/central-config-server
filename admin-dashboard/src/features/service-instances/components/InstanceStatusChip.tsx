import React from 'react';
import { Chip, type ChipProps } from '@mui/material';

export type InstanceStatusType = 'UP' | 'DOWN' | 'UNKNOWN';

interface InstanceStatusChipProps extends ChipProps {
  status: InstanceStatusType;
}

const getStatusColor = (status: InstanceStatusType): ChipProps['color'] => {
  switch (status) {
    case 'UP':
      return 'success';
    case 'DOWN':
      return 'error';
    case 'UNKNOWN':
      return 'warning';
    default:
      return 'default';
  }
};

const getStatusIcon = (status: InstanceStatusType) => {
  switch (status) {
    case 'UP':
      return '🟢';
    case 'DOWN':
      return '🔴';
    case 'UNKNOWN':
      return '🟡';
    default:
      return '⚪';
  }
};

export const InstanceStatusChip: React.FC<InstanceStatusChipProps> = ({ status, ...props }) => {
  const color = getStatusColor(status);
  const icon = getStatusIcon(status);
  
  return (
    <Chip
      label={`${icon} ${status}`}
      color={color}
      size="small"
      {...props}
    />
  );
};
