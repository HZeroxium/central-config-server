import React from 'react';
import { Tabs, Tab, Box, Typography } from '@mui/material';
import { TabPanel } from '@components/common/TabPanel';

interface EnvironmentTabsProps {
  value: number;
  onChange: (event: React.SyntheticEvent, newValue: number) => void;
  environments: string[];
  children: React.ReactNode;
}

export const EnvironmentTabs: React.FC<EnvironmentTabsProps> = ({
  value,
  onChange,
  environments,
  children,
}) => {
  const allEnvironments = ['all', ...environments];

  return (
    <Box>
      <Tabs
        value={value}
        onChange={onChange}
        variant="scrollable"
        scrollButtons="auto"
        sx={{
          borderBottom: 1,
          borderColor: 'divider',
          mb: 2,
        }}
      >
        {allEnvironments.map((env) => (
          <Tab
            key={env}
            label={
              <Box display="flex" alignItems="center" gap={1}>
                <Typography variant="body2" fontWeight={500}>
                  {env === 'all' ? 'All Environments' : env.toUpperCase()}
                </Typography>
              </Box>
            }
            sx={{ textTransform: 'none' }}
          />
        ))}
      </Tabs>

      {allEnvironments.map((env, index) => (
        <TabPanel key={env} value={value} index={index}>
          {children}
        </TabPanel>
      ))}
    </Box>
  );
};

export default EnvironmentTabs;
