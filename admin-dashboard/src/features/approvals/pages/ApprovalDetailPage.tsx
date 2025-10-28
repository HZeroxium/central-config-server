import { useState } from "react";
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
} from "@mui/material";
import Grid from "@mui/material/Grid";
import {
  ArrowBack as ArrowBackIcon,
  CheckCircle as ApproveIcon,
} from "@mui/icons-material";
import PageHeader from "@components/common/PageHeader";
import Loading from "@components/common/Loading";
import {
  useFindApprovalRequestById,
  useSubmitApprovalDecision,
} from "@lib/api/hooks";
import { useAuth } from "@features/auth/authContext";
import { toast } from "@lib/toast/toast";
import { handleApiError } from "@lib/api/errorHandler";
import { DecisionDialog } from "../components/DecisionDialog";
import { ApprovalStepper } from "../components/ApprovalStepper";
import { DecisionTimeline } from "../components/DecisionTimeline";
import { useCanApprove } from "../hooks/useCanApprove";
import { format } from "date-fns";

export default function ApprovalDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { isSysAdmin } = useAuth();
  const [decisionDialogOpen, setDecisionDialogOpen] = useState(false);

  const {
    data: request,
    isLoading,
    error,
    refetch,
  } = useFindApprovalRequestById(id!, {
    query: {
      enabled: !!id,
      staleTime: 10_000,
    },
  });
  
  const { canApprove } = useCanApprove(request);

  // Note: requesterUser fetch removed - manager logic now in useCanApprove hook

  const submitDecisionMutation = useSubmitApprovalDecision();

  const handleSubmitDecision = async (decision: {
    decision: "APPROVE" | "REJECT";
    note?: string;
  }) => {
    if (!id) return;

    submitDecisionMutation.mutate(
      { id, data: decision },
      {
        onSuccess: () => {
          toast.success("Decision submitted successfully");
          setDecisionDialogOpen(false);
          refetch();
        },
        onError: (error) => {
          handleApiError(error);
        },
      }
    );
  };

  // Note: canApprove logic is now handled by useCanApprove hook

  const getStatusColor = (
    status?: string
  ): "default" | "info" | "success" | "error" | "warning" => {
    switch (status) {
      case "PENDING":
        return "warning";
      case "APPROVED":
        return "success";
      case "REJECTED":
        return "error";
      case "CANCELLED":
        return "default";
      default:
        return "info";
    }
  };

  if (isLoading) {
    return <Loading />;
  }

  if (error || !request) {
    return (
      <Box>
        <PageHeader
          title="Approval Request Not Found"
          actions={
            <Button
              variant="outlined"
              startIcon={<ArrowBackIcon />}
              onClick={() => navigate("/approvals")}
            >
              Back
            </Button>
          }
        />
        <Alert severity="error" sx={{ m: 3 }}>
          {error
            ? `Failed to load approval request: ${
                error instanceof Error ? error.message : "Unknown error"
              }`
            : "The requested approval could not be found."}
        </Alert>
      </Box>
    );
  }

  return (
    <Box>
      <PageHeader
        title={`Approval Request: ${request.target?.serviceId || "N/A"}`}
        subtitle={`${request.requestType || "Unknown"} - ${
          request.status || "Unknown"
        }`}
        actions={
          <Box sx={{ display: "flex", gap: 1 }}>
            <Button
              variant="outlined"
              startIcon={<ArrowBackIcon />}
              onClick={() => navigate("/approvals")}
            >
              Back
            </Button>
            {canApprove && request.status === "PENDING" && (
              <Button
                variant="contained"
                startIcon={<ApproveIcon />}
                onClick={() => setDecisionDialogOpen(true)}
              >
                Make Decision
              </Button>
            )}
            {canApprove && eligibleGates.length > 0 && (
              <Typography variant="caption" color="text.secondary" sx={{ alignSelf: 'center', ml: 1 }}>
                Eligible for: {eligibleGates.join(', ')}
              </Typography>
            )}
          </Box>
        }
      />

      <Card>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Request Information
          </Typography>
          <Divider sx={{ mb: 2 }} />

          <Grid container spacing={3}>
            <Grid size={{ xs: 12, md: 6 }}>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Request ID
              </Typography>
              <Typography variant="body1" fontFamily="monospace">
                {request.id}
              </Typography>
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Status
              </Typography>
              <Chip
                label={request.status}
                color={getStatusColor(request.status)}
              />
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Request Type
              </Typography>
              <Typography variant="body1">{request.requestType}</Typography>
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Requester
              </Typography>
              <Typography variant="body1">{request.requesterUserId}</Typography>
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Target Service
              </Typography>
              <Typography variant="body1">
                {request.target?.serviceId || "N/A"}
              </Typography>
            </Grid>

            {request.target?.teamId && (
              <Grid size={{ xs: 12, md: 6 }}>
                <Typography
                  variant="subtitle2"
                  color="text.secondary"
                  gutterBottom
                >
                  Target Team
                </Typography>
                <Typography variant="body1">{request.target.teamId}</Typography>
              </Grid>
            )}

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography
                variant="subtitle2"
                color="text.secondary"
                gutterBottom
              >
                Created At
              </Typography>
              <Typography variant="body1">
                {request.createdAt
                  ? format(new Date(request.createdAt), "MMM dd, yyyy HH:mm:ss")
                  : "N/A"}
              </Typography>
            </Grid>

            {request.note && (
              <Grid size={{ xs: 12 }}>
                <Typography
                  variant="subtitle2"
                  color="text.secondary"
                  gutterBottom
                >
                  Requester Note
                </Typography>
                <Typography variant="body1">{request.note}</Typography>
              </Grid>
            )}
          </Grid>
        </CardContent>
      </Card>

      {/* Approval Progress Stepper */}
      <ApprovalStepper request={request} />

      {/* Decision Timeline */}
      <DecisionTimeline request={request} />

      {/* Decision Dialog */}
      <DecisionDialog
        open={decisionDialogOpen}
        onClose={() => setDecisionDialogOpen(false)}
        onSubmit={handleSubmitDecision}
        loading={submitDecisionMutation.isPending}
      />
    </Box>
  );
}
