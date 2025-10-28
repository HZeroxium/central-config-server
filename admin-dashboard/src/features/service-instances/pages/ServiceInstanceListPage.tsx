import { useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  Box,
  Card,
  CardContent,
  TextField,
  InputAdornment,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Button,
  Alert,
} from "@mui/material";
import Grid from "@mui/material/Grid";
import {
  Search as SearchIcon,
  Refresh as RefreshIcon,
} from "@mui/icons-material";
import PageHeader from "@components/common/PageHeader";
import ConfirmDialog from "@components/common/ConfirmDialog";
import Loading from "@components/common/Loading";
import {
  useFindAllServiceInstances,
  useDeleteServiceInstance,
} from "@lib/api/hooks";
import { ServiceInstanceTable } from "../components/ServiceInstanceTable";
import type {
  FindAllServiceInstancesStatus,
  FindAllServiceInstancesEnvironment,
} from "@lib/api/models";
import { toast } from "@lib/toast/toast";
import { handleApiError } from "@lib/api/errorHandler";

export default function ServiceInstanceListPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [search, setSearch] = useState("");
  const [environmentFilter, setEnvironmentFilter] = useState<
    FindAllServiceInstancesEnvironment | ""
  >("");
  const [statusFilter, setStatusFilter] = useState<
    FindAllServiceInstancesStatus | ""
  >("");
  const [driftFilter, setDriftFilter] = useState<"true" | "false" | "">("");
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [selectedInstanceId, setSelectedInstanceId] = useState<string | null>(
    null
  );

  const { data, isLoading, error, refetch } = useFindAllServiceInstances(
    {
      serviceId: search || undefined,
      status: statusFilter || undefined,
      environment: environmentFilter || undefined,
      hasDrift: driftFilter || undefined,
      page,
      size: pageSize,
    },
    {
      query: {
        staleTime: 30_000,
        refetchInterval: 30000, // 30 seconds
        refetchIntervalInBackground: false,
      },
    }
  );

  const deleteMutation = useDeleteServiceInstance();

  const instances = data?.items || [];
  const metadata = data?.metadata;

  const handleDeleteInstance = async () => {
    if (!selectedInstanceId) return;

    deleteMutation.mutate(
      { instanceId: selectedInstanceId },
      {
        onSuccess: () => {
          toast.success("Instance deleted successfully");
          setDeleteDialogOpen(false);
          setSelectedInstanceId(null);
          refetch();
        },
        onError: (error) => {
          handleApiError(error);
        },
      }
    );
  };

  const handleOpenDeleteDialog = (instanceId: string) => {
    setSelectedInstanceId(instanceId);
    setDeleteDialogOpen(true);
  };

  const handleCloseDeleteDialog = () => {
    setDeleteDialogOpen(false);
    setSelectedInstanceId(null);
  };

  const handleFilterReset = () => {
    setSearch("");
    setEnvironmentFilter("");
    setStatusFilter("");
    setDriftFilter("");
    setPage(0);
  };

  return (
    <Box>
      <PageHeader
        title="Service Instances"
        subtitle="Manage service instances tracked by the system"
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
          {/* Filters */}
          <Grid container spacing={2} sx={{ mb: 3 }}>
            <Grid size={{ xs: 12, md: 3 }}>
              <TextField
                fullWidth
                label="Search by Service Name"
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value);
                  setPage(0);
                }}
                slotProps={{
                  input: {
                    startAdornment: (
                      <InputAdornment position="start">
                        <SearchIcon />
                      </InputAdornment>
                    ),
                  },
                }}
              />
            </Grid>

            <Grid size={{ xs: 12, md: 2 }}>
              <FormControl fullWidth>
                <InputLabel>Environment</InputLabel>
                <Select
                  value={environmentFilter}
                  label="Environment"
                  onChange={(e) => {
                    setEnvironmentFilter(
                      e.target.value as FindAllServiceInstancesEnvironment | ""
                    );
                    setPage(0);
                  }}
                >
                  <MenuItem value="">All</MenuItem>
                  <MenuItem value="dev">Development</MenuItem>
                  <MenuItem value="staging">Staging</MenuItem>
                  <MenuItem value="prod">Production</MenuItem>
                </Select>
              </FormControl>
            </Grid>

            <Grid size={{ xs: 12, md: 2 }}>
              <FormControl fullWidth>
                <InputLabel>Status</InputLabel>
                <Select
                  value={statusFilter}
                  label="Status"
                  onChange={(e) => {
                    setStatusFilter(
                      e.target.value as FindAllServiceInstancesStatus | ""
                    );
                    setPage(0);
                  }}
                >
                  <MenuItem value="">All</MenuItem>
                  <MenuItem value="HEALTHY">Healthy</MenuItem>
                  <MenuItem value="UNHEALTHY">Unhealthy</MenuItem>
                  <MenuItem value="DRIFT">Drift</MenuItem>
                  <MenuItem value="UNKNOWN">Unknown</MenuItem>
                </Select>
              </FormControl>
            </Grid>

            <Grid size={{ xs: 12, md: 2 }}>
              <FormControl fullWidth>
                <InputLabel>Drift Status</InputLabel>
                <Select
                  value={driftFilter}
                  label="Drift Status"
                  onChange={(e) => {
                    setDriftFilter(e.target.value as "true" | "false" | "");
                    setPage(0);
                  }}
                >
                  <MenuItem value="">All</MenuItem>
                  <MenuItem value="true">Has Drift</MenuItem>
                  <MenuItem value="false">No Drift</MenuItem>
                </Select>
              </FormControl>
            </Grid>

            <Grid size={{ xs: 12, md: 3 }}>
              <Button
                fullWidth
                variant="outlined"
                onClick={handleFilterReset}
                sx={{ height: "56px" }}
              >
                Reset Filters
              </Button>
            </Grid>
          </Grid>

          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              Failed to load service instances:{" "}
              {error instanceof Error ? error.message : "Unknown error"}
            </Alert>
          )}

          {isLoading && <Loading />}

          {!isLoading && !error && (
            <ServiceInstanceTable
              instances={instances}
              loading={isLoading}
              page={page}
              pageSize={pageSize}
              totalElements={metadata?.totalElements || 0}
              onPageChange={(newPage: number) => setPage(newPage)}
              onPageSizeChange={(newPageSize: number) => {
                setPageSize(newPageSize);
                setPage(0);
              }}
              onRowClick={(instanceId: string) => {
                const instance = instances.find(
                  (i) => i.instanceId === instanceId
                );
                if (instance) {
                  navigate(
                    `/service-instances/${instance.serviceName}/${instance.instanceId}`
                  );
                }
              }}
              onDelete={handleOpenDeleteDialog}
            />
          )}
        </CardContent>
      </Card>

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        open={deleteDialogOpen}
        title="Delete Service Instance"
        message={`Are you sure you want to delete this instance? This action cannot be undone.`}
        confirmText="Delete"
        cancelText="Cancel"
        onConfirm={handleDeleteInstance}
        onCancel={handleCloseDeleteDialog}
        loading={deleteMutation.isPending}
      />
    </Box>
  );
}
