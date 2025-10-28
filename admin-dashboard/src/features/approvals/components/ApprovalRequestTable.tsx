import {
  DataGrid,
  type GridColDef,
  GridActionsCellItem,
} from "@mui/x-data-grid";
import { Visibility as ViewIcon } from "@mui/icons-material";
import { Box, Chip, LinearProgress, Tooltip } from "@mui/material";
import type { ApprovalRequestResponse } from "@lib/api/models";
import { format } from "date-fns";

interface ApprovalRequestTableProps {
  readonly requests: ApprovalRequestResponse[];
  readonly loading: boolean;
  readonly page: number;
  readonly pageSize: number;
  readonly totalElements: number;
  readonly onPageChange: (page: number) => void;
  readonly onPageSizeChange: (pageSize: number) => void;
  readonly onRowClick: (requestId: string) => void;
}

export function ApprovalRequestTable({
  requests,
  loading,
  page,
  pageSize,
  totalElements,
  onPageChange,
  onPageSizeChange,
  onRowClick,
}: ApprovalRequestTableProps) {
  const getStatusColor = (
    status?: string
  ): "default" | "info" | "success" | "error" | "warning" => {
    switch (status) {
      case "PENDING":
        return "warning";
      case "APPROVED":
        return "success";
      case "REJECTED":
        return "error";
      case "CANCELLED":
        return "default";
      default:
        return "info";
    }
  };

  const columns: GridColDef<ApprovalRequestResponse>[] = [
    {
      field: "id",
      headerName: "Request ID",
      flex: 1,
      minWidth: 200,
      renderCell: (params) => (
        <Box sx={{ fontFamily: "monospace", fontSize: "0.875rem" }}>
          {params.value}
        </Box>
      ),
    },
    {
      field: "requestType",
      headerName: "Type",
      width: 150,
      renderCell: (params) => (
        <Chip label={params.value || "N/A"} size="small" variant="outlined" />
      ),
    },
    {
      field: "requesterUserId",
      headerName: "Requester",
      flex: 1,
      minWidth: 150,
    },
    {
      field: "targetServiceId",
      headerName: "Target Service",
      flex: 1,
      minWidth: 150,
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
      field: "approvalGates",
      headerName: "Gates Progress",
      width: 150,
      renderCell: (params) => {
        const gates = params.value as Array<{ status?: string }> | undefined;
        if (!gates || gates.length === 0) return "-";

        const approved = gates.filter((g) => g.status === "APPROVED").length;
        const total = gates.length;
        const progress = (approved / total) * 100;

        return (
          <Tooltip title={`${approved}/${total} gates approved`}>
            <Box sx={{ width: "100%" }}>
              <LinearProgress
                variant="determinate"
                value={progress}
                sx={{ height: 8, borderRadius: 1 }}
              />
            </Box>
          </Tooltip>
        );
      },
    },
    {
      field: "createdAt",
      headerName: "Created",
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
      width: 80,
      getActions: (params) => [
        <GridActionsCellItem
          key="view"
          icon={<ViewIcon />}
          label="View"
          onClick={() => onRowClick(params.row.id || "")}
          showInMenu={false}
        />,
      ],
    },
  ];

  // Create rows with unique id for DataGrid (requests already have id field)
  const rows = requests;

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
          "& .MuiDataGrid-row": {
            cursor: "pointer",
          },
        }}
      />
    </Box>
  );
}
