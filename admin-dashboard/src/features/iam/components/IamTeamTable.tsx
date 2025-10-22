import React from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  Box,
  Typography,
  IconButton,
  Tooltip,
  CircularProgress,
} from '@mui/material';
import { Visibility as VisibilityIcon, Group as GroupIcon, Person as PersonIcon } from '@mui/icons-material';
import type { IamTeam } from '../types';

interface IamTeamTableProps {
  teams: IamTeam[];
  loading: boolean;
  onTeamSelect: (team: IamTeam) => void;
  selectedTeam?: IamTeam | null;
}

export const IamTeamTable: React.FC<IamTeamTableProps> = ({
  teams,
  loading,
  onTeamSelect,
  selectedTeam,
}) => {
  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (teams.length === 0) {
    return (
      <Box sx={{ textAlign: 'center', p: 3 }}>
        <GroupIcon sx={{ fontSize: 48, color: 'text.secondary', mb: 2 }} />
        <Typography variant="h6" color="text.secondary">
          No teams found
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Try adjusting your search or filter criteria
        </Typography>
      </Box>
    );
  }

  return (
    <TableContainer component={Paper}>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Team Name</TableCell>
            <TableCell>Description</TableCell>
            <TableCell align="center">Members</TableCell>
            <TableCell align="center">Services</TableCell>
            <TableCell>Manager</TableCell>
            <TableCell align="center">Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {teams.map((team) => (
            <TableRow
              key={team.id}
              hover
              selected={selectedTeam?.id === team.id}
              onClick={() => onTeamSelect(team)}
              sx={{ cursor: 'pointer' }}
            >
              <TableCell>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <GroupIcon color="primary" />
                  <Box>
                    <Typography variant="subtitle2" fontWeight="medium">
                      {team.name}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      ID: {team.id}
                    </Typography>
                  </Box>
                </Box>
              </TableCell>

              <TableCell>
                <Typography variant="body2" color="text.secondary" noWrap>
                  {team.description || 'No description'}
                </Typography>
              </TableCell>

              <TableCell align="center">
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 0.5 }}>
                  <PersonIcon fontSize="small" color="action" />
                  <Typography variant="body2" fontWeight="medium">
                    {team.memberCount}
                  </Typography>
                </Box>
              </TableCell>

              <TableCell align="center">
                <Chip
                  label={team.ownedServiceIds.length}
                  size="small"
                  color={team.ownedServiceIds.length > 0 ? 'primary' : 'default'}
                  variant={team.ownedServiceIds.length > 0 ? 'filled' : 'outlined'}
                />
              </TableCell>

              <TableCell>
                {team.managerName ? (
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <PersonIcon fontSize="small" color="action" />
                    <Typography variant="body2">
                      {team.managerName}
                    </Typography>
                  </Box>
                ) : (
                  <Typography variant="body2" color="text.secondary">
                    No manager
                  </Typography>
                )}
              </TableCell>

              <TableCell align="center">
                <Tooltip title="View Details">
                  <IconButton
                    size="small"
                    onClick={(e) => {
                      e.stopPropagation();
                      onTeamSelect(team);
                    }}
                  >
                    <VisibilityIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
};
