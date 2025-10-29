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
import { useFindAllIamUsers } from "@lib/api/hooks";
import { useAuth } from "@lib/keycloak/useAuth";
import { IamUserTable } from "../components/IamUserTable";
import { useDebounce } from "@hooks/useDebounce";

export default function IamUserListPage() {
  const navigate = useNavigate();
  const { isSysAdmin } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();

  // Parse initial state from URL params
  const initialPage = parseInt(searchParams.get("page") || "0", 10);
  const initialPageSize = parseInt(searchParams.get("size") || "20", 10);

  const [page, setPage] = useState(initialPage);
  const [pageSize, setPageSize] = useState(initialPageSize);
  const [search, setSearch] = useState(searchParams.get("username") || "");
  const [emailSearch, setEmailSearch] = useState(
    searchParams.get("email") || ""
  );

  // Debounce search inputs
  const debouncedSearch = useDebounce(search, 400);
  const debouncedEmailSearch = useDebounce(emailSearch, 400);

  // Sync URL params when filters change
  useEffect(() => {
    const params = new URLSearchParams();
    if (debouncedSearch) params.set("username", debouncedSearch);
    if (debouncedEmailSearch) params.set("email", debouncedEmailSearch);
    if (page > 0) params.set("page", page.toString());
    if (pageSize !== 20) params.set("size", pageSize.toString());
    setSearchParams(params, { replace: true });
  }, [debouncedSearch, debouncedEmailSearch, page, pageSize, setSearchParams]);

  // Always call hooks, but control with enabled option
  const { data, isLoading, error, refetch } = useFindAllIamUsers(
    {
      username: debouncedSearch || undefined,
      email: debouncedEmailSearch || undefined,
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

  const users = data?.items || [];
  const metadata = data?.metadata;

  const handleFilterReset = () => {
    setSearch("");
    setEmailSearch("");
    setPage(0);
    setSearchParams({}, { replace: true });
  };

  return (
    <Box>
      <PageHeader
        title="IAM Users"
        subtitle="Manage users and their permissions"
        actions={
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={() => refetch()}
            aria-label="Refresh IAM users"
          >
            Refresh
          </Button>
        }
      />

      <Card>
        <CardContent>
          {/* Filters */}
          <Grid container spacing={2} sx={{ mb: 3 }}>
            <Grid size={{ xs: 12, md: 4 }}>
              <TextField
                fullWidth
                label="Search by Username"
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
                    "aria-label": "Search by username",
                  },
                }}
              />
            </Grid>

            <Grid size={{ xs: 12, md: 4 }}>
              <TextField
                fullWidth
                label="Search by Email"
                value={emailSearch}
                onChange={(e) => {
                  setEmailSearch(e.target.value);
                  setPage(0);
                }}
                slotProps={{
                  input: {
                    startAdornment: (
                      <InputAdornment position="start">
                        <SearchIcon />
                      </InputAdornment>
                    ),
                    "aria-label": "Search by email",
                  },
                }}
              />
            </Grid>

            <Grid size={{ xs: 12, md: 4 }}>
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
              Failed to load users:{" "}
              {(error as Error).message || "Unknown error"}
            </Alert>
          )}

          {isLoading && <TableSkeleton rows={10} columns={5} />}

          {!isLoading && !error && (
            <IamUserTable
              users={users}
              loading={isLoading}
              page={page}
              pageSize={pageSize}
              totalElements={metadata?.totalElements || 0}
              onPageChange={(newPage: number) => setPage(newPage)}
              onPageSizeChange={(newPageSize: number) => {
                setPageSize(newPageSize);
                setPage(0);
              }}
              onRowClick={(userId: string) => navigate(`/iam/users/${userId}`)}
            />
          )}
        </CardContent>
      </Card>
    </Box>
  );
}
