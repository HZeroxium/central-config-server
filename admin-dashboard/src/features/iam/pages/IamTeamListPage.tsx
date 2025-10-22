import React, { useState } from 'react';
import { Box, Button, TextField, InputAdornment, FormControl, InputLabel, Select, MenuItem, Chip, Card, CardContent, Typography, Grid } from '@mui/material';
import { Search as SearchIcon, Refresh as RefreshIcon, Group as GroupIcon, Person as PersonIcon } from '@mui/icons-material';
import { PageHeader } from '@components/common/PageHeader';
import { IamTeamTable } from '../components/IamTeamTable';
import { TeamDetailCard } from '../components/TeamDetailCard';
import {
  useFindAllIamTeams,
  useGetStatsIamTeam,
} from '@lib/api/hooks';
import { useErrorHandler } from '../../../hooks/useErrorHandler';
import type { IamTeam, IamFilter } from '../types';
import { usePermissions } from '@features/auth/hooks/usePermissions';

export const IamTeamListPage: React.FC = () => {
  const [page] = useState(0);
  const [pageSize] = useState(10);
  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState<IamFilter>({});
  const [selectedTeam, setSelectedTeam] = useState<IamTeam | null>(null);

  const { isSysAdmin } = usePermissions();
  const { handleError } = useErrorHandler();

  const {
    data: teamsResponse,
    isLoading,
    refetch,
  } = useFindAllIamTeams(
    {
      criteria: {
        ...filter,
        displayName: search || undefined,
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
  const pageData = teamsResponse;
  const teams = (pageData?.content || []) as IamTeam[];

  const handleTeamSelect = (team: IamTeam) => {
    setSelectedTeam(team);
  };

  const handleClearFilters = () => {
    setFilter({});
    setSearch('');
  };

  return (
    <Box>
      <PageHeader
        title="Teams"
        subtitle="Manage teams and their members"
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
          placeholder="Search teams..."
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
          <InputLabel>Manager</InputLabel>
          <Select
            value={filter.managerId || ''}
            onChange={(e) => setFilter(prev => ({ ...prev, managerId: e.target.value || undefined }))}
            label="Manager"
          >
            <MenuItem value="">All</MenuItem>
            {/* This would be populated from users API */}
            <MenuItem value="manager1">John Doe</MenuItem>
            <MenuItem value="manager2">Jane Smith</MenuItem>
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
            value && (
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
        {/* Teams Table */}
        <Grid size={{ xs: selectedTeam ? 8 : 12 }}>
          <IamTeamTable
            teams={teams}
            loading={isLoading}
            onTeamSelect={handleTeamSelect}
            selectedTeam={selectedTeam}
          />
        </Grid>

        {/* Team Detail Card */}
        {selectedTeam && (
          <Grid size={{ xs: 4 }}>
            <TeamDetailCard
              team={selectedTeam}
              onClose={() => setSelectedTeam(null)}
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
                  <GroupIcon color="primary" />
                  <Box>
                    <Typography variant="h6">{teams.length}</Typography>
                    <Typography variant="body2" color="text.secondary">
                      Total Teams
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
                      {teams.reduce((sum, team) => sum + team.memberCount, 0)}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Total Members
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
                      {teams.filter(team => team.ownedServiceIds.length > 0).length}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Teams with Services
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
                  <PersonIcon color="info" />
                  <Box>
                    <Typography variant="h6">
                      {teams.filter(team => team.managerId).length}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Teams with Managers
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

export default IamTeamListPage;