import React, { useState } from 'react';
import { Box, Button, TextField, InputAdornment, FormControl, InputLabel, Select, MenuItem, Chip } from '@mui/material';
import { Search as SearchIcon, Add as AddIcon, FilterList as FilterIcon } from '@mui/icons-material';
import { PageHeader } from '@components/common/PageHeader';
import { ServiceShareTable } from '../components/ServiceShareTable';
import { ShareFormDrawer } from '../components/ShareFormDrawer';
import { ConfirmDialog } from '@components/common/ConfirmDialog';
import {
  useFindAllServiceShares,
  useGrantServiceShare,
  useRevokeServiceShare,
} from '@lib/api/hooks';
import { useErrorHandler } from '../../../hooks/useErrorHandler';
import type { ServiceShare, ServiceShareFilter } from '../types';
import { usePermissions } from '@features/auth/hooks/usePermissions';

export const ServiceShareListPage: React.FC = () => {
  const [page] = useState(0);
  const [pageSize] = useState(10);
  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState<ServiceShareFilter>({});
  const [formOpen, setFormOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [selectedShare, setSelectedShare] = useState<ServiceShare | null>(null);
  const [showFilters, setShowFilters] = useState(false);

  const { isSysAdmin } = usePermissions();
  const { handleError, showSuccess } = useErrorHandler();

  const {
    data: sharesResponse,
    isLoading,
    refetch,
  } = useFindAllServiceShares(
    {
      filter: {
        ...filter,
      },
      pageable: { page, size: pageSize },
    },
    {
      query: {
        staleTime: 30000,
      },
    }
  );

  const grantShareMutation = useGrantServiceShare();
  const revokeShareMutation = useRevokeServiceShare();

  // Get the page data from API response
  const pageData = sharesResponse;
  const shares = (pageData?.content || []) as ServiceShare[];

  const handleCreateShare = async (data: any) => {
    try {
      await grantShareMutation.mutateAsync({ data });
      setFormOpen(false);
      showSuccess('Service share created successfully');
      refetch();
    } catch (error) {
      handleError(error, 'Failed to create service share');
    }
  };

  const handleRevokeShare = async () => {
    if (!selectedShare) return;
    
    try {
      await revokeShareMutation.mutateAsync({ id: selectedShare.id });
      setDeleteDialogOpen(false);
      setSelectedShare(null);
      showSuccess('Service share revoked successfully');
      refetch();
    } catch (error) {
      handleError(error, 'Failed to revoke service share');
    }
  };

  const handleRevokeClick = (share: ServiceShare) => {
    setSelectedShare(share);
    setDeleteDialogOpen(true);
  };

  const handleEditClick = (share: ServiceShare) => {
    setSelectedShare(share);
    setFormOpen(true);
  };

  const handleClearFilters = () => {
    setFilter({});
    setSearch('');
  };

  const canManageShares = isSysAdmin;

  return (
    <Box>
      <PageHeader
        title="Service Shares"
        subtitle="Manage service access permissions for teams and users"
        actions={
          canManageShares && (
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => setFormOpen(true)}
            >
              Grant Access
            </Button>
          )
        }
      />

      {/* Search and Filters */}
      <Box sx={{ mb: 3, display: 'flex', gap: 2, alignItems: 'center', flexWrap: 'wrap' }}>
        <TextField
          placeholder="Search by service name..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon />
              </InputAdornment>
            ),
          }}
          sx={{ minWidth: 300 }}
        />

        <Button
          variant="outlined"
          startIcon={<FilterIcon />}
          onClick={() => setShowFilters(!showFilters)}
        >
          Filters
        </Button>

        {Object.keys(filter).length > 0 && (
          <Button variant="text" onClick={handleClearFilters}>
            Clear Filters
          </Button>
        )}
      </Box>

      {/* Filter Controls */}
      {showFilters && (
        <Box sx={{ mb: 3, p: 2, bgcolor: 'grey.50', borderRadius: 1, display: 'flex', gap: 2, flexWrap: 'wrap' }}>
          <FormControl sx={{ minWidth: 120 }}>
            <InputLabel>Type</InputLabel>
            <Select
              value={filter.grantedTo || ''}
              onChange={(e) => setFilter(prev => ({ ...prev, grantedTo: e.target.value as any }))}
              label="Type"
            >
              <MenuItem value="">All</MenuItem>
              <MenuItem value="TEAM">Team</MenuItem>
              <MenuItem value="USER">User</MenuItem>
            </Select>
          </FormControl>

          <FormControl sx={{ minWidth: 120 }}>
            <InputLabel>Status</InputLabel>
            <Select
              value={filter.status || ''}
              onChange={(e) => setFilter(prev => ({ ...prev, status: e.target.value as any }))}
              label="Status"
            >
              <MenuItem value="">All</MenuItem>
              <MenuItem value="ACTIVE">Active</MenuItem>
              <MenuItem value="EXPIRED">Expired</MenuItem>
              <MenuItem value="REVOKED">Revoked</MenuItem>
            </Select>
          </FormControl>

          <FormControl sx={{ minWidth: 120 }}>
            <InputLabel>Environment</InputLabel>
            <Select
              value={filter.environment || ''}
              onChange={(e) => setFilter(prev => ({ ...prev, environment: e.target.value }))}
              label="Environment"
            >
              <MenuItem value="">All</MenuItem>
              <MenuItem value="dev">Development</MenuItem>
              <MenuItem value="staging">Staging</MenuItem>
              <MenuItem value="prod">Production</MenuItem>
              <MenuItem value="test">Test</MenuItem>
            </Select>
          </FormControl>
        </Box>
      )}

      {/* Active Filters Display */}
      {Object.keys(filter).length > 0 && (
        <Box sx={{ mb: 2, display: 'flex', gap: 1, flexWrap: 'wrap' }}>
          {Object.entries(filter).map(([key, value]) => (
            value && (
              <Chip
                key={key}
                label={`${key}: ${value}`}
                onDelete={() => setFilter(prev => ({ ...prev, [key]: undefined }))}
                size="small"
              />
            )
          ))}
        </Box>
      )}

      <ServiceShareTable
        shares={shares}
        loading={isLoading}
        onEdit={canManageShares ? handleEditClick : undefined}
        onRevoke={canManageShares ? handleRevokeClick : undefined}
      />

      <ShareFormDrawer
        open={formOpen}
        onClose={() => setFormOpen(false)}
        onSubmit={handleCreateShare}
        share={selectedShare}
      />

      <ConfirmDialog
        open={deleteDialogOpen}
        title="Revoke Service Share"
        message={`Are you sure you want to revoke access for "${selectedShare?.grantedToName}"? This action cannot be undone.`}
        onConfirm={handleRevokeShare}
        onCancel={() => setDeleteDialogOpen(false)}
        confirmText="Revoke"
        cancelText="Cancel"
        loading={revokeShareMutation.isPending}
      />
    </Box>
  );
};

export default ServiceShareListPage;