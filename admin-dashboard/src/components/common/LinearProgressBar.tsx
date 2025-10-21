import React from 'react';
import { LinearProgress, Box } from '@mui/material';

interface LinearProgressBarProps {
  loading?: boolean;
  position?: 'top' | 'bottom';
}

export const LinearProgressBar: React.FC<LinearProgressBarProps> = ({
  loading = false,
  position = 'top',
}) => {
  if (!loading) return null;

  return (
    <Box
      sx={{
        position: 'fixed',
        left: 0,
        right: 0,
        zIndex: 9999,
        [position]: 0,
      }}
    >
      <LinearProgress
        color="primary"
        sx={{
          height: 3,
          '& .MuiLinearProgress-bar': {
            backgroundColor: '#2563eb',
          },
        }}
      />
    </Box>
  );
};

export default LinearProgressBar;
