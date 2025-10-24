import { Card, CardContent, Typography, Grid, Chip, Stack, Box } from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import type { ServiceInstanceSummary } from '@lib/api/models';

interface ConsulServiceCardProps {
  serviceName: string;
  tags?: string[];
  instances?: ServiceInstanceSummary[];
  onClick?: () => void;
}

export default function ConsulServiceCard({ 
  serviceName, 
  tags = [], 
  instances = [],
  onClick 
}: ConsulServiceCardProps) {
  const healthyCount = instances.filter(i => i.healthy).length;
  const totalCount = instances.length;

  return (
    <Card 
      sx={{ 
        cursor: onClick ? 'pointer' : 'default',
        transition: 'all 0.2s',
        '&:hover': onClick ? {
          transform: 'translateY(-4px)',
          boxShadow: 4,
        } : {},
      }}
      onClick={onClick}
    >
      <CardContent>
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 6 }}>
            <Typography variant="h6" gutterBottom>
              {serviceName}
            </Typography>
            {tags.length > 0 && (
              <Stack direction="row" spacing={1} flexWrap="wrap" sx={{ mt: 1 }}>
                {tags.slice(0, 5).map((tag) => (
                  <Chip key={tag} label={tag} size="small" />
                ))}
                {tags.length > 5 && (
                  <Chip label={`+${tags.length - 5} more`} size="small" variant="outlined" />
                )}
              </Stack>
            )}
          </Grid>
          <Grid size={{ xs: 12, md: 6 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end' }}>
              <CheckCircleIcon 
                sx={{ 
                  mr: 1, 
                  color: healthyCount === totalCount ? 'success.main' : 'warning.main' 
                }} 
              />
              <Typography variant="body1">
                {healthyCount}/{totalCount} healthy
              </Typography>
            </Box>
            <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'right', mt: 1 }}>
              {totalCount} {totalCount === 1 ? 'instance' : 'instances'}
            </Typography>
          </Grid>
        </Grid>
      </CardContent>
    </Card>
  );
}

