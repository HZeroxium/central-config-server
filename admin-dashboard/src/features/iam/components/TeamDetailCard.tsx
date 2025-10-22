import React from 'react';
import {
  Card,
  CardContent,
  CardHeader,
  Typography,
  Box,
  Divider,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  IconButton,
  Paper,
} from '@mui/material';
import Grid from '@mui/material/Grid'
import {
  Close as CloseIcon,
  Group as GroupIcon,
  Person as PersonIcon,
  Business as BusinessIcon,
  Schedule as ScheduleIcon,
} from '@mui/icons-material';
import type { IamTeam } from '../types';

interface TeamDetailCardProps {
  team: IamTeam;
  onClose: () => void;
}

export const TeamDetailCard: React.FC<TeamDetailCardProps> = ({ team, onClose }) => {
  return (
    <Card sx={{ height: 'fit-content', position: 'sticky', top: 20 }}>
      <CardHeader
        title={
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <GroupIcon color="primary" />
            <Typography variant="h6">{team.name}</Typography>
          </Box>
        }
        action={
          <IconButton onClick={onClose} size="small">
            <CloseIcon />
          </IconButton>
        }
      />
      <CardContent>
        {/* Basic Information */}
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle2" gutterBottom color="text.secondary">
            Description
          </Typography>
          <Typography variant="body2" paragraph>
            {team.description || 'No description provided'}
          </Typography>
        </Box>

        <Divider sx={{ my: 2 }} />

        {/* Statistics */}
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle2" gutterBottom color="text.secondary">
            Statistics
          </Typography>
          <Grid container spacing={2}>
            <Grid size={{ xs: 6 }}>
              <Paper sx={{ p: 2, textAlign: 'center' }}>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 1, mb: 1 }}>
                  <PersonIcon color="primary" />
                  <Typography variant="h6">{team.memberCount}</Typography>
                </Box>
                <Typography variant="caption" color="text.secondary">
                  Members
                </Typography>
              </Paper>
            </Grid>
            <Grid size={{ xs: 6 }}>
              <Paper sx={{ p: 2, textAlign: 'center' }}>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 1, mb: 1 }}>
                  <BusinessIcon color="secondary" />
                  <Typography variant="h6">{team.ownedServiceIds.length}</Typography>
                </Box>
                <Typography variant="caption" color="text.secondary">
                  Services
                </Typography>
              </Paper>
            </Grid>
          </Grid>
        </Box>

        <Divider sx={{ my: 2 }} />

        {/* Manager Information */}
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle2" gutterBottom color="text.secondary">
            Manager
          </Typography>
          {team.managerName ? (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <PersonIcon fontSize="small" color="action" />
              <Typography variant="body2">{team.managerName}</Typography>
            </Box>
          ) : (
            <Typography variant="body2" color="text.secondary">
              No manager assigned
            </Typography>
          )}
        </Box>

        <Divider sx={{ my: 2 }} />

        {/* Owned Services */}
        <Box sx={{ mb: 3 }}>
          <Typography variant="subtitle2" gutterBottom color="text.secondary">
            Owned Services ({team.ownedServiceIds.length})
          </Typography>
          {team.ownedServiceIds.length > 0 ? (
            <List dense>
              {team.ownedServiceIds.slice(0, 5).map((serviceId, index) => (
                <ListItem key={serviceId} sx={{ px: 0 }}>
                  <ListItemIcon>
                    <BusinessIcon fontSize="small" color="action" />
                  </ListItemIcon>
                  <ListItemText
                    primary={`Service ${index + 1}`}
                    secondary={`ID: ${serviceId}`}
                  />
                </ListItem>
              ))}
              {team.ownedServiceIds.length > 5 && (
                <ListItem sx={{ px: 0 }}>
                  <ListItemText
                    primary={`... and ${team.ownedServiceIds.length - 5} more`}
                    sx={{ fontStyle: 'italic', color: 'text.secondary' }}
                  />
                </ListItem>
              )}
            </List>
          ) : (
            <Typography variant="body2" color="text.secondary">
              No services owned
            </Typography>
          )}
        </Box>

        <Divider sx={{ my: 2 }} />

        {/* Metadata */}
        <Box>
          <Typography variant="subtitle2" gutterBottom color="text.secondary">
            Metadata
          </Typography>
          <List dense>
            <ListItem sx={{ px: 0 }}>
              <ListItemIcon>
                <ScheduleIcon fontSize="small" color="action" />
              </ListItemIcon>
              <ListItemText
                primary="Created"
                secondary={new Date(team.createdAt).toLocaleDateString()}
              />
            </ListItem>
            <ListItem sx={{ px: 0 }}>
              <ListItemIcon>
                <ScheduleIcon fontSize="small" color="action" />
              </ListItemIcon>
              <ListItemText
                primary="Updated"
                secondary={new Date(team.updatedAt).toLocaleDateString()}
              />
            </ListItem>
          </List>
        </Box>
      </CardContent>
    </Card>
  );
};
