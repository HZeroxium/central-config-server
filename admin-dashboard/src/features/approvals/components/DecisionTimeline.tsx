import React from "react";
import { Card, CardContent, Typography, Box, Divider } from "@mui/material";
import { CheckCircle, Cancel, AccessTime, Person } from "@mui/icons-material";
import { format } from "date-fns";
import type { ApprovalRequestResponse } from "@lib/api/models";

interface DecisionTimelineProps {
  request: ApprovalRequestResponse;
}

export const DecisionTimeline: React.FC<DecisionTimelineProps> = ({
  request,
}) => {
  const getDecisionIcon = (decision: string) => {
    switch (decision?.toUpperCase()) {
      case "APPROVED":
        return <CheckCircle color="success" />;
      case "REJECTED":
        return <Cancel color="error" />;
      default:
        return <AccessTime color="action" />;
    }
  };

  const getDecisionColor = (decision: string) => {
    switch (decision?.toUpperCase()) {
      case "APPROVED":
        return "success";
      case "REJECTED":
        return "error";
      default:
        return "default";
    }
  };

  // Type for decision objects (since not available in generated types yet)
  interface Decision {
    decidedAt?: string;
    decision: string;
    decidedBy?: string;
    note?: string;
  }

  // Create timeline events from request data
  const decisions = (request as { decisions?: Decision[] }).decisions || [];

  const timelineEvents = [
    {
      id: "created",
      timestamp: request.createdAt,
      title: "Request Created",
      description: `Request submitted by ${request.requesterUserId}`,
      icon: <Person color="primary" />,
      color: "primary" as const,
    },
    // Add decision events if they exist
    ...decisions.map((decision, index) => ({
      id: `decision-${index}`,
      timestamp: decision.decidedAt,
      title: `${decision.decision} by ${decision.decidedBy}`,
      description: decision.note || "No additional notes",
      icon: getDecisionIcon(decision.decision),
      color: getDecisionColor(decision.decision),
    })),
    // Add final status if completed
    ...(request.status !== "PENDING"
      ? [
          {
            id: "final-status",
            timestamp: request.updatedAt || request.createdAt,
            title: `Request ${request.status}`,
            description:
              request.status === "APPROVED"
                ? "Request has been approved and processed"
                : request.status === "REJECTED"
                ? "Request has been rejected"
                : "Request has been cancelled",
            icon: getDecisionIcon(request.status || "PENDING"),
            color: getDecisionColor(request.status || "PENDING"),
          },
        ]
      : []),
  ].sort(
    (a, b) =>
      new Date(a.timestamp || "").getTime() -
      new Date(b.timestamp || "").getTime()
  );

  if (timelineEvents.length === 0) {
    return (
      <Card sx={{ mt: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Decision Timeline
          </Typography>
          <Typography variant="body2" color="text.secondary">
            No timeline events available.
          </Typography>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card sx={{ mt: 3 }}>
      <CardContent>
        <Typography variant="h6" gutterBottom>
          Decision Timeline
        </Typography>
        <Box sx={{ mt: 2, display: "flex", flexDirection: "column", gap: 2 }}>
          {timelineEvents.map((event) => (
            <Box
              key={event.id}
              sx={{ display: "flex", alignItems: "flex-start", gap: 2 }}
            >
              <Box sx={{ mt: "2px" }}>{event.icon}</Box>
              <Box sx={{ flex: 1 }}>
                <Box
                  sx={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "baseline",
                  }}
                >
                  <Typography variant="subtitle1">{event.title}</Typography>
                  <Typography variant="caption" color="text.secondary">
                    {event.timestamp
                      ? format(new Date(event.timestamp), "MMM dd, yyyy HH:mm")
                      : "Unknown"}
                  </Typography>
                </Box>
                <Typography variant="body2" color="text.secondary">
                  {event.description}
                </Typography>
                <Divider sx={{ mt: 1 }} />
              </Box>
            </Box>
          ))}
        </Box>
      </CardContent>
    </Card>
  );
};
