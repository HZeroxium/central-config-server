import React, { useMemo } from "react";
import {
  DataGrid,
  type GridColDef,
  type GridRenderCellParams,
} from "@mui/x-data-grid";
import { Box, Typography, Chip } from "@mui/material";
import { useNavigate } from "react-router-dom";
import type { DriftEventResponse } from "@lib/api/models";

interface ServiceDriftSummary {
  serviceId: string;
  serviceName: string;
  total: number;
  critical: number;
  high: number;
  medium: number;
  low: number;
}

interface TopServicesDriftTableProps {
  driftEvents: DriftEventResponse[];
  loading?: boolean;
  maxRows?: number;
}

export const TopServicesDriftTable: React.FC<TopServicesDriftTableProps> = ({
  driftEvents,
  loading = false,
  maxRows = 10,
}) => {
  const navigate = useNavigate();

  const serviceSummaries = useMemo((): ServiceDriftSummary[] => {
    const summaryMap = new Map<string, ServiceDriftSummary>();

    for (const event of driftEvents) {
      const serviceName = event.serviceName || "Unknown";
      const serviceId = event.serviceName || "unknown";

      if (!summaryMap.has(serviceId)) {
        summaryMap.set(serviceId, {
          serviceId,
          serviceName,
          total: 0,
          critical: 0,
          high: 0,
          medium: 0,
          low: 0,
        });
      }

      const summary = summaryMap.get(serviceId)!;
      summary.total += 1;

      const severity = (event.severity || "LOW").toUpperCase();
      switch (severity) {
        case "CRITICAL":
          summary.critical += 1;
          break;
        case "HIGH":
          summary.high += 1;
          break;
        case "MEDIUM":
          summary.medium += 1;
          break;
        case "LOW":
          summary.low += 1;
          break;
      }
    }

    return Array.from(summaryMap.values())
      .sort((a, b) => b.total - a.total)
      .slice(0, maxRows);
  }, [driftEvents, maxRows]);

  const columns: GridColDef<ServiceDriftSummary>[] = [
    {
      field: "serviceName",
      headerName: "Service Name",
      flex: 1,
      minWidth: 200,
      renderCell: (params: GridRenderCellParams<ServiceDriftSummary>) => (
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
      field: "total",
      headerName: "Total",
      width: 100,
      align: "center",
      headerAlign: "center",
      renderCell: (params: GridRenderCellParams<ServiceDriftSummary>) => (
        <Typography variant="body2" fontWeight={600}>
          {params.value}
        </Typography>
      ),
    },
    {
      field: "critical",
      headerName: "Critical",
      width: 100,
      align: "center",
      headerAlign: "center",
      renderCell: (params: GridRenderCellParams<ServiceDriftSummary>) => (
        <Chip
          label={params.value}
          color="error"
          size="small"
          sx={{ fontWeight: 600 }}
        />
      ),
    },
    {
      field: "high",
      headerName: "High",
      width: 100,
      align: "center",
      headerAlign: "center",
      renderCell: (params: GridRenderCellParams<ServiceDriftSummary>) => (
        <Chip
          label={params.value}
          color="warning"
          size="small"
          sx={{ fontWeight: 600 }}
        />
      ),
    },
    {
      field: "medium",
      headerName: "Medium",
      width: 100,
      align: "center",
      headerAlign: "center",
      renderCell: (params: GridRenderCellParams<ServiceDriftSummary>) => (
        <Chip
          label={params.value}
          color="info"
          size="small"
          sx={{ fontWeight: 600 }}
        />
      ),
    },
    {
      field: "low",
      headerName: "Low",
      width: 100,
      align: "center",
      headerAlign: "center",
      renderCell: (params: GridRenderCellParams<ServiceDriftSummary>) => (
        <Chip
          label={params.value}
          color="default"
          size="small"
          sx={{ fontWeight: 600 }}
        />
      ),
    },
  ];

  const rows = serviceSummaries.map((summary, index) => ({
    ...summary,
    id: summary.serviceId || `service-${index}`,
  }));

  return (
    <Box sx={{ height: 400, width: "100%" }}>
      <DataGrid
        rows={rows}
        columns={columns}
        loading={loading}
        pageSizeOptions={[5, 10]}
        initialState={{
          pagination: {
            paginationModel: { page: 0, pageSize: 5 },
          },
        }}
        disableRowSelectionOnClick
        onRowClick={(params) => {
          // Navigate to service detail or drift events filtered by service
          const serviceName = params.row.serviceName;
          if (serviceName && serviceName !== "Unknown") {
            navigate(`/drift-events?serviceName=${encodeURIComponent(serviceName)}`);
          }
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
                  No drift events found
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  No services have drift events in the selected time range.
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
};

