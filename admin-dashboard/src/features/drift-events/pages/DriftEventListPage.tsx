import React, { useState, useEffect } from 'react';
import { Box, Button, Alert } from '@mui/material';
import { Refresh as RefreshIcon, AutoAwesome as AutoRefreshIcon } from '@mui/icons-material';
import { PageHeader } from '@components/common/PageHeader';
import { useGetDriftEventsQuery } from '../api';
import type { DriftEvent, UpdateDriftEventRequest } from '../types';
import { usePermissions } from '@features/auth/hooks/usePermissions';
import { DriftEventTable } from '../components/DriftEventTable';
import { DriftFilterBar } from '../components/DriftFilterBar';
import { ResolveDialog } from '../components/ResolveDialog';

const DriftEventListPage: React.FC = () => {
  const [page] = useState(0);
  const [pageSize] = useState(10);
  const [serviceName, setServiceName] = useState('');
  const [status, setStatus] = useState('');
  const [severity, setSeverity] = useState('');
  const [environment, setEnvironment] = useState('');
  const [resolveDialogOpen, setResolveDialogOpen] = useState(false);
  const [selectedEvent, setSelectedEvent] = useState<DriftEvent | null>(null);
  const [autoRefresh, setAutoRefresh] = useState(false);

  const { canViewDriftEvents } = usePermissions();

  const { data, isLoading, refetch } = useGetDriftEventsQuery({
    page,
    size: pageSize,
    serviceName: serviceName || undefined,
    status: status || undefined,
    severity: severity || undefined,
    environment: environment || undefined,
  });

  // Auto-refresh every 30 seconds when enabled
  useEffect(() => {
    if (!autoRefresh) return;

    const interval = setInterval(() => {
      refetch();
    }, 30000); // 30 seconds

    return () => clearInterval(interval);
  }, [autoRefresh, refetch]);

  const handleResolveEvent = async (update: UpdateDriftEventRequest) => {
    if (!selectedEvent) return;
    
    try {
      // This would call the update mutation
      console.log('Resolving drift event:', selectedEvent.id, update);
      setResolveDialogOpen(false);
      setSelectedEvent(null);
      refetch(); // Refresh the data
    } catch (error) {
      console.error('Failed to resolve drift event:', error);
    }
  };

  const handleResolveClick = (event: DriftEvent) => {
    setSelectedEvent(event);
    setResolveDialogOpen(true);
  };

  const handleViewClick = (eventId: string) => {
    // Navigate to event detail page
    console.log('Navigate to drift event:', eventId);
  };

  const handleRefresh = () => {
    refetch();
  };

  const handleToggleAutoRefresh = () => {
    setAutoRefresh(!autoRefresh);
  };

  const handleClearFilters = () => {
    setServiceName('');
    setStatus('');
    setSeverity('');
    setEnvironment('');
  };

  if (!canViewDriftEvents) {
    return (
      <Box>
        <PageHeader title="Drift Events" />
        <Alert severity="warning">
          You don't have permission to view drift events.
        </Alert>
      </Box>
    );
  }

  return (
    <Box>
      <PageHeader
        title="Drift Events"
        actions={
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Button
              variant={autoRefresh ? 'contained' : 'outlined'}
              startIcon={<AutoRefreshIcon />}
              onClick={handleToggleAutoRefresh}
              color={autoRefresh ? 'success' : 'primary'}
            >
              {autoRefresh ? 'Auto Refresh ON' : 'Auto Refresh OFF'}
            </Button>
            <Button
              variant="outlined"
              startIcon={<RefreshIcon />}
              onClick={handleRefresh}
              disabled={isLoading}
            >
              Refresh
            </Button>
          </Box>
        }
      />
      
      {autoRefresh && (
        <Alert severity="info" sx={{ mb: 2 }}>
          Auto-refresh is enabled. Data will refresh every 30 seconds.
        </Alert>
      )}
      
      <DriftFilterBar
        serviceName={serviceName}
        status={status}
        severity={severity}
        environment={environment}
        onServiceNameChange={setServiceName}
        onStatusChange={setStatus}
        onSeverityChange={setSeverity}
        onEnvironmentChange={setEnvironment}
        onClearFilters={handleClearFilters}
      />

      <DriftEventTable
        events={data?.content || []}
        loading={isLoading}
        onView={handleViewClick}
        onResolve={handleResolveClick}
      />

      <ResolveDialog
        open={resolveDialogOpen}
        onClose={() => setResolveDialogOpen(false)}
        onSubmit={handleResolveEvent}
        eventTitle={selectedEvent?.serviceName}
      />
    </Box>
  );
};

export default DriftEventListPage;
