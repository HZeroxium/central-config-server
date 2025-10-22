import React, { useState } from 'react';
import { Box, Button, TextField, InputAdornment, FormControl, InputLabel, Select, MenuItem, Chip, Tabs, Tab } from '@mui/material';
import { Search as SearchIcon, FilterList as FilterIcon, Refresh as RefreshIcon } from '@mui/icons-material';
import { PageHeader } from '@components/common/PageHeader';
import { ApprovalRequestTable } from '../components/ApprovalRequestTable';
import { DecisionDialog } from '../components/DecisionDialog';
import {
  useFindAllApprovalRequests,
  useSubmitApprovalDecision,
  useCancelApprovalRequest,
} from '@lib/api/hooks';
import { useErrorHandler } from '@hooks/useErrorHandler';
import type { ApprovalRequest, ApprovalRequestFilter, RequestStatus } from '../types';
import { REQUEST_STATUSES, STATUS_LABELS } from '../types';
import { usePermissions } from '@features/auth/hooks/usePermissions';

export const ApprovalListPage: React.FC = () => {
  const [page] = useState(0);
  const [pageSize] = useState(10);
  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState<ApprovalRequestFilter>({});
  const [decisionDialogOpen, setDecisionDialogOpen] = useState(false);
  const [selectedRequest, setSelectedRequest] = useState<ApprovalRequest | null>(null);
  const [showFilters, setShowFilters] = useState(false);
  const [activeTab, setActiveTab] = useState<RequestStatus>('PENDING');

  const { isSysAdmin } = usePermissions();
  const { handleError, showSuccess } = useErrorHandler();

  const {
    data: requestsResponse,
    isLoading,
    refetch,
  } = useFindAllApprovalRequests(
    {
      filter: {
        search: search || undefined,
      },
      pageable: { page, size: pageSize },
    },
    {
      query: {
        staleTime: 10000, // 10 seconds for real-time updates
        refetchInterval: 30000, // Auto-refresh every 30 seconds
      },
    }
  );

  const submitDecisionMutation = useSubmitApprovalDecision();
  const cancelRequestMutation = useCancelApprovalRequest();

  // Get the page data from API response
  const pageData = requestsResponse;
  const requests = (pageData?.content || []) as ApprovalRequest[];

  const handleSubmitDecision = async (decision: any) => {
    if (!selectedRequest) return;
    
    try {
      await submitDecisionMutation.mutateAsync({
        id: selectedRequest.id,
        data: {
          decision: decision.decision,
          note: decision.note,
        },
      });
      setDecisionDialogOpen(false);
      setSelectedRequest(null);
      showSuccess('Decision submitted successfully');
      refetch();
    } catch (error) {
      handleError(error, 'Failed to submit decision');
    }
  };

  const handleCancelRequest = async (request: ApprovalRequest) => {
    try {
      await cancelRequestMutation.mutateAsync({ id: request.id });
      showSuccess('Request cancelled successfully');
      refetch();
    } catch (error) {
      handleError(error, 'Failed to cancel request');
    }
  };

  const handleDecisionClick = (request: ApprovalRequest) => {
    setSelectedRequest(request);
    setDecisionDialogOpen(true);
  };

  const handleTabChange = (_: React.SyntheticEvent, newValue: RequestStatus) => {
    setActiveTab(newValue);
  };

  const handleClearFilters = () => {
    setFilter({});
    setSearch('');
  };

  const canApprove = isSysAdmin;

  return (
    <Box>
      <PageHeader
        title="Approval Requests"
        subtitle="Review and approve service access and configuration changes"
        actions={
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={() => refetch()}
            disabled={isLoading}
          >
            Refresh
          </Button>
        }
      />

      {/* Status Tabs */}
      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
        <Tabs value={activeTab} onChange={handleTabChange}>
          {Object.entries(REQUEST_STATUSES).map(([key, status]) => (
            <Tab
              key={key}
              label={`${STATUS_LABELS[status]} (${requests.length})`}
              value={status}
            />
          ))}
        </Tabs>
      </Box>

      {/* Search and Filters */}
      <Box sx={{ mb: 3, display: 'flex', gap: 2, alignItems: 'center', flexWrap: 'wrap' }}>
        <TextField
          placeholder="Search by service name or requester..."
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
              value={filter.requestType || ''}
              onChange={(e) => setFilter(prev => ({ ...prev, requestType: e.target.value as any }))}
              label="Type"
            >
              <MenuItem value="">All</MenuItem>
              <MenuItem value="SERVICE_SHARE">Service Share</MenuItem>
              <MenuItem value="CONFIG_CHANGE">Config Change</MenuItem>
              <MenuItem value="DRIFT_RESOLUTION">Drift Resolution</MenuItem>
              <MenuItem value="SERVICE_DELETION">Service Deletion</MenuItem>
            </Select>
          </FormControl>

          <FormControl sx={{ minWidth: 120 }}>
            <InputLabel>Priority</InputLabel>
            <Select
              value={filter.priority || ''}
              onChange={(e) => setFilter(prev => ({ ...prev, priority: e.target.value as any }))}
              label="Priority"
            >
              <MenuItem value="">All</MenuItem>
              <MenuItem value="LOW">Low</MenuItem>
              <MenuItem value="MEDIUM">Medium</MenuItem>
              <MenuItem value="HIGH">High</MenuItem>
              <MenuItem value="CRITICAL">Critical</MenuItem>
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

      <ApprovalRequestTable
        requests={requests}
        loading={isLoading}
        onDecision={canApprove ? handleDecisionClick : undefined}
        onCancel={canApprove ? handleCancelRequest : undefined}
      />

      <DecisionDialog
        open={decisionDialogOpen}
        onClose={() => setDecisionDialogOpen(false)}
        onSubmit={handleSubmitDecision}
        request={selectedRequest}
      />
    </Box>
  );
};

export default ApprovalListPage;