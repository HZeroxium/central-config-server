import {
  DataGrid,
  type GridColDef,
  GridActionsCellItem,
} from "@mui/x-data-grid";
import {
  Delete as DeleteIcon,
  Visibility as ViewIcon,
} from "@mui/icons-material";
import { Box, Chip, Typography } from "@mui/material";
import type { ServiceInstanceResponse } from "@lib/api/models";
import { useAuth } from "@features/auth/context";
import InstanceStatusChip from "./InstanceStatusChip";
import DriftIndicator from "./DriftIndicator";
import { format } from "date-fns";

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
}: Readonly<ServiceInstanceTableProps>) {
  const { isSysAdmin, permissions } = useAuth();

  const canDelete = (serviceName?: string) => {
    if (isSysAdmin) return true;
    if (!serviceName) return false;
    // Check if user's team owns this service or has edit permissions
    return permissions?.ownedServiceIds?.includes(serviceName) || false;
  };

  const columns: GridColDef<ServiceInstanceResponse>[] = [
    {
      field: "serviceId",
      headerName: "Service ID",
      flex: 1,
      minWidth: 180,
      renderCell: (params) => (
        <Box sx={{ fontWeight: 600, color: "primary.main" }}>
          {params.row.serviceId}
        </Box>
      ),
    },
    {
      field: "instanceId",
      headerName: "Instance ID",
      flex: 1,
      minWidth: 200,
    },
    {
      field: "environment",
      headerName: "Environment",
      width: 120,
      renderCell: (params) => {
        const envColors: Record<
          string,
          "info" | "warning" | "error" | "default"
        > = {
          dev: "info",
          staging: "warning",
          prod: "error",
        };
        const color = envColors[params.value as string] || "default";
        return (
          <Chip
            label={params.value?.toUpperCase()}
            color={color}
            size="small"
            variant="outlined"
          />
        );
      },
    },
    {
      field: "status",
      headerName: "Status",
      width: 120,
      renderCell: (params) => <InstanceStatusChip status={params.value} />,
    },
    {
      field: "hasDrift",
      headerName: "Drift",
      width: 100,
      align: "center",
      renderCell: (params) => (
        <DriftIndicator
          hasDrift={params.value}
          driftDetectedAt={params.row.driftDetectedAt}
          serviceId={params.row.serviceId}
          instanceId={params.row.instanceId}
        />
      ),
    },
    {
      field: "version",
      headerName: "Version",
      width: 100,
    },
    {
      field: "host",
      headerName: "Host",
      width: 150,
    },
    {
      field: "port",
      headerName: "Port",
      width: 80,
    },
    {
      field: "lastSeenAt",
      headerName: "Last Seen",
      width: 160,
      renderCell: (params) => {
        if (!params.value) return "-";
        try {
          return format(new Date(params.value), "MMM dd, yyyy HH:mm");
        } catch {
          return params.value;
        }
      },
    },
    {
      field: "actions",
      type: "actions",
      headerName: "Actions",
      width: 100,
      getActions: (params) => {
        const actions = [
          <GridActionsCellItem
            key="view"
            icon={<ViewIcon />}
            label="View details"
            onClick={() => onRowClick(params.row.instanceId || "")}
            showInMenu={false}
          />,
        ];

        if (canDelete(params.row.serviceId)) {
          actions.push(
            <GridActionsCellItem
              key="delete"
              icon={<DeleteIcon color="error" />}
              label="Delete this instance"
              onClick={() => onDelete(params.row.instanceId || "")}
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
    id: instance.instanceId || `${instance.serviceId}-${Math.random()}`,
  }));

  return (
    <Box sx={{ height: 600, width: "100%" }}>
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
                  No service instances found
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Try adjusting your filters or check back later.
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
  );
}
