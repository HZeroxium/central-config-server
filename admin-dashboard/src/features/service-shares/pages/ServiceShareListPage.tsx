import React, { useState } from 'react';
import { Box, TextField, InputAdornment, FormControl, InputLabel, Select, MenuItem, Button } from '@mui/material';
import { Search as SearchIcon, Add as AddIcon } from '@mui/icons-material';
import { PageHeader } from '@components/common/PageHeader';
import { ConfirmDialog } from '@components/common/ConfirmDialog';
import {
  useGetServiceSharesQuery,
  useRevokeServiceShareMutation,
} from '../api';
import type { ServiceShare, CreateServiceShareRequest } from '../types';
import { ServiceShareTable } from '../components/ServiceShareTable';
import { ShareFormDrawer } from '../components/ShareFormDrawer';

const ServiceShareListPage: React.FC = () => {
  const [page] = useState(0);
  const [pageSize] = useState(10);
  const [search, setSearch] = useState('');
  const [grantToTypeFilter, setGrantToTypeFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [shareDrawerOpen, setShareDrawerOpen] = useState(false);
  const [selectedShare, setSelectedShare] = useState<ServiceShare | null>(null);

  const { data, isLoading, refetch } = useGetServiceSharesQuery({
    page,
    size: pageSize,
    serviceId: search || undefined,
    grantToType: grantToTypeFilter || undefined,
    isActive: statusFilter === 'active' ? true : statusFilter === 'inactive' ? false : undefined,
  });

  const [revokeShare, { isLoading: revokeLoading }] = useRevokeServiceShareMutation();

  const handleRevokeShare = async () => {
    if (!selectedShare) return;
    
    try {
      await revokeShare(selectedShare.id).unwrap();
      setDeleteDialogOpen(false);
      setSelectedShare(null);
    } catch (error) {
      console.error('Failed to revoke share:', error);
    }
  };

  const handleRevokeClick = (shareId: string) => {
    const shareToRevoke = data?.content.find(s => s.id === shareId);
    if (shareToRevoke) {
      setSelectedShare(shareToRevoke);
      setDeleteDialogOpen(true);
    }
  };

  const handleViewClick = (shareId: string) => {
    // Navigate to share detail page
    console.log('Navigate to share:', shareId);
  };

  const handleCreateShare = (shareData: CreateServiceShareRequest) => {
    console.log('Creating share:', shareData);
    setShareDrawerOpen(false);
    refetch();
  };

  return (
    <Box>
      <PageHeader
        title="Service Shares"
        actions={
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={() => setShareDrawerOpen(true)}
          >
            Grant Share
          </Button>
        }
      />
      
      <Box sx={{ mb: 3, display: 'flex', gap: 2, flexWrap: 'wrap' }}>
        <TextField
          placeholder="Search by service ID..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon />
              </InputAdornment>
            ),
          }}
          sx={{ minWidth: 250 }}
        />
        
        <FormControl sx={{ minWidth: 120 }}>
          <InputLabel>Grant To Type</InputLabel>
          <Select
            value={grantToTypeFilter}
            onChange={(e) => setGrantToTypeFilter(e.target.value)}
            label="Grant To Type"
          >
            <MenuItem value="">All</MenuItem>
            <MenuItem value="TEAM">Team</MenuItem>
            <MenuItem value="USER">User</MenuItem>
          </Select>
        </FormControl>

        <FormControl sx={{ minWidth: 120 }}>
          <InputLabel>Status</InputLabel>
          <Select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            label="Status"
          >
            <MenuItem value="">All</MenuItem>
            <MenuItem value="active">Active</MenuItem>
            <MenuItem value="inactive">Inactive</MenuItem>
          </Select>
        </FormControl>
      </Box>

      <ServiceShareTable
        shares={data?.content || []}
        loading={isLoading}
        onView={handleViewClick}
        onRevoke={handleRevokeClick}
      />

      <ShareFormDrawer
        open={shareDrawerOpen}
        onClose={() => setShareDrawerOpen(false)}
        serviceId=""
        onSubmit={handleCreateShare}
        loading={false}
      />

      <ConfirmDialog
        open={deleteDialogOpen}
        title="Revoke Service Share"
        message={`Are you sure you want to revoke this share for ${selectedShare?.grantToId}? This action cannot be undone.`}
        onConfirm={handleRevokeShare}
        onCancel={() => setDeleteDialogOpen(false)}
        confirmText="Revoke Share"
        cancelText="Cancel"
        loading={revokeLoading}
      />
    </Box>
  );
};

export default ServiceShareListPage;
