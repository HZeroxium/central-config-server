import React from 'react';
import { Card, CardContent, Typography, List, ListItem, ListItemText, ListItemIcon, Box } from '@mui/material';
import { 
  CheckCircle as CheckCircleIcon,
  Warning as WarningIcon,
  Info as InfoIcon,
  Schedule as ScheduleIcon,
} from '@mui/icons-material';

interface ActivityItem {
  id: string;
  type: 'approval' | 'drift' | 'service' | 'user';
  message: string;
  timestamp: string;
  severity?: 'low' | 'medium' | 'high' | 'critical';
}

interface RecentActivityListProps {
  activities: ActivityItem[];
  title?: string;
}

const getActivityIcon = (type: ActivityItem['type'], severity?: ActivityItem['severity']) => {
  switch (type) {
    case 'approval':
      return <CheckCircleIcon color="success" />;
    case 'drift':
      const driftColor = severity === 'critical' ? 'error' : severity === 'high' ? 'warning' : 'info';
      return <WarningIcon color={driftColor as any} />;
    case 'service':
      return <InfoIcon color="primary" />;
    case 'user':
      return <ScheduleIcon color="action" />;
    default:
      return <InfoIcon color="action" />;
  }
};

const formatTimestamp = (timestamp: string) => {
  const date = new Date(timestamp);
  const now = new Date();
  const diffInHours = Math.floor((now.getTime() - date.getTime()) / (1000 * 60 * 60));
  
  if (diffInHours < 1) {
    return 'Just now';
  } else if (diffInHours < 24) {
    return `${diffInHours}h ago`;
  } else {
    const diffInDays = Math.floor(diffInHours / 24);
    return `${diffInDays}d ago`;
  }
};

export const RecentActivityList: React.FC<RecentActivityListProps> = ({ 
  activities, 
  title = 'Recent Activity' 
}) => {
  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Typography variant="h6" gutterBottom>
          {title}
        </Typography>
        <List>
          {activities.map((activity) => (
            <ListItem key={activity.id} sx={{ px: 0 }}>
              <ListItemIcon>
                {getActivityIcon(activity.type, activity.severity)}
              </ListItemIcon>
              <ListItemText
                primary={activity.message}
                secondary={
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Typography variant="caption" color="text.secondary">
                      {formatTimestamp(activity.timestamp)}
                    </Typography>
                    {activity.severity && (
                      <Typography 
                        variant="caption" 
                        color={`${activity.severity}.main`}
                        sx={{ 
                          textTransform: 'uppercase', 
                          fontWeight: 500,
                          fontSize: '0.7rem'
                        }}
                      >
                        {activity.severity}
                      </Typography>
                    )}
                  </Box>
                }
              />
            </ListItem>
          ))}
        </List>
      </CardContent>
    </Card>
  );
};
