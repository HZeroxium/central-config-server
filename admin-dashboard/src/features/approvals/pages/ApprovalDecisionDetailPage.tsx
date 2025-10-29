import { useParams, useNavigate } from "react-router-dom";
import {
  Box,
  Card,
  CardContent,
  Typography,
  Chip,
  Button,
  Alert,
  Divider,
  Link,
} from "@mui/material";
import Grid from "@mui/material/Grid";
import {
  ArrowBack as ArrowBackIcon,
  Link as LinkIcon,
} from "@mui/icons-material";
import PageHeader from "@components/common/PageHeader";
import { DetailPageSkeleton } from "@components/common/skeletons";
import { useFindApprovalDecisionById } from "@lib/api/hooks";
import { format } from "date-fns";
import { UserInfoDisplay } from "@components/common/UserInfoDisplay";

export default function ApprovalDecisionDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const {
    data: decision,
    isLoading,
    error,
  } = useFindApprovalDecisionById(id!, {
    query: {
      enabled: !!id,
      staleTime: 10_000,
    },
  });

  const getDecisionColor = (
    decision?: string
  ): "success" | "error" | "default" => {
    switch (decision?.toUpperCase()) {
      case "APPROVE":
        return "success";
      case "REJECT":
        return "error";
      default:
        return "default";
    }
  };

  const getGateColor = (gate?: string): "primary" | "secondary" | "default" => {
    switch (gate) {
      case "SYS_ADMIN":
        return "primary";
      case "LINE_MANAGER":
        return "secondary";
      default:
        return "default";
    }
  };

  if (isLoading) {
    return <DetailPageSkeleton />;
  }

  if (error || !decision) {
    const errorMessage =
      error instanceof Error ? error.message : "Unknown error";
    return (
      <Box>
        <PageHeader
          title="Approval Decision Not Found"
          actions={
            <Button
              variant="outlined"
              startIcon={<ArrowBackIcon />}
              onClick={() => navigate("/approval-decisions")}
            >
              Back
            </Button>
          }
        />
        <Alert severity="error" sx={{ m: 3 }}>
          {error
            ? `Failed to load approval decision: ${errorMessage}`
            : "The requested approval decision could not be found."}
        </Alert>
      </Box>
    );
  }

  return (
    <Box>
      <PageHeader
        title={`Approval Decision: ${decision.id || "N/A"}`}
        subtitle={`${decision.decision || "Unknown"} - ${
          decision.gate || "Unknown Gate"
        }`}
        actions={
          <Box sx={{ display: "flex", gap: 1 }}>
            <Button
              variant="outlined"
              startIcon={<ArrowBackIcon />}
              onClick={() => navigate("/approval-decisions")}
            >
              Back
            </Button>
            {decision.requestId && (
              <Button
                variant="contained"
                startIcon={<LinkIcon />}
                onClick={() => navigate(`/approvals/${decision.requestId}`)}
              >
                View Request
              </Button>
            )}
          </Box>
        }
      />

      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Decision Information
          </Typography>
          <Divider sx={{ mb: 2 }} />

          <Grid container spacing={3}>
            <Grid size={{ xs: 12, md: 6 }}>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Decision ID
              </Typography>
              <Typography variant="body1" fontFamily="monospace">
                {decision.id || "N/A"}
              </Typography>
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Decision
              </Typography>
              <Chip
                label={decision.decision || "Unknown"}
                color={getDecisionColor(decision.decision)}
              />
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Gate
              </Typography>
              <Chip
                label={decision.gate || "N/A"}
                color={getGateColor(decision.gate)}
                variant="outlined"
              />
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Request ID
              </Typography>
              {decision.requestId ? (
                <Link
                  component="button"
                  variant="body1"
                  onClick={() => navigate(`/approvals/${decision.requestId}`)}
                  sx={{
                    fontFamily: "monospace",
                    textDecoration: "underline",
                    cursor: "pointer",
                  }}
                >
                  {decision.requestId}
                </Link>
              ) : (
                <Typography variant="body1">N/A</Typography>
              )}
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Approver
              </Typography>
              {decision.approverUserId ? (
                <UserInfoDisplay userId={decision.approverUserId} mode="full" />
              ) : (
                <Typography variant="body1">Unknown</Typography>
              )}
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Decision Date
              </Typography>
              <Typography variant="body1">
                {decision.decidedAt
                  ? format(
                      new Date(decision.decidedAt),
                      "MMM dd, yyyy HH:mm:ss"
                    )
                  : "N/A"}
              </Typography>
            </Grid>

            {decision.note && (
              <Grid size={{ xs: 12 }}>
                <Typography
                  variant="subtitle2"
                  color="text.secondary"
                  gutterBottom
                >
                  Notes
                </Typography>
                <Typography variant="body1">{decision.note}</Typography>
              </Grid>
            )}
          </Grid>
        </CardContent>
      </Card>

      {/* Context Card */}
      {decision.requestId && (
        <Card sx={{ mt: 3 }}>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Related Request
            </Typography>
            <Divider sx={{ mb: 2 }} />
            <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
              <Typography variant="body1">
                This decision is part of approval request:
              </Typography>
              <Button
                variant="outlined"
                startIcon={<LinkIcon />}
                onClick={() => navigate(`/approvals/${decision.requestId}`)}
              >
                View Request {decision.requestId}
              </Button>
            </Box>
          </CardContent>
        </Card>
      )}
    </Box>
  );
}
