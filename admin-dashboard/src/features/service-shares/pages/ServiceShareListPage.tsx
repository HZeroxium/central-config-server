import { useState, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import {
  Box,
  Button,
  Card,
  CardContent,
  TextField,
  InputAdornment,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Alert,
  Tooltip,
} from "@mui/material";
import Grid from "@mui/material/Grid";
import {
  Search as SearchIcon,
  Refresh as RefreshIcon,
  Add as AddIcon,
} from "@mui/icons-material";
import PageHeader from "@components/common/PageHeader";
import { TableSkeleton } from "@components/common/skeletons";
import ConfirmDialog from "@components/common/ConfirmDialog";
import { useFindAllServiceShares, useRevokeServiceShare } from "@lib/api/hooks";
import { getFindAllServiceSharesQueryKey } from "@lib/api/generated/service-shares/service-shares";
import { useAuth } from "@features/auth/context";
import { toast } from "@lib/toast/toast";
import { handleApiError } from "@lib/api/errorHandler";
import { ServiceShareTable } from "../components/ServiceShareTable";
import { ShareFormDrawer } from "../components/ShareFormDrawer";
import type { ServiceShareResponse } from "@lib/api/models";
import { useDebounce } from "@hooks/useDebounce";

export default function ServiceShareListPage() {
  const navigate = useNavigate();
  const { isSysAdmin } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const queryClient = useQueryClient();

  // Parse initial state from URL params
  const initialPage = parseInt(searchParams.get("page") || "0", 10);
  const initialPageSize = parseInt(searchParams.get("size") || "20", 10);

  const [page, setPage] = useState(initialPage);
  const [pageSize, setPageSize] = useState(initialPageSize);
  const [search, setSearch] = useState(searchParams.get("search") || "");
  const [grantToTypeFilter, setGrantToTypeFilter] = useState<
    "TEAM" | "USER" | ""
  >((searchParams.get("grantToType") as "TEAM" | "USER" | null) || "");
  const [formDrawerOpen, setFormDrawerOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [selectedShareId, setSelectedShareId] = useState<string | null>(null);

  // Debounce search input
  const debouncedSearch = useDebounce(search, 400);

  // Sync URL params when filters change
  useEffect(() => {
    const params = new URLSearchParams();
    if (debouncedSearch) params.set("search", debouncedSearch);
    if (grantToTypeFilter) params.set("grantToType", grantToTypeFilter);
    if (page > 0) params.set("page", page.toString());
    if (pageSize !== 20) params.set("size", pageSize.toString());
    setSearchParams(params, { replace: true });
  }, [debouncedSearch, grantToTypeFilter, page, pageSize, setSearchParams]);

  const { data, isLoading, error, refetch } = useFindAllServiceShares(
    {
      serviceId: debouncedSearch || undefined,
      grantToType: grantToTypeFilter || undefined,
      page,
      size: pageSize,
    },
    {
      query: {
        staleTime: 30_000,
      },
    }
  );

  const revokeShareMutation = useRevokeServiceShare();

  const shares: ServiceShareResponse[] = Array.isArray(data)
    ? data
    : data?.items || [];
  const metadata = data && "metadata" in data ? data.metadata : undefined;

  const handleRevokeShare = async () => {
    if (!selectedShareId) return;

    revokeShareMutation.mutate(
      { id: selectedShareId },
      {
        onSuccess: () => {
          toast.success("Service share revoked successfully");
          setDeleteDialogOpen(false);
          setSelectedShareId(null);
          // Invalidate list query to refresh data
          queryClient.invalidateQueries({
            queryKey: getFindAllServiceSharesQueryKey({
              serviceId: debouncedSearch || undefined,
              grantToType: grantToTypeFilter || undefined,
              page,
              size: pageSize,
            }),
          });
        },
        onError: (error) => {
          handleApiError(error);
        },
      }
    );
  };

  const handleOpenRevokeDialog = (shareId: string) => {
    setSelectedShareId(shareId);
    setDeleteDialogOpen(true);
  };

  const handleCloseRevokeDialog = () => {
    setDeleteDialogOpen(false);
    setSelectedShareId(null);
  };

  const handleFilterReset = () => {
    setSearch("");
    setGrantToTypeFilter("");
    setPage(0);
    setSearchParams({}, { replace: true });
  };

  const handleShareSuccess = () => {
    toast.success("Service share created successfully");
    setFormDrawerOpen(false);
    // Invalidate list query to refresh data
    queryClient.invalidateQueries({
      queryKey: getFindAllServiceSharesQueryKey({
        serviceId: debouncedSearch || undefined,
        grantToType: grantToTypeFilter || undefined,
        page,
        size: pageSize,
      }),
    });
  };

  return (
    <Box>
      <PageHeader
        title="Service Shares"
        subtitle="Manage service access shares"
        actions={
          <>
            <Tooltip title="Refresh service shares list" placement="bottom">
              <Button
                variant="outlined"
                startIcon={<RefreshIcon />}
                onClick={() => refetch()}
              >
                Refresh
              </Button>
            </Tooltip>
            {isSysAdmin && (
              <Tooltip title="Create a new service share" placement="bottom">
                <Button
                  variant="contained"
                  startIcon={<AddIcon />}
                  onClick={() => setFormDrawerOpen(true)}
                >
                  Create Share
                </Button>
              </Tooltip>
            )}
          </>
        }
      />

      <Card sx={{ boxShadow: 2 }}>
        <CardContent sx={{ p: 3 }}>
          {/* Filters */}
          <Grid container spacing={2} sx={{ mb: 3 }}>
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField
                fullWidth
                label="Search by Service ID"
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
                    "aria-label": "Search by service ID",
                  },
                }}
              />
            </Grid>

            <Grid size={{ xs: 12, md: 3 }}>
              <FormControl fullWidth>
                <InputLabel>Grant To Type</InputLabel>
                <Select
                  value={grantToTypeFilter}
                  label="Grant To Type"
                  onChange={(e) => {
                    setGrantToTypeFilter(e.target.value);
                    setPage(0);
                  }}
                >
                  <MenuItem value="">All</MenuItem>
                  <MenuItem value="TEAM">Team</MenuItem>
                  <MenuItem value="USER">User</MenuItem>
                </Select>
              </FormControl>
            </Grid>

            <Grid size={{ xs: 12, md: 5 }}>
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
              Failed to load service shares:{" "}
              {error instanceof Error ? error.message : "Unknown error"}
            </Alert>
          )}

          {isLoading && <TableSkeleton rows={10} columns={5} />}

          {!isLoading && !error && (
            <ServiceShareTable
              shares={shares}
              loading={isLoading}
              page={page}
              pageSize={pageSize}
              totalElements={metadata?.totalElements || shares.length}
              onPageChange={(newPage: number) => setPage(newPage)}
              onPageSizeChange={(newPageSize: number) => {
                setPageSize(newPageSize);
                setPage(0);
              }}
              onRowClick={(shareId: string) =>
                navigate(`/service-shares/${shareId}`)
              }
              onRevoke={handleOpenRevokeDialog}
            />
          )}
        </CardContent>
      </Card>

      {/* Create Share Drawer */}
      <ShareFormDrawer
        open={formDrawerOpen}
        onClose={() => setFormDrawerOpen(false)}
        onSuccess={handleShareSuccess}
      />

      {/* Revoke Confirmation Dialog */}
      <ConfirmDialog
        open={deleteDialogOpen}
        title="Revoke Service Share"
        message="Are you sure you want to revoke this service share? The user/team will lose access immediately."
        confirmText="Revoke"
        cancelText="Cancel"
        onConfirm={handleRevokeShare}
        onCancel={handleCloseRevokeDialog}
        loading={revokeShareMutation.isPending}
      />
    </Box>
  );
}
