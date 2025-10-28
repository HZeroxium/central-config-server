import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
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
} from '@mui/material';
import Grid from '@mui/material/Grid';
import { Search as SearchIcon, Refresh as RefreshIcon } from '@mui/icons-material';
import PageHeader from '@components/common/PageHeader';
import Loading from '@components/common/Loading';
import { useFindAllDriftEvents, useUpdateDriftEvent } from '@lib/api/hooks';
import { DriftEventTable } from '../components/DriftEventTable';
import { ResolveDialog } from '../components/ResolveDialog';
import { toast } from '@lib/toast/toast';
import { handleApiError } from '@lib/api/errorHandler';
import type { 
  FindAllDriftEventsStatus, 
  FindAllDriftEventsSeverity,
  DriftEventUpdateRequest,
  DriftEventUpdateRequestStatus
} from '@lib/api/models';

export default function DriftEventListPage() {
  const navigate = useNavigate();

  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<FindAllDriftEventsStatus | ''>('');
  const [severityFilter, setSeverityFilter] = useState<FindAllDriftEventsSeverity | ''>('');
  const [unresolvedOnly, setUnresolvedOnly] = useState(false);
  const [autoRefresh, setAutoRefresh] = useState(false);
  const [resolveDialogOpen, setResolveDialogOpen] = useState(false);
  const [selectedEventId, setSelectedEventId] = useState<string | null>(null);
  const [resolveAction, setResolveAction] = useState<DriftEventUpdateRequestStatus>('RESOLVED');

  const { data, isLoading, error, refetch } = useFindAllDriftEvents(
    {
      serviceName: search || undefined,
      status: statusFilter || undefined,
      severity: severityFilter || undefined,
      unresolvedOnly: unresolvedOnly ? 'true' : undefined,
      page,
      size: pageSize,
    },
    {
      query: {
        staleTime: 10_000,
        refetchInterval: 30000, // 30 seconds
        refetchIntervalInBackground: false,
      },
    }
  );

  const events = data?.items || [];
  const metadata = data?.metadata;

  const updateMutation = useUpdateDriftEvent();

  const handleResolve = (eventId: string) => {
    setSelectedEventId(eventId);
    setResolveAction('RESOLVED');
    setResolveDialogOpen(true);
  };

  const handleIgnore = (eventId: string) => {
    setSelectedEventId(eventId);
    setResolveAction('IGNORED');
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
          toast.success(`Drift event ${resolveAction?.toLowerCase() || 'updated'} successfully`);
          setResolveDialogOpen(false);
          setSelectedEventId(null);
          refetch();
        },
        onError: (error) => {
          handleApiError(error);
        },
      }
    );
  };

  const handleFilterReset = () => {
    setSearch('');
    setStatusFilter('');
    setSeverityFilter('');
    setUnresolvedOnly(false);
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
            <Button variant="outlined" startIcon={<RefreshIcon />} onClick={() => refetch()}>
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
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <SearchIcon />
                    </InputAdornment>
                  ),
                }}
              />
            </Grid>

            <Grid size={{ xs: 12, md: 2 }}>
              <FormControl fullWidth>
                <InputLabel>Status</InputLabel>
                <Select
                  value={statusFilter}
                  label="Status"
                  onChange={(e) => {
                    setStatusFilter(e.target.value as FindAllDriftEventsStatus | '');
                    setPage(0);
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
                    setSeverityFilter(e.target.value as FindAllDriftEventsSeverity | '');
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

            <Grid size={{ xs: 12, md: 3 }}>
              <Button
                fullWidth
                variant="outlined"
                onClick={handleFilterReset}
                sx={{ height: '56px' }}
              >
                Reset Filters
              </Button>
            </Grid>
          </Grid>

          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              Failed to load drift events: {(error as any).detail || 'Unknown error'}
            </Alert>
          )}

          {isLoading && <Loading />}

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
              onRowClick={(eventId: string) => navigate(`/drift-events/${eventId}`)}
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
        eventTitle={selectedEventId || ''}
      />
    </Box>
  );
}
