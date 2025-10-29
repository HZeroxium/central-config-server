import {
  DataGrid,
  type GridColDef,
  GridActionsCellItem,
} from "@mui/x-data-grid";
import { Visibility as ViewIcon } from "@mui/icons-material";
import { Box, Chip, Typography } from "@mui/material";
import type { ApprovalDecisionResponse } from "@lib/api/models";
import { format } from "date-fns";
import { UserInfoDisplay } from "@components/common/UserInfoDisplay";

interface ApprovalDecisionTableProps {
  readonly decisions: ApprovalDecisionResponse[];
  readonly loading: boolean;
  readonly page: number;
  readonly pageSize: number;
  readonly totalElements: number;
  readonly onPageChange: (page: number) => void;
  readonly onPageSizeChange: (pageSize: number) => void;
  readonly onRowClick: (decisionId: string) => void;
  readonly onRequestClick?: (requestId: string) => void;
}

export function ApprovalDecisionTable({
  decisions,
  loading,
  page,
  pageSize,
  totalElements,
  onPageChange,
  onPageSizeChange,
  onRowClick,
  onRequestClick,
}: ApprovalDecisionTableProps) {
  const getDecisionColor = (
    decision?: string
  ): "success" | "error" | "default" => {
    switch (decision?.toUpperCase()) {
      case "APPROVE":
        return "success";
      case "REJECT":
        return "error";
      default:
        return "default";
    }
  };

  const getGateColor = (gate?: string): "primary" | "secondary" | "default" => {
    switch (gate) {
      case "SYS_ADMIN":
        return "primary";
      case "LINE_MANAGER":
        return "secondary";
      default:
        return "default";
    }
  };

  const columns: GridColDef<ApprovalDecisionResponse>[] = [
    {
      field: "id",
      headerName: "Decision ID",
      flex: 1,
      minWidth: 200,
      renderCell: (params) => (
        <Box sx={{ fontFamily: "monospace", fontSize: "0.875rem" }}>
          {params.value || "N/A"}
        </Box>
      ),
    },
    {
      field: "requestId",
      headerName: "Request ID",
      flex: 1,
      minWidth: 200,
      renderCell: (params) => (
        <Box
          sx={{
            fontFamily: "monospace",
            fontSize: "0.875rem",
            color: onRequestClick ? "primary.main" : "inherit",
            cursor: onRequestClick ? "pointer" : "default",
            textDecoration: onRequestClick ? "underline" : "none",
          }}
          onClick={(e) => {
            if (onRequestClick && params.value) {
              e.stopPropagation();
              onRequestClick(params.value);
            }
          }}
        >
          {params.value || "N/A"}
        </Box>
      ),
    },
    {
      field: "approverUserId",
      headerName: "Approver",
      flex: 1,
      minWidth: 200,
      renderCell: (params) => {
        const userId = params.value as string | undefined;
        if (!userId) {
          return <Typography variant="body2">Unknown</Typography>;
        }
        return <UserInfoDisplay userId={userId} mode="compact" />;
      },
    },
    {
      field: "gate",
      headerName: "Gate",
      width: 150,
      renderCell: (params) => (
        <Chip
          label={params.value || "N/A"}
          size="small"
          color={getGateColor(params.value as string)}
          variant="outlined"
        />
      ),
    },
    {
      field: "decision",
      headerName: "Decision",
      width: 120,
      renderCell: (params) => (
        <Chip
          label={params.value || "Unknown"}
          size="small"
          color={getDecisionColor(params.value as string)}
        />
      ),
    },
    {
      field: "decidedAt",
      headerName: "Decision Date",
      width: 180,
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
      field: "note",
      headerName: "Notes",
      flex: 1,
      minWidth: 200,
      renderCell: (params) => (
        <Box
          sx={{
            overflow: "hidden",
            textOverflow: "ellipsis",
            whiteSpace: "nowrap",
          }}
          title={params.value as string}
        >
          {params.value || "-"}
        </Box>
      ),
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
          label="View details"
          onClick={() => {
            if (params.row.id) {
              onRowClick(params.row.id);
            }
          }}
          showInMenu={false}
        />,
      ],
    },
  ];

  return (
    <Box sx={{ height: 600, width: "100%" }}>
      <DataGrid
        rows={decisions}
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
        getRowId={(row) =>
          row.id ||
          `decision-${row.requestId}-${row.approverUserId}-${row.decidedAt}`
        }
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
                  No approval decisions found
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
