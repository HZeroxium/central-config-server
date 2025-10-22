import React, { useState } from 'react';
import { Box, Button, TextField, InputAdornment, FormControl, InputLabel, Select, MenuItem, Chip, Card, CardContent, Typography, Grid } from '@mui/material';
import { Search as SearchIcon, Refresh as RefreshIcon, Person as PersonIcon, Group as GroupIcon } from '@mui/icons-material';
import { PageHeader } from '@components/common/PageHeader';
import { IamUserTable } from '../components/IamUserTable';
import { UserDetailCard } from '../components/UserDetailCard';
import {
  useFindAllIamUsers,
  useGetStatsIamUser,
} from '@lib/api/hooks';
import { useErrorHandler } from '../../../hooks/useErrorHandler';
import type { IamUser, IamFilter, UserRole } from '../types';
import { USER_ROLES, ROLE_LABELS } from '../types';
import { usePermissions } from '@features/auth/hooks/usePermissions';

export const IamUserListPage: React.FC = () => {
  const [page] = useState(0);
  const [pageSize] = useState(10);
  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState<IamFilter>({});
  const [selectedUser, setSelectedUser] = useState<IamUser | null>(null);

  const { isSysAdmin } = usePermissions();
  const { handleError } = useErrorHandler();

  const {
    data: usersResponse,
    isLoading,
    refetch,
  } = useFindAllIamUsers(
    {
      criteria: {
        ...filter,
        username: search || undefined,
      },
      userContext: {
        userId: 'current', // This would be the current user ID
      },
      pageable: { page, size: pageSize },
    },
    {
      query: {
        staleTime: 30000,
      },
    }
  );

  // Get the page data from API response
  const pageData = usersResponse;
  const users = (pageData?.content || []) as IamUser[];

  const handleUserSelect = (user: IamUser) => {
    setSelectedUser(user);
  };

  const handleClearFilters = () => {
    setFilter({});
    setSearch('');
  };

  return (
    <Box>
      <PageHeader
        title="Users"
        subtitle="Manage users and their team memberships"
        actions={
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={() => refetch()}
            disabled={isLoading}
          >
            Refresh
          </Button>
        }
      />

      {/* Search and Filters */}
      <Box sx={{ mb: 3, display: 'flex', gap: 2, alignItems: 'center', flexWrap: 'wrap' }}>
        <TextField
          placeholder="Search users..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon />
              </InputAdornment>
            ),
          }}
          sx={{ minWidth: 300 }}
        />

        <FormControl sx={{ minWidth: 120 }}>
          <InputLabel>Role</InputLabel>
          <Select
            value={filter.role || ''}
            onChange={(e) => setFilter(prev => ({ ...prev, role: e.target.value || undefined }))}
            label="Role"
          >
            <MenuItem value="">All</MenuItem>
            {Object.entries(ROLE_LABELS).map(([value, label]) => (
              <MenuItem key={value} value={value}>
                {label}
              </MenuItem>
            ))}
          </Select>
        </FormControl>

        <FormControl sx={{ minWidth: 120 }}>
          <InputLabel>Status</InputLabel>
          <Select
            value={filter.isActive === undefined ? '' : filter.isActive.toString()}
            onChange={(e) => setFilter(prev => ({ 
              ...prev, 
              isActive: e.target.value === '' ? undefined : e.target.value === 'true' 
            }))}
            label="Status"
          >
            <MenuItem value="">All</MenuItem>
            <MenuItem value="true">Active</MenuItem>
            <MenuItem value="false">Inactive</MenuItem>
          </Select>
        </FormControl>

        <FormControl sx={{ minWidth: 120 }}>
          <InputLabel>Team</InputLabel>
          <Select
            value={filter.teamId || ''}
            onChange={(e) => setFilter(prev => ({ ...prev, teamId: e.target.value || undefined }))}
            label="Team"
          >
            <MenuItem value="">All</MenuItem>
            {/* This would be populated from teams API */}
            <MenuItem value="team1">Development Team</MenuItem>
            <MenuItem value="team2">QA Team</MenuItem>
            <MenuItem value="team3">DevOps Team</MenuItem>
          </Select>
        </FormControl>

        {Object.keys(filter).length > 0 && (
          <Button variant="text" onClick={handleClearFilters}>
            Clear Filters
          </Button>
        )}
      </Box>

      {/* Active Filters Display */}
      {Object.keys(filter).length > 0 && (
        <Box sx={{ mb: 2, display: 'flex', gap: 1, flexWrap: 'wrap' }}>
          {Object.entries(filter).map(([key, value]) => (
            value !== undefined && value !== '' && (
              <Chip
                key={key}
                label={`${key}: ${value}`}
                onDelete={() => setFilter(prev => ({ ...prev, [key]: undefined }))}
                size="small"
              />
            )
          ))}
        </Box>
      )}

      <Grid container spacing={3}>
        {/* Users Table */}
        <Grid size={{ xs: selectedUser ? 8 : 12 }}>
          <IamUserTable
            users={users}
            loading={isLoading}
            onUserSelect={handleUserSelect}
            selectedUser={selectedUser}
          />
        </Grid>

        {/* User Detail Card */}
        {selectedUser && (
          <Grid size={{ xs: 4 }}>
            <UserDetailCard
              user={selectedUser}
              onClose={() => setSelectedUser(null)}
            />
          </Grid>
        )}
      </Grid>

      {/* Summary Cards */}
      <Box sx={{ mt: 3 }}>
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, sm: 6, md: 3 }}>
            <Card>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <PersonIcon color="primary" />
                  <Box>
                    <Typography variant="h6">{users.length}</Typography>
                    <Typography variant="body2" color="text.secondary">
                      Total Users
                    </Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>

          <Grid size={{ xs: 12, sm: 6, md: 3 }}>
            <Card>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <PersonIcon color="success" />
                  <Box>
                    <Typography variant="h6">
                      {users.filter(user => user.isActive).length}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Active Users
                    </Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>

          <Grid size={{ xs: 12, sm: 6, md: 3 }}>
            <Card>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <GroupIcon color="warning" />
                  <Box>
                    <Typography variant="h6">
                      {users.filter(user => user.teamIds.length > 0).length}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Users in Teams
                    </Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>

          <Grid size={{ xs: 12, sm: 6, md: 3 }}>
            <Card>
              <CardContent>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <PersonIcon color="error" />
                  <Box>
                    <Typography variant="h6">
                      {users.filter(user => user.roles.includes(USER_ROLES.SYS_ADMIN)).length}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      System Admins
                    </Typography>
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </Box>
    </Box>
  );
};

export default IamUserListPage;