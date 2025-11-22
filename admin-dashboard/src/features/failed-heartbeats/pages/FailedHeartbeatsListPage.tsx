import { useState } from "react";
import { useSearchParams } from "react-router-dom";
import {
  Box,
  Card,
  CardContent,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Button,
  Alert,
} from "@mui/material";
import Grid from "@mui/material/Grid";
import { Refresh as RefreshIcon } from "@mui/icons-material";
import PageHeader from "@components/common/PageHeader";
import { TableSkeleton } from "@components/common/skeletons";
import { FailedHeartbeatsTable } from "../components/FailedHeartbeatsTable";
import { FailedHeartbeatDetailDialog } from "../components/FailedHeartbeatDetailDialog";
import { RedriveDialog } from "../components/RedriveDialog";
import { UpdateStatusDialog } from "../components/UpdateStatusDialog";
import {
  useFailedHeartbeatsList,
  useFailedHeartbeatDetail,
  useFailedHeartbeatOperations,
} from "../hooks/useFailedHeartbeats";
import type { FindAllFailedHeartbeatsParams, FindAllFailedHeartbeatsStatus } from "@lib/api/models";
import type { UpdateStatusRequestStatus } from "@lib/api/models";

export default function FailedHeartbeatsListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [page, setPage] = useState(
    parseInt(searchParams.get("page") || "0", 10)
  );
  const [pageSize, setPageSize] = useState(
    parseInt(searchParams.get("size") || "20", 10)
  );
  const [statusFilter, setStatusFilter] = useState<string>(
    searchParams.get("status") || ""
  );
  const [serviceFilter, setServiceFilter] = useState<string>(
    searchParams.get("service") || ""
  );

  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [showDetailDialog, setShowDetailDialog] = useState(false);
  const [showRedriveDialog, setShowRedriveDialog] = useState(false);
  const [showUpdateStatusDialog, setShowUpdateStatusDialog] = useState(false);

  const params: FindAllFailedHeartbeatsParams = {
    page,
    size: pageSize,
    status: statusFilter as FindAllFailedHeartbeatsStatus | undefined,
    serviceName: serviceFilter || undefined,
  };

  const {
    failedHeartbeats,
    metadata,
    isLoading,
    error,
    refetch,
  } = useFailedHeartbeatsList(params);

  const {
    failedHeartbeat: selectedHeartbeat,
    isLoading: _detailLoading,
  } = useFailedHeartbeatDetail(selectedId || "");

  const {
    redrive,
    updateStatus,
    isRedriving,
    isUpdating,
  } = useFailedHeartbeatOperations();

  const handleView = (id: string) => {
    setSelectedId(id);
    setShowDetailDialog(true);
  };

  const handleRedrive = (id: string) => {
    setSelectedId(id);
    setShowRedriveDialog(true);
  };

  const handleUpdateStatus = (id: string) => {
    setSelectedId(id);
    setShowUpdateStatusDialog(true);
  };

  const handleConfirmRedrive = (force: boolean) => {
    if (selectedId) {
      redrive(selectedId, force);
      setShowRedriveDialog(false);
      setSelectedId(null);
    }
  };

  const handleConfirmUpdateStatus = (
    status: UpdateStatusRequestStatus,
    notes?: string
  ) => {
    if (selectedId) {
      updateStatus(selectedId, status, notes);
      setShowUpdateStatusDialog(false);
      setSelectedId(null);
    }
  };

  const handleFilterReset = () => {
    setStatusFilter("");
    setServiceFilter("");
    setPage(0);
    setSearchParams({});
  };

  return (
    <Box>
      <PageHeader
        title="Failed Heartbeats"
        subtitle="Monitor and manage failed heartbeat processing"
        actions={
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={() => refetch()}
          >
            Refresh
          </Button>
        }
      />

      <Card>
        <CardContent>
          <Grid container spacing={2} sx={{ mb: 3 }}>
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField
                fullWidth
                label="Filter by Service"
                value={serviceFilter}
                onChange={(e) => {
                  setServiceFilter(e.target.value);
                  setPage(0);
                }}
                size="small"
              />
            </Grid>

            <Grid size={{ xs: 12, md: 4 }}>
              <FormControl fullWidth size="small">
                <InputLabel>Status</InputLabel>
                <Select
                  value={statusFilter}
                  onChange={(e) => {
                    setStatusFilter(e.target.value);
                    setPage(0);
                  }}
                  label="Status"
                >
                  <MenuItem value="">All</MenuItem>
                  <MenuItem value="INVESTIGATING">Investigating</MenuItem>
                  <MenuItem value="RESOLVED">Resolved</MenuItem>
                  <MenuItem value="IGNORED">Ignored</MenuItem>
                </Select>
              </FormControl>
            </Grid>

            <Grid size={{ xs: 12, md: 2 }}>
              <Button
                fullWidth
                variant="outlined"
                onClick={handleFilterReset}
                sx={{ height: "40px" }}
              >
                Reset
              </Button>
            </Grid>
          </Grid>

          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              Failed to load failed heartbeats:{" "}
              {error.detail || "Unknown error"}
            </Alert>
          )}

          {isLoading && <TableSkeleton rows={10} columns={7} />}

          {!isLoading && !error && (
            <FailedHeartbeatsTable
              failedHeartbeats={failedHeartbeats}
              loading={isLoading}
              page={page}
              pageSize={pageSize}
              totalElements={metadata?.totalElements || 0}
              onPageChange={(newPage) => {
                setPage(newPage);
                setSearchParams((prev) => {
                  const newParams = new URLSearchParams(prev);
                  newParams.set("page", newPage.toString());
                  return newParams;
                });
              }}
              onPageSizeChange={(newSize) => {
                setPageSize(newSize);
                setPage(0);
                setSearchParams((prev) => {
                  const newParams = new URLSearchParams(prev);
                  newParams.set("size", newSize.toString());
                  newParams.set("page", "0");
                  return newParams;
                });
              }}
              onView={handleView}
              onRedrive={handleRedrive}
              onUpdateStatus={handleUpdateStatus}
            />
          )}
        </CardContent>
      </Card>

      <FailedHeartbeatDetailDialog
        open={showDetailDialog}
        onClose={() => {
          setShowDetailDialog(false);
          setSelectedId(null);
        }}
        failedHeartbeat={selectedHeartbeat || null}
        onRedrive={() => {
          if (selectedId) {
            setShowDetailDialog(false);
            handleRedrive(selectedId);
          }
        }}
        onUpdateStatus={() => {
          if (selectedId) {
            setShowDetailDialog(false);
            handleUpdateStatus(selectedId);
          }
        }}
        isRedriving={isRedriving}
      />

      <RedriveDialog
        open={showRedriveDialog}
        onClose={() => {
          setShowRedriveDialog(false);
          setSelectedId(null);
        }}
        onConfirm={handleConfirmRedrive}
        loading={isRedriving}
        serviceName={selectedHeartbeat?.serviceName}
        instanceId={selectedHeartbeat?.instanceId}
      />

      <UpdateStatusDialog
        open={showUpdateStatusDialog}
        onClose={() => {
          setShowUpdateStatusDialog(false);
          setSelectedId(null);
        }}
        onConfirm={handleConfirmUpdateStatus}
        loading={isUpdating}
        currentStatus={selectedHeartbeat?.status}
      />
    </Box>
  );
}

