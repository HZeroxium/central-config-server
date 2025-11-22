import {
  DataGrid,
  type GridColDef,
  GridActionsCellItem,
  GridToolbar,
} from "@mui/x-data-grid";
import {
  Visibility as ViewIcon,
  Refresh as RedriveIcon,
  Edit as EditIcon,
} from "@mui/icons-material";
import { Chip, Tooltip, Typography } from "@mui/material";
import type { FailedHeartbeatResponse } from "@lib/api/models";
import { format } from "date-fns";

interface FailedHeartbeatsTableProps {
  readonly failedHeartbeats: FailedHeartbeatResponse[];
  readonly loading: boolean;
  readonly page: number;
  readonly pageSize: number;
  readonly totalElements: number;
  readonly onPageChange: (page: number) => void;
  readonly onPageSizeChange: (pageSize: number) => void;
  readonly onView: (id: string) => void;
  readonly onRedrive: (id: string) => void;
  readonly onUpdateStatus: (id: string) => void;
}

export const getStatusColor = (
  status?: string
): "default" | "info" | "success" | "error" | "warning" => {
  switch (status) {
    case "INVESTIGATING":
      return "warning";
    case "RESOLVED":
      return "success";
    case "IGNORED":
      return "default";
    default:
      return "info";
  }
};

export function FailedHeartbeatsTable({
  failedHeartbeats,
  loading,
  page,
  pageSize,
  totalElements,
  onPageChange,
  onPageSizeChange,
  onView,
  onRedrive,
  onUpdateStatus,
}: FailedHeartbeatsTableProps) {
  const columns: GridColDef<FailedHeartbeatResponse>[] = [
    {
      field: "serviceName",
      headerName: "Service",
      width: 150,
      flex: 1,
    },
    {
      field: "instanceId",
      headerName: "Instance ID",
      width: 200,
      flex: 1,
    },
    {
      field: "environment",
      headerName: "Environment",
      width: 120,
    },
    {
      field: "exceptionMessage",
      headerName: "Error Message",
      width: 300,
      flex: 2,
      renderCell: (params) => (
        <Tooltip title={params.value || ""}>
          <Typography
            variant="body2"
            sx={{
              overflow: "hidden",
              textOverflow: "ellipsis",
              whiteSpace: "nowrap",
              maxWidth: 300,
            }}
          >
            {params.value || "N/A"}
          </Typography>
        </Tooltip>
      ),
    },
    {
      field: "status",
      headerName: "Status",
      width: 150,
      renderCell: (params) => (
        <Chip
          label={params.value || "UNKNOWN"}
          color={getStatusColor(params.value)}
          size="small"
        />
      ),
    },
    {
      field: "firstSeenAt",
      headerName: "First Seen",
      width: 180,
      renderCell: (params) =>
        params.value
          ? format(new Date(params.value), "PPpp")
          : "N/A",
    },
    {
      field: "retryCount",
      headerName: "Retries",
      width: 100,
      type: "number",
    },
    {
      field: "actions",
      type: "actions",
      headerName: "Actions",
      width: 150,
      getActions: (params) => [
        <GridActionsCellItem
          key="view"
          icon={<ViewIcon />}
          label="View details"
          onClick={() => onView(params.row.id || "")}
          showInMenu={false}
        />,
        <GridActionsCellItem
          key="redrive"
          icon={<RedriveIcon />}
          label="Re-drive"
          onClick={() => onRedrive(params.row.id || "")}
          showInMenu={false}
        />,
        <GridActionsCellItem
          key="update-status"
          icon={<EditIcon />}
          label="Update status"
          onClick={() => onUpdateStatus(params.row.id || "")}
          showInMenu={false}
        />,
      ],
    },
  ];

  return (
    <DataGrid
      rows={failedHeartbeats}
      columns={columns}
      loading={loading}
      paginationMode="server"
      paginationModel={{ page, pageSize }}
      rowCount={totalElements}
      onPaginationModelChange={(model) => {
        onPageChange(model.page);
        onPageSizeChange(model.pageSize);
      }}
      getRowId={(row) => row.id || ""}
      slots={{
        toolbar: GridToolbar,
      }}
      slotProps={{
        toolbar: {
          showQuickFilter: true,
        },
      }}
      sx={{
        height: 600,
        "& .MuiDataGrid-cell": {
          overflow: "visible",
        },
      }}
    />
  );
}

