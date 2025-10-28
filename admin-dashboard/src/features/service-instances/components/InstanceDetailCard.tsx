import React from "react";
import {
  Box,
  Typography,
  Chip,
  Grid,
  Accordion,
  AccordionSummary,
  AccordionDetails,
} from "@mui/material";
import { ExpandMore as ExpandMoreIcon } from "@mui/icons-material";
import { DetailCard } from "@components/common/DetailCard";
import InstanceStatusChip from "./InstanceStatusChip";
import DriftIndicator from "./DriftIndicator";
import type { ServiceInstance } from "../types";

interface InstanceDetailCardProps {
  instance: ServiceInstance;
}

export const InstanceDetailCard: React.FC<InstanceDetailCardProps> = ({
  instance,
}) => {
  const formatDateTime = (dateTime: string) => {
    return new Date(dateTime).toLocaleString();
  };

  const formatMetadata = (metadata: Record<string, string>) => {
    return Object.entries(metadata).map(([key, value]) => (
      <Box
        key={key}
        sx={{ display: "flex", justifyContent: "space-between", mb: 1 }}
      >
        <Typography variant="body2" color="text.secondary">
          {key}:
        </Typography>
        <Typography variant="body2" fontWeight={500}>
          {value}
        </Typography>
      </Box>
    ));
  };

  const formatConfigHashes = (hashes: Record<string, string>) => {
    return Object.entries(hashes).map(([key, value]) => (
      <Box
        key={key}
        sx={{ display: "flex", justifyContent: "space-between", mb: 1 }}
      >
        <Typography variant="body2" color="text.secondary">
          {key}:
        </Typography>
        <Typography variant="body2" fontFamily="monospace" fontSize="0.8rem">
          {value.substring(0, 16)}...
        </Typography>
      </Box>
    ));
  };

  return (
    <Box>
      <DetailCard title="Instance Information">
        <Grid container spacing={3}>
          <Grid size={{ xs: 12, md: 6 }}>
            <Box sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
              <Box>
                <Typography variant="subtitle2" color="text.secondary">
                  Service Name
                </Typography>
                <Typography variant="h6" fontWeight={500}>
                  {instance.serviceName}
                </Typography>
              </Box>
              <Box>
                <Typography variant="subtitle2" color="text.secondary">
                  Instance ID
                </Typography>
                <Typography variant="body1" fontFamily="monospace">
                  {instance.instanceId}
                </Typography>
              </Box>
              <Box>
                <Typography variant="subtitle2" color="text.secondary">
                  Host:Port
                </Typography>
                <Typography variant="body1" fontFamily="monospace">
                  {instance.host}:{instance.port}
                </Typography>
              </Box>
              <Box>
                <Typography variant="subtitle2" color="text.secondary">
                  Environment
                </Typography>
                <Chip
                  label={instance.environment.toUpperCase()}
                  variant="outlined"
                />
              </Box>
            </Box>
          </Grid>

          <Grid size={{ xs: 12, md: 6 }}>
            <Box sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
              <Box>
                <Typography variant="subtitle2" color="text.secondary">
                  Version
                </Typography>
                <Typography variant="body1">
                  {instance.version || "N/A"}
                </Typography>
              </Box>
              <Box>
                <Typography variant="subtitle2" color="text.secondary">
                  Status
                </Typography>
                <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                  <InstanceStatusChip status={instance.status} />
                  <DriftIndicator hasDrift={instance.hasDrift} />
                </Box>
              </Box>
              <Box>
                <Typography variant="subtitle2" color="text.secondary">
                  Last Seen
                </Typography>
                <Typography variant="body1">
                  {formatDateTime(instance.lastSeenAt)}
                </Typography>
              </Box>
              <Box>
                <Typography variant="subtitle2" color="text.secondary">
                  Created At
                </Typography>
                <Typography variant="body1">
                  {formatDateTime(instance.createdAt)}
                </Typography>
              </Box>
            </Box>
          </Grid>
        </Grid>
      </DetailCard>

      {instance.metadata && Object.keys(instance.metadata).length > 0 && (
        <DetailCard title="Metadata">
          <Accordion>
            <AccordionSummary expandIcon={<ExpandMoreIcon />}>
              <Typography variant="subtitle1">
                Instance Metadata ({Object.keys(instance.metadata).length}{" "}
                items)
              </Typography>
            </AccordionSummary>
            <AccordionDetails>
              {formatMetadata(instance.metadata)}
            </AccordionDetails>
          </Accordion>
        </DetailCard>
      )}

      {instance.configHash &&
        typeof instance.configHash === "object" &&
        Object.keys(instance.configHash).length > 0 && (
          <DetailCard title="Configuration Hashes">
            <Box sx={{ display: "flex", flexDirection: "column", gap: 1 }}>
              {formatConfigHashes(
                instance.configHash as Record<string, string>
              )}
            </Box>
          </DetailCard>
        )}
    </Box>
  );
};
