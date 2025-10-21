import React from 'react';
import { type GridColDef, GridActionsCellItem } from '@mui/x-data-grid';
import {
  Visibility as ViewIcon,
  Block as RevokeIcon,
} from '@mui/icons-material';
import { DataTable } from '@components/common/DataTable';
import { ChipStatus } from '@components/common/ChipStatus';
import { PermissionChips } from './PermissionChips';
import type { ServiceShare } from '../types';
import { usePermissions } from '@features/auth/hooks/usePermissions';

interface ServiceShareTableProps {
  shares: ServiceShare[];
  loading?: boolean;
  onView: (id: string) => void;
  onRevoke: (id: string) => void;
}

export const ServiceShareTable: React.FC<ServiceShareTableProps> = ({
  shares,
  loading,
  onView,
  onRevoke,
}) => {
  const { canManageApplicationServices } = usePermissions();

  const columns: GridColDef[] = [
    { 
      field: 'serviceId', 
      headerName: 'Service ID', 
      width: 200,
      renderCell: (params) => (
        <strong>{params.value}</strong>
      ),
    },
    { 
      field: 'grantToType', 
      headerName: 'Grant To Type', 
      width: 120,
      renderCell: (params) => (
        <span style={{ textTransform: 'capitalize' }}>
          {params.value}
        </span>
      ),
    },
    { 
      field: 'grantToId', 
      headerName: 'Grant To ID', 
      width: 150,
    },
    {
      field: 'permissions',
      headerName: 'Permissions',
      width: 200,
      renderCell: (params) => <PermissionChips permissions={params.value} />,
    },
    {
      field: 'environments',
      headerName: 'Environments',
      width: 150,
      renderCell: (params) => {
        const envs = params.value;
        if (!envs || envs.length === 0) {
          return <span style={{ color: '#666' }}>All</span>;
        }
        return (
          <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
            {envs.map((env: string) => (
              <ChipStatus key={env} status={env.toUpperCase()} />
            ))}
          </div>
        );
      },
    },
    {
      field: 'expiresAt',
      headerName: 'Expires At',
      width: 150,
      type: 'dateTime',
      valueFormatter: (value: any) => {
        if (!value) return 'Never';
        return new Date(value).toLocaleDateString();
      },
    },
    {
      field: 'isActive',
      headerName: 'Status',
      width: 100,
      renderCell: (params) => <ChipStatus status={params.value ? 'ACTIVE' : 'INACTIVE'} />,
    },
    {
      field: 'grantedAt',
      headerName: 'Granted At',
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
        const share = params.row as ServiceShare;
        const canRevoke = canManageApplicationServices && share.isActive;
        
        const actions = [
          <GridActionsCellItem
            key="view"
            icon={<ViewIcon />}
            label="View"
            onClick={() => onView(params.id as string)}
            showInMenu
          />,
        ];

        if (canRevoke) {
          actions.push(
            <GridActionsCellItem
              key="revoke"
              icon={<RevokeIcon />}
              label="Revoke"
              onClick={() => onRevoke(params.id as string)}
              showInMenu
            />
          );
        }

        return actions;
      },
    },
  ];

  return <DataTable rows={shares} columns={columns} loading={loading} getRowId={(row) => row.id} />;
};
