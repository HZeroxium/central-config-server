import React from 'react';
import { type GridColDef, GridActionsCellItem } from '@mui/x-data-grid';
import {
  Visibility as ViewIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
} from '@mui/icons-material';
import { DataTable } from '@components/common/DataTable';
import { InstanceStatusChip } from './InstanceStatusChip';
import { DriftIndicator } from './DriftIndicator';
import type { ServiceInstance } from '../types';
import { usePermissions } from '@features/auth/hooks/usePermissions';

interface ServiceInstanceTableProps {
  instances: ServiceInstance[];
  loading?: boolean;
  onView: (id: string) => void;
  onEdit: (instance: ServiceInstance) => void;
  onDelete: (id: string) => void;
}

export const ServiceInstanceTable: React.FC<ServiceInstanceTableProps> = ({
  instances,
  loading,
  onView,
  onEdit,
  onDelete,
}) => {
  const { canManageApplicationServices } = usePermissions();

  const columns: GridColDef[] = [
    { 
      field: 'serviceName', 
      headerName: 'Service Name', 
      width: 200,
      renderCell: (params) => (
        <strong>{params.value}</strong>
      ),
    },
    { 
      field: 'instanceId', 
      headerName: 'Instance ID', 
      width: 150,
    },
    { 
      field: 'host', 
      headerName: 'Host:Port', 
      width: 150,
      renderCell: (params) => `${params.row.host}:${params.row.port}`,
    },
    { 
      field: 'environment', 
      headerName: 'Environment', 
      width: 120,
      renderCell: (params) => (
        <span style={{ textTransform: 'uppercase', fontWeight: 500 }}>
          {params.value}
        </span>
      ),
    },
    { 
      field: 'version', 
      headerName: 'Version', 
      width: 120,
      renderCell: (params) => params.value || 'N/A',
    },
    {
      field: 'status',
      headerName: 'Status',
      width: 120,
      renderCell: (params) => <InstanceStatusChip status={params.value} />,
    },
    {
      field: 'hasDrift',
      headerName: 'Drift',
      width: 80,
      align: 'center',
      renderCell: (params) => (
        <DriftIndicator hasDrift={params.value} />
      ),
    },
    {
      field: 'lastSeenAt',
      headerName: 'Last Seen',
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
      width: 120,
      getActions: (params) => {
        const actions = [
          <GridActionsCellItem
            key="view"
            icon={<ViewIcon />}
            label="View"
            onClick={() => onView(params.id as string)}
            showInMenu
          />,
        ];

        if (canManageApplicationServices) {
          actions.push(
            <GridActionsCellItem
              key="edit"
              icon={<EditIcon />}
              label="Edit"
              onClick={() => onEdit(params.row as ServiceInstance)}
              showInMenu
            />
          );
          actions.push(
            <GridActionsCellItem
              key="delete"
              icon={<DeleteIcon />}
              label="Delete"
              onClick={() => onDelete(params.id as string)}
              showInMenu
            />
          );
        }

        return actions;
      },
    },
  ];

  return <DataTable rows={instances} columns={columns} loading={loading} getRowId={(row) => row.id} />;
};
