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
} from "@mui/material";
import { CheckCircle, Cancel, AccessTime } from "@mui/icons-material";
import type {
  ApprovalRequestResponse,
  ApprovalRequestApprovalGate,
} from "@lib/api/models";

interface ApprovalDecision {
  gate?: string;
  decision: string;
  decidedBy?: string;
  note?: string;
  decidedAt?: string;
}

interface ApprovalStepperProps {
  request: ApprovalRequestResponse;
}

export const ApprovalStepper: React.FC<ApprovalStepperProps> = ({
  request,
}) => {
  // Extract decisions from request metadata or empty array
  const decisions: ApprovalDecision[] =
    (request && typeof request === "object" && "decisions" in request
      ? (request as { decisions?: ApprovalDecision[] }).decisions
      : []) || [];

  const getStepIcon = (gate: ApprovalRequestApprovalGate) => {
    // Check if this gate has been completed
    const gateDecisions = decisions.filter((d) => d.gate === gate.gate) || [];
    const isCompleted = gateDecisions.length >= (gate.minApprovals || 1);
    const hasRejection = gateDecisions.some((d) => d.decision === "REJECTED");

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
    const isCompleted = gateDecisions.length >= (gate.minApprovals || 1);
    const hasRejection = gateDecisions.some((d) => d.decision === "REJECTED");

    if (hasRejection) return "REJECTED";
    if (isCompleted) return "APPROVED";
    return "PENDING";
  };

  if (!request.required || request.required.length === 0) {
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

  return (
    <Card sx={{ mt: 3 }}>
      <CardContent>
        <Typography variant="h6" gutterBottom>
          Approval Progress
        </Typography>
        <Divider sx={{ mb: 2 }} />

        <Stepper orientation="vertical">
          {request.required.map((gate, index) => {
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
                        gateDecisions.filter((d) => d.decision === "APPROVED")
                          .length
                      }
                    </Typography>

                    {gateDecisions.length > 0 && (
                      <Box sx={{ mt: 2 }}>
                        <Typography variant="subtitle2" gutterBottom>
                          Decisions:
                        </Typography>
                        {gateDecisions.map((decision) => (
                          <Box
                            key={`${decision.gate}-${decision.decidedBy}-${decision.decidedAt}`}
                            sx={{ ml: 2, mb: 1 }}
                          >
                            <Typography variant="body2">
                              <Chip
                                label={decision.decision}
                                size="small"
                                color={
                                  decision.decision === "APPROVED"
                                    ? "success"
                                    : "error"
                                }
                                sx={{ mr: 1 }}
                              />
                              by {decision.decidedBy}
                              {decision.note && (
                                <Typography
                                  variant="caption"
                                  color="text.secondary"
                                  display="block"
                                >
                                  Note: {decision.note}
                                </Typography>
                              )}
                            </Typography>
                          </Box>
                        ))}
                      </Box>
                    )}
                  </Box>
                </StepContent>
              </Step>
            );
          })}
        </Stepper>
      </CardContent>
    </Card>
  );
};
