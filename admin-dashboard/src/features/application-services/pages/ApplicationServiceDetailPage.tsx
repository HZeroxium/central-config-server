import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Typography,
  Chip,
  Button,
  Stack,
  Tabs,
  Tab,
  Alert,
} from '@mui/material';
import Grid from '@mui/material/Grid';
import {
  Edit as EditIcon,
  Share as ShareIcon,
  ArrowBack as BackIcon,
} from '@mui/icons-material';
import { PageHeader } from '@components/common/PageHeader';
import { DetailCard } from '@components/common/DetailCard';
import { TabPanel } from '@components/common/TabPanel';
import { ApplicationServiceForm } from '../components/ApplicationServiceForm';
import { ServiceShareDrawer } from '../components/ServiceShareDrawer';
import { useGetApplicationServiceByIdQuery, useUpdateApplicationServiceMutation } from '../api';
import { usePermissions } from '@features/auth/hooks/usePermissions';
export const ApplicationServiceDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [tabValue, setTabValue] = useState(0);
  const [formOpen, setFormOpen] = useState(false);
  const [shareDrawerOpen, setShareDrawerOpen] = useState(false);

  const { canEditService } = usePermissions();

  const {
    data: service,
    isLoading,
    error,
  } = useGetApplicationServiceByIdQuery(id!, {
    skip: !id,
  });

  const [updateService, { isLoading: updateLoading }] = useUpdateApplicationServiceMutation();

  const handleUpdateService = async (data: any) => {
    if (!service) return;
    
    try {
      await updateService({
        id: service.id,
        service: data,
      }).unwrap();
      setFormOpen(false);
    } catch (error) {
      console.error('Failed to update service:', error);
    }
  };

  const handleShareService = async (data: any) => {
    // TODO: Implement share service API call
    console.log('Share service data:', data);
    setShareDrawerOpen(false);
  };

  const breadcrumbs = [
    { label: 'Services', href: '/services' },
    { label: service?.displayName || 'Service' },
  ];

  if (error) {
    return (
      <Box>
        <PageHeader
          title="Service Not Found"
          breadcrumbs={[{ label: 'Services', href: '/services' }]}
        />
        <Alert severity="error">
          The requested service could not be found or you don't have permission to view it.
        </Alert>
      </Box>
    );
  }

  if (isLoading || !service) {
    return (
      <Box>
        <PageHeader title="Loading..." />
        {/* Loading skeleton could go here */}
      </Box>
    );
  }

  return (
    <Box>
      <PageHeader
        title={service.displayName}
        subtitle={service.id}
        breadcrumbs={breadcrumbs}
        actions={
          <Stack direction="row" spacing={1}>
            <Button
              startIcon={<BackIcon />}
              onClick={() => navigate('/services')}
            >
              Back to Services
            </Button>
            {canEditService && (
              <>
                <Button
                  variant="outlined"
                  startIcon={<EditIcon />}
                  onClick={() => setFormOpen(true)}
                >
                  Edit
                </Button>
                <Button
                  variant="contained"
                  startIcon={<ShareIcon />}
                  onClick={() => setShareDrawerOpen(true)}
                >
                  Share
                </Button>
              </>
            )}
          </Stack>
        }
      />

      <Box sx={{ mb: 3 }}>
        <Tabs
          value={tabValue}
          onChange={(_, newValue) => setTabValue(newValue)}
          sx={{ borderBottom: 1, borderColor: 'divider' }}
        >
          <Tab label="Overview" />
          <Tab label="Instances" />
          <Tab label="Shares" />
          <Tab label="Drift Events" />
        </Tabs>
      </Box>

      <TabPanel value={tabValue} index={0}>
        <Grid container spacing={3}>
          <Grid size={{ xs: 12, md: 8 }}>
            <DetailCard title="Service Information">
              <Grid container spacing={2}>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <Typography variant="subtitle2" color="text.secondary">
                    Service ID
                  </Typography>
                  <Typography variant="body1" sx={{ fontFamily: 'monospace' }}>
                    {service.id}
                  </Typography>
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <Typography variant="subtitle2" color="text.secondary">
                    Display Name
                  </Typography>
                  <Typography variant="body1">
                    {service.displayName}
                  </Typography>
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <Typography variant="subtitle2" color="text.secondary">
                    Owner Team
                  </Typography>
                  <Typography variant="body1">
                    {service.ownerTeamId}
                  </Typography>
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <Typography variant="subtitle2" color="text.secondary">
                    Lifecycle
                  </Typography>
                  <Typography variant="body1">
                    {service.lifecycle || 'Not set'}
                  </Typography>
                </Grid>
                <Grid size={{ xs: 12 }}>
                  <Typography variant="subtitle2" color="text.secondary">
                    Repository URL
                  </Typography>
                  <Typography variant="body1">
                    {service.repoUrl || 'Not set'}
                  </Typography>
                </Grid>
                <Grid size={{ xs: 12 }}>
                  <Typography variant="subtitle2" color="text.secondary">
                    Created
                  </Typography>
                  <Typography variant="body1">
                    {new Date(service.createdAt).toLocaleString()}
                  </Typography>
                </Grid>
                <Grid size={{ xs: 12 }}>
                  <Typography variant="subtitle2" color="text.secondary">
                    Last Updated
                  </Typography>
                  <Typography variant="body1">
                    {new Date(service.updatedAt).toLocaleString()}
                  </Typography>
                </Grid>
              </Grid>
            </DetailCard>
          </Grid>

          <Grid size={{ xs: 12, md: 4 }}>
            <DetailCard title="Environments">
              <Box display="flex" flexWrap="wrap" gap={1}>
                {service.environments.map((env) => (
                  <Chip
                    key={env}
                    label={env.toUpperCase()}
                    color="primary"
                    variant="outlined"
                  />
                ))}
              </Box>
            </DetailCard>

            <DetailCard title="Tags">
              <Box display="flex" flexWrap="wrap" gap={1}>
                {service.tags.length > 0 ? (
                  service.tags.map((tag) => (
                    <Chip
                      key={tag}
                      label={tag}
                      size="small"
                      variant="outlined"
                    />
                  ))
                ) : (
                  <Typography color="text.secondary">
                    No tags
                  </Typography>
                )}
              </Box>
            </DetailCard>

            <DetailCard title="Attributes">
              {Object.keys(service.attributes).length > 0 ? (
                <Box>
                  {Object.entries(service.attributes).map(([key, value]) => (
                    <Box key={key} sx={{ mb: 1 }}>
                      <Typography variant="caption" color="text.secondary">
                        {key}
                      </Typography>
                      <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                        {value}
                      </Typography>
                    </Box>
                  ))}
                </Box>
              ) : (
                <Typography color="text.secondary">
                  No attributes
                </Typography>
              )}
            </DetailCard>
          </Grid>
        </Grid>
      </TabPanel>

      <TabPanel value={tabValue} index={1}>
        <Alert severity="info">
          Service instances will be displayed here. This feature is coming soon.
        </Alert>
      </TabPanel>

      <TabPanel value={tabValue} index={2}>
        <Alert severity="info">
          Service shares will be displayed here. This feature is coming soon.
        </Alert>
      </TabPanel>

      <TabPanel value={tabValue} index={3}>
        <Alert severity="info">
          Drift events will be displayed here. This feature is coming soon.
        </Alert>
      </TabPanel>

      {/* Edit Form */}
      <ApplicationServiceForm
        open={formOpen}
        mode="edit"
        initialData={service}
        onSubmit={handleUpdateService}
        onClose={() => setFormOpen(false)}
        loading={updateLoading}
      />

      {/* Share Drawer */}
      <ServiceShareDrawer
        open={shareDrawerOpen}
        serviceId={service.id}
        onClose={() => setShareDrawerOpen(false)}
        onSubmit={handleShareService}
      />
    </Box>
  );
};

export default ApplicationServiceDetailPage;
