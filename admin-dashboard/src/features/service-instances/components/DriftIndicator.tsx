import React from 'react';
import { Tooltip, IconButton } from '@mui/material';
import { Warning as WarningIcon } from '@mui/icons-material';

interface DriftIndicatorProps {
  hasDrift: boolean;
  driftDetails?: string;
}

export const DriftIndicator: React.FC<DriftIndicatorProps> = ({ hasDrift, driftDetails }) => {
  if (!hasDrift) {
    return null;
  }

  return (
    <Tooltip title={driftDetails || 'Configuration drift detected'}>
      <IconButton size="small" color="warning">
        <WarningIcon fontSize="small" />
      </IconButton>
    </Tooltip>
  );
};
