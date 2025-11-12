import {
  DataGrid,
  type GridColDef,
  type GridRenderCellParams,
  GridActionsCellItem,
} from "@mui/x-data-grid";
import { Box, Typography, Chip } from "@mui/material";
import {
  Visibility as ViewIcon,
  HealthAndSafety as HealthIcon,
} from "@mui/icons-material";
import { useNavigate } from "react-router-dom";
import { useState } from "react";
import type { ConsulServicesMap } from "@lib/api/models";
import { ChipList } from "@components/common/ChipList";
import { ServiceHealthModal } from "./ServiceHealthModal";
import { useGetServiceRegistryServiceInstances } from "@lib/api/generated/service-registry/service-registry";

interface ConsulServiceTableProps {
  servicesData: ConsulServicesMap | undefined;
  loading: boolean;
  searchTerm?: string;
  tagFilter?: string;
}

interface ServiceRow {
  id: string;
  serviceName: string;
  tags: string[];
  tagCount: number;
  hasHealthyInstances?: boolean;
}

export default function ConsulServiceTable({
  servicesData,
  loading,
  searchTerm = "",
  tagFilter,
}: Readonly<ConsulServiceTableProps>) {
  const navigate = useNavigate();
  const [selectedService, setSelectedService] = useState<string | null>(null);
  const [healthModalOpen, setHealthModalOpen] = useState(false);

  // Fetch instances for selected service
  const { data: instancesData } = useGetServiceRegistryServiceInstances(
    selectedService || "",
    { passing: false },
    {
      query: {
        enabled: !!selectedService && healthModalOpen,
        staleTime: 30_000,
      },
    }
  );

  // Transform services map to table rows with client-side filtering
  const rows: ServiceRow[] = servicesData?.services
    ? Object.entries(servicesData.services)
        .filter(([serviceName, tags]) => {
          // Search filter
          if (searchTerm && !serviceName.toLowerCase().includes(searchTerm.toLowerCase())) {
            return false;
          }
          // Tag filter
          if (tagFilter && (!tags || !tags.includes(tagFilter))) {
            return false;
          }
          return true;
        })
        .map(([serviceName, tags]) => ({
          id: serviceName,
          serviceName,
          tags: tags || [],
          tagCount: tags?.length || 0,
          hasHealthyInstances: true, // This would come from instance data if available
        }))
    : [];

  const handleViewDetails = (serviceName: string) => {
    navigate(`/registry/${serviceName}`);
  };

  const handleHealthCheck = (serviceName: string) => {
    setSelectedService(serviceName);
    setHealthModalOpen(true);
  };

  const columns: GridColDef<ServiceRow>[] = [
    {
      field: "serviceName",
      headerName: "Service Name",
      flex: 1,
      minWidth: 200,
      sortable: true,
      renderCell: (params: GridRenderCellParams<ServiceRow>) => (
        <Box
          sx={{
            fontFamily: "monospace",
            fontWeight: 600,
            color: "primary.main",
          }}
        >
          {params.value}
        </Box>
      ),
    },
    {
      field: "hasHealthyInstances",
      headerName: "Status",
      width: 120,
      align: "center",
      headerAlign: "center",
      sortable: true,
      renderCell: (params: GridRenderCellParams<ServiceRow>) => (
        <Chip
          label={params.value ? "Healthy" : "Unknown"}
          color={params.value ? "success" : "default"}
          size="small"
          icon={<HealthIcon />}
          sx={{ fontWeight: 600 }}
        />
      ),
    },
    {
      field: "tagCount",
      headerName: "Tag Count",
      width: 120,
      align: "center",
      headerAlign: "center",
      sortable: true,
      renderCell: (params: GridRenderCellParams<ServiceRow>) => (
        <Typography variant="body2" fontWeight={600}>
          {params.value}
        </Typography>
      ),
    },
    {
      field: "tags",
      headerName: "Tags",
      flex: 2,
      minWidth: 300,
      sortable: false,
      filterable: false,
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
    {
      field: "actions",
      type: "actions",
      headerName: "Actions",
      width: 120,
      getActions: (params) => [
        <GridActionsCellItem
          key="view"
          icon={<ViewIcon />}
          label="View details"
          onClick={() => handleViewDetails(params.row.serviceName)}
          showInMenu={false}
        />,
        <GridActionsCellItem
          key="health"
          icon={<HealthIcon />}
          label="Health check"
          onClick={() => handleHealthCheck(params.row.serviceName)}
          showInMenu={false}
        />,
      ],
    },
  ];

  return (
    <>
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
            sorting: {
              sortModel: [{ field: "serviceName", sort: "asc" }],
            },
          }}
          disableRowSelectionOnClick
          onRowClick={(params) => {
            handleViewDetails(params.row.serviceName);
          }}
          slotProps={{
            noRowsOverlay: {
              children: (
                <Box
                  sx={{
                    display: "flex",
                    flexDirection: "column",
                    alignItems: "center",
                    justifyContent: "center",
                    height: "100%",
                    gap: 2,
                  }}
                >
                  <Typography variant="h6" color="text.secondary">
                    No services found
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {searchTerm || tagFilter
                      ? "Try adjusting your filters or check back later."
                      : "No services are currently registered in Consul."}
                  </Typography>
                </Box>
              ),
            },
          }}
          sx={{
            "& .MuiDataGrid-cell": {
              display: "flex",
              alignItems: "center",
              py: 1.5,
            },
            "& .MuiDataGrid-row": {
              cursor: "pointer",
              "&:hover": {
                backgroundColor: (theme) =>
                  theme.palette.mode === "light"
                    ? "rgba(37, 99, 235, 0.04)"
                    : "rgba(96, 165, 250, 0.08)",
              },
            },
            "& .MuiDataGrid-columnHeader": {
              backgroundColor: (theme) =>
                theme.palette.mode === "light"
                  ? "rgba(37, 99, 235, 0.04)"
                  : "rgba(96, 165, 250, 0.08)",
              fontWeight: 600,
            },
            "& .MuiDataGrid-columnHeaderTitle": {
              fontWeight: 600,
            },
          }}
        />
      </Box>

      {selectedService && (
        <ServiceHealthModal
          open={healthModalOpen}
          onClose={() => {
            setHealthModalOpen(false);
            setSelectedService(null);
          }}
          serviceName={selectedService}
          instances={instancesData?.instances || []}
        />
      )}
    </>
  );
}
