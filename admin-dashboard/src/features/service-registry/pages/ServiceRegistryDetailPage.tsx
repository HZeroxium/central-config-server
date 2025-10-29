import { useParams } from "react-router-dom";
import {
  Box,
  Card,
  CardContent,
  Typography,
  Alert,
  Grid,
  Stack,
  Divider,
} from "@mui/material";
import { useGetServiceRegistryServiceInstances } from "@lib/api/generated/service-registry/service-registry";
import PageHeader from "@components/common/PageHeader";
import { DetailPageSkeleton } from "@components/common/skeletons";
import Breadcrumbs from "@components/common/Breadcrumbs";
import ConsulHealthBadge from "../components/ConsulHealthBadge";
import { getErrorMessage } from "@lib/api/errorHandler";

export default function ServiceRegistryDetailPage() {
  const { serviceName } = useParams<{ serviceName: string }>();

  const {
    data: instancesData,
    isLoading,
    error,
  } = useGetServiceRegistryServiceInstances(
    serviceName || "",
    { passing: false },
    {
      query: {
        enabled: !!serviceName,
        staleTime: 30_000,
      },
    }
  );

  if (!serviceName) {
    return <Alert severity="error">Service name is required</Alert>;
  }

  return (
    <Box>
      <Breadcrumbs />

      <PageHeader
        title={`Service: ${serviceName}`}
        subtitle="Consul service details"
      />

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          Failed to load service details: {getErrorMessage(error)}
        </Alert>
      )}

      {isLoading && <DetailPageSkeleton />}

      {!isLoading && !error && (
        <Stack spacing={3}>
          {/* Service Overview */}
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Service Overview
              </Typography>
              <Divider sx={{ my: 2 }} />
              <Grid container spacing={2}>
                <Grid size={{ xs: 12, md: 6 }}>
                  <Typography variant="body2" color="text.secondary">
                    Service Name
                  </Typography>
                  <Typography variant="body1">{serviceName}</Typography>
                </Grid>
                <Grid size={{ xs: 12, md: 6 }}>
                  <Typography variant="body2" color="text.secondary">
                    Total Instances
                  </Typography>
                  <Typography variant="body1">
                    {instancesData?.instances?.length || 0}
                  </Typography>
                </Grid>
              </Grid>
            </CardContent>
          </Card>

          {/* Instances */}
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Instances
              </Typography>
              <Divider sx={{ my: 2 }} />
              {instancesData?.instances &&
              instancesData.instances.length > 0 ? (
                <Stack spacing={2}>
                  {instancesData.instances.map((instance) => (
                    <Card key={instance.instanceId} variant="outlined">
                      <CardContent>
                        <Grid container spacing={2}>
                          <Grid size={{ xs: 12, md: 3 }}>
                            <Typography variant="body2" color="text.secondary">
                              Instance ID
                            </Typography>
                            <Typography variant="body1">
                              {instance.instanceId}
                            </Typography>
                          </Grid>
                          <Grid size={{ xs: 12, md: 3 }}>
                            <Typography variant="body2" color="text.secondary">
                              Host
                            </Typography>
                            <Typography variant="body1">
                              {instance.host}
                            </Typography>
                          </Grid>
                          <Grid size={{ xs: 12, md: 2 }}>
                            <Typography variant="body2" color="text.secondary">
                              Port
                            </Typography>
                            <Typography variant="body1">
                              {instance.port}
                            </Typography>
                          </Grid>
                          <Grid size={{ xs: 12, md: 2 }}>
                            <Typography variant="body2" color="text.secondary">
                              Status
                            </Typography>
                            <ConsulHealthBadge
                              status={
                                instance.healthy
                                  ? "passing"
                                  : ("critical" as
                                      | "passing"
                                      | "warning"
                                      | "critical")
                              }
                            />
                          </Grid>
                          <Grid size={{ xs: 12, md: 2 }}>
                            <Typography variant="body2" color="text.secondary">
                              URI
                            </Typography>
                            <Typography
                              variant="body1"
                              sx={{ wordBreak: "break-all" }}
                            >
                              {instance.uri}
                            </Typography>
                          </Grid>
                        </Grid>
                      </CardContent>
                    </Card>
                  ))}
                </Stack>
              ) : (
                <Alert severity="info">No instances found</Alert>
              )}
            </CardContent>
          </Card>
        </Stack>
      )}
    </Box>
  );
}
