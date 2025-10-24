import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Button,
  Card,
  CardContent,
  TextField,
  InputAdornment,
  Alert,
} from '@mui/material';
import Grid from '@mui/material/Grid';
import { Search as SearchIcon, Refresh as RefreshIcon } from '@mui/icons-material';
import PageHeader from '@components/common/PageHeader';
import Loading from '@components/common/Loading';
import { useFindAllIamUsers } from '@lib/api/hooks';
import { useAuth } from '@features/auth/authContext';
import { IamUserTable } from '../components/IamUserTable';

export default function IamUserListPage() {
  const navigate = useNavigate();
  const { isSysAdmin } = useAuth();

  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [search, setSearch] = useState('');
  const [emailSearch, setEmailSearch] = useState('');

  // Only SYS_ADMIN can access IAM pages
  if (!isSysAdmin) {
    return (
      <Box>
        <PageHeader title="Access Denied" />
        <Alert severity="error" sx={{ m: 3 }}>
          You do not have permission to access this page. This feature is restricted to system administrators.
        </Alert>
      </Box>
    );
  }

  const { data, isLoading, error, refetch } = useFindAllIamUsers(
    {
      username: search || undefined,
      email: emailSearch || undefined,
      page,
      size: pageSize,
    },
    {
      query: {
        staleTime: 30_000,
      },
    }
  );

  const users = data?.items || [];
  const metadata = data?.metadata;

  const handleFilterReset = () => {
    setSearch('');
    setEmailSearch('');
    setPage(0);
  };

  return (
    <Box>
      <PageHeader
        title="IAM Users"
        subtitle="Manage users and their permissions"
        actions={
          <Button variant="outlined" startIcon={<RefreshIcon />} onClick={() => refetch()}>
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
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <SearchIcon />
                    </InputAdornment>
                  ),
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
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <SearchIcon />
                    </InputAdornment>
                  ),
                }}
              />
            </Grid>

            <Grid size={{ xs: 12, md: 4 }}>
              <Button
                fullWidth
                variant="outlined"
                onClick={handleFilterReset}
                sx={{ height: '56px' }}
              >
                Reset Filters
              </Button>
            </Grid>
          </Grid>

          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              Failed to load users: {(error as any).detail || 'Unknown error'}
            </Alert>
          )}

          {isLoading && <Loading />}

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
