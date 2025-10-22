import React, { useState, useEffect } from 'react';
import { Box, Button, Alert } from '@mui/material';
import { Refresh as RefreshIcon, AutoAwesome as AutoRefreshIcon } from '@mui/icons-material';
import { PageHeader } from '@components/common/PageHeader';
import { useFindAllDriftEvents, useUpdateDriftEvent } from '@lib/api/hooks';
import type { DriftEvent, UpdateDriftEventRequest } from '../types';
import { usePermissions } from '@features/auth/hooks/usePermissions';
import { DriftEventTable } from '../components/DriftEventTable';
import { DriftFilterBar } from '../components/DriftFilterBar';
import { ResolveDialog } from '../components/ResolveDialog';
import type { FindAllDriftEventsStatus, FindAllDriftEventsSeverity } from '@lib/api/models';

const DriftEventListPage: React.FC = () => {
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [serviceName, setServiceName] = useState('');
  const [status, setStatus] = useState<FindAllDriftEventsStatus | ''>('');
  const [severity, setSeverity] = useState<FindAllDriftEventsSeverity | ''>('');
  const [environment, setEnvironment] = useState('');
  const [resolveDialogOpen, setResolveDialogOpen] = useState(false);
  const [selectedEvent, setSelectedEvent] = useState<DriftEvent | null>(null);
  const [autoRefresh, setAutoRefresh] = useState(false);

  const { permissions } = usePermissions();

  const { data: eventsResponse, isLoading, refetch } = useFindAllDriftEvents(
    {
      serviceName: serviceName || undefined,
      status: (status as FindAllDriftEventsStatus) || undefined,
      severity: (severity as FindAllDriftEventsSeverity) || undefined,
      pageable: { page, size: pageSize },
    },
    {
      query: {
        staleTime: 10000, // 10 seconds for drift events (more real-time)
        refetchInterval: autoRefresh ? 30000 : false, // Auto-refresh if enabled
      },
    }
  );

  const updateEventMutation = useUpdateDriftEvent();

  // Get the page data from API response
  const pageData = eventsResponse;
  const events = (pageData?.content || []) as DriftEvent[];

  const handleResolveEvent = async (update: UpdateDriftEventRequest) => {
    if (!selectedEvent) return;
    
    try {
      await updateEventMutation.mutateAsync({
        id: selectedEvent.id,
        data: update as any,
      });
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

  // Permission check - allow if user has permissions object
  // Individual permissions are checked by ProtectedRoute
  if (!permissions) {
    return (
      <Box>
        <PageHeader title="Drift Events" />
        <Alert severity="warning">
          Loading permissions...
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
        events={events}
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
