import React from "react";
import {
  Card,
  CardContent,
  Typography,
  Box,
  Stepper,
  Step,
  StepLabel,
  StepContent,
  Chip,
  Divider,
  Skeleton,
  Alert,
} from "@mui/material";
import { CheckCircle, Cancel, AccessTime } from "@mui/icons-material";
import type { ApprovalRequestApprovalGate } from "@lib/api/models";
import { useFindApprovalDecisionsByRequestId } from "@lib/api/hooks";
import { useApproverInfo } from "../hooks/useApproverInfo";
import { UserInfoDisplay } from "@components/common/UserInfoDisplay";

interface ApprovalStepperProps {
  requestId: string;
  requiredGates: ApprovalRequestApprovalGate[];
}

export const ApprovalStepper: React.FC<ApprovalStepperProps> = ({
  requestId,
  requiredGates,
}) => {
  const {
    data: decisionsData,
    isLoading: decisionsLoading,
    error: decisionsError,
  } = useFindApprovalDecisionsByRequestId(requestId, undefined, {
    query: {
      enabled: !!requestId,
      staleTime: 10_000,
      refetchInterval: 30_000, // Auto-refresh every 30s
    },
  });

  const decisions = decisionsData?.items || [];

  // Extract approver user IDs
  const approverUserIds = decisions
    .map((d) => d.approverUserId)
    .filter((id): id is string => !!id);

  const { isLoading: approversLoading } = useApproverInfo(approverUserIds);

  const getStepIcon = (gate: ApprovalRequestApprovalGate) => {
    const gateDecisions = decisions.filter((d) => d.gate === gate.gate) || [];
    const approvedCount = gateDecisions.filter(
      (d) => d.decision === "APPROVE"
    ).length;
    const rejectedCount = gateDecisions.filter(
      (d) => d.decision === "REJECT"
    ).length;
    const isCompleted = approvedCount >= (gate.minApprovals || 1);
    const hasRejection = rejectedCount > 0;

    if (hasRejection) {
      return <Cancel color="error" />;
    }
    if (isCompleted) {
      return <CheckCircle color="success" />;
    }
    return <AccessTime color="action" />;
  };

  const getGateStatus = (
    gate: ApprovalRequestApprovalGate
  ): "APPROVED" | "REJECTED" | "PENDING" => {
    const gateDecisions = decisions.filter((d) => d.gate === gate.gate) || [];
    const approvedCount = gateDecisions.filter(
      (d) => d.decision === "APPROVE"
    ).length;
    const rejectedCount = gateDecisions.filter(
      (d) => d.decision === "REJECT"
    ).length;
    const isCompleted = approvedCount >= (gate.minApprovals || 1);

    if (rejectedCount > 0) return "REJECTED";
    if (isCompleted) return "APPROVED";
    return "PENDING";
  };

  if (!requiredGates || requiredGates.length === 0) {
    return (
      <Card sx={{ mt: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Approval Progress
          </Typography>
          <Typography variant="body2" color="text.secondary">
            No approval gates required for this request.
          </Typography>
        </CardContent>
      </Card>
    );
  }

  if (decisionsError) {
    return (
      <Card sx={{ mt: 3 }}>
        <CardContent>
          <Alert severity="error">
            Failed to load approval decisions:{" "}
            {decisionsError instanceof Error
              ? decisionsError.message
              : "Unknown error"}
          </Alert>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card sx={{ mt: 3 }}>
      <CardContent>
        <Typography variant="h6" gutterBottom>
          Approval Progress
        </Typography>
        <Divider sx={{ mb: 2 }} />

        {decisionsLoading ? (
          <Box>
            <Skeleton variant="rectangular" height={200} />
          </Box>
        ) : (
          <Stepper orientation="vertical">
            {requiredGates.map((gate, index) => {
              const gateDecisions =
                decisions.filter((d) => d.gate === gate.gate) || [];
              const status = getGateStatus(gate);

              return (
                <Step
                  key={index}
                  active={status === "PENDING"}
                  completed={status === "APPROVED"}
                >
                  <StepLabel
                    slots={{ stepIcon: () => getStepIcon(gate) }}
                    error={status === "REJECTED"}
                  >
                    <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                      <Typography variant="subtitle1">
                        {gate.gate || `Gate ${index + 1}`}
                      </Typography>
                      <Chip
                        label={status}
                        size="small"
                        color={
                          status === "APPROVED"
                            ? "success"
                            : status === "REJECTED"
                            ? "error"
                            : "warning"
                        }
                      />
                    </Box>
                  </StepLabel>
                  <StepContent>
                    <Box sx={{ mt: 1 }}>
                      <Typography
                        variant="body2"
                        color="text.secondary"
                        gutterBottom
                      >
                        Minimum Approvals Required: {gate.minApprovals || 1}
                      </Typography>
                      <Typography
                        variant="body2"
                        color="text.secondary"
                        gutterBottom
                      >
                        Current Approvals:{" "}
                        {
                          gateDecisions.filter((d) => d.decision === "APPROVE")
                            .length
                        }
                      </Typography>

                      {gateDecisions.length > 0 && (
                        <Box sx={{ mt: 2 }}>
                          <Typography variant="subtitle2" gutterBottom>
                            Decisions:
                          </Typography>
                          {gateDecisions.map((decision) => {
                            const approverUserId = decision.approverUserId;
                            return (
                              <Box
                                key={
                                  decision.id ||
                                  `${decision.gate}-${decision.approverUserId}-${decision.decidedAt}`
                                }
                                sx={{ ml: 2, mb: 1 }}
                              >
                                <Box
                                  sx={{
                                    display: "flex",
                                    alignItems: "center",
                                    gap: 1,
                                    flexWrap: "wrap",
                                  }}
                                >
                                  <Chip
                                    label={decision.decision || "UNKNOWN"}
                                    size="small"
                                    color={
                                      decision.decision === "APPROVE"
                                        ? "success"
                                        : "error"
                                    }
                                  />
                                  <Typography variant="body2">by </Typography>
                                  {approverUserId ? (
                                    approversLoading ? (
                                      <Skeleton variant="text" width={100} />
                                    ) : (
                                      <UserInfoDisplay
                                        userId={approverUserId}
                                        mode="compact"
                                      />
                                    )
                                  ) : (
                                    <Typography variant="body2">
                                      Unknown
                                    </Typography>
                                  )}
                                  {decision.decidedAt && (
                                    <Typography
                                      variant="caption"
                                      color="text.secondary"
                                    >
                                      (
                                      {new Date(
                                        decision.decidedAt
                                      ).toLocaleString()}
                                      )
                                    </Typography>
                                  )}
                                </Box>
                                {decision.note && (
                                  <Typography
                                    variant="caption"
                                    color="text.secondary"
                                    display="block"
                                    sx={{ ml: 4, mt: 0.5 }}
                                  >
                                    Note: {decision.note}
                                  </Typography>
                                )}
                              </Box>
                            );
                          })}
                        </Box>
                      )}
                    </Box>
                  </StepContent>
                </Step>
              );
            })}
          </Stepper>
        )}
      </CardContent>
    </Card>
  );
};
