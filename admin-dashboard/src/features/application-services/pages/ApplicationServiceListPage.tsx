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
  Drawer,
  FormControlLabel,
  Switch,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
} from '@mui/material';
import Grid from '@mui/material/Grid';
import { Add as AddIcon, Search as SearchIcon, Refresh as RefreshIcon } from '@mui/icons-material';
import PageHeader from '@components/common/PageHeader';
import Loading from '@components/common/Loading';
import ConfirmDialog from '@components/common/ConfirmDialog';
import {
  useFindAllApplicationServices,
  useDeleteApplicationService,
  useCreateApprovalRequest,
} from '@lib/api/hooks';
import { useAuth } from '@features/auth/authContext';
import { toast } from '@lib/toast/toast';
import { handleApiError } from '@lib/api/errorHandler';
import { ApplicationServiceTable } from '../components/ApplicationServiceTable';
import { ApplicationServiceForm } from '../components/ApplicationServiceForm';
import type { FindAllApplicationServicesParams } from '@lib/api/models';

export default function ApplicationServiceListPage() {
  const navigate = useNavigate();
  const { isSysAdmin, permissions, userInfo } = useAuth();

  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [search, setSearch] = useState('');
  const [lifecycleFilter, setLifecycleFilter] = useState<FindAllApplicationServicesParams['lifecycle'] | ''>('');
  const [ownerTeamFilter, setOwnerTeamFilter] = useState('');
  const [environmentFilter, setEnvironmentFilter] = useState('');
  const [showUnassignedOnly, setShowUnassignedOnly] = useState(false);
  
  const [formDrawerOpen, setFormDrawerOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [ownershipRequestDialogOpen, setOwnershipRequestDialogOpen] = useState(false);
  const [selectedServiceId, setSelectedServiceId] = useState<string | null>(null);
  const [ownershipNote, setOwnershipNote] = useState('');

  const { data, isLoading, error, refetch } = useFindAllApplicationServices(
    {
      search: search || undefined,
      ownerTeamId: ownerTeamFilter || undefined,
      lifecycle: lifecycleFilter || undefined,
      page,
      size: pageSize,
    },
    {
      query: {
        staleTime: 30_000,
      },
    }
  );

  const deleteMutation = useDeleteApplicationService();
  const createApprovalRequestMutation = useCreateApprovalRequest();

  // Server-side filtering handles visibility (orphaned + team-owned + shared services)
  // No client-side filtering needed - trust server response
  const services = data?.items || [];
  const metadata = data?.metadata;

  const canCreate = isSysAdmin || permissions?.actions?.['APPLICATION_SERVICE']?.includes('CREATE');

  const handleDeleteService = async () => {
    if (!selectedServiceId) return;

    deleteMutation.mutate(
      { id: selectedServiceId },
      {
        onSuccess: () => {
          toast.success('Service deleted successfully');
          setDeleteDialogOpen(false);
          setSelectedServiceId(null);
          refetch();
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
    setSearch('');
    setLifecycleFilter('');
    setOwnerTeamFilter('');
    setEnvironmentFilter('');
    setShowUnassignedOnly(false);
    setPage(0);
  };

  const handleCreateSuccess = () => {
    toast.success('Service created successfully');
    setFormDrawerOpen(false);
    refetch();
  };

  const handleRequestOwnership = (serviceId: string) => {
    setSelectedServiceId(serviceId);
    setOwnershipRequestDialogOpen(true);
    setOwnershipNote('');
  };

  const handleSubmitOwnershipRequest = async () => {
    if (!selectedServiceId || !userInfo?.teamIds?.[0]) {
      toast.error('Cannot submit ownership request: No team found');
      return;
    }

    createApprovalRequestMutation.mutate(
      {
        serviceId: selectedServiceId,
        data: {
          serviceId: selectedServiceId,
          targetTeamId: userInfo.teamIds[0],
          note: ownershipNote || undefined,
        },
      },
      {
        onSuccess: () => {
          toast.success('Ownership request submitted successfully');
          setOwnershipRequestDialogOpen(false);
          setSelectedServiceId(null);
          setOwnershipNote('');
          refetch();
        },
        onError: (error) => {
          handleApiError(error);
        },
      }
    );
  };

  const handleCancelOwnershipRequest = () => {
    setOwnershipRequestDialogOpen(false);
    setSelectedServiceId(null);
    setOwnershipNote('');
  };

  return (
    <Box>
      <PageHeader
        title="Application Services"
        subtitle="Manage registered application services"
        actions={
          <>
            <Button
              variant="outlined"
              startIcon={<RefreshIcon />}
              onClick={() => refetch()}
            >
              Refresh
            </Button>
            {canCreate && (
              <Button
                variant="contained"
                startIcon={<AddIcon />}
                onClick={() => setFormDrawerOpen(true)}
              >
                Create Service
              </Button>
            )}
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
                label="Search by Name"
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
                <InputLabel>Lifecycle</InputLabel>
                <Select
                  value={lifecycleFilter}
                  label="Lifecycle"
                  onChange={(e) => {
                    setLifecycleFilter(e.target.value as FindAllApplicationServicesParams['lifecycle'] | '');
                    setPage(0);
                  }}
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
              />
            </Grid>

            <Grid size={{ xs: 12, md: 2 }}>
              <FormControlLabel
                control={
                  <Switch
                    checked={showUnassignedOnly}
                    onChange={(e) => {
                      setShowUnassignedOnly(e.target.checked);
                      if (e.target.checked) {
                        setOwnerTeamFilter('');
                      }
                      setPage(0);
                    }}
                  />
                }
                label="Unassigned Only"
                sx={{ height: '56px', display: 'flex', alignItems: 'center' }}
              />
            </Grid>

            <Grid size={{ xs: 12, md: 1 }}>
              <Button
                fullWidth
                variant="outlined"
                onClick={handleFilterReset}
                sx={{ height: '56px' }}
              >
                Reset
              </Button>
            </Grid>
          </Grid>

          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              Failed to load services: {(error as any).detail || 'Unknown error'}
            </Alert>
          )}

          {isLoading && <Loading />}

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
              onRowClick={(serviceId: string) => navigate(`/application-services/${serviceId}`)}
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
        PaperProps={{
          sx: { width: { xs: '100%', sm: 600 } },
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

      {/* Ownership Request Dialog */}
      <Dialog
        open={ownershipRequestDialogOpen}
        onClose={handleCancelOwnershipRequest}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Request Service Ownership</DialogTitle>
        <DialogContent>
          <DialogContentText sx={{ mb: 2 }}>
            Submit a request to claim ownership of this service for your team. The request will need approval from system administrators.
          </DialogContentText>
          <TextField
            autoFocus
            margin="dense"
            label="Note (Optional)"
            placeholder="Explain why your team should own this service..."
            fullWidth
            multiline
            rows={4}
            value={ownershipNote}
            onChange={(e) => setOwnershipNote(e.target.value)}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCancelOwnershipRequest} disabled={createApprovalRequestMutation.isPending}>
            Cancel
          </Button>
          <Button
            onClick={handleSubmitOwnershipRequest}
            variant="contained"
            disabled={createApprovalRequestMutation.isPending}
          >
            Submit Request
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
