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
} from '@mui/material';
import Grid from '@mui/material/Grid';
import { Add as AddIcon, Search as SearchIcon, Refresh as RefreshIcon } from '@mui/icons-material';
import PageHeader from '@components/common/PageHeader';
import Loading from '@components/common/Loading';
import ConfirmDialog from '@components/common/ConfirmDialog';
import {
  useFindAllApplicationServices,
  useDeleteApplicationService,
} from '@lib/api/hooks';
import { useAuth } from '@features/auth/authContext';
import { toast } from '@lib/toast/toast';
import { handleApiError } from '@lib/api/errorHandler';
import { ApplicationServiceTable } from '../components/ApplicationServiceTable';
import { ApplicationServiceForm } from '../components/ApplicationServiceForm';
import { ClaimOwnershipDialog } from '../components/ClaimOwnershipDialog';
import type { ApplicationServiceResponse, FindAllApplicationServicesParams} from '@lib/api/models';

export default function ApplicationServiceListPage() {
  const navigate = useNavigate();
  const { isSysAdmin, permissions } = useAuth();

  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [search, setSearch] = useState('');
  const [lifecycleFilter, setLifecycleFilter] = useState<FindAllApplicationServicesParams['lifecycle'] | ''>('');
  const [ownerTeamFilter, setOwnerTeamFilter] = useState('');
  const [environmentFilter, setEnvironmentFilter] = useState('');
  const [showUnassignedOnly, setShowUnassignedOnly] = useState(false);
  
  const [formDrawerOpen, setFormDrawerOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [claimDialogOpen, setClaimDialogOpen] = useState(false);
  const [selectedServiceId, setSelectedServiceId] = useState<string | null>(null);
  const [selectedServiceName, setSelectedServiceName] = useState<string>('');

  const { data, isLoading, error, refetch } = useFindAllApplicationServices(
    {
      search: search || undefined,
      ownerTeamId: showUnassignedOnly ? 'null' : (ownerTeamFilter || undefined),
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

  // Server-side filtering handles visibility (orphaned + team-owned + shared services)
  // No client-side filtering needed - trust server response
  const services: ApplicationServiceResponse[] = data?.items || [];
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
    const service = services.find(s => s.id === serviceId);
    setSelectedServiceId(serviceId);
    setSelectedServiceName(service?.displayName || serviceId);
    setClaimDialogOpen(true);
  };

  const handleCloseClaimDialog = () => {
    setClaimDialogOpen(false);
    setSelectedServiceId(null);
    setSelectedServiceName('');
  };

  const handleClaimSuccess = (_requestId: string) => {
    toast.success('Ownership request submitted successfully');
    refetch();
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
                label="Orphans Only"
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
              Failed to load services: {error && typeof error === 'object' && 'detail' in error ? (error as { detail?: string }).detail : 'Unknown error'}
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

      {/* Claim Ownership Dialog */}
      <ClaimOwnershipDialog
        open={claimDialogOpen}
        serviceId={selectedServiceId || ''}
        serviceName={selectedServiceName}
        onClose={handleCloseClaimDialog}
        onSuccess={handleClaimSuccess}
      />
    </Box>
  );
}
