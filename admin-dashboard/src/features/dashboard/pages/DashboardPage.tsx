import { useMemo, useState, useEffect } from "react";
import { useQueryClient } from "@tanstack/react-query";
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
import { DashboardSkeleton } from "@components/common/skeletons";
import { StatsCard } from "../components/StatsCard";
import { ServiceDistributionChart } from "../components/ServiceDistributionChart";
import { InstanceStatusChart } from "../components/InstanceStatusChart";
import { DriftEventsChart } from "../components/DriftEventsChart";
import { DriftEventsStackedAreaChart } from "../components/DriftEventsStackedAreaChart";
import { RecentActivityList } from "../components/RecentActivityList";
import { TimeRangeSelector, type TimeRange } from "../components/TimeRangeSelector";
import { TopServicesDriftTable } from "../components/TopServicesDriftTable";
import {
  useFindAllApplicationServices,
  useFindAllServiceInstances,
  useFindAllDriftEvents,
  useFindAllApprovalRequests,
} from "@lib/api/hooks";
import { getFindAllApplicationServicesQueryKey } from "@lib/api/generated/application-services/application-services";
import { getFindAllServiceInstancesQueryKey } from "@lib/api/generated/service-instances/service-instances";
import { getFindAllDriftEventsQueryKey } from "@lib/api/generated/drift-events/drift-events";
import { getFindAllApprovalRequestsQueryKey } from "@lib/api/generated/approval-requests/approval-requests";
import type {
  ActivityItem,
  ServiceDistributionData,
  InstanceStatusData,
  DriftEventsData,
} from "../types";
import { parseTimestamp } from "@lib/utils/dateUtils";

export default function DashboardPage() {
  const [lastUpdated, setLastUpdated] = useState<Date>(new Date());
  const [timeRange, setTimeRange] = useState<TimeRange>("7d");
  const [customStartDate, setCustomStartDate] = useState<Date | null>(null);
  const [customEndDate, setCustomEndDate] = useState<Date | null>(null);
  const [previousStats, setPreviousStats] = useState<{
    totalServices: number;
    totalInstances: number;
    pendingApprovals: number;
    unresolvedDrifts: number;
  } | null>(null);
  const queryClient = useQueryClient();

  // Optimized: Reduced page sizes for faster initial load
  // Stats cards use metadata.totalElements, charts use sample data
  const {
    data: servicesData,
    isLoading: servicesLoading,
    error: servicesError,
  } = useFindAllApplicationServices(
    { page: 0, size: 50 }, // Reduced from 100
    { query: { staleTime: 60_000 } }
  );

  const {
    data: instancesData,
    isLoading: instancesLoading,
    error: instancesError,
  } = useFindAllServiceInstances(
    { page: 0, size: 50 }, // Reduced from 100
    { query: { staleTime: 60_000 } }
  );

  // Calculate date range based on timeRange selection
  const dateRange = useMemo(() => {
    const endDate = customEndDate || new Date();
    const startDate = customStartDate || (() => {
      const days = timeRange === "7d" ? 7 : timeRange === "30d" ? 30 : 90;
      const date = new Date(endDate);
      date.setDate(date.getDate() - days);
      return date;
    })();
    return { startDate, endDate };
  }, [timeRange, customStartDate, customEndDate]);

  const {
    data: driftsData,
    isLoading: driftsLoading,
    error: driftsError,
  } = useFindAllDriftEvents(
    { status: "DETECTED", page: 0, size: 100 }, // Increased for time range support
    { query: { staleTime: 30_000 } }
  );

  const {
    data: approvalsData,
    isLoading: approvalsLoading,
    error: approvalsError,
  } = useFindAllApprovalRequests(
    { status: "PENDING", page: 0, size: 50 }, // Reduced from 100
    { query: { staleTime: 30_000 } }
  );

  const isLoading =
    servicesLoading || instancesLoading || driftsLoading || approvalsLoading;
  const hasError =
    servicesError || instancesError || driftsError || approvalsError;

  const handleRefresh = async () => {
    // Invalidate all dashboard queries to trigger refetch
    queryClient.invalidateQueries({
      queryKey: getFindAllApplicationServicesQueryKey({ page: 0, size: 50 }),
    });
    queryClient.invalidateQueries({
      queryKey: getFindAllServiceInstancesQueryKey({ page: 0, size: 50 }),
    });
    queryClient.invalidateQueries({
      queryKey: getFindAllDriftEventsQueryKey({
        status: "DETECTED",
        page: 0,
        size: 50,
      }),
    });
    queryClient.invalidateQueries({
      queryKey: getFindAllApprovalRequestsQueryKey({
        status: "PENDING",
        page: 0,
        size: 50,
      }),
    });
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

  // Store previous stats for trend calculation
  useEffect(() => {
    if (statsData && !previousStats) {
      setPreviousStats(statsData);
    }
  }, [statsData, previousStats]);

  // Calculate trends
  const trends = useMemo(() => {
    if (!previousStats) return null;
    return {
      totalServices: statsData.totalServices - previousStats.totalServices,
      totalInstances: statsData.totalInstances - previousStats.totalInstances,
      pendingApprovals: statsData.pendingApprovals - previousStats.pendingApprovals,
      unresolvedDrifts: statsData.unresolvedDrifts - previousStats.unresolvedDrifts,
    };
  }, [statsData, previousStats]);

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
    const { startDate, endDate } = dateRange;

    // Calculate number of days
    const daysDiff = Math.ceil(
      (endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24)
    );
    const numDays = Math.min(daysDiff, 90); // Cap at 90 days for performance

    // Group by date and severity
    const dates: Record<
      string,
      { critical: number; high: number; medium: number; low: number }
    > = {};

    // Initialize all dates in range
    for (let i = numDays - 1; i >= 0; i--) {
      const date = new Date(endDate);
      date.setDate(date.getDate() - i);
      const dateStr = date.toISOString().split("T")[0];
      dates[dateStr] = { critical: 0, high: 0, medium: 0, low: 0 };
    }

    // Filter and group drift events
    for (const drift of drifts) {
      if (!drift.detectedAt) continue;

      try {
        const driftDate = parseTimestamp(drift.detectedAt);
        if (!driftDate) continue;

        // Check if drift is within date range
        if (driftDate < startDate || driftDate > endDate) continue;

        const dateStr = driftDate.toISOString().split("T")[0];
        if (dates[dateStr] !== undefined) {
          const severity = (drift.severity || "LOW").toUpperCase();
          switch (severity) {
            case "CRITICAL":
              dates[dateStr].critical += 1;
              break;
            case "HIGH":
              dates[dateStr].high += 1;
              break;
            case "MEDIUM":
              dates[dateStr].medium += 1;
              break;
            case "LOW":
            default:
              dates[dateStr].low += 1;
              break;
          }
        }
      } catch {
        continue; // Skip invalid dates
      }
    }

    // Format dates for display
    return Object.entries(dates)
      .map(([date, severities]) => ({
        date: new Date(date).toLocaleDateString("en-US", {
          month: "short",
          day: "numeric",
        }),
        critical: severities.critical,
        high: severities.high,
        medium: severities.medium,
        low: severities.low,
      }))
      .filter((item) => {
        // Only include dates with data or show all for small ranges
        return numDays <= 30 || item.critical + item.high + item.medium + item.low > 0;
      });
  }, [driftsData, dateRange]);

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

  // Keyboard shortcut for refresh
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key === "r") {
        event.preventDefault();
        handleRefresh();
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (isLoading) {
    return <DashboardSkeleton />;
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

      {/* Time Range Selector */}
      <Box sx={{ mb: 3, display: "flex", justifyContent: "flex-end" }}>
        <TimeRangeSelector
          value={timeRange}
          onChange={(range) => {
            setTimeRange(range);
            if (range !== "custom") {
              setCustomStartDate(null);
              setCustomEndDate(null);
            }
          }}
          customStartDate={customStartDate}
          customEndDate={customEndDate}
          onCustomDateChange={(start, end) => {
            setCustomStartDate(start);
            setCustomEndDate(end);
            if (start && end) {
              setTimeRange("custom");
            }
          }}
        />
      </Box>

      {/* Stats Cards */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <StatsCard
            title="Total Services"
            value={statsData.totalServices}
            icon={<AppsIcon />}
            color="primary"
            to="/application-services"
            trend={
              trends && previousStats && previousStats.totalServices > 0
                ? {
                    value: Math.round(
                      (trends.totalServices / previousStats.totalServices) * 100
                    ),
                    isPositive: trends.totalServices >= 0,
                  }
                : undefined
            }
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <StatsCard
            title="Service Instances"
            value={statsData.totalInstances}
            icon={<StorageIcon />}
            color="info"
            to="/service-instances"
            trend={
              trends && previousStats && previousStats.totalInstances > 0
                ? {
                    value: Math.round(
                      (trends.totalInstances / previousStats.totalInstances) * 100
                    ),
                    isPositive: trends.totalInstances >= 0,
                  }
                : undefined
            }
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <StatsCard
            title="Pending Approvals"
            value={statsData.pendingApprovals}
            icon={<AssignmentIcon />}
            color="warning"
            to="/approvals"
            trend={
              trends && previousStats && previousStats.pendingApprovals > 0
                ? {
                    value: Math.round(
                      (trends.pendingApprovals / previousStats.pendingApprovals) * 100
                    ),
                    isPositive: trends.pendingApprovals <= 0, // Lower is better
                  }
                : undefined
            }
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <StatsCard
            title="Active Drift Events"
            value={statsData.unresolvedDrifts}
            icon={<WarningIcon />}
            color="error"
            to="/drift-events"
            trend={
              trends && previousStats && previousStats.unresolvedDrifts > 0
                ? {
                    value: Math.round(
                      (trends.unresolvedDrifts / previousStats.unresolvedDrifts) * 100
                    ),
                    isPositive: trends.unresolvedDrifts <= 0, // Lower is better
                  }
                : undefined
            }
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

      {/* Charts */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid size={{ xs: 12, md: 6 }}>
          <DriftEventsChart data={driftEventsData} loading={driftsLoading} />
        </Grid>
        <Grid size={{ xs: 12, md: 6 }}>
          <DriftEventsStackedAreaChart
            data={driftEventsData}
            loading={driftsLoading}
            title="Drift Events by Severity (Stacked)"
          />
        </Grid>
      </Grid>

      {/* Top Services and Recent Activity */}
      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 8 }}>
          <Box sx={{ mb: 3 }}>
            <Typography variant="h6" sx={{ mb: 2 }}>
              Top Services with Drift Events
            </Typography>
            <TopServicesDriftTable
              driftEvents={driftsData?.items || []}
              loading={driftsLoading}
              maxRows={10}
            />
          </Box>
        </Grid>
        <Grid size={{ xs: 12, md: 4 }}>
          <RecentActivityList activities={recentActivityData} />
        </Grid>
      </Grid>
    </Box>
  );
}
