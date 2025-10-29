import { useState, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
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
  Switch,
  FormControlLabel,
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
import { TableSkeleton } from "@components/common/skeletons";
import { DateRangeFilter } from "@components/common/filters";
import { useFindAllDriftEvents, useUpdateDriftEvent } from "@lib/api/hooks";
import { getFindAllDriftEventsQueryKey } from "@lib/api/generated/drift-events/drift-events";
import { DriftEventTable } from "../components/DriftEventTable";
import { ResolveDialog } from "../components/ResolveDialog";
import { toast } from "@lib/toast/toast";
import { handleApiError } from "@lib/api/errorHandler";
import { useDebounce } from "@hooks/useDebounce";
import { formatISO } from "date-fns";
import type {
  FindAllDriftEventsStatus,
  FindAllDriftEventsSeverity,
  DriftEventUpdateRequest,
  DriftEventUpdateRequestStatus,
} from "@lib/api/models";

export default function DriftEventListPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const queryClient = useQueryClient();

  // Parse initial state from URL params
  const initialPage = parseInt(searchParams.get("page") || "0", 10);
  const initialPageSize = parseInt(searchParams.get("size") || "20", 10);

  const [page, setPage] = useState(initialPage);
  const [pageSize, setPageSize] = useState(initialPageSize);
  const [search, setSearch] = useState(searchParams.get("search") || "");
  const [statusFilter, setStatusFilter] = useState<
    FindAllDriftEventsStatus | ""
  >((searchParams.get("status") as FindAllDriftEventsStatus | null) || "");
  const [severityFilter, setSeverityFilter] = useState<
    FindAllDriftEventsSeverity | ""
  >((searchParams.get("severity") as FindAllDriftEventsSeverity | null) || "");
  const [unresolvedOnly, setUnresolvedOnly] = useState(
    searchParams.get("unresolvedOnly") === "true"
  );
  const [detectedAtFrom, setDetectedAtFrom] = useState<Date | null>(
    searchParams.get("detectedAtFrom")
      ? new Date(searchParams.get("detectedAtFrom")!)
      : null
  );
  const [detectedAtTo, setDetectedAtTo] = useState<Date | null>(
    searchParams.get("detectedAtTo")
      ? new Date(searchParams.get("detectedAtTo")!)
      : null
  );
  const [showAdvancedFilters, setShowAdvancedFilters] = useState(
    searchParams.get("detectedAtFrom") !== null ||
      searchParams.get("detectedAtTo") !== null
  );
  const [autoRefresh, setAutoRefresh] = useState(false);
  const [resolveDialogOpen, setResolveDialogOpen] = useState(false);
  const [selectedEventId, setSelectedEventId] = useState<string | null>(null);
  const [resolveAction, setResolveAction] =
    useState<DriftEventUpdateRequestStatus>("RESOLVED");

  // Debounce search input
  const debouncedSearch = useDebounce(search, 400);

  // Sync URL params when filters change
  useEffect(() => {
    const params = new URLSearchParams();
    if (debouncedSearch) params.set("search", debouncedSearch);
    if (statusFilter) params.set("status", statusFilter);
    if (severityFilter) params.set("severity", severityFilter);
    if (unresolvedOnly) params.set("unresolvedOnly", "true");
    if (detectedAtFrom) {
      params.set(
        "detectedAtFrom",
        formatISO(detectedAtFrom, { representation: "date" })
      );
    }
    if (detectedAtTo) {
      params.set(
        "detectedAtTo",
        formatISO(detectedAtTo, { representation: "date" })
      );
    }
    if (page > 0) params.set("page", page.toString());
    if (pageSize !== 20) params.set("size", pageSize.toString());
    setSearchParams(params, { replace: true });
  }, [
    debouncedSearch,
    statusFilter,
    severityFilter,
    unresolvedOnly,
    detectedAtFrom,
    detectedAtTo,
    page,
    pageSize,
    setSearchParams,
  ]);

  const { data, isLoading, error, refetch } = useFindAllDriftEvents(
    {
      serviceName: debouncedSearch || undefined,
      status: statusFilter || undefined,
      severity: severityFilter || undefined,
      unresolvedOnly: unresolvedOnly ? "true" : undefined,
      detectedAtFrom: detectedAtFrom
        ? formatISO(detectedAtFrom, { representation: "date" })
        : undefined,
      detectedAtTo: detectedAtTo
        ? formatISO(detectedAtTo, { representation: "date" })
        : undefined,
      page,
      size: pageSize,
    },
    {
      query: {
        staleTime: 10_000,
        refetchInterval: autoRefresh ? 30000 : undefined, // 30 seconds if auto-refresh enabled
        refetchIntervalInBackground: false,
      },
    }
  );

  const events = data?.items || [];
  const metadata = data?.metadata;

  const updateMutation = useUpdateDriftEvent();

  const handleResolve = (eventId: string) => {
    setSelectedEventId(eventId);
    setResolveAction("RESOLVED");
    setResolveDialogOpen(true);
  };

  const handleIgnore = (eventId: string) => {
    setSelectedEventId(eventId);
    setResolveAction("IGNORED");
    setResolveDialogOpen(true);
  };

  const handleSubmitResolution = (updateRequest: DriftEventUpdateRequest) => {
    if (!selectedEventId) return;

    updateMutation.mutate(
      {
        id: selectedEventId,
        data: {
          ...updateRequest,
          status: updateRequest.status || resolveAction,
        },
      },
      {
        onSuccess: () => {
          toast.success(
            `Drift event ${
              resolveAction?.toLowerCase() || "updated"
            } successfully`
          );
          setResolveDialogOpen(false);
          setSelectedEventId(null);
          // Invalidate list query to refresh data
          queryClient.invalidateQueries({
            queryKey: getFindAllDriftEventsQueryKey({
              serviceName: debouncedSearch || undefined,
              status: statusFilter || undefined,
              severity: severityFilter || undefined,
              unresolvedOnly: unresolvedOnly ? "true" : undefined,
              detectedAtFrom: detectedAtFrom
                ? formatISO(detectedAtFrom, { representation: "date" })
                : undefined,
              detectedAtTo: detectedAtTo
                ? formatISO(detectedAtTo, { representation: "date" })
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

  const handleFilterReset = () => {
    setSearch("");
    setStatusFilter("");
    setSeverityFilter("");
    setUnresolvedOnly(false);
    setDetectedAtFrom(null);
    setDetectedAtTo(null);
    setShowAdvancedFilters(false);
    setPage(0);
    setSearchParams({}, { replace: true });
  };

  const handleDateRangeChange = (
    startDate: Date | null,
    endDate: Date | null
  ) => {
    setDetectedAtFrom(startDate);
    setDetectedAtTo(endDate);
    setPage(0);
  };

  return (
    <Box>
      <PageHeader
        title="Drift Events"
        subtitle="Monitor configuration drift across service instances"
        actions={
          <>
            <FormControlLabel
              control={
                <Switch
                  checked={autoRefresh}
                  onChange={(e) => setAutoRefresh(e.target.checked)}
                />
              }
              label="Auto-refresh"
            />
            <Button
              variant="outlined"
              startIcon={<RefreshIcon />}
              onClick={() => refetch()}
              aria-label="Refresh drift events"
            >
              Refresh
            </Button>
          </>
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
                  },
                }}
              />
            </Grid>

            <Grid size={{ xs: 12, md: 2 }}>
              <FormControl fullWidth>
                <InputLabel>Status</InputLabel>
                <Select
                  value={statusFilter}
                  onChange={(e) => {
                    setStatusFilter(
                      e.target.value as FindAllDriftEventsStatus | ""
                    );
                  }}
                >
                  <MenuItem value="">All</MenuItem>
                  <MenuItem value="DETECTED">Detected</MenuItem>
                  <MenuItem value="RESOLVED">Resolved</MenuItem>
                  <MenuItem value="IGNORED">Ignored</MenuItem>
                </Select>
              </FormControl>
            </Grid>

            <Grid size={{ xs: 12, md: 2 }}>
              <FormControl fullWidth>
                <InputLabel>Severity</InputLabel>
                <Select
                  value={severityFilter}
                  label="Severity"
                  onChange={(e) => {
                    setSeverityFilter(
                      e.target.value as FindAllDriftEventsSeverity | ""
                    );
                    setPage(0);
                  }}
                >
                  <MenuItem value="">All</MenuItem>
                  <MenuItem value="LOW">Low</MenuItem>
                  <MenuItem value="MEDIUM">Medium</MenuItem>
                  <MenuItem value="HIGH">High</MenuItem>
                  <MenuItem value="CRITICAL">Critical</MenuItem>
                </Select>
              </FormControl>
            </Grid>

            <Grid size={{ xs: 12, md: 2 }}>
              <FormControlLabel
                control={
                  <Switch
                    checked={unresolvedOnly}
                    onChange={(e) => {
                      setUnresolvedOnly(e.target.checked);
                      setPage(0);
                    }}
                  />
                }
                label="Unresolved Only"
                sx={{ mt: 2 }}
              />
            </Grid>

            <Grid size={{ xs: 12, md: 1 }}>
              <Button
                fullWidth
                variant="outlined"
                onClick={() => setShowAdvancedFilters(!showAdvancedFilters)}
                endIcon={
                  showAdvancedFilters ? <ExpandLessIcon /> : <ExpandMoreIcon />
                }
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
                  label="Detected Date Range"
                  startDate={detectedAtFrom}
                  endDate={detectedAtTo}
                  onChange={handleDateRangeChange}
                  helperText="Filter events by detection date"
                />
              </Grid>
            </Grid>
          </Collapse>

          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              Failed to load drift events: {error.detail || "Unknown error"}
            </Alert>
          )}

          {isLoading && <TableSkeleton rows={10} columns={6} />}

          {!isLoading && !error && (
            <DriftEventTable
              events={events}
              loading={isLoading}
              page={page}
              pageSize={pageSize}
              totalElements={metadata?.totalElements || 0}
              onPageChange={(newPage: number) => setPage(newPage)}
              onPageSizeChange={(newPageSize: number) => {
                setPageSize(newPageSize);
                setPage(0);
              }}
              onRowClick={(eventId: string) =>
                navigate(`/drift-events/${eventId}`)
              }
              onResolve={handleResolve}
              onIgnore={handleIgnore}
            />
          )}
        </CardContent>
      </Card>

      {/* Resolve/Ignore Dialog */}
      <ResolveDialog
        open={resolveDialogOpen}
        onClose={() => {
          setResolveDialogOpen(false);
          setSelectedEventId(null);
        }}
        onSubmit={handleSubmitResolution}
        loading={updateMutation.isPending}
        eventTitle={selectedEventId || ""}
      />
    </Box>
  );
}
