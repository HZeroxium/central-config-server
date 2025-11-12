import { useState, useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import {
  Box,
  Button,
  Card,
  CardContent,
  Alert,
} from "@mui/material";
import Grid from "@mui/material/Grid";
import {
  Refresh as RefreshIcon,
} from "@mui/icons-material";
import PageHeader from "@components/common/PageHeader";
import { TableSkeleton } from "@components/common/skeletons";
import { SearchFieldWithToggle } from "@components/common/SearchFieldWithToggle";
import { useFindAllIamUsers } from "@lib/api/hooks";
import { useAuth } from "@lib/keycloak/useAuth";
import { IamUserTable } from "../components/IamUserTable";
import { useSearchWithToggle } from "@hooks/useSearchWithToggle";

export default function IamUserListPage() {
  const navigate = useNavigate();
  const { isSysAdmin } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();

  // Parse initial state from URL params
  const initialPage = parseInt(searchParams.get("page") || "0", 10);
  const initialPageSize = parseInt(searchParams.get("size") || "20", 10);

  const [page, setPage] = useState(initialPage);
  const [pageSize, setPageSize] = useState(initialPageSize);

  // Search with toggle hooks for username and email
  const {
    search: usernameSearch,
    setSearch: setUsernameSearch,
    effectiveSearch: effectiveUsernameSearch,
    realtimeEnabled: usernameRealtimeEnabled,
    setRealtimeEnabled: setUsernameRealtimeEnabled,
    handleManualSearch: handleUsernameManualSearch,
    handleReset: resetUsernameSearch,
    isDebouncing: usernameIsDebouncing,
  } = useSearchWithToggle({
    storageKey: "iam-users-username-realtime",
    defaultRealtimeEnabled: true,
    debounceDelay: 800,
    initialSearch: searchParams.get("username") || "",
    onDebounceComplete: () => {
      // Reset page when debounce completes (search triggers)
      setPage(0);
    },
  });

  const {
    search: emailSearch,
    setSearch: setEmailSearch,
    effectiveSearch: effectiveEmailSearch,
    realtimeEnabled: emailRealtimeEnabled,
    setRealtimeEnabled: setEmailRealtimeEnabled,
    handleManualSearch: handleEmailManualSearch,
    handleReset: resetEmailSearch,
    isDebouncing: emailIsDebouncing,
  } = useSearchWithToggle({
    storageKey: "iam-users-email-realtime",
    defaultRealtimeEnabled: true,
    debounceDelay: 800,
    initialSearch: searchParams.get("email") || "",
    onDebounceComplete: () => {
      // Reset page when debounce completes (search triggers)
      setPage(0);
    },
  });

  // Sync URL params when filters change
  useEffect(() => {
    const params = new URLSearchParams();
    if (effectiveUsernameSearch) params.set("username", effectiveUsernameSearch);
    if (effectiveEmailSearch) params.set("email", effectiveEmailSearch);
    if (page > 0) params.set("page", page.toString());
    if (pageSize !== 20) params.set("size", pageSize.toString());
    setSearchParams(params, { replace: true });
  }, [effectiveUsernameSearch, effectiveEmailSearch, page, pageSize, setSearchParams]);

  // Always call hooks, but control with enabled option
  const { data, isLoading, error, refetch } = useFindAllIamUsers(
    {
      username: effectiveUsernameSearch || undefined,
      email: effectiveEmailSearch || undefined,
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
    resetUsernameSearch();
    resetEmailSearch();
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
              <SearchFieldWithToggle
                value={usernameSearch}
                onChange={(value) => {
                  setUsernameSearch(value);
                  // Don't reset page on every keystroke - only when search triggers
                }}
                onSearch={() => {
                  handleUsernameManualSearch();
                  setPage(0); // Reset page when manual search is triggered
                }}
                label="Search by Username"
                placeholder="Search by username"
                realtimeEnabled={usernameRealtimeEnabled}
                onRealtimeToggle={(enabled) => {
                  setUsernameRealtimeEnabled(enabled);
                  // Reset page when toggling modes
                  setPage(0);
                }}
                loading={isLoading}
                isDebouncing={usernameIsDebouncing}
                resultCount={metadata?.totalElements}
                helperText={
                  usernameRealtimeEnabled
                    ? "Search updates automatically as you type"
                    : "Click search button or press Enter to search"
                }
                aria-label="Search by username"
              />
            </Grid>

            <Grid size={{ xs: 12, md: 4 }}>
              <SearchFieldWithToggle
                value={emailSearch}
                onChange={(value) => {
                  setEmailSearch(value);
                  // Don't reset page on every keystroke - only when search triggers
                }}
                onSearch={() => {
                  handleEmailManualSearch();
                  setPage(0); // Reset page when manual search is triggered
                }}
                label="Search by Email"
                placeholder="Search by email"
                realtimeEnabled={emailRealtimeEnabled}
                onRealtimeToggle={(enabled) => {
                  setEmailRealtimeEnabled(enabled);
                  // Reset page when toggling modes
                  setPage(0);
                }}
                loading={isLoading}
                isDebouncing={emailIsDebouncing}
                resultCount={metadata?.totalElements}
                helperText={
                  emailRealtimeEnabled
                    ? "Search updates automatically as you type"
                    : "Click search button or press Enter to search"
                }
                aria-label="Search by email"
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
