import React, { useMemo } from 'react';
import { Box, Grid, CircularProgress, Alert } from '@mui/material';
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
import {
  useFindAllApplicationServices,
  useFindAllServiceInstances,
  useFindAllDriftEvents,
  useFindAllApprovalRequests,
  useGetServiceRegistryService,
} from '@lib/api/hooks';
import { useErrorHandler } from '../../../hooks/useErrorHandler';

const DashboardPage: React.FC = () => {
  const { handleError } = useErrorHandler();

  // Fetch data from APIs
  const { data: servicesResponse, isLoading: servicesLoading, error: servicesError } = useFindAllApplicationServices(
    { filter: undefined, pageable: { page: 0, size: 1000 } },
    { query: { staleTime: 60000 } }
  );

  const { data: instancesResponse, isLoading: instancesLoading, error: instancesError } = useFindAllServiceInstances(
    { pageable: { page: 0, size: 1000 } },
    { query: { staleTime: 60000 } }
  );

  const { data: driftsResponse, isLoading: driftsLoading, error: driftsError } = useFindAllDriftEvents(
    { pageable: { page: 0, size: 1000 } },
    { query: { staleTime: 30000 } }
  );

  const { data: approvalsResponse, isLoading: approvalsLoading, error: approvalsError } = useFindAllApprovalRequests(
    { pageable: { page: 0, size: 1000 } },
    { query: { staleTime: 30000 } }
  );

  const { data: registryResponse, isLoading: registryLoading, error: registryError } = useGetServiceRegistryService('services');

  // Get data from API responses
  const servicesData = servicesResponse;
  const instancesData = instancesResponse;
  const driftsData = driftsResponse;
  const approvalsData = approvalsResponse;
  const registryData = registryResponse;

  // Calculate statistics from real data
  const statsData = useMemo(() => {
    const totalServices = servicesData?.content?.length || 0;
    const totalInstances = instancesData?.content?.length || 0;
    const pendingApprovals = approvalsData?.content?.length || 0;
    const unresolvedDrifts = driftsData?.content?.filter((drift: any) => drift.status === 'UNRESOLVED').length || 0;

    return {
      totalServices,
      totalInstances,
      pendingApprovals,
      unresolvedDrifts,
    };
  }, [servicesData, instancesData, approvalsData, driftsData]);

  // Calculate chart data from real data
  const serviceDistributionData = useMemo(() => {
    // Group services by team (mock data for now - would need team info from services)
    return [
      { name: 'Team Alpha', value: Math.floor(statsData.totalServices * 0.4), color: '#2563eb' },
      { name: 'Team Beta', value: Math.floor(statsData.totalServices * 0.3), color: '#60a5fa' },
      { name: 'Team Gamma', value: Math.floor(statsData.totalServices * 0.2), color: '#93c5fd' },
      { name: 'Team Delta', value: Math.floor(statsData.totalServices * 0.1), color: '#dbeafe' },
    ];
  }, [statsData.totalServices]);

  const instanceStatusData = useMemo(() => {
    const instances = instancesData?.content || [];
    const statusCounts = instances.reduce((acc: any, instance: any) => {
      const status = instance.status || 'UNKNOWN';
      acc[status] = (acc[status] || 0) + 1;
      return acc;
    }, {});

    return [
      { name: 'HEALTHY', value: statusCounts.HEALTHY || 0, color: '#10b981' },
      { name: 'UNHEALTHY', value: statusCounts.UNHEALTHY || 0, color: '#ef4444' },
      { name: 'DRIFT', value: statusCounts.DRIFT || 0, color: '#f59e0b' },
      { name: 'UNKNOWN', value: statusCounts.UNKNOWN || 0, color: '#6b7280' },
    ].filter(item => item.value > 0);
  }, [instancesData]);

  const driftEventsData = useMemo(() => {
    const drifts = driftsData?.content || [];
    // Group by date and severity (last 7 days)
    const last7Days = Array.from({ length: 7 }, (_, i) => {
      const date = new Date();
      date.setDate(date.getDate() - i);
      return date.toISOString().split('T')[0];
    }).reverse();

    return last7Days.map(date => {
      const dayDrifts = drifts.filter((drift: any) => 
        drift.createdAt?.startsWith(date)
      );
      
      return {
        date,
        critical: dayDrifts.filter((d: any) => d.severity === 'CRITICAL').length,
        high: dayDrifts.filter((d: any) => d.severity === 'HIGH').length,
        medium: dayDrifts.filter((d: any) => d.severity === 'MEDIUM').length,
        low: dayDrifts.filter((d: any) => d.severity === 'LOW').length,
      };
    });
  }, [driftsData]);

  const recentActivities = useMemo(() => {
    const activities: any[] = [];
    
    // Add recent approvals
    const approvals = approvalsData?.content || [];
    approvals.slice(0, 3).forEach((approval: any) => {
      activities.push({
        id: `approval-${approval.id}`,
      type: 'approval' as const,
        message: `${approval.requestType} request ${approval.status.toLowerCase()}`,
        timestamp: approval.createdAt,
      });
    });

    // Add recent drifts
    const drifts = driftsData?.content || [];
    drifts.slice(0, 3).forEach((drift: any) => {
      activities.push({
        id: `drift-${drift.id}`,
      type: 'drift' as const,
        message: `${drift.severity} drift detected in ${drift.serviceName}`,
        timestamp: drift.createdAt,
        severity: drift.severity?.toLowerCase(),
      });
    });

    // Sort by timestamp and take most recent
    return activities
      .sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())
      .slice(0, 5);
  }, [approvalsData, driftsData]);

  // Loading and error states
  const isLoading = servicesLoading || instancesLoading || driftsLoading || approvalsLoading || registryLoading;
  const hasError = servicesError || instancesError || driftsError || approvalsError || registryError;

  if (hasError) {
    return (
      <Box>
        <PageHeader title="Dashboard" />
        <Alert severity="error" sx={{ mb: 3 }}>
          Failed to load dashboard data. Please try refreshing the page.
        </Alert>
      </Box>
    );
  }

  if (isLoading) {
    return (
      <Box>
        <PageHeader title="Dashboard" />
        <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 400 }}>
          <CircularProgress size={60} />
        </Box>
      </Box>
    );
  }

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