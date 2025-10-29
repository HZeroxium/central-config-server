import { useState, useMemo } from "react";
import { useNavigate } from "react-router-dom";
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
} from "@mui/material";
import Grid from "@mui/material/Grid";
import {
  Search as SearchIcon,
  Refresh as RefreshIcon,
  CheckCircle as ApprovalIcon,
} from "@mui/icons-material";
import PageHeader from "@components/common/PageHeader";
import Loading from "@components/common/Loading";
import { useFindAllApprovalRequests } from "@lib/api/hooks";
import { ApprovalRequestTable } from "../components/ApprovalRequestTable";
import { useAuth } from "@features/auth/context";
import { useDebounce } from "@hooks/useDebounce";

export default function ApprovalListPage() {
  const navigate = useNavigate();
  const { isSysAdmin, userInfo } = useAuth();

  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("");
  const [requestTypeFilter, setRequestTypeFilter] = useState("");
  const [activeTab, setActiveTab] = useState(0);
  const [showMyApprovalsOnly, setShowMyApprovalsOnly] = useState(false);

  // Debounce search input
  const debouncedSearch = useDebounce(search, 400);

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

          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              Failed to load approval requests:{" "}
              {error.detail || "Unknown error"}
            </Alert>
          )}

          {isLoading && <Loading />}

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
