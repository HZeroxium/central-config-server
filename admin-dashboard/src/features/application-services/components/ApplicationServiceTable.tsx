import React from 'react';
import { type GridColDef, GridActionsCellItem } from '@mui/x-data-grid';
import {
  Visibility as ViewIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Share as ShareIcon,
} from '@mui/icons-material';
import { DataTable } from '@components/common/DataTable';
import { ChipStatus } from '@components/common/ChipStatus';
import type { ApplicationService } from '../types';
import { usePermissions } from '@features/auth/hooks/usePermissions';

interface ApplicationServiceTableProps {
  services: ApplicationService[];
  loading?: boolean;
  onView?: (service: ApplicationService) => void;
  onEdit?: (service: ApplicationService) => void;
  onDelete?: (service: ApplicationService) => void;
  onShare?: (service: ApplicationService) => void;
}

export const ApplicationServiceTable: React.FC<ApplicationServiceTableProps> = ({
  services,
  loading = false,
  onView,
  onEdit,
  onDelete,
  onShare,
}) => {
  const { canEditService, canDeleteService, canShareService } = usePermissions();

  const columns: GridColDef[] = [
    {
      field: 'id',
      headerName: 'Service ID',
      width: 200,
      renderCell: (params) => (
        <span style={{ fontWeight: 500, fontFamily: 'monospace' }}>
          {params.value}
        </span>
      ),
    },
    {
      field: 'displayName',
      headerName: 'Display Name',
      width: 250,
      flex: 1,
    },
    {
      field: 'ownerTeamId',
      headerName: 'Owner Team',
      width: 180,
      renderCell: (params) => (
        <ChipStatus
          status="ACTIVE"
          label={params.value}
          variant="outlined"
        />
      ),
    },
    {
      field: 'environments',
      headerName: 'Environments',
      width: 200,
      renderCell: (params) => (
        <div style={{ display: 'flex', gap: '4px', flexWrap: 'wrap' }}>
          {params.value.map((env: string) => (
            <ChipStatus
              key={env}
              status={env.toUpperCase()}
              label={env}
              size="small"
            />
          ))}
        </div>
      ),
    },
    {
      field: 'lifecycle',
      headerName: 'Lifecycle',
      width: 120,
      renderCell: (params) => params.value ? (
        <ChipStatus
          status={params.value}
          label={params.value}
        />
      ) : (
        <span style={{ color: '#666' }}>Not set</span>
      ),
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
        const service = params.row as ApplicationService;
        const actions = [];

        if (onView) {
          actions.push(
            <GridActionsCellItem
              key="view"
              icon={<ViewIcon />}
              label="View"
              onClick={() => onView(service)}
            />
          );
        }

        if (canEditService(service.id) && onEdit) {
          actions.push(
            <GridActionsCellItem
              key="edit"
              icon={<EditIcon />}
              label="Edit"
              onClick={() => onEdit(service)}
            />
          );
        }

        if (canShareService(service.id) && onShare) {
          actions.push(
            <GridActionsCellItem
              key="share"
              icon={<ShareIcon />}
              label="Share"
              onClick={() => onShare(service)}
            />
          );
        }

        if (canDeleteService(service.id) && onDelete) {
          actions.push(
            <GridActionsCellItem
              key="delete"
              icon={<DeleteIcon />}
              label="Delete"
              onClick={() => onDelete(service)}
              showInMenu
            />
          );
        }

        return actions;
      },
    },
  ];

  return (
    <DataTable
      rows={services}
      columns={columns}
      loading={loading}
      getRowId={(row) => row.id}
      noRowsMessage="No application services found"
    />
  );
};

export default ApplicationServiceTable;
