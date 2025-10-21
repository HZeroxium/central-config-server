import React from 'react';
import { DataGrid, type DataGridProps, type GridColDef, type GridRowsProp } from '@mui/x-data-grid';
import { Paper } from '@mui/material';

interface DataTableProps extends Omit<DataGridProps, 'rows' | 'columns'> {
  rows: GridRowsProp;
  columns: GridColDef[];
  loading?: boolean;
  height?: number;
  noRowsMessage?: string;
}

export const DataTable: React.FC<DataTableProps> = ({
  rows,
  columns,
  loading = false,
  height = 400,
  noRowsMessage = 'No data available',
  ...dataGridProps
}) => {
  return (
    <Paper sx={{ height, width: '100%' }}>
      <DataGrid
        rows={rows}
        columns={columns}
        loading={loading}
        initialState={{
          pagination: {
            paginationModel: { page: 0, pageSize: 10 },
          },
        }}
        pageSizeOptions={[5, 10, 25, 50]}
        disableRowSelectionOnClick
        disableColumnMenu={false}
        disableColumnFilter={false}
        disableColumnSorting={false}
        sx={{
          border: 'none',
          '& .MuiDataGrid-cell': {
            borderBottom: '1px solid #f0f0f0',
          },
          '& .MuiDataGrid-columnHeaders': {
            backgroundColor: '#f8fafc',
            borderBottom: '2px solid #e2e8f0',
          },
          '& .MuiDataGrid-row:hover': {
            backgroundColor: '#f8fafc',
          },
        }}
        slots={{
          noRowsOverlay: () => (
            <div style={{ padding: '20px', textAlign: 'center', color: '#666' }}>
              {noRowsMessage}
            </div>
          ),
        }}
        {...dataGridProps}
      />
    </Paper>
  );
};

export default DataTable;
