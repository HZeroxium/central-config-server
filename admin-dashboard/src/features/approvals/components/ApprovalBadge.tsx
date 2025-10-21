import React from 'react';
import { Chip, type ChipProps } from '@mui/material';

export type ApprovalStatusType = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

interface ApprovalBadgeProps extends ChipProps {
  status: ApprovalStatusType;
}

const getStatusColor = (status: ApprovalStatusType): ChipProps['color'] => {
  switch (status) {
    case 'APPROVED':
      return 'success';
    case 'REJECTED':
      return 'error';
    case 'PENDING':
      return 'warning';
    case 'CANCELLED':
      return 'default';
    default:
      return 'default';
  }
};

const getStatusIcon = (status: ApprovalStatusType) => {
  switch (status) {
    case 'APPROVED':
      return '✅';
    case 'REJECTED':
      return '❌';
    case 'PENDING':
      return '⏳';
    case 'CANCELLED':
      return '🚫';
    default:
      return '⚪';
  }
};

export const ApprovalBadge: React.FC<ApprovalBadgeProps> = ({ status, ...props }) => {
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
