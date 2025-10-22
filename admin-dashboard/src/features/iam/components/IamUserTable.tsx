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
  Avatar,
} from '@mui/material';
import { Visibility as VisibilityIcon, Person as PersonIcon, Group as GroupIcon } from '@mui/icons-material';
import type { IamUser, UserRole } from '../types';
import { ROLE_LABELS, ROLE_COLORS } from '../types';

interface IamUserTableProps {
  users: IamUser[];
  loading: boolean;
  onUserSelect: (user: IamUser) => void;
  selectedUser?: IamUser | null;
}

export const IamUserTable: React.FC<IamUserTableProps> = ({
  users,
  loading,
  onUserSelect,
  selectedUser,
}) => {
  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (users.length === 0) {
    return (
      <Box sx={{ textAlign: 'center', p: 3 }}>
        <PersonIcon sx={{ fontSize: 48, color: 'text.secondary', mb: 2 }} />
        <Typography variant="h6" color="text.secondary">
          No users found
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Try adjusting your search or filter criteria
        </Typography>
      </Box>
    );
  }

  const getInitials = (firstName: string, lastName: string) => {
    return `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase();
  };

  const getRoleColor = (role: UserRole) => {
    return ROLE_COLORS[role] as 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning';
  };

  return (
    <TableContainer component={Paper}>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>User</TableCell>
            <TableCell>Email</TableCell>
            <TableCell>Teams</TableCell>
            <TableCell>Roles</TableCell>
            <TableCell>Status</TableCell>
            <TableCell>Last Login</TableCell>
            <TableCell align="center">Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {users.map((user) => (
            <TableRow
              key={user.id}
              hover
              selected={selectedUser?.id === user.id}
              onClick={() => onUserSelect(user)}
              sx={{ cursor: 'pointer' }}
            >
              <TableCell>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                  <Avatar sx={{ width: 32, height: 32 }}>
                    {getInitials(user.firstName, user.lastName)}
                  </Avatar>
                  <Box>
                    <Typography variant="subtitle2" fontWeight="medium">
                      {user.firstName} {user.lastName}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      @{user.username}
                    </Typography>
                  </Box>
                </Box>
              </TableCell>

              <TableCell>
                <Typography variant="body2">
                  {user.email}
                </Typography>
              </TableCell>

              <TableCell>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, maxWidth: 200 }}>
                  {user.teamNames.slice(0, 2).map((teamName, index) => (
                    <Chip
                      key={index}
                      label={teamName}
                      size="small"
                      color="primary"
                      variant="outlined"
                    />
                  ))}
                  {user.teamNames.length > 2 && (
                    <Chip
                      label={`+${user.teamNames.length - 2}`}
                      size="small"
                      color="default"
                      variant="outlined"
                    />
                  )}
                </Box>
              </TableCell>

              <TableCell>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                  {user.roles.map((role) => (
                    <Chip
                      key={role}
                      label={ROLE_LABELS[role as UserRole] || role}
                      size="small"
                      color={getRoleColor(role as UserRole)}
                      variant="filled"
                    />
                  ))}
                </Box>
              </TableCell>

              <TableCell>
                <Chip
                  label={user.isActive ? 'Active' : 'Inactive'}
                  size="small"
                  color={user.isActive ? 'success' : 'default'}
                  variant={user.isActive ? 'filled' : 'outlined'}
                />
              </TableCell>

              <TableCell>
                {user.lastLoginAt ? (
                  <Typography variant="body2" color="text.secondary">
                    {new Date(user.lastLoginAt).toLocaleDateString()}
                  </Typography>
                ) : (
                  <Typography variant="body2" color="text.secondary">
                    Never
                  </Typography>
                )}
              </TableCell>

              <TableCell align="center">
                <Tooltip title="View Details">
                  <IconButton
                    size="small"
                    onClick={(e) => {
                      e.stopPropagation();
                      onUserSelect(user);
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
