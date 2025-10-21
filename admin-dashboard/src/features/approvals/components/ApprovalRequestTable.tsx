import React from 'react';
import { type GridColDef, GridActionsCellItem } from '@mui/x-data-grid';
import {
  Visibility as ViewIcon,
  CheckCircle as ApproveIcon,
  Cancel as RejectIcon,
  Close as CancelIcon,
} from '@mui/icons-material';
import { DataTable } from '@components/common/DataTable';
import { ApprovalBadge } from './ApprovalBadge';
import type { ApprovalRequest } from '../types';
import { usePermissions } from '@features/auth/hooks/usePermissions';

interface ApprovalRequestTableProps {
  requests: ApprovalRequest[];
  loading?: boolean;
  onView: (id: string) => void;
  onApprove: (request: ApprovalRequest) => void;
  onReject: (request: ApprovalRequest) => void;
  onCancel: (id: string) => void;
}

export const ApprovalRequestTable: React.FC<ApprovalRequestTableProps> = ({
  requests,
  loading,
  onView,
  onApprove,
  onReject,
  onCancel,
}) => {
  const { canApproveRequests } = usePermissions();

  const columns: GridColDef[] = [
    { 
      field: 'id', 
      headerName: 'Request ID', 
      width: 150,
      renderCell: (params) => (
        <span style={{ fontFamily: 'monospace', fontSize: '0.8rem' }}>
          {params.value.substring(0, 8)}...
        </span>
      ),
    },
    { 
      field: 'requestType', 
      headerName: 'Type', 
      width: 150,
      renderCell: (params) => (
        <span style={{ textTransform: 'capitalize' }}>
          {params.value.replace(/_/g, ' ')}
        </span>
      ),
    },
    { 
      field: 'target', 
      headerName: 'Target', 
      width: 200,
      renderCell: (params) => {
        const target = params.value;
        if (target.serviceName) {
          return <strong>{target.serviceName}</strong>;
        }
        if (target.serviceId) {
          return <span style={{ fontFamily: 'monospace' }}>{target.serviceId}</span>;
        }
        if (target.configKey) {
          return <span style={{ fontFamily: 'monospace' }}>{target.configKey}</span>;
        }
        return 'N/A';
      },
    },
    { 
      field: 'environment', 
      headerName: 'Environment', 
      width: 120,
      renderCell: (params) => {
        const env = params.row.target?.environment;
        return env ? (
          <span style={{ textTransform: 'uppercase', fontWeight: 500 }}>
            {env}
          </span>
        ) : 'N/A';
      },
    },
    {
      field: 'status',
      headerName: 'Status',
      width: 130,
      renderCell: (params) => <ApprovalBadge status={params.value} />,
    },
    { 
      field: 'requesterUsername', 
      headerName: 'Requester', 
      width: 150,
    },
    {
      field: 'createdAt',
      headerName: 'Created',
      width: 150,
      type: 'dateTime',
      valueFormatter: (value: any) => {
        if (!value) return '';
        return new Date(value).toLocaleDateString();
      },
    },
    {
      field: 'actions',
      type: 'actions',
      headerName: 'Actions',
      width: 150,
      getActions: (params) => {
        const request = params.row as ApprovalRequest;
        const isPending = request.status === 'PENDING';
        const isOwnRequest = request.requesterUserId === 'current-user-id'; // This should come from auth context
        
        const actions = [
          <GridActionsCellItem
            key="view"
            icon={<ViewIcon />}
            label="View"
            onClick={() => onView(params.id as string)}
            showInMenu
          />,
        ];

        if (canApproveRequests && isPending && !isOwnRequest) {
          actions.push(
            <GridActionsCellItem
              key="approve"
              icon={<ApproveIcon />}
              label="Approve"
              onClick={() => onApprove(request)}
              showInMenu
            />
          );
          actions.push(
            <GridActionsCellItem
              key="reject"
              icon={<RejectIcon />}
              label="Reject"
              onClick={() => onReject(request)}
              showInMenu
            />
          );
        }

        if (isPending && isOwnRequest) {
          actions.push(
            <GridActionsCellItem
              key="cancel"
              icon={<CancelIcon />}
              label="Cancel"
              onClick={() => onCancel(params.id as string)}
              showInMenu
            />
          );
        }

        return actions;
      },
    },
  ];

  return <DataTable rows={requests} columns={columns} loading={loading} getRowId={(row) => row.id} />;
};
