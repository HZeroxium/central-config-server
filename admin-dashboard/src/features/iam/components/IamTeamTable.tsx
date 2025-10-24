import { DataGrid, type GridColDef, GridActionsCellItem } from '@mui/x-data-grid';
import { Visibility as ViewIcon } from '@mui/icons-material';
import { Box, Chip } from '@mui/material';
import type { IamTeamResponse } from '@lib/api/models';

interface IamTeamTableProps {
  teams: IamTeamResponse[];
  loading: boolean;
  page: number;
  pageSize: number;
  totalElements: number;
  onPageChange: (page: number) => void;
  onPageSizeChange: (pageSize: number) => void;
  onRowClick: (teamId: string) => void;
}

export function IamTeamTable({
  teams,
  loading,
  page,
  pageSize,
  totalElements,
  onPageChange,
  onPageSizeChange,
  onRowClick,
}: IamTeamTableProps) {
  const columns: GridColDef<IamTeamResponse>[] = [
    {
      field: 'id',
      headerName: 'Team ID',
      flex: 1,
      minWidth: 200,
      renderCell: (params) => (
        <Box sx={{ fontFamily: 'monospace', fontWeight: 600 }}>{params.value}</Box>
      ),
    },
    {
      field: 'displayName',
      headerName: 'Display Name',
      flex: 1.5,
      minWidth: 200,
    },
    {
      field: 'members',
      headerName: 'Members',
      width: 120,
      renderCell: (params) => {
        const members = params.value as string[] | undefined;
        const count = members?.length || 0;
        return (
          <Chip
            label={`${count} members`}
            size="small"
            variant="outlined"
            color={count > 0 ? 'primary' : 'default'}
          />
        );
      },
    },
    {
      field: 'syncStatus',
      headerName: 'Sync Status',
      width: 120,
      renderCell: (params) => {
        const status = params.value as string | undefined;
        const color = status === 'SYNCED' ? 'success' : status === 'PENDING' ? 'warning' : 'default';
        return (
          <Chip
            label={status || 'Unknown'}
            size="small"
            color={color}
          />
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
          onClick={() => onRowClick(params.row.teamId || '')}
          showInMenu={false}
        />,
      ],
    },
  ];

  // Create rows with unique id for DataGrid (teams already have id field)
  const rows = teams;

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
