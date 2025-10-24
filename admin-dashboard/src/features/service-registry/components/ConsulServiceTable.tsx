import { DataGrid, type GridColDef, type GridRenderCellParams } from '@mui/x-data-grid';
import { Box, Chip, Stack } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import type { ConsulServicesMap } from '@lib/api/models';

interface ConsulServiceTableProps {
  servicesData: ConsulServicesMap | undefined;
  loading: boolean;
}

interface ServiceRow {
  id: string;
  serviceName: string;
  tags: string[];
  tagCount: number;
}

export default function ConsulServiceTable({ servicesData, loading }: ConsulServiceTableProps) {
  const navigate = useNavigate();

  // Transform services map to table rows
  const rows: ServiceRow[] = servicesData?.services
    ? Object.entries(servicesData.services).map(([serviceName, tags]) => ({
        id: serviceName,
        serviceName,
        tags: tags || [],
        tagCount: tags?.length || 0,
      }))
    : [];

  const columns: GridColDef<ServiceRow>[] = [
    {
      field: 'serviceName',
      headerName: 'Service Name',
      flex: 1,
      minWidth: 200,
    },
    {
      field: 'tagCount',
      headerName: 'Tags',
      width: 100,
      align: 'center',
      headerAlign: 'center',
    },
    {
      field: 'tags',
      headerName: 'Tag List',
      flex: 2,
      minWidth: 300,
      renderCell: (params: GridRenderCellParams<ServiceRow>) => {
        const tags = params.row.tags || [];
        return (
          <Stack direction="row" spacing={0.5} flexWrap="wrap" sx={{ py: 0.5 }}>
            {tags.slice(0, 5).map((tag: string) => (
              <Chip key={tag} label={tag} size="small" variant="outlined" />
            ))}
            {tags.length > 5 && (
              <Chip label={`+${tags.length - 5} more`} size="small" variant="outlined" />
            )}
          </Stack>
        );
      },
    },
  ];

  return (
    <Box sx={{ height: 600, width: '100%' }}>
      <DataGrid
        rows={rows}
        columns={columns}
        loading={loading}
        pageSizeOptions={[10, 20, 50, 100]}
        initialState={{
          pagination: {
            paginationModel: { page: 0, pageSize: 20 },
          },
        }}
        disableRowSelectionOnClick
        onRowClick={(params) => {
          navigate(`/registry/${params.row.serviceName}`);
        }}
        sx={{
          '& .MuiDataGrid-row': {
            cursor: 'pointer',
          },
        }}
      />
    </Box>
  );
}

