import { useState, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import {
  Box,
  Button,
  Card,
  CardContent,
  TextField,
  InputAdornment,
  Alert,
} from "@mui/material";
import Grid from "@mui/material/Grid";
import {
  Search as SearchIcon,
  Refresh as RefreshIcon,
} from "@mui/icons-material";
import PageHeader from "@components/common/PageHeader";
import { TableSkeleton } from "@components/common/skeletons";
import { useFindAllIamTeams } from "@lib/api/hooks";
import { useAuth } from "@lib/keycloak/useAuth";
import { IamTeamTable } from "../components/IamTeamTable";
import { useDebounce } from "@hooks/useDebounce";

export default function IamTeamListPage() {
  const navigate = useNavigate();
  const { isSysAdmin } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();

  // Parse initial state from URL params
  const initialPage = parseInt(searchParams.get("page") || "0", 10);
  const initialPageSize = parseInt(searchParams.get("size") || "20", 10);

  const [page, setPage] = useState(initialPage);
  const [pageSize, setPageSize] = useState(initialPageSize);
  const [search, setSearch] = useState(searchParams.get("displayName") || "");

  // Debounce search input
  const debouncedSearch = useDebounce(search, 400);

  // Sync URL params when filters change
  useEffect(() => {
    const params = new URLSearchParams();
    if (debouncedSearch) params.set("displayName", debouncedSearch);
    if (page > 0) params.set("page", page.toString());
    if (pageSize !== 20) params.set("size", pageSize.toString());
    setSearchParams(params, { replace: true });
  }, [debouncedSearch, page, pageSize, setSearchParams]);

  // Always call hooks, but control with enabled option
  const { data, isLoading, error, refetch } = useFindAllIamTeams(
    {
      displayName: debouncedSearch || undefined,
      page,
      size: pageSize,
    },
    {
      query: {
        enabled: isSysAdmin,
        staleTime: 30_000,
      },
    }
  );

  // Only SYS_ADMIN can access IAM pages
  if (!isSysAdmin) {
    return (
      <Box>
        <PageHeader title="Access Denied" />
        <Alert severity="error" sx={{ m: 3 }}>
          You do not have permission to access this page. This feature is
          restricted to system administrators.
        </Alert>
      </Box>
    );
  }

  const teams = data?.items || [];
  const metadata = data?.metadata;

  const handleFilterReset = () => {
    setSearch("");
    setPage(0);
    setSearchParams({}, { replace: true });
  };

  return (
    <Box>
      <PageHeader
        title="IAM Teams"
        subtitle="Manage teams and their members"
        actions={
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={() => refetch()}
            aria-label="Refresh IAM teams"
          >
            Refresh
          </Button>
        }
      />

      <Card>
        <CardContent>
          {/* Filters */}
          <Grid container spacing={2} sx={{ mb: 3 }}>
            <Grid size={{ xs: 12, md: 6 }}>
              <TextField
                fullWidth
                label="Search by Team Name"
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
                    "aria-label": "Search by team name",
                  },
                }}
              />
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Button
                fullWidth
                variant="outlined"
                onClick={handleFilterReset}
                sx={{ height: "56px" }}
                aria-label="Reset all filters"
              >
                Reset Filters
              </Button>
            </Grid>
          </Grid>

          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              Failed to load teams:{" "}
              {(error as Error).message || "Unknown error"}
            </Alert>
          )}

          {isLoading && <TableSkeleton rows={10} columns={4} />}

          {!isLoading && !error && (
            <IamTeamTable
              teams={teams}
              loading={isLoading}
              page={page}
              pageSize={pageSize}
              totalElements={metadata?.totalElements || 0}
              onPageChange={(newPage: number) => setPage(newPage)}
              onPageSizeChange={(newPageSize: number) => {
                setPageSize(newPageSize);
                setPage(0);
              }}
              onRowClick={(teamId: string) => navigate(`/iam/teams/${teamId}`)}
            />
          )}
        </CardContent>
      </Card>
    </Box>
  );
}
