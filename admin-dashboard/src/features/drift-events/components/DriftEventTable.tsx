import {
  DataGrid,
  type GridColDef,
  GridActionsCellItem,
} from "@mui/x-data-grid";
import {
  Visibility as ViewIcon,
  CheckCircle as ResolveIcon,
  RemoveCircle as IgnoreIcon,
} from "@mui/icons-material";
import { Box, Chip } from "@mui/material";
import type { DriftEventResponse } from "@lib/api/models";
import { format } from "date-fns";

interface DriftEventTableProps {
  events: DriftEventResponse[];
  loading: boolean;
  page: number;
  pageSize: number;
  totalElements: number;
  onPageChange: (page: number) => void;
  onPageSizeChange: (pageSize: number) => void;
  onRowClick: (eventId: string) => void;
  onResolve?: (eventId: string) => void;
  onIgnore?: (eventId: string) => void;
}

export function DriftEventTable({
  events,
  loading,
  page,
  pageSize,
  totalElements,
  onPageChange,
  onPageSizeChange,
  onRowClick,
  onResolve,
  onIgnore,
}: Readonly<DriftEventTableProps>) {
  const getStatusColor = (
    status?: string
  ): "default" | "success" | "warning" | "error" => {
    switch (status) {
      case "DETECTED":
        return "warning";
      case "RESOLVED":
        return "success";
      case "IGNORED":
        return "default";
      default:
        return "error";
    }
  };

  const getSeverityColor = (
    severity?: string
  ): "default" | "info" | "warning" | "error" => {
    switch (severity) {
      case "LOW":
        return "info";
      case "MEDIUM":
        return "warning";
      case "HIGH":
        return "warning";
      case "CRITICAL":
        return "error";
      default:
        return "default";
    }
  };

  const columns: GridColDef<DriftEventResponse>[] = [
    {
      field: "serviceName",
      headerName: "Service Name",
      flex: 1,
      minWidth: 150,
      renderCell: (params) => (
        <Box sx={{ fontWeight: 600 }}>{params.value}</Box>
      ),
    },
    {
      field: "instanceId",
      headerName: "Instance ID",
      flex: 1,
      minWidth: 200,
      renderCell: (params) => (
        <Box sx={{ fontFamily: "monospace", fontSize: "0.875rem" }}>
          {params.value}
        </Box>
      ),
    },
    {
      field: "status",
      headerName: "Status",
      width: 120,
      renderCell: (params) => (
        <Chip
          label={params.value || "Unknown"}
          color={getStatusColor(params.value)}
          size="small"
        />
      ),
    },
    {
      field: "severity",
      headerName: "Severity",
      width: 120,
      renderCell: (params) => (
        <Chip
          label={params.value || "Unknown"}
          color={getSeverityColor(params.value)}
          size="small"
          variant="outlined"
        />
      ),
    },
    {
      field: "detectedAt",
      headerName: "Detected At",
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
      field: "resolvedAt",
      headerName: "Resolved At",
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
      width: 120,
      getActions: (params) => {
        const actions = [
          <GridActionsCellItem
            key="view"
            icon={<ViewIcon />}
            label="View details"
            onClick={() => onRowClick(params.row.id || "")}
            showInMenu={false}
          />,
        ];

        // Only show Resolve/Ignore for DETECTED status
        if (params.row.status === "DETECTED" && onResolve && onIgnore) {
          actions.push(
            <GridActionsCellItem
              key="resolve"
              icon={
                <ResolveIcon
                  style={{ color: "var(--mui-palette-success-main)" }}
                />
              }
              label="Resolve this event"
              onClick={() => onResolve(params.row.id || "")}
              showInMenu={false}
            />,
            <GridActionsCellItem
              key="ignore"
              icon={
                <IgnoreIcon style={{ color: "var(--mui-palette-grey-500)" }} />
              }
              label="Ignore this event"
              onClick={() => onIgnore(params.row.id || "")}
              showInMenu={false}
            />
          );
        }

        return actions;
      },
    },
  ];

  // Create rows with unique id for DataGrid (events already have id field)
  const rows = events;

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
