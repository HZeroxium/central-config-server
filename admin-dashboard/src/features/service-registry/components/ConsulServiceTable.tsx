import {
  DataGrid,
  type GridColDef,
  type GridRenderCellParams,
} from "@mui/x-data-grid";
import { Box } from "@mui/material";
import { useNavigate } from "react-router-dom";
import type { ConsulServicesMap } from "@lib/api/models";
import { ChipList } from "@components/common/ChipList";

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

export default function ConsulServiceTable({
  servicesData,
  loading,
}: Readonly<ConsulServiceTableProps>) {
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
      field: "serviceName",
      headerName: "Service Name",
      flex: 1,
      minWidth: 200,
    },
    {
      field: "tagCount",
      headerName: "Tags",
      width: 100,
      align: "center",
      headerAlign: "center",
    },
    {
      field: "tags",
      headerName: "Tag List",
      flex: 2,
      minWidth: 300,
      renderCell: (params: GridRenderCellParams<ServiceRow>) => {
        const tags = params.row.tags || [];
        return (
          <ChipList
            items={tags}
            maxVisible={5}
            variant="outlined"
            chipProps={{ color: "default" }}
          />
        );
      },
    },
  ];

  return (
    <Box sx={{ height: 600, width: "100%" }}>
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
          "& .MuiDataGrid-row": {
            cursor: "pointer",
          },
        }}
      />
    </Box>
  );
}
