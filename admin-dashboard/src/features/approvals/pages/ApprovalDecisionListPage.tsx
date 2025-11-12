import { useState, useMemo, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import {
  Box,
  Button,
  Card,
  CardContent,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Alert,
  Grid,
  Skeleton,
} from "@mui/material";
import {
  Refresh as RefreshIcon,
  Assessment as StatsIcon,
} from "@mui/icons-material";
import PageHeader from "@components/common/PageHeader";
import { TableSkeleton } from "@components/common/skeletons";
import StatCard from "@components/common/StatCard";
import { ManualSearchField } from "@components/common/ManualSearchField";
import { useFindAllApprovalDecisions } from "@lib/api/hooks";
import type {
  FindAllApprovalDecisionsGate,
  FindAllApprovalDecisionsDecision,
} from "@lib/api/models";
import { ApprovalDecisionTable } from "../components/ApprovalDecisionTable";
import { useManualSearch } from "@hooks/useManualSearch";

export default function ApprovalDecisionListPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  // Parse initial state from URL params
  const initialPage = parseInt(searchParams.get("page") || "0", 10);
  const initialPageSize = parseInt(searchParams.get("size") || "20", 10);

  const [page, setPage] = useState(initialPage);
  const [pageSize, setPageSize] = useState(initialPageSize);
  const [gateFilter, setGateFilter] = useState<
    FindAllApprovalDecisionsGate | ""
  >((searchParams.get("gate") as FindAllApprovalDecisionsGate | null) || "");
  const [decisionFilter, setDecisionFilter] = useState<
    FindAllApprovalDecisionsDecision | ""
  >(
    (searchParams.get("decision") as FindAllApprovalDecisionsDecision | null) ||
      ""
  );

  // Manual search hook for request ID
  const {
    search: requestIdFilter,
    setSearch: setRequestIdFilter,
    submittedSearch: submittedRequestIdFilter,
    handleSearch,
    handleReset: resetRequestIdFilter,
    handleKeyPress,
    isPending,
  } = useManualSearch({
    initialSearch: searchParams.get("requestId") || "",
  });

  // Sync URL params when filters change
  useEffect(() => {
    const params = new URLSearchParams();
    if (submittedRequestIdFilter)
      params.set("requestId", submittedRequestIdFilter);
    if (gateFilter) params.set("gate", gateFilter);
    if (decisionFilter) params.set("decision", decisionFilter);
    if (page > 0) params.set("page", page.toString());
    if (pageSize !== 20) params.set("size", pageSize.toString());
    setSearchParams(params, { replace: true });
  }, [
    submittedRequestIdFilter,
    gateFilter,
    decisionFilter,
    page,
    pageSize,
    setSearchParams,
  ]);

  const { data, isLoading, error, refetch } = useFindAllApprovalDecisions(
    {
      requestId: submittedRequestIdFilter || undefined,
      gate: gateFilter || undefined,
      decision: decisionFilter || undefined,
      page,
      size: pageSize,
    },
    {
      query: {
        staleTime: 10_000,
        refetchInterval: 30_000, // Auto-refresh every 30s
      },
    }
  );

  const decisions = useMemo(() => data?.items || [], [data?.items]);
  const metadata = data?.metadata;

  // Calculate stats from filtered results
  // Note: These stats reflect the current filtered dataset, not all data
  const stats = useMemo(() => {
    const total = metadata?.totalElements || 0;
    // Calculate stats from all filtered decisions
    const approved = decisions.filter((d) => d.decision === "APPROVE").length;
    const rejected = decisions.filter((d) => d.decision === "REJECT").length;
    const sysAdminGate = decisions.filter((d) => d.gate === "SYS_ADMIN").length;
    const lineManagerGate = decisions.filter(
      (d) => d.gate === "LINE_MANAGER"
    ).length;

    return {
      total,
      approved,
      rejected,
      sysAdminGate,
      lineManagerGate,
    };
  }, [decisions, metadata]);

  const handleFilterReset = () => {
    resetRequestIdFilter();
    setGateFilter("");
    setDecisionFilter("");
    setPage(0);
    setSearchParams({}, { replace: true });
  };

  const handleDecisionClick = (decisionId: string) => {
    navigate(`/approval-decisions/${decisionId}`);
  };

  const handleRequestClick = (requestId: string) => {
    navigate(`/approvals/${requestId}`);
  };

  return (
    <Box>
      <PageHeader
        title="Approval Decisions"
        subtitle="View all approval decisions and track decision history"
        actions={
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={() => refetch()}
            aria-label="Refresh approval decisions"
          >
            Refresh
          </Button>
        }
      />

      {/* Stats Cards */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid size={{ xs: 12, sm: 6, md: 2.4 }}>
          {isLoading ? (
            <Skeleton variant="rectangular" height={120} />
          ) : (
            <StatCard
              title="Total Decisions"
              value={stats.total}
              icon={<StatsIcon />}
              color="primary"
            />
          )}
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 2.4 }}>
          {isLoading ? (
            <Skeleton variant="rectangular" height={120} />
          ) : (
            <StatCard
              title="Approved"
              value={stats.approved ?? "-"}
              icon={<StatsIcon />}
              color="success"
            />
          )}
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 2.4 }}>
          {isLoading ? (
            <Skeleton variant="rectangular" height={120} />
          ) : (
            <StatCard
              title="Rejected"
              value={stats.rejected ?? "-"}
              icon={<StatsIcon />}
              color="error"
            />
          )}
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 2.4 }}>
          {isLoading ? (
            <Skeleton variant="rectangular" height={120} />
          ) : (
            <StatCard
              title="SYS_ADMIN Gate"
              value={stats.sysAdminGate ?? "-"}
              icon={<StatsIcon />}
              color="info"
            />
          )}
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 2.4 }}>
          {isLoading ? (
            <Skeleton variant="rectangular" height={120} />
          ) : (
            <StatCard
              title="LINE_MANAGER Gate"
              value={stats.lineManagerGate ?? "-"}
              icon={<StatsIcon />}
              color="warning"
            />
          )}
        </Grid>
      </Grid>

      <Card>
        <CardContent>
          {/* Filters */}
          <Grid container spacing={2} sx={{ mb: 3 }}>
            <Grid size={{ xs: 12, md: 4 }}>
              <ManualSearchField
                value={requestIdFilter}
                onChange={(value) => {
                  setRequestIdFilter(value);
                  // Don't reset page on every keystroke - only when search triggers
                }}
                onSearch={() => {
                  handleSearch();
                  setPage(0); // Reset page when search is triggered
                }}
                onKeyPress={handleKeyPress}
                label="Request ID (Exact Match)"
                placeholder="Enter exact approval request ID"
                loading={isLoading}
                isPending={isPending}
                resultCount={metadata?.totalElements}
                tooltipText="Enter exact approval request ID."
                helperText="Click search button or press Enter to search"
                aria-label="Search by request ID"
              />
            </Grid>

            <Grid size={{ xs: 12, md: 2 }}>
              <FormControl fullWidth>
                <InputLabel>Gate</InputLabel>
                <Select
                  value={gateFilter}
                  label="Gate"
                  onChange={(e) => {
                    setGateFilter(
                      e.target.value as FindAllApprovalDecisionsGate | ""
                    );
                    setPage(0);
                  }}
                  aria-label="Filter by approval gate"
                >
                  <MenuItem value="">All</MenuItem>
                  <MenuItem value="SYS_ADMIN">SYS_ADMIN</MenuItem>
                  <MenuItem value="LINE_MANAGER">LINE_MANAGER</MenuItem>
                </Select>
              </FormControl>
            </Grid>

            <Grid size={{ xs: 12, md: 2 }}>
              <FormControl fullWidth>
                <InputLabel>Decision</InputLabel>
                <Select
                  value={decisionFilter}
                  label="Decision"
                  onChange={(e) => {
                    setDecisionFilter(
                      e.target.value as FindAllApprovalDecisionsDecision | ""
                    );
                    setPage(0);
                  }}
                  aria-label="Filter by decision"
                >
                  <MenuItem value="">All</MenuItem>
                  <MenuItem value="APPROVE">APPROVE</MenuItem>
                  <MenuItem value="REJECT">REJECT</MenuItem>
                </Select>
              </FormControl>
            </Grid>

            <Grid size={{ xs: 12, md: 2 }}>
              <Button
                fullWidth
                variant="outlined"
                onClick={handleFilterReset}
                sx={{ height: "56px" }}
                aria-label="Reset all filters"
              >
                Reset Filters
              </Button>
            </Grid>
          </Grid>

          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              Failed to load approval decisions:{" "}
              {(error as Error).message || "Unknown error"}
            </Alert>
          )}

          {isLoading && <TableSkeleton rows={10} columns={7} />}

          {!isLoading && !error && (
            <ApprovalDecisionTable
              decisions={decisions}
              loading={isLoading}
              page={page}
              pageSize={pageSize}
              totalElements={metadata?.totalElements || 0}
              onPageChange={(newPage: number) => setPage(newPage)}
              onPageSizeChange={(newPageSize: number) => {
                setPageSize(newPageSize);
                setPage(0);
              }}
              onRowClick={handleDecisionClick}
              onRequestClick={handleRequestClick}
            />
          )}
        </CardContent>
      </Card>
    </Box>
  );
}
