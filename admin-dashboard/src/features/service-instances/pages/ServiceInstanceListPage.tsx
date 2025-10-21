import React, { useState } from 'react';
import { Box, TextField, InputAdornment, FormControl, InputLabel, Select, MenuItem, Button } from '@mui/material';
import { Search as SearchIcon, Refresh as RefreshIcon } from '@mui/icons-material';
import { PageHeader } from '@components/common/PageHeader';
import { ConfirmDialog } from '@components/common/ConfirmDialog';
import {
  useGetServiceInstancesQuery,
  useDeleteServiceInstanceMutation,
} from '../api';
import type { ServiceInstance } from '../types';
import { ServiceInstanceTable } from '../components/ServiceInstanceTable';

const ServiceInstanceListPage: React.FC = () => {
  const [page] = useState(0);
  const [pageSize] = useState(10);
  const [search, setSearch] = useState('');
  const [environmentFilter, setEnvironmentFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [driftFilter, setDriftFilter] = useState('');
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [selectedInstance, setSelectedInstance] = useState<ServiceInstance | null>(null);


  const { data, isLoading, refetch } = useGetServiceInstancesQuery({
    page,
    size: pageSize,
    serviceName: search || undefined,
    environment: environmentFilter || undefined,
    status: statusFilter || undefined,
    hasDrift: driftFilter === 'true' ? true : driftFilter === 'false' ? false : undefined,
  });

  const [deleteInstance, { isLoading: deleteLoading }] = useDeleteServiceInstanceMutation();

  const handleDeleteInstance = async () => {
    if (!selectedInstance) return;
    
    try {
      await deleteInstance(selectedInstance.id).unwrap();
      setDeleteDialogOpen(false);
      setSelectedInstance(null);
    } catch (error) {
      console.error('Failed to delete instance:', error);
    }
  };

  const handleDeleteClick = (instanceId: string) => {
    const instanceToDelete = data?.content.find(i => i.id === instanceId);
    if (instanceToDelete) {
      setSelectedInstance(instanceToDelete);
      setDeleteDialogOpen(true);
    }
  };

  const handleViewClick = (instanceId: string) => {
    // Navigate to instance detail page
    console.log('Navigate to instance:', instanceId);
  };

  const handleEditClick = (instance: ServiceInstance) => {
    // Open edit dialog
    console.log('Edit instance:', instance);
  };

  const handleRefresh = () => {
    refetch();
  };

  return (
    <Box>
      <PageHeader
        title="Service Instances"
        actions={
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={handleRefresh}
            disabled={isLoading}
          >
            Refresh
          </Button>
        }
      />
      
      <Box sx={{ mb: 3, display: 'flex', gap: 2, flexWrap: 'wrap' }}>
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
          sx={{ minWidth: 250 }}
        />
        
        <FormControl sx={{ minWidth: 120 }}>
          <InputLabel>Environment</InputLabel>
          <Select
            value={environmentFilter}
            onChange={(e) => setEnvironmentFilter(e.target.value)}
            label="Environment"
          >
            <MenuItem value="">All</MenuItem>
            <MenuItem value="dev">Dev</MenuItem>
            <MenuItem value="staging">Staging</MenuItem>
            <MenuItem value="prod">Prod</MenuItem>
            <MenuItem value="test">Test</MenuItem>
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
            <MenuItem value="UP">UP</MenuItem>
            <MenuItem value="DOWN">DOWN</MenuItem>
            <MenuItem value="UNKNOWN">UNKNOWN</MenuItem>
          </Select>
        </FormControl>

        <FormControl sx={{ minWidth: 120 }}>
          <InputLabel>Drift</InputLabel>
          <Select
            value={driftFilter}
            onChange={(e) => setDriftFilter(e.target.value)}
            label="Drift"
          >
            <MenuItem value="">All</MenuItem>
            <MenuItem value="true">Has Drift</MenuItem>
            <MenuItem value="false">No Drift</MenuItem>
          </Select>
        </FormControl>
      </Box>

      <ServiceInstanceTable
        instances={data?.content || []}
        loading={isLoading}
        onView={handleViewClick}
        onEdit={handleEditClick}
        onDelete={handleDeleteClick}
      />

      <ConfirmDialog
        open={deleteDialogOpen}
        title="Delete Service Instance"
        message={`Are you sure you want to delete instance "${selectedInstance?.instanceId}"? This action cannot be undone.`}
        onConfirm={handleDeleteInstance}
        onCancel={() => setDeleteDialogOpen(false)}
        confirmText="Delete"
        cancelText="Cancel"
        loading={deleteLoading}
      />
    </Box>
  );
};

export default ServiceInstanceListPage;
