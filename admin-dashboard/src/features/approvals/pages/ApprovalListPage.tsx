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
  Tabs,
  Tab,
  Alert,
  FormControlLabel,
  Switch,
  Badge,
  Collapse,
} from "@mui/material";
import Grid from "@mui/material/Grid";
import {
  Search as SearchIcon,
  Refresh as RefreshIcon,
  CheckCircle as ApprovalIcon,
  ExpandMore as ExpandMoreIcon,
  ExpandLess as ExpandLessIcon,
} from "@mui/icons-material";
import PageHeader from "@components/common/PageHeader";
import { TableSkeleton } from "@components/common/skeletons";
import { DateRangeFilter } from "@components/common/filters";
import { useFindAllApprovalRequests } from "@lib/api/hooks";
import { ApprovalRequestTable } from "../components/ApprovalRequestTable";
import { useAuth } from "@features/auth/context";
import { useDebounce } from "@hooks/useDebounce";
import { formatISO } from "date-fns";

export default function ApprovalListPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const { isSysAdmin, userInfo } = useAuth();

  // Parse initial state from URL params
  const initialPage = parseInt(searchParams.get("page") || "0", 10);
  const initialPageSize = parseInt(searchParams.get("size") || "20", 10);
  const initialTab = parseInt(searchParams.get("tab") || "0", 10);

  const [page, setPage] = useState(initialPage);
  const [pageSize, setPageSize] = useState(initialPageSize);
  const [search, setSearch] = useState(searchParams.get("search") || "");
  const [statusFilter, setStatusFilter] = useState(
    searchParams.get("status") || ""
  );
  const [requestTypeFilter, setRequestTypeFilter] = useState(
    searchParams.get("requestType") || ""
  );
  const [activeTab, setActiveTab] = useState(initialTab);
  const [showMyApprovalsOnly, setShowMyApprovalsOnly] = useState(
    searchParams.get("showMyApprovalsOnly") === "true"
  );
  const [fromDate, setFromDate] = useState<Date | null>(
    searchParams.get("fromDate")
      ? new Date(searchParams.get("fromDate")!)
      : null
  );
  const [toDate, setToDate] = useState<Date | null>(
    searchParams.get("toDate") ? new Date(searchParams.get("toDate")!) : null
  );
  const [showAdvancedFilters, setShowAdvancedFilters] = useState(
    searchParams.get("fromDate") !== null || searchParams.get("toDate") !== null
  );

  // Debounce search input
  const debouncedSearch = useDebounce(search, 400);

  // Sync URL params when filters change
  useEffect(() => {
    const params = new URLSearchParams();
    if (debouncedSearch) params.set("search", debouncedSearch);
    if (statusFilter) params.set("status", statusFilter);
    if (requestTypeFilter) params.set("requestType", requestTypeFilter);
    if (showMyApprovalsOnly) params.set("showMyApprovalsOnly", "true");
    if (fromDate) {
      params.set("fromDate", formatISO(fromDate, { representation: "date" }));
    }
    if (toDate) {
      params.set("toDate", formatISO(toDate, { representation: "date" }));
    }
    if (activeTab > 0) params.set("tab", activeTab.toString());
    if (page > 0) params.set("page", page.toString());
    if (pageSize !== 20) params.set("size", pageSize.toString());
    setSearchParams(params, { replace: true });
  }, [
    debouncedSearch,
    statusFilter,
    requestTypeFilter,
    showMyApprovalsOnly,
    fromDate,
    toDate,
    activeTab,
    page,
    pageSize,
    setSearchParams,
  ]);

  // Map tab to status filter
  const tabToStatus: Record<number, string | undefined> = {
    0: undefined, // All
    1: "PENDING",
    2: "APPROVED",
    3: "REJECTED",
    4: "CANCELLED",
  };

  const currentStatus = statusFilter || tabToStatus[activeTab];

  const { data, isLoading, error, refetch } = useFindAllApprovalRequests(
    {
      requesterUserId: debouncedSearch || undefined,
      status: currentStatus,
      requestType: requestTypeFilter || undefined,
      fromDate: fromDate
        ? formatISO(fromDate, { representation: "date" })
        : undefined,
      toDate: toDate
        ? formatISO(toDate, { representation: "date" })
        : undefined,
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

  const allRequests = useMemo(() => data?.items || [], [data?.items]);
  const metadata = data?.metadata;

  // Filter requests for "Pending My Approval"
  const requests = useMemo(() => {
    if (!showMyApprovalsOnly) return allRequests;

    return allRequests.filter((request) => {
      // Only show pending requests
      if (request.status !== "PENDING") return false;

      // SYS_ADMIN can approve everything
      if (isSysAdmin) return true;

      // LINE_MANAGER can approve if they are the manager of the requester
      if (request.snapshot?.managerId === userInfo?.userId) return true;

      return false;
    });
  }, [allRequests, showMyApprovalsOnly, isSysAdmin, userInfo?.userId]);

  // Count pending approvals for badge
  const pendingMyApprovalCount = useMemo(() => {
    if (!allRequests) return 0;
    return allRequests.filter((request) => {
      if (request.status !== "PENDING") return false;
      if (isSysAdmin) return true;
      if (request.snapshot?.managerId === userInfo?.userId) return true;
      return false;
    }).length;
  }, [allRequests, isSysAdmin, userInfo?.userId]);

  const handleTabChange = (_: React.SyntheticEvent, newValue: number) => {
    setActiveTab(newValue);
    setPage(0);
  };

  const handleFilterReset = () => {
    setSearch("");
    setStatusFilter("");
    setRequestTypeFilter("");
    setShowMyApprovalsOnly(false);
    setFromDate(null);
    setToDate(null);
    setShowAdvancedFilters(false);
    setActiveTab(0);
    setPage(0);
    setSearchParams({}, { replace: true });
  };

  const handleDateRangeChange = (
    startDate: Date | null,
    endDate: Date | null
  ) => {
    setFromDate(startDate);
    setToDate(endDate);
    setPage(0);
  };

  return (
    <Box>
      <PageHeader
        title="Approval Requests"
        subtitle="Manage service access approval requests"
        actions={
          <>
            {pendingMyApprovalCount > 0 && (
              <Badge
                badgeContent={pendingMyApprovalCount}
                color="warning"
                sx={{ mr: 2 }}
              >
                <ApprovalIcon color="action" />
              </Badge>
            )}
            <Button
              variant="outlined"
              startIcon={<RefreshIcon />}
              onClick={() => refetch()}
              aria-label="Refresh approval requests"
            >
              Refresh
            </Button>
          </>
        }
      />

      <Card>
        <CardContent>
          {/* Status Tabs */}
          <Box sx={{ borderBottom: 1, borderColor: "divider", mb: 3 }}>
            <Tabs value={activeTab} onChange={handleTabChange}>
              <Tab label="All" />
              <Tab label="Pending" />
              <Tab label="Approved" />
              <Tab label="Rejected" />
              <Tab label="Cancelled" />
            </Tabs>
          </Box>

          {/* Pending My Approval Toggle */}
          <Box sx={{ mb: 2 }}>
            <FormControlLabel
              control={
                <Switch
                  checked={showMyApprovalsOnly}
                  onChange={(e) => {
                    setShowMyApprovalsOnly(e.target.checked);
                    setPage(0);
                  }}
                  color="warning"
                />
              }
              label={
                <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                  <ApprovalIcon fontSize="small" />
                  Show Pending My Approval Only
                  {pendingMyApprovalCount > 0 && (
                    <Badge
                      badgeContent={pendingMyApprovalCount}
                      color="warning"
                    />
                  )}
                </Box>
              }
            />
          </Box>

          {/* Filters */}
          <Grid container spacing={2} sx={{ mb: 3 }}>
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField
                fullWidth
                label="Search by Requester User ID"
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value);
                  setPage(0);
                }}
                disabled={showMyApprovalsOnly}
                slotProps={{
                  input: {
                    startAdornment: (
                      <InputAdornment position="start">
                        <SearchIcon />
                      </InputAdornment>
                    ),
                    "aria-label": "Search by requester user ID",
                  },
                }}
              />
            </Grid>

            <Grid size={{ xs: 12, md: 3 }}>
              <FormControl fullWidth>
                <InputLabel>Request Type</InputLabel>
                <Select
                  value={requestTypeFilter}
                  label="Request Type"
                  onChange={(e) => {
                    setRequestTypeFilter(e.target.value);
                    setPage(0);
                  }}
                  disabled={showMyApprovalsOnly}
                  aria-label="Filter by request type"
                >
                  <MenuItem value="">All</MenuItem>
                  <MenuItem value="SERVICE_ACCESS">Service Access</MenuItem>
                  <MenuItem value="PERMISSION_GRANT">Permission Grant</MenuItem>
                  <MenuItem value="ROLE_ASSIGNMENT">Role Assignment</MenuItem>
                </Select>
              </FormControl>
            </Grid>

            <Grid size={{ xs: 12, md: 3 }}>
              <FormControl fullWidth>
                <InputLabel>Status Override</InputLabel>
                <Select
                  value={statusFilter}
                  label="Status Override"
                  onChange={(e) => {
                    setStatusFilter(e.target.value);
                    setPage(0);
                  }}
                  disabled={showMyApprovalsOnly}
                  aria-label="Override status filter"
                >
                  <MenuItem value="">Use Tab Filter</MenuItem>
                  <MenuItem value="PENDING">Pending</MenuItem>
                  <MenuItem value="APPROVED">Approved</MenuItem>
                  <MenuItem value="REJECTED">Rejected</MenuItem>
                  <MenuItem value="CANCELLED">Cancelled</MenuItem>
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
                Reset
              </Button>
            </Grid>
          </Grid>

          {/* Advanced Filters */}
          <Collapse in={showAdvancedFilters}>
            <Grid container spacing={2} sx={{ mb: 2, mt: 1 }}>
              <Grid size={{ xs: 12, md: 6 }}>
                <DateRangeFilter
                  label="Request Date Range"
                  startDate={fromDate}
                  endDate={toDate}
                  onChange={handleDateRangeChange}
                  helperText="Filter requests by creation date"
                />
              </Grid>
            </Grid>
          </Collapse>

          {/* More/Less Button */}
          <Box sx={{ mb: 2 }}>
            <Button
              variant="text"
              size="small"
              onClick={() => setShowAdvancedFilters(!showAdvancedFilters)}
              endIcon={
                showAdvancedFilters ? <ExpandLessIcon /> : <ExpandMoreIcon />
              }
            >
              {showAdvancedFilters
                ? "Hide Advanced Filters"
                : "Show Advanced Filters"}
            </Button>
          </Box>

          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              Failed to load approval requests:{" "}
              {error.detail || "Unknown error"}
            </Alert>
          )}

          {isLoading && <TableSkeleton rows={10} columns={6} />}

          {!isLoading && !error && (
            <ApprovalRequestTable
              requests={requests}
              loading={isLoading}
              page={page}
              pageSize={pageSize}
              totalElements={metadata?.totalElements || 0}
              onPageChange={(newPage: number) => setPage(newPage)}
              onPageSizeChange={(newPageSize: number) => {
                setPageSize(newPageSize);
                setPage(0);
              }}
              onRowClick={(requestId: string) =>
                navigate(`/approvals/${requestId}`)
              }
            />
          )}
        </CardContent>
      </Card>
    </Box>
  );
}
