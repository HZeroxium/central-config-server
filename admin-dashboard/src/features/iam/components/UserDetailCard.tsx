import React from 'react';
import {
  Card,
  CardContent,
  CardHeader,
  Typography,
  Box,
  Chip,
  Divider,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  IconButton,
  Paper,
  Avatar,
} from '@mui/material';
import Grid from '@mui/material/Grid'
import {
  Close as CloseIcon,
  Person as PersonIcon,
  Email as EmailIcon,
  Group as GroupIcon,
  Business as BusinessIcon,
  Schedule as ScheduleIcon,
  CheckCircle as CheckCircleIcon,
  Cancel as CancelIcon,
} from '@mui/icons-material';
import type { IamUser, UserRole } from '../types';
import { ROLE_LABELS, ROLE_COLORS } from '../types';

interface UserDetailCardProps {
  user: IamUser;
  onClose: () => void;
}

export const UserDetailCard: React.FC<UserDetailCardProps> = ({ user, onClose }) => {
  const getInitials = (firstName: string, lastName: string) => {
    return `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase();
  };

  const getRoleColor = (role: UserRole) => {
    return ROLE_COLORS[role] as 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning';
  };

  return (
    <Card sx={{ height: 'fit-content', position: 'sticky', top: 20 }}>
      <CardHeader
        title={
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <Avatar sx={{ width: 40, height: 40 }}>
              {getInitials(user.firstName, user.lastName)}
            </Avatar>
            <Box>
              <Typography variant="h6">{user.firstName} {user.lastName}</Typography>
              <Typography variant="body2" color="text.secondary">
                @{user.username}
              </Typography>
            </Box>
          </Box>
        }
        action={
          <IconButton onClick={onClose} size="small">
            <CloseIcon />
          </IconButton>
        }
      />
      <CardContent>
        {/* Contact Information */}
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle2" gutterBottom color="text.secondary">
            Contact Information
          </Typography>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
            <EmailIcon fontSize="small" color="action" />
            <Typography variant="body2">{user.email}</Typography>
          </Box>
        </Box>

        <Divider sx={{ my: 2 }} />

        {/* Status and Roles */}
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle2" gutterBottom color="text.secondary">
            Status & Roles
          </Typography>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
            {user.isActive ? (
              <CheckCircleIcon color="success" fontSize="small" />
            ) : (
              <CancelIcon color="error" fontSize="small" />
            )}
            <Chip
              label={user.isActive ? 'Active' : 'Inactive'}
              size="small"
              color={user.isActive ? 'success' : 'default'}
              variant={user.isActive ? 'filled' : 'outlined'}
            />
          </Box>
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
        </Box>

        <Divider sx={{ my: 2 }} />

        {/* Team Memberships */}
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle2" gutterBottom color="text.secondary">
            Team Memberships ({user.teamNames.length})
          </Typography>
          {user.teamNames.length > 0 ? (
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
              {user.teamNames.map((teamName, index) => (
                <Chip
                  key={index}
                  label={teamName}
                  size="small"
                  color="primary"
                  variant="outlined"
                />
              ))}
            </Box>
          ) : (
            <Typography variant="body2" color="text.secondary">
              Not assigned to any teams
            </Typography>
          )}
        </Box>

        <Divider sx={{ my: 2 }} />

        {/* Manager Information */}
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle2" gutterBottom color="text.secondary">
            Manager
          </Typography>
          {user.managerName ? (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <PersonIcon fontSize="small" color="action" />
              <Typography variant="body2">{user.managerName}</Typography>
            </Box>
          ) : (
            <Typography variant="body2" color="text.secondary">
              No manager assigned
            </Typography>
          )}
        </Box>

        <Divider sx={{ my: 2 }} />

        {/* Activity Information */}
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle2" gutterBottom color="text.secondary">
            Activity
          </Typography>
          <List dense>
            <ListItem sx={{ px: 0 }}>
              <ListItemIcon>
                <ScheduleIcon fontSize="small" color="action" />
              </ListItemIcon>
              <ListItemText
                primary="Last Login"
                secondary={user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString() : 'Never'}
              />
            </ListItem>
            <ListItem sx={{ px: 0 }}>
              <ListItemIcon>
                <ScheduleIcon fontSize="small" color="action" />
              </ListItemIcon>
              <ListItemText
                primary="Created"
                secondary={new Date(user.createdAt).toLocaleDateString()}
              />
            </ListItem>
            <ListItem sx={{ px: 0 }}>
              <ListItemIcon>
                <ScheduleIcon fontSize="small" color="action" />
              </ListItemIcon>
              <ListItemText
                primary="Updated"
                secondary={new Date(user.updatedAt).toLocaleDateString()}
              />
            </ListItem>
          </List>
        </Box>

        <Divider sx={{ my: 2 }} />

        {/* Quick Stats */}
        <Box>
          <Typography variant="subtitle2" gutterBottom color="text.secondary">
            Quick Stats
          </Typography>
          <Grid container spacing={1}>
            <Grid size={{ xs: 6 }}>
              <Paper sx={{ p: 1.5, textAlign: 'center' }}>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 0.5, mb: 0.5 }}>
                  <GroupIcon fontSize="small" color="primary" />
                  <Typography variant="h6">{user.teamIds.length}</Typography>
                </Box>
                <Typography variant="caption" color="text.secondary">
                  Teams
                </Typography>
              </Paper>
            </Grid>
            <Grid size={{ xs: 6 }}>
              <Paper sx={{ p: 1.5, textAlign: 'center' }}>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 0.5, mb: 0.5 }}>
                  <BusinessIcon fontSize="small" color="secondary" />
                  <Typography variant="h6">{user.roles.length}</Typography>
                </Box>
                <Typography variant="caption" color="text.secondary">
                  Roles
                </Typography>
              </Paper>
            </Grid>
          </Grid>
        </Box>
      </CardContent>
    </Card>
  );
};
