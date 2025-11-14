import { useState, useCallback, useMemo } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import { keepPreviousData } from "@tanstack/react-query";
import {
  Box,
  Button,
  Card,
  CardContent,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Alert,
  Drawer,
  Tooltip,
} from "@mui/material";
import Grid from "@mui/material/Grid";
import {
  Add as AddIcon,
  Refresh as RefreshIcon,
} from "@mui/icons-material";
import PageHeader from "@components/common/PageHeader";
import { TableSkeleton } from "@components/common/skeletons";
import ConfirmDialog from "@components/common/ConfirmDialog";
import { SearchFieldWithToggle } from "@components/common/SearchFieldWithToggle";
import {
  useFindAllApplicationServices,
  useDeleteApplicationService,
} from "@lib/api/hooks";
import { getFindAllApplicationServicesQueryKey } from "@lib/api/generated/application-services/application-services";
import { useAuth } from "@features/auth/context";
import { toast } from "@lib/toast/toast";
import { handleApiError } from "@lib/api/errorHandler";
import { ApplicationServiceTable } from "../components/ApplicationServiceTable";
import { ApplicationServiceForm } from "../components/ApplicationServiceForm";
import { ClaimOwnershipDialog } from "../components/ClaimOwnershipDialog";
import { useSearchWithToggle } from "@hooks/useSearchWithToggle";
import { useDebouncedUrlSync } from "@hooks/useDebouncedUrlSync";
import type {
  ApplicationServiceResponse,
  FindAllApplicationServicesParams,
} from "@lib/api/models";

export default function ApplicationServiceListPage() {
  const navigate = useNavigate();
  const { isSysAdmin, permissions } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const queryClient = useQueryClient();

  // Parse initial state from URL params
  const initialPage = parseInt(searchParams.get("page") || "0", 10);
  const initialPageSize = parseInt(searchParams.get("size") || "20", 10);

  const [page, setPage] = useState(initialPage);
  const [pageSize, setPageSize] = useState(initialPageSize);
  const [lifecycleFilter, setLifecycleFilter] = useState<
    FindAllApplicationServicesParams["lifecycle"] | ""
  >(
    (searchParams.get("lifecycle") as
      | FindAllApplicationServicesParams["lifecycle"]
      | null) || ""
  );
  const [ownerTeamFilter, setOwnerTeamFilter] = useState(
    searchParams.get("ownerTeamId") || ""
  );
  const [environmentFilter, setEnvironmentFilter] = useState(
    searchParams.get("environment") || ""
  );
  const [showUnassignedOnly, setShowUnassignedOnly] = useState(
    searchParams.get("unassignedOnly") === "true"
  );

  const [formDrawerOpen, setFormDrawerOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [claimDialogOpen, setClaimDialogOpen] = useState(false);
  const [selectedServiceId, setSelectedServiceId] = useState<string | null>(
    null
  );
  const [selectedServiceName, setSelectedServiceName] = useState<string>("");

  // Search with toggle hook
  const {
    search,
    setSearch,
    effectiveSearch,
    realtimeEnabled,
    setRealtimeEnabled,
    handleManualSearch,
    handleReset: resetSearch,
    isDebouncing,
  } = useSearchWithToggle({
    storageKey: "app-services-search-realtime",
    defaultRealtimeEnabled: true,
    debounceDelay: 800,
    initialSearch: searchParams.get("search") || "",
    onDebounceComplete: () => {
      // Reset page when debounce completes (search triggers)
      setPage(0);
    },
  });

  // Memoize search handlers to prevent unnecessary re-renders
  const handleSearchChange = useCallback(
    (value: string) => {
      setSearch(value);
    },
    [setSearch]
  );

  const handleSearchSubmit = useCallback(() => {
    handleManualSearch();
    setPage(0);
  }, [handleManualSearch]);

  const handleRealtimeToggle = useCallback(
    (enabled: boolean) => {
      setRealtimeEnabled(enabled);
      setPage(0);
    },
    [setRealtimeEnabled]
  );

  // Memoize values object to prevent unnecessary useDebouncedUrlSync triggers
  // This prevents the hook from running on every render when values haven't changed
  const urlSyncValues = useMemo(
    () => ({
      search: effectiveSearch || undefined,
      lifecycle: lifecycleFilter || undefined,
      ownerTeamId: showUnassignedOnly ? undefined : ownerTeamFilter || undefined,
      environment: environmentFilter || undefined,
      unassignedOnly: showUnassignedOnly ? true : undefined,
      page: page > 0 ? page : undefined,
      size: pageSize !== 20 ? pageSize : undefined,
    }),
    [
      effectiveSearch,
      lifecycleFilter,
      showUnassignedOnly,
      ownerTeamFilter,
      environmentFilter,
      page,
      pageSize,
    ]
  );

  // Debounced URL sync to prevent blocking UI thread during typing
  useDebouncedUrlSync({
    values: urlSyncValues,
    debounceDelay: 300, // Shorter delay for URL sync (search debounce is 800ms)
    enabled: true,
  });

  const { data, isLoading, error, refetch } = useFindAllApplicationServices(
    {
      search: effectiveSearch || undefined,
      ownerTeamId: showUnassignedOnly ? undefined : ownerTeamFilter || undefined,
      lifecycle: lifecycleFilter || undefined,
      page,
      size: pageSize,
      environment: environmentFilter as FindAllApplicationServicesParams["environment"] | undefined,
    },
    {
      query: {
        placeholderData: keepPreviousData, // Prevents flickering during refetch
      },
    }
  );

  const deleteMutation = useDeleteApplicationService();

  // Server-side filtering handles visibility (orphaned + team-owned + shared services)
  // No client-side filtering needed - trust server response
  const services: ApplicationServiceResponse[] = data?.items || [];
  const metadata = data?.metadata;

  const canCreate =
    isSysAdmin ||
    permissions?.actions?.["APPLICATION_SERVICE"]?.includes("CREATE");

  const handleDeleteService = async () => {
    if (!selectedServiceId) return;

    deleteMutation.mutate(
      { id: selectedServiceId },
      {
        onSuccess: () => {
          toast.success("Service deleted successfully");
          setDeleteDialogOpen(false);
          setSelectedServiceId(null);
          // Invalidate list query to refresh data
          queryClient.invalidateQueries({
            queryKey: getFindAllApplicationServicesQueryKey({
              search: effectiveSearch || undefined,
              ownerTeamId: showUnassignedOnly
                ? "null"
                : ownerTeamFilter || undefined,
              lifecycle: lifecycleFilter || undefined,
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

  const handleOpenDeleteDialog = (serviceId: string) => {
    setSelectedServiceId(serviceId);
    setDeleteDialogOpen(true);
  };

  const handleCloseDeleteDialog = () => {
    setDeleteDialogOpen(false);
    setSelectedServiceId(null);
  };

  const handleFilterReset = () => {
    resetSearch();
    setLifecycleFilter("");
    setOwnerTeamFilter("");
    setEnvironmentFilter("");
    setShowUnassignedOnly(false);
    setPage(0);
    setSearchParams({}, { replace: true });
  };

  const handleCreateSuccess = () => {
    toast.success("Service created successfully");
    setFormDrawerOpen(false);
    // Invalidate list query to refresh data
    queryClient.invalidateQueries({
      queryKey: getFindAllApplicationServicesQueryKey({
        search: effectiveSearch || undefined,
        ownerTeamId: showUnassignedOnly ? "null" : ownerTeamFilter || undefined,
        lifecycle: lifecycleFilter || undefined,
        page,
        size: pageSize,
      }),
    });
  };

  const handleRequestOwnership = (serviceId: string) => {
    const service = services.find((s) => s.id === serviceId);
    setSelectedServiceId(serviceId);
    setSelectedServiceName(service?.displayName || serviceId);
    setClaimDialogOpen(true);
  };

  const handleCloseClaimDialog = () => {
    setClaimDialogOpen(false);
    setSelectedServiceId(null);
    setSelectedServiceName("");
  };

  const handleClaimSuccess = () => {
    toast.success("Ownership request submitted successfully");
    // Invalidate list query to refresh data
    queryClient.invalidateQueries({
      queryKey: getFindAllApplicationServicesQueryKey({
        search: effectiveSearch || undefined,
        ownerTeamId: showUnassignedOnly ? "null" : ownerTeamFilter || undefined,
        lifecycle: lifecycleFilter || undefined,
        page,
        size: pageSize,
      }),
    });
  };

  return (
    <Box>
      <PageHeader
        title="Application Services"
        subtitle="Manage registered application services"
        actions={
          <>
            <Tooltip title="Refresh services list" placement="bottom">
              <Button
                variant="outlined"
                startIcon={<RefreshIcon />}
                onClick={() => refetch()}
                aria-label="Refresh application services"
              >
                Refresh
              </Button>
            </Tooltip>
            {canCreate && (
              <Tooltip
                title="Create a new application service"
                placement="bottom"
              >
                <Button
                  variant="contained"
                  startIcon={<AddIcon />}
                  onClick={() => setFormDrawerOpen(true)}
                >
                  Create Service
                </Button>
              </Tooltip>
            )}
          </>
        }
      />

      <Card>
        <CardContent>
          {/* Filters */}
          <Grid container spacing={2} sx={{ mb: 3 }}>
            <Grid size={{ xs: 12, md: 4 }}>
              <SearchFieldWithToggle
                value={search}
                onChange={handleSearchChange}
                onSearch={handleSearchSubmit}
                label="Search by Name"
                placeholder="Search by service name"
                realtimeEnabled={realtimeEnabled}
                onRealtimeToggle={handleRealtimeToggle}
                loading={isLoading}
                isDebouncing={isDebouncing}
                resultCount={metadata?.totalElements}
                helperText={
                  realtimeEnabled
                    ? "Search updates automatically as you type"
                    : "Click search button or press Enter to search"
                }
                aria-label="Search by service name"
              />
            </Grid>

            <Grid size={{ xs: 12, md: 2 }}>
              <FormControl fullWidth>
                <InputLabel>Lifecycle</InputLabel>
                <Select
                  value={lifecycleFilter}
                  label="Lifecycle"
                  onChange={(e) => {
                    setLifecycleFilter(
                      e.target.value as
                        | FindAllApplicationServicesParams["lifecycle"]
                        | ""
                    );
                    setPage(0);
                  }}
                  aria-label="Filter by lifecycle"
                >
                  <MenuItem value="">All</MenuItem>
                  <MenuItem value="ACTIVE">Active</MenuItem>
                  <MenuItem value="DEPRECATED">Deprecated</MenuItem>
                  <MenuItem value="RETIRED">Retired</MenuItem>
                </Select>
              </FormControl>
            </Grid>

            <Grid size={{ xs: 12, md: 2 }}>
              <FormControl fullWidth>
                <InputLabel>Environment</InputLabel>
                <Select
                  value={environmentFilter}
                  label="Environment"
                  onChange={(e) => {
                    setEnvironmentFilter(e.target.value);
                    setPage(0);
                  }}
                  aria-label="Filter by environment"
                >
                  <MenuItem value="">All</MenuItem>
                  <MenuItem value="dev">Dev</MenuItem>
                  <MenuItem value="staging">Staging</MenuItem>
                  <MenuItem value="prod">Prod</MenuItem>
                </Select>
              </FormControl>
            </Grid>

            <Grid size={{ xs: 12, md: 2 }}>
              <TextField
                fullWidth
                label="Owner Team ID"
                value={ownerTeamFilter}
                onChange={(e) => {
                  setOwnerTeamFilter(e.target.value);
                  setPage(0);
                }}
                disabled={showUnassignedOnly}
                slotProps={{
                  input: {
                    "aria-label": "Filter by owner team ID",
                  },
                }}
              />
            </Grid>

            {/* <Grid size={{ xs: 12, md: 2 }}>
              <FormControlLabel
                control={
                  <Switch
                    checked={showUnassignedOnly}
                    onChange={(e) => {
                      setShowUnassignedOnly(e.target.checked);
                      if (e.target.checked) {
                        setOwnerTeamFilter("");
                      }
                      setPage(0);
                    }}
                  />
                }
                label="Orphans Only"
                sx={{ height: "56px", display: "flex", alignItems: "center" }}
              />
            </Grid> */}

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
              Failed to load services:{" "}
              {error && typeof error === "object" && "detail" in error
                ? (error as { detail?: string }).detail
                : "Unknown error"}
            </Alert>
          )}

          {isLoading && <TableSkeleton rows={10} columns={6} />}

          {!isLoading && !error && (
            <ApplicationServiceTable
              services={services}
              loading={isLoading}
              page={page}
              pageSize={pageSize}
              totalElements={metadata?.totalElements || 0}
              onPageChange={(newPage: number) => setPage(newPage)}
              onPageSizeChange={(newPageSize: number) => {
                setPageSize(newPageSize);
                setPage(0);
              }}
              onRowClick={(serviceId: string) =>
                navigate(`/application-services/${serviceId}`)
              }
              onDelete={handleOpenDeleteDialog}
              onRequestOwnership={handleRequestOwnership}
            />
          )}
        </CardContent>
      </Card>

      {/* Create Service Drawer */}
      <Drawer
        anchor="right"
        open={formDrawerOpen}
        onClose={() => setFormDrawerOpen(false)}
        slotProps={{
          paper: {
            sx: { width: { xs: "100%", sm: 600 } },
          },
        }}
      >
        <ApplicationServiceForm
          mode="create"
          onSuccess={handleCreateSuccess}
          onCancel={() => setFormDrawerOpen(false)}
        />
      </Drawer>

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        open={deleteDialogOpen}
        title="Delete Application Service"
        message="Are you sure you want to delete this service? This action cannot be undone and will affect all associated instances."
        confirmText="Delete"
        cancelText="Cancel"
        onConfirm={handleDeleteService}
        onCancel={handleCloseDeleteDialog}
        loading={deleteMutation.isPending}
      />

      {/* Claim Ownership Dialog */}
      <ClaimOwnershipDialog
        open={claimDialogOpen}
        serviceId={selectedServiceId || ""}
        serviceName={selectedServiceName}
        onClose={handleCloseClaimDialog}
        onSuccess={handleClaimSuccess}
      />
    </Box>
  );
}
