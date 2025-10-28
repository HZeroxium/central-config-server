import { DataGrid, type GridColDef, GridActionsCellItem } from '@mui/x-data-grid';
import { Delete as DeleteIcon, Visibility as ViewIcon, PersonAdd as RequestOwnershipIcon, Warning as WarningIcon } from '@mui/icons-material';
import { Box, Chip } from '@mui/material';
import type { ApplicationServiceResponse } from '@lib/api/models';
import { useAuth } from '@features/auth/authContext';

interface ApplicationServiceTableProps {
  services: ApplicationServiceResponse[];
  loading: boolean;
  page: number;
  pageSize: number;
  totalElements: number;
  onPageChange: (page: number) => void;
  onPageSizeChange: (pageSize: number) => void;
  onRowClick: (serviceId: string) => void;
  onDelete: (serviceId: string) => void;
  onRequestOwnership?: (serviceId: string) => void;
}

export function ApplicationServiceTable({
  services,
  loading,
  page,
  pageSize,
  totalElements,
  onPageChange,
  onPageSizeChange,
  onRowClick,
  onDelete,
  onRequestOwnership,
}: ApplicationServiceTableProps) {
  const { isSysAdmin, permissions, userInfo } = useAuth();

  const canDelete = (serviceId?: string) => {
    if (isSysAdmin) return true;
    if (!serviceId) return false;
    return permissions?.ownedServiceIds?.includes(serviceId) || false;
  };

  const isOrphaned = (service: ApplicationServiceResponse) => {
    return !service.ownerTeamId || service.ownerTeamId === null;
  };

  const isShared = (service: ApplicationServiceResponse) => {
    // Service is shared if:
    // 1. It has an owner team (not orphaned)
    // 2. User is not a member of the owner team
    // 3. User can see it (which means it must be shared)
    if (!service.ownerTeamId || !userInfo?.teamIds) return false;
    return !userInfo.teamIds.includes(service.ownerTeamId);
  };

  const columns: GridColDef<ApplicationServiceResponse>[] = [
    {
      field: 'id',
      headerName: 'Service ID',
      flex: 1,
      minWidth: 200,
      renderCell: (params) => (
        <Box sx={{ fontFamily: 'monospace', fontWeight: 600, color: 'primary.main' }}>
          {params.value}
        </Box>
      ),
    },
    {
      field: 'displayName',
      headerName: 'Display Name',
      flex: 1.5,
      minWidth: 220,
    },
    {
      field: 'ownerTeamId',
      headerName: 'Owner Team',
      width: 250,
      renderCell: (params) => {
        const service = params.row;
        if (!params.value) {
          return (
            <Chip 
              label="Orphan" 
              color="warning" 
              size="small"
              icon={<WarningIcon />}
              sx={{ fontWeight: 600 }}
            />
          );
        }
        
        // Show "SHARED" badge for services shared to user's team
        if (isShared(service)) {
          return (
            <Box sx={{ display: 'flex', gap: 0.5, alignItems: 'center' }}>
              <Chip label={params.value} variant="outlined" size="small" />
              <Chip 
                label="SHARED" 
                color="info" 
                size="small"
                sx={{ fontWeight: 600 }}
              />
            </Box>
          );
        }
        
        return <Chip label={params.value} variant="outlined" size="small" />;
      },
    },
    {
      field: 'lifecycle',
      headerName: 'Lifecycle',
      width: 120,
      renderCell: (params) => {
        const lifecycleColors: Record<string, 'success' | 'warning' | 'error' | 'default'> = {
          ACTIVE: 'success',
          DEPRECATED: 'warning',
          RETIRED: 'error',
        };
        const color = lifecycleColors[params.value as string] || 'default';
        return <Chip label={params.value} color={color} size="small" />;
      },
    },
    {
      field: 'environments',
      headerName: 'Environments',
      flex: 1,
      minWidth: 150,
      renderCell: (params) => {
        const envs = params.value as string[] | undefined;
        if (!envs || envs.length === 0) return '-';
        return (
          <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
            {envs.slice(0, 3).map((env) => (
              <Chip
                key={env}
                label={env.toUpperCase()}
                size="small"
                variant="outlined"
                color={env === 'prod' ? 'error' : env === 'staging' ? 'warning' : 'info'}
              />
            ))}
            {envs.length > 3 && <Chip label={`+${envs.length - 3}`} size="small" variant="outlined" />}
          </Box>
        );
      },
    },
    {
      field: 'tags',
      headerName: 'Tags',
      flex: 1,
      minWidth: 150,
      renderCell: (params) => {
        const tags = params.value as string[] | undefined;
        if (!tags || tags.length === 0) return '-';
        return (
          <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
            {tags.slice(0, 2).map((tag) => (
              <Chip key={tag} label={tag} size="small" variant="filled" color="default" />
            ))}
            {tags.length > 2 && <Chip label={`+${tags.length - 2}`} size="small" />}
          </Box>
        );
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
            onClick={() => onRowClick(params.row.id || '')}
            showInMenu={false}
          />,
        ];

        // Add Request Ownership action for orphaned services
        if (isOrphaned(params.row) && onRequestOwnership) {
          actions.push(
            <GridActionsCellItem
              key="request-ownership"
              icon={<RequestOwnershipIcon style={{ color: 'var(--mui-palette-warning-main)' }} />}
              label="Request Ownership"
              onClick={() => onRequestOwnership(params.row.id || '')}
              showInMenu={false}
            />
          );
        }

        if (canDelete(params.row.id)) {
          actions.push(
            <GridActionsCellItem
              key="delete"
              icon={<DeleteIcon />}
              label="Delete"
              onClick={() => onDelete(params.row.id || '')}
              showInMenu={false}
            />
          );
        }

        return actions;
      },
    },
  ];

  // Create rows with unique id for DataGrid (service already has id field)
  const rows = services;

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
        getRowClassName={(params) => {
          return isOrphaned(params.row) ? 'orphaned-service-row' : '';
        }}
        sx={{
          '& .MuiDataGrid-row': {
            cursor: 'pointer',
          },
          '& .orphaned-service-row': {
            backgroundColor: 'warning.lighter',
            '&:hover': {
              backgroundColor: 'warning.light',
            },
          },
        }}
      />
    </Box>
  );
}
