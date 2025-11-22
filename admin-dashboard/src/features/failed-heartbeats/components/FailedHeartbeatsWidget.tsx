import { Box, Card, CardContent, Typography, Button } from "@mui/material";
import {
  Error as ErrorIcon,
  CheckCircle as SuccessIcon,
} from "@mui/icons-material";
import { useNavigate } from "react-router-dom";
import { useFailedHeartbeatsList } from "../hooks/useFailedHeartbeats";
import { formatDistanceToNow } from "date-fns";

export function FailedHeartbeatsWidget() {
  const navigate = useNavigate();
  const { failedHeartbeats, isLoading } = useFailedHeartbeatsList({
    page: 0,
    size: 10, // Only fetch recent ones for widget
  });

  const count = failedHeartbeats.length;
  const recentFailures = failedHeartbeats
    .filter((fh) => fh.firstSeenAt)
    .slice(0, 3);

  if (isLoading) {
    return (
      <Card>
        <CardContent>
          <Typography variant="body2" color="text.secondary">
            Loading...
          </Typography>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardContent>
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            mb: 2,
          }}
        >
          <Typography variant="h6">Failed Heartbeats</Typography>
          {count > 0 ? (
            <ErrorIcon color="error" />
          ) : (
            <SuccessIcon color="success" />
          )}
        </Box>

        <Box sx={{ mb: 2 }}>
          <Typography variant="h4" color={count > 0 ? "error.main" : "success.main"}>
            {count}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {count === 1 ? "failure" : "failures"} detected
          </Typography>
        </Box>

        {recentFailures.length > 0 && (
          <Box sx={{ mb: 2 }}>
            <Typography variant="subtitle2" gutterBottom>
              Recent Failures:
            </Typography>
            {recentFailures.map((failure) => (
              <Typography
                key={failure.id}
                variant="caption"
                display="block"
                color="text.secondary"
                sx={{ mb: 0.5 }}
              >
                {failure.serviceName} -{" "}
                {failure.firstSeenAt &&
                  formatDistanceToNow(new Date(failure.firstSeenAt), {
                    addSuffix: true,
                  })}
              </Typography>
            ))}
          </Box>
        )}

        <Button
          variant="outlined"
          fullWidth
          onClick={() => navigate("/failed-heartbeats")}
          size="small"
        >
          View All
        </Button>
      </CardContent>
    </Card>
  );
}

