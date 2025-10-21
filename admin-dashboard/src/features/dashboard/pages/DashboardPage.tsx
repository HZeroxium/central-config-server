import React from 'react';
import { Box, Grid } from '@mui/material';
import {
  Apps as AppsIcon,
  Storage as StorageIcon,
  Warning as WarningIcon,
  Assignment as AssignmentIcon,
} from '@mui/icons-material';
import { PageHeader } from '@components/common/PageHeader';
import { StatsCard } from '../components/StatsCard';
import { ServiceDistributionChart } from '../components/ServiceDistributionChart';
import { InstanceStatusChart } from '../components/InstanceStatusChart';
import { DriftEventsChart } from '../components/DriftEventsChart';
import { RecentActivityList } from '../components/RecentActivityList';

const DashboardPage: React.FC = () => {
  // Mock data - in a real app, this would come from API calls
  const statsData = {
    totalServices: 42,
    totalInstances: 156,
    pendingApprovals: 8,
    unresolvedDrifts: 12,
  };

  const serviceDistributionData = [
    { name: 'Team Alpha', value: 15, color: '#2563eb' },
    { name: 'Team Beta', value: 12, color: '#60a5fa' },
    { name: 'Team Gamma', value: 10, color: '#93c5fd' },
    { name: 'Team Delta', value: 5, color: '#dbeafe' },
  ];

  const instanceStatusData = [
    { name: 'UP', value: 142, color: '#10b981' },
    { name: 'DOWN', value: 8, color: '#ef4444' },
    { name: 'UNKNOWN', value: 6, color: '#f59e0b' },
  ];

  const driftEventsData = [
    { date: '2024-01-01', critical: 2, high: 4, medium: 8, low: 12 },
    { date: '2024-01-02', critical: 1, high: 3, medium: 6, low: 10 },
    { date: '2024-01-03', critical: 3, high: 5, medium: 7, low: 9 },
    { date: '2024-01-04', critical: 2, high: 4, medium: 9, low: 11 },
    { date: '2024-01-05', critical: 1, high: 2, medium: 5, low: 8 },
    { date: '2024-01-06', critical: 2, high: 3, medium: 6, low: 10 },
    { date: '2024-01-07', critical: 1, high: 4, medium: 8, low: 12 },
  ];

  const recentActivities = [
    {
      id: '1',
      type: 'approval' as const,
      message: 'Service ownership request approved for user-service',
      timestamp: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
    },
    {
      id: '2',
      type: 'drift' as const,
      message: 'Critical drift detected in payment-service',
      timestamp: new Date(Date.now() - 4 * 60 * 60 * 1000).toISOString(),
      severity: 'critical' as const,
    },
    {
      id: '3',
      type: 'service' as const,
      message: 'New service auth-service registered',
      timestamp: new Date(Date.now() - 6 * 60 * 60 * 1000).toISOString(),
    },
    {
      id: '4',
      type: 'drift' as const,
      message: 'Medium drift detected in notification-service',
      timestamp: new Date(Date.now() - 8 * 60 * 60 * 1000).toISOString(),
      severity: 'medium' as const,
    },
    {
      id: '5',
      type: 'approval' as const,
      message: 'Config change request pending approval',
      timestamp: new Date(Date.now() - 12 * 60 * 60 * 1000).toISOString(),
    },
  ];

  return (
    <Box>
      <PageHeader title="Dashboard" />
      
      {/* Stats Cards */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <StatsCard
            title="Total Services"
            value={statsData.totalServices}
            icon={<AppsIcon />}
            color="primary"
            trend={{ value: 12, isPositive: true }}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <StatsCard
            title="Service Instances"
            value={statsData.totalInstances}
            icon={<StorageIcon />}
            color="info"
            trend={{ value: 5, isPositive: true }}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <StatsCard
            title="Pending Approvals"
            value={statsData.pendingApprovals}
            icon={<AssignmentIcon />}
            color="warning"
            trend={{ value: 25, isPositive: false }}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <StatsCard
            title="Unresolved Drifts"
            value={statsData.unresolvedDrifts}
            icon={<WarningIcon />}
            color="error"
            trend={{ value: 8, isPositive: true }}
          />
        </Grid>
      </Grid>

      {/* Charts Row */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid size={{ xs: 12, md: 6 }}>
          <ServiceDistributionChart data={serviceDistributionData} />
        </Grid>
        <Grid size={{ xs: 12, md: 6 }}>
          <InstanceStatusChart data={instanceStatusData} />
        </Grid>
      </Grid>

      {/* Bottom Row */}
      <Grid container spacing={3}>
        <Grid size={{ xs: 12, lg: 8 }}>
          <DriftEventsChart data={driftEventsData} />
        </Grid>
        <Grid size={{ xs: 12, lg: 4 }}>
          <RecentActivityList activities={recentActivities} />
        </Grid>
      </Grid>
    </Box>
  );
};

export default DashboardPage;