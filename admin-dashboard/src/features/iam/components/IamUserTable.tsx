import { DataGrid, type GridColDef, GridActionsCellItem } from '@mui/x-data-grid';
import { Visibility as ViewIcon } from '@mui/icons-material';
import { Box, Chip, Avatar } from '@mui/material';
import type { IamUserResponse } from '@lib/api/models';

interface IamUserTableProps {
  users: IamUserResponse[];
  loading: boolean;
  page: number;
  pageSize: number;
  totalElements: number;
  onPageChange: (page: number) => void;
  onPageSizeChange: (pageSize: number) => void;
  onRowClick: (userId: string) => void;
}

export function IamUserTable({
  users,
  loading,
  page,
  pageSize,
  totalElements,
  onPageChange,
  onPageSizeChange,
  onRowClick,
}: IamUserTableProps) {
  const columns: GridColDef<IamUserResponse>[] = [
    {
      field: 'username',
      headerName: 'Username',
      flex: 1,
      minWidth: 150,
      renderCell: (params) => (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Avatar sx={{ width: 32, height: 32, bgcolor: 'primary.main', fontSize: '0.875rem' }}>
            {params.value?.charAt(0).toUpperCase()}
          </Avatar>
          <Box sx={{ fontWeight: 600 }}>{params.value}</Box>
        </Box>
      ),
    },
    {
      field: 'email',
      headerName: 'Email',
      flex: 1.5,
      minWidth: 200,
    },
    {
      field: 'firstName',
      headerName: 'First Name',
      flex: 1,
      minWidth: 120,
    },
    {
      field: 'lastName',
      headerName: 'Last Name',
      flex: 1,
      minWidth: 120,
    },
    {
      field: 'roles',
      headerName: 'Roles',
      flex: 1,
      minWidth: 150,
      renderCell: (params) => {
        const roles = params.value as string[] | undefined;
        if (!roles || roles.length === 0) return '-';
        return (
          <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
            {roles.slice(0, 2).map((role) => (
              <Chip
                key={role}
                label={role}
                size="small"
                color={role === 'SYS_ADMIN' ? 'error' : 'default'}
                variant="outlined"
              />
            ))}
            {roles.length > 2 && <Chip label={`+${roles.length - 2}`} size="small" variant="outlined" />}
          </Box>
        );
      },
    },
    {
      field: 'teamIds',
      headerName: 'Teams',
      flex: 1,
      minWidth: 100,
      renderCell: (params) => {
        const teams = params.value as string[] | undefined;
        if (!teams || teams.length === 0) return '-';
        return (
          <Chip label={`${teams.length} teams`} size="small" variant="outlined" color="info" />
        );
      },
    },
    {
      field: 'actions',
      type: 'actions',
      headerName: 'Actions',
      width: 80,
      getActions: (params) => [
        <GridActionsCellItem
          key="view"
          icon={<ViewIcon />}
          label="View"
          onClick={() => onRowClick(params.row.userId || '')}
          showInMenu={false}
        />,
      ],
    },
  ];

  // Create rows with unique id for DataGrid
  const rows = users.map((user) => ({
    ...user,
    id: user.userId || user.username || Math.random().toString(),
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
