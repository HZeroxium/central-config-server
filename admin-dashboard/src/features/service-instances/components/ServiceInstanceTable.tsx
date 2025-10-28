import { DataGrid, type GridColDef, GridActionsCellItem } from '@mui/x-data-grid';
import { Delete as DeleteIcon, Visibility as ViewIcon } from '@mui/icons-material';
import { Box, Chip } from '@mui/material';
import type { ServiceInstanceResponse } from '@lib/api/models';
import { useAuth } from '@features/auth/authContext';
import InstanceStatusChip from './InstanceStatusChip';
import DriftIndicator from './DriftIndicator';
import { format } from 'date-fns';

interface ServiceInstanceTableProps {
  instances: ServiceInstanceResponse[];
  loading: boolean;
  page: number;
  pageSize: number;
  totalElements: number;
  onPageChange: (page: number) => void;
  onPageSizeChange: (pageSize: number) => void;
  onRowClick: (instanceId: string) => void;
  onDelete: (instanceId: string) => void;
}

export function ServiceInstanceTable({
  instances,
  loading,
  page,
  pageSize,
  totalElements,
  onPageChange,
  onPageSizeChange,
  onRowClick,
  onDelete,
}: ServiceInstanceTableProps) {
  const { isSysAdmin, permissions } = useAuth();

  const canDelete = (serviceName?: string) => {
    if (isSysAdmin) return true;
    if (!serviceName) return false;
    // Check if user's team owns this service or has edit permissions
    return permissions?.ownedServiceIds?.includes(serviceName) || false;
  };

  const columns: GridColDef<ServiceInstanceResponse>[] = [
    {
      field: 'serviceName',
      headerName: 'Service Name',
      flex: 1,
      minWidth: 180,
      renderCell: (params) => (
        <Box sx={{ fontWeight: 600, color: 'primary.main' }}>{params.value}</Box>
      ),
    },
    {
      field: 'instanceId',
      headerName: 'Instance ID',
      flex: 1,
      minWidth: 200,
    },
    {
      field: 'environment',
      headerName: 'Environment',
      width: 120,
      renderCell: (params) => {
        const envColors: Record<string, string> = {
          dev: 'info',
          staging: 'warning',
          prod: 'error',
        };
        const color = envColors[params.value as string] || 'default';
        return (
          <Chip
            label={params.value?.toUpperCase()}
            color={color as any}
            size="small"
            variant="outlined"
          />
        );
      },
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
      width: 100,
      align: 'center',
      renderCell: (params) => (
        <DriftIndicator 
          hasDrift={params.value} 
          driftDetectedAt={params.row.driftDetectedAt}
          serviceId={params.row.serviceName}
          instanceId={params.row.instanceId}
        />
      ),
    },
    {
      field: 'version',
      headerName: 'Version',
      width: 100,
    },
    {
      field: 'host',
      headerName: 'Host',
      width: 150,
    },
    {
      field: 'port',
      headerName: 'Port',
      width: 80,
    },
    {
      field: 'lastSeenAt',
      headerName: 'Last Seen',
      width: 160,
      renderCell: (params) => {
        if (!params.value) return '-';
        try {
          return format(new Date(params.value), 'MMM dd, yyyy HH:mm');
        } catch {
          return params.value;
        }
      },
    },
    {
      field: 'actions',
      type: 'actions',
      headerName: 'Actions',
      width: 100,
      getActions: (params) => {
        const actions = [
          <GridActionsCellItem
            key="view"
            icon={<ViewIcon />}
            label="View"
            onClick={() => onRowClick(params.row.instanceId || '')}
            showInMenu={false}
          />,
        ];

        if (canDelete(params.row.serviceName)) {
          actions.push(
            <GridActionsCellItem
              key="delete"
              icon={<DeleteIcon />}
              label="Delete"
              onClick={() => onDelete(params.row.instanceId || '')}
              showInMenu={false}
            />
          );
        }

        return actions;
      },
    },
  ];

  // Create rows with unique id for DataGrid
  const rows = instances.map((instance) => ({
    ...instance,
    id: instance.instanceId || `${instance.serviceName}-${Math.random()}`,
  }));

  return (
    <Box sx={{ height: 600, width: '100%' }}>
      <DataGrid
        rows={rows}
        columns={columns}
        loading={loading}
        paginationMode="server"
        rowCount={totalElements}
        paginationModel={{ page, pageSize }}
        onPaginationModelChange={(model) => {
          if (model.page !== page) {
            onPageChange(model.page);
          }
          if (model.pageSize !== pageSize) {
            onPageSizeChange(model.pageSize);
          }
        }}
        pageSizeOptions={[10, 20, 50, 100]}
        disableRowSelectionOnClick
        sx={{
          '& .MuiDataGrid-row': {
            cursor: 'pointer',
          },
        }}
      />
    </Box>
  );
}
