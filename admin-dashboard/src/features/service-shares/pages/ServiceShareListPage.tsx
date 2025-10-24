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
} from '@mui/material';
import Grid from '@mui/material/Grid';
import { Search as SearchIcon, Refresh as RefreshIcon, Add as AddIcon } from '@mui/icons-material';
import PageHeader from '@components/common/PageHeader';
import Loading from '@components/common/Loading';
import ConfirmDialog from '@components/common/ConfirmDialog';
import { useFindAllServiceShares, useRevokeServiceShare } from '@lib/api/hooks';
import { useAuth } from '@features/auth/authContext';
import { toast } from '@lib/toast/toast';
import { handleApiError } from '@lib/api/errorHandler';
import { ServiceShareTable } from '../components/ServiceShareTable';
import { ShareFormDrawer } from '../components/ShareFormDrawer';

export default function ServiceShareListPage() {
  const navigate = useNavigate();
  const { isSysAdmin } = useAuth();

  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [search, setSearch] = useState('');
  const [grantToTypeFilter, setGrantToTypeFilter] = useState('');
  const [formDrawerOpen, setFormDrawerOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [selectedShareId, setSelectedShareId] = useState<string | null>(null);

  const { data, isLoading, error, refetch } = useFindAllServiceShares(
    {
      serviceId: search || undefined,
      grantToType: (grantToTypeFilter || undefined) as any,
      page,
      size: pageSize,
    },
    {
      query: {
        staleTime: 30_000,
      },
    }
  );

  const revokeShareMutation = useRevokeServiceShare();

  const shares = Array.isArray(data) ? data : (data as any)?.items || [];
  const metadata = (data as any)?.metadata;

  const handleRevokeShare = async () => {
    if (!selectedShareId) return;

    revokeShareMutation.mutate(
      { id: selectedShareId },
      {
        onSuccess: () => {
          toast.success('Service share revoked successfully');
          setDeleteDialogOpen(false);
          setSelectedShareId(null);
          refetch();
        },
        onError: (error) => {
          handleApiError(error);
        },
      }
    );
  };

  const handleOpenRevokeDialog = (shareId: string) => {
    setSelectedShareId(shareId);
    setDeleteDialogOpen(true);
  };

  const handleCloseRevokeDialog = () => {
    setDeleteDialogOpen(false);
    setSelectedShareId(null);
  };

  const handleFilterReset = () => {
    setSearch('');
    setGrantToTypeFilter('');
    setPage(0);
  };

  const handleShareSuccess = () => {
    toast.success('Service share created successfully');
    setFormDrawerOpen(false);
    refetch();
  };

  return (
    <Box>
      <PageHeader
        title="Service Shares"
        subtitle="Manage service access shares"
        actions={
          <>
            <Button variant="outlined" startIcon={<RefreshIcon />} onClick={() => refetch()}>
              Refresh
            </Button>
            {isSysAdmin && (
              <Button
                variant="contained"
                startIcon={<AddIcon />}
                onClick={() => setFormDrawerOpen(true)}
              >
                Create Share
              </Button>
            )}
          </>
        }
      />

      <Card>
        <CardContent>
          {/* Filters */}
          <Grid container spacing={2} sx={{ mb: 3 }}>
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField
                fullWidth
                label="Search by Service ID"
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

            <Grid size={{ xs: 12, md: 3 }}>
              <FormControl fullWidth>
                <InputLabel>Grant To Type</InputLabel>
                <Select
                  value={grantToTypeFilter}
                  label="Grant To Type"
                  onChange={(e) => {
                    setGrantToTypeFilter(e.target.value);
                    setPage(0);
                  }}
                >
                  <MenuItem value="">All</MenuItem>
                  <MenuItem value="TEAM">Team</MenuItem>
                  <MenuItem value="USER">User</MenuItem>
                </Select>
              </FormControl>
            </Grid>

            <Grid size={{ xs: 12, md: 5 }}>
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
              Failed to load service shares: {(error as any).detail || 'Unknown error'}
            </Alert>
          )}

          {isLoading && <Loading />}

          {!isLoading && !error && (
            <ServiceShareTable
              shares={shares}
              loading={isLoading}
              page={page}
              pageSize={pageSize}
              totalElements={metadata?.totalElements || shares.length}
              onPageChange={(newPage: number) => setPage(newPage)}
              onPageSizeChange={(newPageSize: number) => {
                setPageSize(newPageSize);
                setPage(0);
              }}
              onRowClick={(shareId: string) => navigate(`/service-shares/${shareId}`)}
              onRevoke={handleOpenRevokeDialog}
            />
          )}
        </CardContent>
      </Card>

      {/* Create Share Drawer */}
      <ShareFormDrawer
        open={formDrawerOpen}
        onClose={() => setFormDrawerOpen(false)}
        onSuccess={handleShareSuccess}
      />

      {/* Revoke Confirmation Dialog */}
      <ConfirmDialog
        open={deleteDialogOpen}
        title="Revoke Service Share"
        message="Are you sure you want to revoke this service share? The user/team will lose access immediately."
        confirmText="Revoke"
        cancelText="Cancel"
        onConfirm={handleRevokeShare}
        onCancel={handleCloseRevokeDialog}
        loading={revokeShareMutation.isPending}
      />
    </Box>
  );
}
