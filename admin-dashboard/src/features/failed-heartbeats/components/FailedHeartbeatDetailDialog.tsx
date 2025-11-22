import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  Box,
  Divider,
  Chip,
  Stack,
  Alert,
  Paper,
} from "@mui/material";
import Grid from "@mui/material/Grid";
import {
  Refresh as RedriveIcon,
  Edit as EditIcon,
  Close as CloseIcon,
} from "@mui/icons-material";
import { format } from "date-fns";
import type { FailedHeartbeatResponse } from "@lib/api/models";
import { getStatusColor } from "./FailedHeartbeatsTable";

interface FailedHeartbeatDetailDialogProps {
  open: boolean;
  onClose: () => void;
  failedHeartbeat: FailedHeartbeatResponse | null;
  onRedrive: () => void;
  onUpdateStatus: () => void;
  isRedriving?: boolean;
}

export function FailedHeartbeatDetailDialog({
  open,
  onClose,
  failedHeartbeat,
  onRedrive,
  onUpdateStatus,
  isRedriving = false,
}: FailedHeartbeatDetailDialogProps) {
  if (!failedHeartbeat) {
    return null;
  }

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="md"
      fullWidth
      aria-labelledby="failed-heartbeat-dialog-title"
    >
      <DialogTitle id="failed-heartbeat-dialog-title">
        Failed Heartbeat Details
      </DialogTitle>
      <DialogContent>
        <Stack spacing={3}>
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, md: 6 }}>
              <Typography variant="subtitle2" color="text.secondary">
                Service Name
              </Typography>
              <Typography variant="body1" fontWeight={500}>
                {failedHeartbeat.serviceName || "N/A"}
              </Typography>
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography variant="subtitle2" color="text.secondary">
                Instance ID
              </Typography>
              <Typography variant="body1" fontFamily="monospace">
                {failedHeartbeat.instanceId || "N/A"}
              </Typography>
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography variant="subtitle2" color="text.secondary">
                Environment
              </Typography>
              <Typography variant="body1">
                {failedHeartbeat.environment || "N/A"}
              </Typography>
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography variant="subtitle2" color="text.secondary">
                Status
              </Typography>
              <Chip
                label={failedHeartbeat.status || "UNKNOWN"}
                color={getStatusColor(failedHeartbeat.status)}
                size="small"
              />
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography variant="subtitle2" color="text.secondary">
                Retry Count
              </Typography>
              <Typography variant="body1">
                {failedHeartbeat.retryCount ?? 0}
              </Typography>
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography variant="subtitle2" color="text.secondary">
                First Seen
              </Typography>
              <Typography variant="body1">
                {failedHeartbeat.firstSeenAt
                  ? format(new Date(failedHeartbeat.firstSeenAt), "PPpp")
                  : "N/A"}
              </Typography>
            </Grid>

            {failedHeartbeat.lastSeenAt && (
              <Grid size={{ xs: 12, md: 6 }}>
                <Typography variant="subtitle2" color="text.secondary">
                  Last Seen
                </Typography>
                <Typography variant="body1">
                  {format(new Date(failedHeartbeat.lastSeenAt), "PPpp")}
                </Typography>
              </Grid>
            )}

            {failedHeartbeat.resolvedAt && (
              <Grid size={{ xs: 12, md: 6 }}>
                <Typography variant="subtitle2" color="text.secondary">
                  Resolved At
                </Typography>
                <Typography variant="body1">
                  {format(new Date(failedHeartbeat.resolvedAt), "PPpp")}
                </Typography>
              </Grid>
            )}

            {failedHeartbeat.resolvedBy && (
              <Grid size={{ xs: 12, md: 6 }}>
                <Typography variant="subtitle2" color="text.secondary">
                  Resolved By
                </Typography>
                <Typography variant="body1">
                  {failedHeartbeat.resolvedBy}
                </Typography>
              </Grid>
            )}
          </Grid>

          <Divider />

          {failedHeartbeat.exceptionMessage && (
            <Box>
              <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                Exception Message
              </Typography>
              <Alert severity="error" sx={{ mb: 2 }}>
                {failedHeartbeat.exceptionMessage}
              </Alert>
            </Box>
          )}

          {failedHeartbeat.exceptionClass && (
            <Box>
              <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                Exception Class
              </Typography>
              <Typography variant="body2" fontFamily="monospace">
                {failedHeartbeat.exceptionClass}
              </Typography>
            </Box>
          )}

          {failedHeartbeat.notes && (
            <Box>
              <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                Notes
              </Typography>
              <Paper variant="outlined" sx={{ p: 2 }}>
                <Typography variant="body2">{failedHeartbeat.notes}</Typography>
              </Paper>
            </Box>
          )}

          {failedHeartbeat.payload && (
            <Box>
              <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                Heartbeat Payload
              </Typography>
              <Paper
                variant="outlined"
                sx={{ p: 2, maxHeight: 200, overflow: "auto" }}
              >
                <Typography
                  variant="body2"
                  fontFamily="monospace"
                  component="pre"
                  sx={{ whiteSpace: "pre-wrap", wordBreak: "break-word" }}
                >
                  {JSON.stringify(failedHeartbeat.payload, null, 2)}
                </Typography>
              </Paper>
            </Box>
          )}

          {failedHeartbeat.originalTopic && (
            <Box>
              <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                Kafka Details
              </Typography>
              <Typography variant="body2">
                Topic: {failedHeartbeat.originalTopic}
                {failedHeartbeat.originalPartition !== undefined &&
                  `, Partition: ${failedHeartbeat.originalPartition}`}
                {failedHeartbeat.originalOffset !== undefined &&
                  `, Offset: ${failedHeartbeat.originalOffset}`}
              </Typography>
            </Box>
          )}
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} startIcon={<CloseIcon />}>
          Close
        </Button>
        <Button
          variant="outlined"
          onClick={onUpdateStatus}
          startIcon={<EditIcon />}
        >
          Update Status
        </Button>
        <Button
          variant="contained"
          onClick={onRedrive}
          startIcon={<RedriveIcon />}
          disabled={isRedriving}
        >
          {isRedriving ? "Re-driving..." : "Re-drive"}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

