import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, TextField, InputAdornment, FormControl, InputLabel, Select, MenuItem, Button } from '@mui/material';
import { Search as SearchIcon, Refresh as RefreshIcon } from '@mui/icons-material';
import { PageHeader } from '@components/common/PageHeader';
import { ConfirmDialog } from '@components/common/ConfirmDialog';
import {
  useFindAllServiceInstances,
  useDeleteServiceInstance,
} from '@lib/api/hooks';
import type { ServiceInstance } from '../types';
import { ServiceInstanceTable } from '../components/ServiceInstanceTable';
import type { FindAllServiceInstancesStatus } from '@lib/api/models';

const ServiceInstanceListPage: React.FC = () => {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [search, setSearch] = useState('');
  const [environmentFilter, setEnvironmentFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState<FindAllServiceInstancesStatus | ''>('');
  const [driftFilter, setDriftFilter] = useState('');
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [selectedInstance, setSelectedInstance] = useState<ServiceInstance | null>(null);

  const { data: instancesResponse, isLoading, refetch } = useFindAllServiceInstances(
    {
      serviceName: search || undefined,
      status: (statusFilter as FindAllServiceInstancesStatus) || undefined,
      environment: environmentFilter || undefined,
      hasDrift: driftFilter ? driftFilter === 'true' : undefined,
      pageable: { page, size: pageSize },
    },
    {
      query: {
        staleTime: 30000,
      },
    }
  );

  const deleteInstanceMutation = useDeleteServiceInstance();

  // Get the page data from API response
  const pageData = instancesResponse;
  const instances = (pageData?.content || []) as ServiceInstance[];

  const handleDeleteInstance = async () => {
    if (!selectedInstance) return;
    
    try {
      await deleteInstanceMutation.mutateAsync({ 
        serviceName: selectedInstance.serviceName, 
        instanceId: selectedInstance.instanceId 
      });
      setDeleteDialogOpen(false);
      setSelectedInstance(null);
    } catch (error) {
      console.error('Failed to delete instance:', error);
    }
  };

  const handleDeleteClick = (instanceId: string) => {
    const instanceToDelete = instances.find(i => i.instanceId === instanceId);
    if (instanceToDelete) {
      setSelectedInstance(instanceToDelete);
      setDeleteDialogOpen(true);
    }
  };

  const handleViewClick = (instanceId: string) => {
    // Find the instance to get serviceName
    const instance = instances.find(inst => inst.instanceId === instanceId);
    if (instance) {
      navigate(`/service-instances/${encodeURIComponent(instance.serviceName)}/${encodeURIComponent(instanceId)}`);
    }
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
        instances={instances}
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
        loading={deleteInstanceMutation.isPending}
      />
    </Box>
  );
};

export default ServiceInstanceListPage;
