import React from 'react';
import {
  Box,
  Typography,
  Chip,
  Avatar,
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
} from '@mui/material';
import {
  Person as PersonIcon,
  Email as EmailIcon,
  Groups as GroupsIcon,
  AdminPanelSettings as AdminIcon,
  SupervisorAccount as ManagerIcon,
} from '@mui/icons-material';
import { useSelector } from 'react-redux';
import { type RootState } from '@app/store';
import { PageHeader } from '@components/common/PageHeader';
import { DetailCard } from '@components/common/DetailCard';
import Grid from '@mui/material/Grid'

export const ProfilePage: React.FC = () => {
  const { userInfo, permissions } = useSelector((state: RootState) => state.auth);

  if (!userInfo) {
    return (
      <Box>
        <PageHeader title="Profile" />
        <Typography>No user information available.</Typography>
      </Box>
    );
  }

  const getInitials = (firstName: string, lastName: string) => {
    return `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase();
  };

  return (
    <Box>
      <PageHeader title="Profile" />
      
      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 4 }}> 
          <DetailCard title="User Information">
            <Box display="flex" flexDirection="column" alignItems="center" gap={2}>
              <Avatar
                sx={{
                  width: 80,
                  height: 80,
                  bgcolor: 'primary.main',
                  fontSize: '2rem',
                }}
              >
                {getInitials(userInfo.firstName, userInfo.lastName)}
              </Avatar>
              <Typography variant="h6" fontWeight={600}>
                {userInfo.firstName} {userInfo.lastName}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {userInfo.username}
              </Typography>
            </Box>
          </DetailCard>
        </Grid>

        <Grid size={{ xs: 12, md: 8 }}> 
          <DetailCard title="Contact Information">
            <List>
              <ListItem>
                <ListItemIcon>
                  <EmailIcon />
                </ListItemIcon>
                <ListItemText
                  primary="Email"
                  secondary={userInfo.email}
                />
              </ListItem>
              <ListItem>
                <ListItemIcon>
                  <PersonIcon />
                </ListItemIcon>
                <ListItemText
                  primary="User ID"
                  secondary={userInfo.userId}
                />
              </ListItem>
            </List>
          </DetailCard>
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}> 
          <DetailCard title="Roles">
            <Box display="flex" flexWrap="wrap" gap={1}>
              {userInfo.roles.map((role) => (
                <Chip
                  key={role}
                  label={role}
                  color={role === 'SYS_ADMIN' ? 'error' : 'primary'}
                  variant="outlined"
                  icon={role === 'SYS_ADMIN' ? <AdminIcon /> : undefined}
                />
              ))}
            </Box>
          </DetailCard>
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}> 
          <DetailCard title="Teams">
            <Box display="flex" flexWrap="wrap" gap={1}>
              {userInfo.teamIds.length > 0 ? (
                userInfo.teamIds.map((teamId) => (
                  <Chip
                    key={teamId}
                    label={teamId}
                    color="secondary"
                    variant="outlined"
                    icon={<GroupsIcon />}
                  />
                ))
              ) : (
                <Typography color="text.secondary">
                  No teams assigned
                </Typography>
              )}
            </Box>
          </DetailCard>
        </Grid>

        {userInfo.managerId && (
          <Grid size={{ xs: 12 }}> 
            <DetailCard title="Manager">
              <ListItem>
                <ListItemIcon>
                  <ManagerIcon />
                </ListItemIcon>
                <ListItemText
                  primary="Reports to"
                  secondary={userInfo.managerId}
                />
              </ListItem>
            </DetailCard>
          </Grid>
        )}

        {permissions && (
          <Grid size={{ xs: 12 }}> 
            <DetailCard title="Permissions">
              <Grid container spacing={2}>
                <Grid size={{ xs: 12, md: 6 }}> 
                  <Typography variant="subtitle2" gutterBottom>
                    Allowed Routes
                  </Typography>
                  <Box display="flex" flexWrap="wrap" gap={1}>
                    {permissions.allowedRoutes.map((route) => (
                      <Chip
                        key={route}
                        label={route}
                        size="small"
                        variant="outlined"
                      />
                    ))}
                  </Box>
                </Grid>
                <Grid size={{ xs: 12, md: 6 }}> 
                  <Typography variant="subtitle2" gutterBottom>
                    Features
                  </Typography>
                  <Box display="flex" flexWrap="wrap" gap={1}>
                    {Object.entries(permissions.features).map(([feature, enabled]) => (
                      <Chip
                        key={feature}
                        label={feature}
                        size="small"
                        color={enabled ? 'success' : 'default'}
                        variant={enabled ? 'filled' : 'outlined'}
                      />
                    ))}
                  </Box>
                </Grid>
              </Grid>
            </DetailCard>
          </Grid>
        )}
      </Grid>
    </Box>
  );
};

export default ProfilePage;
