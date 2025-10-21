import React, { useState } from 'react';
import { Box } from '@mui/material';
import { PageHeader } from '@components/common/PageHeader';
import { ConfirmDialog } from '@components/common/ConfirmDialog';
import {
  useGetApprovalRequestsQuery,
  useSubmitDecisionMutation,
  useCancelApprovalRequestMutation,
} from '../api';
import type { ApprovalRequest, DecisionRequest } from '../types';
import { ApprovalRequestTable } from '../components/ApprovalRequestTable';
import { ApprovalTabs } from '../components/ApprovalTabs';
import { DecisionDialog } from '../components/DecisionDialog';

const ApprovalListPage: React.FC = () => {
  const [page] = useState(0);
  const [pageSize] = useState(10);
  const [activeTab, setActiveTab] = useState('PENDING');
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [decisionDialogOpen, setDecisionDialogOpen] = useState(false);
  const [selectedRequest, setSelectedRequest] = useState<ApprovalRequest | null>(null);


  const { data, isLoading } = useGetApprovalRequestsQuery({
    page,
    size: pageSize,
    status: activeTab,
  });

  const [submitDecision, { isLoading: decisionLoading }] = useSubmitDecisionMutation();
  const [cancelRequest, { isLoading: cancelLoading }] = useCancelApprovalRequestMutation();

  const handleDecision = async (decision: DecisionRequest) => {
    if (!selectedRequest) return;
    
    try {
      await submitDecision({ id: selectedRequest.id, decision }).unwrap();
      setDecisionDialogOpen(false);
      setSelectedRequest(null);
    } catch (error) {
      console.error('Failed to submit decision:', error);
    }
  };

  const handleCancelRequest = async () => {
    if (!selectedRequest) return;
    
    try {
      await cancelRequest(selectedRequest.id).unwrap();
      setDeleteDialogOpen(false);
      setSelectedRequest(null);
    } catch (error) {
      console.error('Failed to cancel request:', error);
    }
  };

  const handleApproveClick = (request: ApprovalRequest) => {
    setSelectedRequest(request);
    setDecisionDialogOpen(true);
  };

  const handleRejectClick = (request: ApprovalRequest) => {
    setSelectedRequest(request);
    setDecisionDialogOpen(true);
  };

  const handleCancelClick = (requestId: string) => {
    const requestToCancel = data?.content.find(r => r.id === requestId);
    if (requestToCancel) {
      setSelectedRequest(requestToCancel);
      setDeleteDialogOpen(true);
    }
  };

  const handleViewClick = (requestId: string) => {
    // Navigate to request detail page
    console.log('Navigate to request:', requestId);
  };

  // Calculate counts for tabs
  const pendingCount = data?.content.filter(r => r.status === 'PENDING').length || 0;
  const approvedCount = data?.content.filter(r => r.status === 'APPROVED').length || 0;
  const rejectedCount = data?.content.filter(r => r.status === 'REJECTED').length || 0;
  const cancelledCount = data?.content.filter(r => r.status === 'CANCELLED').length || 0;

  return (
    <Box>
      <PageHeader title="Approval Requests" />
      
      <ApprovalTabs
        activeTab={activeTab}
        onTabChange={setActiveTab}
        pendingCount={pendingCount}
        approvedCount={approvedCount}
        rejectedCount={rejectedCount}
        cancelledCount={cancelledCount}
      />

      <ApprovalRequestTable
        requests={data?.content || []}
        loading={isLoading}
        onView={handleViewClick}
        onApprove={handleApproveClick}
        onReject={handleRejectClick}
        onCancel={handleCancelClick}
      />

      <DecisionDialog
        open={decisionDialogOpen}
        onClose={() => setDecisionDialogOpen(false)}
        onSubmit={handleDecision}
        loading={decisionLoading}
        requestTitle={selectedRequest?.requestType}
      />

      <ConfirmDialog
        open={deleteDialogOpen}
        title="Cancel Approval Request"
        message={`Are you sure you want to cancel this ${selectedRequest?.requestType} request? This action cannot be undone.`}
        onConfirm={handleCancelRequest}
        onCancel={() => setDeleteDialogOpen(false)}
        confirmText="Cancel Request"
        cancelText="Keep Request"
        loading={cancelLoading}
      />
    </Box>
  );
};

export default ApprovalListPage;
