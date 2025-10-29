import React, { useMemo } from "react";
import {
  Card,
  CardContent,
  Typography,
  Box,
  Divider,
  Skeleton,
  Alert,
} from "@mui/material";
import { CheckCircle, Cancel, AccessTime, Person } from "@mui/icons-material";
import { format } from "date-fns";
import type { ApprovalRequestResponse } from "@lib/api/models";
import { useFindApprovalDecisionsByRequestId } from "@lib/api/hooks";
import { useApproverInfo } from "../hooks/useApproverInfo";
import { UserInfoDisplay } from "@components/common/UserInfoDisplay";

interface DecisionTimelineProps {
  request: ApprovalRequestResponse;
  requestId: string;
}

export const DecisionTimeline: React.FC<DecisionTimelineProps> = ({
  request,
  requestId,
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

  const decisions = useMemo(
    () => decisionsData?.items || [],
    [decisionsData?.items]
  );

  // Extract approver user IDs
  const approverUserIds = useMemo(() => {
    return decisions
      .map((d) => d.approverUserId)
      .filter((id): id is string => !!id);
  }, [decisions]);

  const { isLoading: approversLoading } = useApproverInfo(approverUserIds);

  const getDecisionIcon = (decision?: string) => {
    switch (decision?.toUpperCase()) {
      case "APPROVE":
        return <CheckCircle color="success" />;
      case "REJECT":
        return <Cancel color="error" />;
      default:
        return <AccessTime color="action" />;
    }
  };

  const getDecisionColor = (
    decision?: string
  ): "success" | "error" | "primary" | "default" => {
    switch (decision?.toUpperCase()) {
      case "APPROVE":
        return "success";
      case "REJECT":
        return "error";
      default:
        return "default";
    }
  };

  const timelineEvents = useMemo(() => {
    interface TimelineEvent {
      id: string;
      timestamp?: string;
      title: string;
      description: string;
      icon: React.ReactNode;
      color: "success" | "error" | "primary" | "default";
      approverUserId?: string;
    }

    const events: TimelineEvent[] = [];

    // Add created event
    events.push({
      id: "created",
      timestamp: request.createdAt,
      title: "Request Created",
      description: `Request submitted by ${request.requesterUserId}`,
      icon: <Person color="primary" />,
      color: "primary",
    });

    // Add decision events
    decisions.forEach((decision, index) => {
      events.push({
        id: decision.id || `decision-${index}`,
        timestamp: decision.decidedAt,
        title: `${decision.decision || "UNKNOWN"} Decision`,
        description: decision.note || "No additional notes",
        approverUserId: decision.approverUserId,
        icon: getDecisionIcon(decision.decision),
        color: getDecisionColor(decision.decision),
      });
    });

    // Add final status if completed
    if (request.status !== "PENDING") {
      events.push({
        id: "final-status",
        timestamp: request.updatedAt || request.createdAt,
        title: `Request ${request.status}`,
        description:
          request.status === "APPROVED"
            ? "Request has been approved and processed"
            : request.status === "REJECTED"
            ? "Request has been rejected"
            : "Request has been cancelled",
        icon: getDecisionIcon(request.status),
        color: getDecisionColor(request.status),
      });
    }

    return events.sort(
      (a, b) =>
        new Date(a.timestamp || "").getTime() -
        new Date(b.timestamp || "").getTime()
    );
  }, [request, decisions]);

  if (decisionsError) {
    return (
      <Card sx={{ mt: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Decision Timeline
          </Typography>
          <Alert severity="error">
            Failed to load decisions:{" "}
            {decisionsError instanceof Error
              ? decisionsError.message
              : "Unknown error"}
          </Alert>
        </CardContent>
      </Card>
    );
  }

  if (decisionsLoading) {
    return (
      <Card sx={{ mt: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Decision Timeline
          </Typography>
          <Box sx={{ mt: 2, display: "flex", flexDirection: "column", gap: 2 }}>
            {[1, 2, 3].map((i) => (
              <Skeleton key={i} variant="rectangular" height={60} />
            ))}
          </Box>
        </CardContent>
      </Card>
    );
  }

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
          {timelineEvents.map((event) => {
            const approverUserId = event.approverUserId;
            return (
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
                      flexWrap: "wrap",
                      gap: 1,
                    }}
                  >
                    <Typography variant="subtitle1">{event.title}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {event.timestamp
                        ? format(
                            new Date(event.timestamp),
                            "MMM dd, yyyy HH:mm"
                          )
                        : "Unknown"}
                    </Typography>
                  </Box>
                  <Box sx={{ mt: 0.5 }}>
                    {approverUserId && (
                      <Box
                        sx={{
                          display: "flex",
                          alignItems: "center",
                          gap: 1,
                          mb: 0.5,
                        }}
                      >
                        <Typography variant="body2" color="text.secondary">
                          Approver:{" "}
                        </Typography>
                        {approversLoading ? (
                          <Skeleton variant="text" width={100} />
                        ) : (
                          <UserInfoDisplay
                            userId={approverUserId}
                            mode="compact"
                          />
                        )}
                      </Box>
                    )}
                    <Typography variant="body2" color="text.secondary">
                      {event.description}
                    </Typography>
                  </Box>
                  <Divider sx={{ mt: 1 }} />
                </Box>
              </Box>
            );
          })}
        </Box>
      </CardContent>
    </Card>
  );
};
