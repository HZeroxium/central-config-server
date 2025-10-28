import {
  DataGrid,
  type GridColDef,
  GridActionsCellItem,
} from "@mui/x-data-grid";
import {
  Visibility as ViewIcon,
  Cancel as RevokeIcon,
} from "@mui/icons-material";
import { Box, Chip } from "@mui/material";
import { format } from "date-fns";
import type { ServiceShareResponse } from "@/lib/api/models";

interface ServiceShareTableProps {
  shares: ServiceShareResponse[];
  loading: boolean;
  page: number;
  pageSize: number;
  totalElements: number;
  onPageChange: (page: number) => void;
  onPageSizeChange: (pageSize: number) => void;
  onRowClick: (shareId: string) => void;
  onRevoke: (shareId: string) => void;
}

export function ServiceShareTable({
  shares,
  loading,
  page,
  pageSize,
  totalElements,
  onPageChange,
  onPageSizeChange,
  onRowClick,
  onRevoke,
}: ServiceShareTableProps) {
  const columns: GridColDef[] = [
    {
      field: "serviceId",
      headerName: "Service ID",
      flex: 1,
      minWidth: 150,
      renderCell: (params) => (
        <Box sx={{ fontFamily: "monospace", fontWeight: 600 }}>
          {params.value}
        </Box>
      ),
    },
    {
      field: "grantToType",
      headerName: "Grant To Type",
      width: 120,
      renderCell: (params) => (
        <Chip
          label={params.value}
          size="small"
          color={params.value === "TEAM" ? "primary" : "default"}
        />
      ),
    },
    {
      field: "grantToId",
      headerName: "Grant To",
      flex: 1,
      minWidth: 150,
    },
    {
      field: "permissions",
      headerName: "Permissions",
      flex: 1,
      minWidth: 200,
      renderCell: (params) => {
        const perms = params.value as string[] | undefined;
        if (!perms || perms.length === 0) return "-";
        return (
          <Box sx={{ display: "flex", gap: 0.5, flexWrap: "wrap" }}>
            {perms.slice(0, 2).map((perm) => (
              <Chip key={perm} label={perm} size="small" variant="outlined" />
            ))}
            {perms.length > 2 && (
              <Chip
                label={`+${perms.length - 2}`}
                size="small"
                variant="outlined"
              />
            )}
          </Box>
        );
      },
    },
    {
      field: "environments",
      headerName: "Environments",
      flex: 1,
      minWidth: 150,
      renderCell: (params) => {
        const envs = params.value as string[] | undefined;
        if (!envs || envs.length === 0) return "All";
        return (
          <Box sx={{ display: "flex", gap: 0.5, flexWrap: "wrap" }}>
            {envs.slice(0, 2).map((env) => (
              <Chip
                key={env}
                label={env.toUpperCase()}
                size="small"
                variant="filled"
              />
            ))}
            {envs.length > 2 && (
              <Chip label={`+${envs.length - 2}`} size="small" />
            )}
          </Box>
        );
      },
    },
    {
      field: "createdAt",
      headerName: "Created At",
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
      getActions: (params) => [
        <GridActionsCellItem
          key="view"
          icon={<ViewIcon />}
          label="View details"
          onClick={() => onRowClick(params.row.id || "")}
          showInMenu={false}
        />,
        <GridActionsCellItem
          key="revoke"
          icon={<RevokeIcon color="error" />}
          label="Revoke this share"
          onClick={() => onRevoke(params.row.id || "")}
          showInMenu={false}
        />,
      ],
    },
  ];

  // Create rows with unique id for DataGrid
  const rows = shares.map((share) => ({
    ...share,
    id: share.id || Math.random().toString(),
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
