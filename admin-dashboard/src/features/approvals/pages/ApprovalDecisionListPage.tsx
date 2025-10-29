import { useState, useMemo, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import {
  Box,
  Button,
  Card,
  CardContent,
  TextField,
  InputAdornment,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Alert,
  Grid,
  Skeleton,
} from "@mui/material";
import {
  Search as SearchIcon,
  Refresh as RefreshIcon,
  Assessment as StatsIcon,
} from "@mui/icons-material";
import PageHeader from "@components/common/PageHeader";
import Loading from "@components/common/Loading";
import StatCard from "@components/common/StatCard";
import { useFindAllApprovalDecisions } from "@lib/api/hooks";
import type {
  FindAllApprovalDecisionsGate,
  FindAllApprovalDecisionsDecision,
} from "@lib/api/models";
import { ApprovalDecisionTable } from "../components/ApprovalDecisionTable";
import { useDebounce } from "@hooks/useDebounce";

export default function ApprovalDecisionListPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  // Parse initial state from URL params
  const initialPage = parseInt(searchParams.get("page") || "0", 10);
  const initialPageSize = parseInt(searchParams.get("size") || "20", 10);

  const [page, setPage] = useState(initialPage);
  const [pageSize, setPageSize] = useState(initialPageSize);
  const [requestIdFilter, setRequestIdFilter] = useState(
    searchParams.get("requestId") || ""
  );
  const [approverUserIdFilter, setApproverUserIdFilter] = useState(
    searchParams.get("approverUserId") || ""
  );
  const [gateFilter, setGateFilter] = useState<
    FindAllApprovalDecisionsGate | ""
  >((searchParams.get("gate") as FindAllApprovalDecisionsGate | null) || "");
  const [decisionFilter, setDecisionFilter] = useState<
    FindAllApprovalDecisionsDecision | ""
  >(
    (searchParams.get("decision") as FindAllApprovalDecisionsDecision | null) ||
      ""
  );

  // Debounce search inputs
  const debouncedRequestIdFilter = useDebounce(requestIdFilter, 400);
  const debouncedApproverUserIdFilter = useDebounce(approverUserIdFilter, 400);

  // Sync URL params when filters change
  useEffect(() => {
    const params = new URLSearchParams();
    if (debouncedRequestIdFilter)
      params.set("requestId", debouncedRequestIdFilter);
    if (debouncedApproverUserIdFilter)
      params.set("approverUserId", debouncedApproverUserIdFilter);
    if (gateFilter) params.set("gate", gateFilter);
    if (decisionFilter) params.set("decision", decisionFilter);
    if (page > 0) params.set("page", page.toString());
    if (pageSize !== 20) params.set("size", pageSize.toString());
    setSearchParams(params, { replace: true });
  }, [
    debouncedRequestIdFilter,
    debouncedApproverUserIdFilter,
    gateFilter,
    decisionFilter,
    page,
    pageSize,
    setSearchParams,
  ]);

  const { data, isLoading, error, refetch } = useFindAllApprovalDecisions(
    {
      requestId: debouncedRequestIdFilter || undefined,
      approverUserId: debouncedApproverUserIdFilter || undefined,
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

  // Note: Stats calculation would ideally come from server-side aggregation
  // For now, we show totals from the current filtered results
  // In a production system, these stats should be calculated server-side
  const stats = useMemo(() => {
    const total = metadata?.totalElements || 0;
    // Calculate stats from current page data (limited accuracy)
    // In production, these should come from separate API endpoint
    const approved = decisions.filter((d) => d.decision === "APPROVE").length;
    const rejected = decisions.filter((d) => d.decision === "REJECT").length;
    const sysAdminGate = decisions.filter((d) => d.gate === "SYS_ADMIN").length;
    const lineManagerGate = decisions.filter(
      (d) => d.gate === "LINE_MANAGER"
    ).length;

    return {
      total,
      approved: metadata?.totalElements ? undefined : approved, // Show undefined if we have server totals
      rejected: metadata?.totalElements ? undefined : rejected,
      sysAdminGate: metadata?.totalElements ? undefined : sysAdminGate,
      lineManagerGate: metadata?.totalElements ? undefined : lineManagerGate,
    };
  }, [decisions, metadata]);

  const handleFilterReset = () => {
    setRequestIdFilter("");
    setApproverUserIdFilter("");
    setGateFilter("");
    setDecisionFilter("");
    setPage(0);
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
            <Grid size={{ xs: 12, md: 3 }}>
              <TextField
                fullWidth
                label="Filter by Request ID"
                value={requestIdFilter}
                onChange={(e) => {
                  setRequestIdFilter(e.target.value);
                  setPage(0);
                }}
                slotProps={{
                  input: {
                    startAdornment: (
                      <InputAdornment position="start">
                        <SearchIcon />
                      </InputAdornment>
                    ),
                    "aria-label": "Filter by request ID",
                  },
                }}
              />
            </Grid>

            <Grid size={{ xs: 12, md: 3 }}>
              <TextField
                fullWidth
                label="Filter by Approver User ID"
                value={approverUserIdFilter}
                onChange={(e) => {
                  setApproverUserIdFilter(e.target.value);
                  setPage(0);
                }}
                slotProps={{
                  input: {
                    startAdornment: (
                      <InputAdornment position="start">
                        <SearchIcon />
                      </InputAdornment>
                    ),
                    "aria-label": "Filter by approver user ID",
                  },
                }}
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

          {isLoading && <Loading />}

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
