import { useMemo, useState } from "react";
import { Box, Alert, Button, Typography } from "@mui/material";
import Grid from "@mui/material/Grid";
import {
  Apps as AppsIcon,
  Storage as StorageIcon,
  Warning as WarningIcon,
  Assignment as AssignmentIcon,
  Refresh as RefreshIcon,
} from "@mui/icons-material";
import PageHeader from "@components/common/PageHeader";
import Loading from "@components/common/Loading";
import { StatsCard } from "../components/StatsCard";
import { ServiceDistributionChart } from "../components/ServiceDistributionChart";
import { InstanceStatusChart } from "../components/InstanceStatusChart";
import { DriftEventsChart } from "../components/DriftEventsChart";
import { RecentActivityList } from "../components/RecentActivityList";
import {
  useFindAllApplicationServices,
  useFindAllServiceInstances,
  useFindAllDriftEvents,
  useFindAllApprovalRequests,
} from "@lib/api/hooks";
import type {
  ActivityItem,
  ServiceDistributionData,
  InstanceStatusData,
  DriftEventsData,
} from "../types";

export default function DashboardPage() {
  const [lastUpdated, setLastUpdated] = useState<Date>(new Date());

  // Fetch dashboard data with higher page sizes for overview
  const {
    data: servicesData,
    isLoading: servicesLoading,
    error: servicesError,
    refetch: refetchServices,
  } = useFindAllApplicationServices(
    { page: 0, size: 100 },
    { query: { staleTime: 60_000 } }
  );

  const {
    data: instancesData,
    isLoading: instancesLoading,
    error: instancesError,
    refetch: refetchInstances,
  } = useFindAllServiceInstances(
    { page: 0, size: 100 },
    { query: { staleTime: 60_000 } }
  );

  const {
    data: driftsData,
    isLoading: driftsLoading,
    error: driftsError,
    refetch: refetchDrifts,
  } = useFindAllDriftEvents(
    { status: "DETECTED", page: 0, size: 100 },
    { query: { staleTime: 30_000 } }
  );

  const {
    data: approvalsData,
    isLoading: approvalsLoading,
    error: approvalsError,
    refetch: refetchApprovals,
  } = useFindAllApprovalRequests(
    { status: "PENDING", page: 0, size: 100 },
    { query: { staleTime: 30_000 } }
  );

  const isLoading =
    servicesLoading || instancesLoading || driftsLoading || approvalsLoading;
  const hasError =
    servicesError || instancesError || driftsError || approvalsError;

  const handleRefresh = async () => {
    await Promise.all([
      refetchServices(),
      refetchInstances(),
      refetchDrifts(),
      refetchApprovals(),
    ]);
    setLastUpdated(new Date());
  };

  // Calculate statistics from real data
  const statsData = useMemo(() => {
    const totalServices = servicesData?.metadata?.totalElements || 0;
    const totalInstances = instancesData?.metadata?.totalElements || 0;
    const pendingApprovals = approvalsData?.metadata?.totalElements || 0;
    const unresolvedDrifts = driftsData?.metadata?.totalElements || 0;

    return {
      totalServices,
      totalInstances,
      pendingApprovals,
      unresolvedDrifts,
    };
  }, [servicesData, instancesData, approvalsData, driftsData]);

  // Calculate chart data from real data
  const serviceDistributionData = useMemo((): ServiceDistributionData[] => {
    const services = servicesData?.items || [];
    const teamCounts: Record<string, number> = {};

    for (const service of services) {
      const team = service.ownerTeamId || "Unknown";
      teamCounts[team] = (teamCounts[team] || 0) + 1;
    }

    return Object.entries(teamCounts)
      .map(([name, value], index) => ({
        name,
        value,
        color: ["#2563eb", "#60a5fa", "#93c5fd", "#dbeafe", "#bfdbfe"][
          index % 5
        ],
      }))
      .slice(0, 5); // Top 5 teams
  }, [servicesData]);

  const instanceStatusData = useMemo((): InstanceStatusData[] => {
    const instances = instancesData?.items || [];
    const statusCounts: Record<string, number> = {};

    for (const instance of instances) {
      const status = instance.status || "UNKNOWN";
      statusCounts[status] = (statusCounts[status] || 0) + 1;
    }

    return [
      { name: "HEALTHY", value: statusCounts.HEALTHY || 0, color: "#10b981" },
      {
        name: "UNHEALTHY",
        value: statusCounts.UNHEALTHY || 0,
        color: "#ef4444",
      },
      { name: "DRIFT", value: statusCounts.DRIFT || 0, color: "#f59e0b" },
      { name: "UNKNOWN", value: statusCounts.UNKNOWN || 0, color: "#6b7280" },
    ].filter((item) => item.value > 0);
  }, [instancesData]);

  const driftEventsData = useMemo((): DriftEventsData[] => {
    const drifts = driftsData?.items || [];

    // Group by date (last 7 days) and severity
    const dates: Record<
      string,
      { critical: number; high: number; medium: number; low: number }
    > = {};
    const today = new Date();

    for (let i = 6; i >= 0; i--) {
      const date = new Date(today);
      date.setDate(date.getDate() - i);
      const dateStr = date.toISOString().split("T")[0];
      dates[dateStr] = { critical: 0, high: 0, medium: 0, low: 0 };
    }

    for (const drift of drifts) {
      if (drift.detectedAt) {
        const dateStr = drift.detectedAt.split("T")[0];
        if (dates[dateStr] !== undefined) {
          const severity = (drift.severity || "LOW").toLowerCase() as
            | "critical"
            | "high"
            | "medium"
            | "low";
          dates[dateStr][severity] = (dates[dateStr][severity] || 0) + 1;
        }
      }
    }

    return Object.entries(dates).map(([date, severities]) => ({
      date: new Date(date).toLocaleDateString("en-US", {
        month: "short",
        day: "numeric",
      }),
      critical: severities.critical,
      high: severities.high,
      medium: severities.medium,
      low: severities.low,
    }));
  }, [driftsData]);

  const recentActivityData = useMemo((): ActivityItem[] => {
    const activities: ActivityItem[] = [];

    // Add recent approvals
    for (const approval of (approvalsData?.items || []).slice(0, 3)) {
      activities.push({
        id: approval.id || `approval-${Date.now()}`,
        type: "approval",
        message: `Approval request for ${
          approval.target?.serviceId || "service"
        } - ${approval.status}`,
        timestamp: approval.createdAt || new Date().toISOString(),
      });
    }

    // Add recent drift events
    for (const drift of (driftsData?.items || []).slice(0, 3)) {
      activities.push({
        id: drift.id || `drift-${Date.now()}`,
        type: "drift",
        message: `Drift detected on ${
          drift.serviceName
        } instance ${drift.instanceId?.substring(0, 8)}`,
        timestamp: drift.detectedAt || new Date().toISOString(),
        severity: drift.severity?.toLowerCase() as ActivityItem["severity"],
      });
    }

    // Sort by timestamp descending
    return [...activities]
      .sort(
        (a, b) =>
          new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
      )
      .slice(0, 10);
  }, [approvalsData, driftsData]);

  if (isLoading) {
    return <Loading />;
  }

  if (hasError) {
    return (
      <Box>
        <PageHeader
          title="Dashboard"
          subtitle="Overview of services and system health"
        />
        <Alert severity="error" sx={{ m: 3 }}>
          Failed to load dashboard data. Please try refreshing the page.
        </Alert>
      </Box>
    );
  }

  return (
    <Box>
      <PageHeader
        title="Dashboard"
        subtitle="Overview of services and system health"
        actions={
          <Box sx={{ display: "flex", gap: 2, alignItems: "center" }}>
            <Typography variant="caption" color="text.secondary">
              Last updated: {lastUpdated.toLocaleTimeString()}
            </Typography>
            <Button
              variant="outlined"
              startIcon={<RefreshIcon />}
              onClick={handleRefresh}
              disabled={isLoading}
            >
              Refresh
            </Button>
          </Box>
        }
      />

      {/* Stats Cards */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <StatsCard
            title="Total Services"
            value={statsData.totalServices}
            icon={<AppsIcon />}
            color="primary"
            to="/application-services"
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <StatsCard
            title="Service Instances"
            value={statsData.totalInstances}
            icon={<StorageIcon />}
            color="info"
            to="/service-instances"
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <StatsCard
            title="Pending Approvals"
            value={statsData.pendingApprovals}
            icon={<AssignmentIcon />}
            color="warning"
            to="/approvals"
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <StatsCard
            title="Active Drift Events"
            value={statsData.unresolvedDrifts}
            icon={<WarningIcon />}
            color="error"
            to="/drift-events"
          />
        </Grid>
      </Grid>

      {/* Charts */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid size={{ xs: 12, md: 6 }}>
          <ServiceDistributionChart data={serviceDistributionData} />
        </Grid>
        <Grid size={{ xs: 12, md: 6 }}>
          <InstanceStatusChart data={instanceStatusData} />
        </Grid>
      </Grid>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 8 }}>
          <DriftEventsChart data={driftEventsData} />
        </Grid>
        <Grid size={{ xs: 12, md: 4 }}>
          <RecentActivityList activities={recentActivityData} />
        </Grid>
      </Grid>
    </Box>
  );
}
