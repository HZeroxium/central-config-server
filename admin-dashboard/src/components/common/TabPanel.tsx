import React from 'react';
import { Box, type BoxProps } from '@mui/material';

interface TabPanelProps extends BoxProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

export const TabPanel: React.FC<TabPanelProps> = ({
  children,
  value,
  index,
  sx,
  ...other
}) => {
  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`simple-tabpanel-${index}`}
      aria-labelledby={`simple-tab-${index}`}
      {...(other as React.HTMLAttributes<HTMLDivElement>)}
    >
      {value === index && (
        <Box sx={sx || { p: 3 }}>
          {children}
        </Box>
      )}
    </div>
  );
};

export default TabPanel;
