import { useState, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import {
  Box,
  Card,
  CardContent,
  TextField,
  InputAdornment,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Button,
  Alert,
  Collapse,
} from "@mui/material";
import Grid from "@mui/material/Grid";
import {
  Search as SearchIcon,
  Refresh as RefreshIcon,
  ExpandMore as ExpandMoreIcon,
  ExpandLess as ExpandLessIcon,
} from "@mui/icons-material";
import PageHeader from "@components/common/PageHeader";
import ConfirmDialog from "@components/common/ConfirmDialog";
import { TableSkeleton } from "@components/common/skeletons";
import { DateRangeFilter } from "@components/common/filters";
import {
  useFindAllServiceInstances,
  useDeleteServiceInstance,
} from "@lib/api/hooks";
import { getFindAllServiceInstancesQueryKey } from "@lib/api/generated/service-instances/service-instances";
import { ServiceInstanceTable } from "../components/ServiceInstanceTable";
import type {
  FindAllServiceInstancesStatus,
  FindAllServiceInstancesEnvironment,
} from "@lib/api/models";
import { toast } from "@lib/toast/toast";
import { handleApiError } from "@lib/api/errorHandler";
import { useDebounce } from "@hooks/useDebounce";
import { formatISO } from "date-fns";

export default function ServiceInstanceListPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const queryClient = useQueryClient();

  // Parse initial state from URL params
  const initialPage = parseInt(searchParams.get("page") || "0", 10);
  const initialPageSize = parseInt(searchParams.get("size") || "20", 10);

  const [page, setPage] = useState(initialPage);
  const [pageSize, setPageSize] = useState(initialPageSize);
  const [search, setSearch] = useState(searchParams.get("search") || "");
  const [environmentFilter, setEnvironmentFilter] = useState<
    FindAllServiceInstancesEnvironment | ""
  >(
    (searchParams.get(
      "environment"
    ) as FindAllServiceInstancesEnvironment | null) || ""
  );
  const [statusFilter, setStatusFilter] = useState<
    FindAllServiceInstancesStatus | ""
  >((searchParams.get("status") as FindAllServiceInstancesStatus | null) || "");
  const [driftFilter, setDriftFilter] = useState<"true" | "false" | "">(
    (searchParams.get("drift") as "true" | "false" | null) || ""
  );
  const [lastSeenAtFrom, setLastSeenAtFrom] = useState<Date | null>(
    searchParams.get("lastSeenAtFrom")
      ? new Date(searchParams.get("lastSeenAtFrom")!)
      : null
  );
  const [lastSeenAtTo, setLastSeenAtTo] = useState<Date | null>(
    searchParams.get("lastSeenAtTo")
      ? new Date(searchParams.get("lastSeenAtTo")!)
      : null
  );
  const [showAdvancedFilters, setShowAdvancedFilters] = useState(
    searchParams.get("lastSeenAtFrom") !== null ||
      searchParams.get("lastSeenAtTo") !== null
  );
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [selectedInstanceId, setSelectedInstanceId] = useState<string | null>(
    null
  );

  // Debounce search input
  const debouncedSearch = useDebounce(search, 400);

  // Sync URL params when filters change
  useEffect(() => {
    const params = new URLSearchParams();
    if (debouncedSearch) params.set("search", debouncedSearch);
    if (environmentFilter) params.set("environment", environmentFilter);
    if (statusFilter) params.set("status", statusFilter);
    if (driftFilter) params.set("drift", driftFilter);
    if (lastSeenAtFrom) {
      params.set("lastSeenAtFrom", formatISO(lastSeenAtFrom, { representation: "date" }));
    }
    if (lastSeenAtTo) {
      params.set("lastSeenAtTo", formatISO(lastSeenAtTo, { representation: "date" }));
    }
    if (page > 0) params.set("page", page.toString());
    if (pageSize !== 20) params.set("size", pageSize.toString());
    setSearchParams(params, { replace: true });
  }, [
    debouncedSearch,
    environmentFilter,
    statusFilter,
    driftFilter,
    lastSeenAtFrom,
    lastSeenAtTo,
    page,
    pageSize,
    setSearchParams,
  ]);

  const { data, isLoading, error, refetch } = useFindAllServiceInstances(
    {
      serviceId: debouncedSearch || undefined,
      status: statusFilter || undefined,
      environment: environmentFilter || undefined,
      hasDrift: driftFilter || undefined,
      lastSeenAtFrom: lastSeenAtFrom
        ? formatISO(lastSeenAtFrom, { representation: "date" })
        : undefined,
      lastSeenAtTo: lastSeenAtTo
        ? formatISO(lastSeenAtTo, { representation: "date" })
        : undefined,
      page,
      size: pageSize,
    },
    {
      query: {
        staleTime: 30_000,
        refetchInterval: 30000, // 30 seconds
        refetchIntervalInBackground: false,
      },
    }
  );

  const deleteMutation = useDeleteServiceInstance();

  const instances = data?.items || [];
  const metadata = data?.metadata;

  const handleDeleteInstance = async () => {
    if (!selectedInstanceId) return;

    deleteMutation.mutate(
      { instanceId: selectedInstanceId },
      {
        onSuccess: () => {
          toast.success("Instance deleted successfully");
          setDeleteDialogOpen(false);
          setSelectedInstanceId(null);
          // Invalidate list query to refresh data
          queryClient.invalidateQueries({
            queryKey: getFindAllServiceInstancesQueryKey({
              serviceId: debouncedSearch || undefined,
              status: statusFilter || undefined,
              environment: environmentFilter || undefined,
              hasDrift: driftFilter || undefined,
              lastSeenAtFrom: lastSeenAtFrom
                ? formatISO(lastSeenAtFrom, { representation: "date" })
                : undefined,
              lastSeenAtTo: lastSeenAtTo
                ? formatISO(lastSeenAtTo, { representation: "date" })
                : undefined,
              page,
              size: pageSize,
            }),
          });
        },
        onError: (error) => {
          handleApiError(error);
        },
      }
    );
  };

  const handleOpenDeleteDialog = (instanceId: string) => {
    setSelectedInstanceId(instanceId);
    setDeleteDialogOpen(true);
  };

  const handleCloseDeleteDialog = () => {
    setDeleteDialogOpen(false);
    setSelectedInstanceId(null);
  };

  const handleFilterReset = () => {
    setSearch("");
    setEnvironmentFilter("");
    setStatusFilter("");
    setDriftFilter("");
    setLastSeenAtFrom(null);
    setLastSeenAtTo(null);
    setShowAdvancedFilters(false);
    setPage(0);
    setSearchParams({}, { replace: true });
  };

  const handleDateRangeChange = (startDate: Date | null, endDate: Date | null) => {
    setLastSeenAtFrom(startDate);
    setLastSeenAtTo(endDate);
    setPage(0);
  };

  return (
    <Box>
      <PageHeader
        title="Service Instances"
        subtitle="Manage service instances tracked by the system"
        actions={
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={() => refetch()}
            aria-label="Refresh service instances"
          >
            Refresh
          </Button>
        }
      />

      <Card>
        <CardContent>
          {/* Filters */}
          <Grid container spacing={2} sx={{ mb: 3 }}>
            <Grid size={{ xs: 12, md: 3 }}>
              <TextField
                fullWidth
                label="Search by Service Name"
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value);
                  setPage(0);
                }}
                slotProps={{
                  input: {
                    startAdornment: (
                      <InputAdornment position="start">
                        <SearchIcon />
                      </InputAdornment>
                    ),
                    "aria-label": "Search by service name",
                  },
                }}
              />
            </Grid>

            <Grid size={{ xs: 12, md: 2 }}>
              <FormControl fullWidth>
                <InputLabel>Environment</InputLabel>
                <Select
                  value={environmentFilter}
                  label="Environment"
                  onChange={(e) => {
                    setEnvironmentFilter(
                      e.target.value as FindAllServiceInstancesEnvironment | ""
                    );
                    setPage(0);
                  }}
                  aria-label="Filter by environment"
                >
                  <MenuItem value="">All</MenuItem>
                  <MenuItem value="dev">Development</MenuItem>
                  <MenuItem value="staging">Staging</MenuItem>
                  <MenuItem value="prod">Production</MenuItem>
                </Select>
              </FormControl>
            </Grid>

            <Grid size={{ xs: 12, md: 2 }}>
              <FormControl fullWidth>
                <InputLabel>Status</InputLabel>
                <Select
                  value={statusFilter}
                  label="Status"
                  onChange={(e) => {
                    setStatusFilter(
                      e.target.value as FindAllServiceInstancesStatus | ""
                    );
                    setPage(0);
                  }}
                  aria-label="Filter by status"
                >
                  <MenuItem value="">All</MenuItem>
                  <MenuItem value="HEALTHY">Healthy</MenuItem>
                  <MenuItem value="UNHEALTHY">Unhealthy</MenuItem>
                  <MenuItem value="DRIFT">Drift</MenuItem>
                  <MenuItem value="UNKNOWN">Unknown</MenuItem>
                </Select>
              </FormControl>
            </Grid>

            <Grid size={{ xs: 12, md: 2 }}>
              <FormControl fullWidth>
                <InputLabel>Drift Status</InputLabel>
                <Select
                  value={driftFilter}
                  label="Drift Status"
                  onChange={(e) => {
                    setDriftFilter(e.target.value as "true" | "false" | "");
                    setPage(0);
                  }}
                  aria-label="Filter by drift status"
                >
                  <MenuItem value="">All</MenuItem>
                  <MenuItem value="true">Has Drift</MenuItem>
                  <MenuItem value="false">No Drift</MenuItem>
                </Select>
              </FormControl>
            </Grid>

            <Grid size={{ xs: 12, md: 1 }}>
              <Button
                fullWidth
                variant="outlined"
                onClick={() => setShowAdvancedFilters(!showAdvancedFilters)}
                endIcon={showAdvancedFilters ? <ExpandLessIcon /> : <ExpandMoreIcon />}
                sx={{ height: "56px" }}
                aria-label="Toggle advanced filters"
              >
                {showAdvancedFilters ? "Less" : "More"}
              </Button>
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

          {/* Advanced Filters */}
          <Collapse in={showAdvancedFilters}>
            <Grid container spacing={2} sx={{ mb: 2, mt: 1 }}>
              <Grid size={{ xs: 12, md: 6 }}>
                <DateRangeFilter
                  label="Last Seen Date Range"
                  startDate={lastSeenAtFrom}
                  endDate={lastSeenAtTo}
                  onChange={handleDateRangeChange}
                  helperText="Filter instances by last seen date"
                />
              </Grid>
            </Grid>
          </Collapse>

          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              Failed to load service instances:{" "}
              {error instanceof Error ? error.message : "Unknown error"}
            </Alert>
          )}

          {isLoading && <TableSkeleton rows={10} columns={7} />}

          {!isLoading && !error && (
            <ServiceInstanceTable
              instances={instances}
              loading={isLoading}
              page={page}
              pageSize={pageSize}
              totalElements={metadata?.totalElements || 0}
              onPageChange={(newPage: number) => setPage(newPage)}
              onPageSizeChange={(newPageSize: number) => {
                setPageSize(newPageSize);
                setPage(0);
              }}
              onRowClick={(instanceId: string) => {
                const instance = instances.find(
                  (i) => i.instanceId === instanceId
                );
                if (instance) {
                  navigate(
                    `/service-instances/${instance.serviceName}/${instance.instanceId}`
                  );
                }
              }}
              onDelete={handleOpenDeleteDialog}
            />
          )}
        </CardContent>
      </Card>

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        open={deleteDialogOpen}
        title="Delete Service Instance"
        message={`Are you sure you want to delete this instance? This action cannot be undone.`}
        confirmText="Delete"
        cancelText="Cancel"
        onConfirm={handleDeleteInstance}
        onCancel={handleCloseDeleteDialog}
        loading={deleteMutation.isPending}
      />
    </Box>
  );
}
