import { useMemo } from "react";
import { useAuth } from "@features/auth/context";
import type { ApprovalRequestResponse } from "@lib/api/models";

interface ApprovalDecision {
  gate?: string;
  decision: "APPROVED" | "REJECTED";
  decidedBy?: string;
  note?: string;
  decidedAt?: string;
}

function extractDecisions(
  request: ApprovalRequestResponse
): ApprovalDecision[] {
  return (
    (request && typeof request === "object" && "decisions" in request
      ? (request as { decisions?: ApprovalDecision[] }).decisions
      : []) || []
  );
}

function isGateCompleted(
  gate: { gate?: string; minApprovals?: number },
  decisions: ApprovalDecision[]
): boolean {
  const gateDecisions = decisions.filter((d) => d.gate === gate.gate) || [];
  const currentApprovals = gateDecisions.filter(
    (d) => d.decision === "APPROVED"
  ).length;
  const hasRejection = gateDecisions.some((d) => d.decision === "REJECTED");

  return hasRejection || currentApprovals >= (gate.minApprovals || 1);
}

function canUserApproveGate(
  gate: { gate?: string },
  isSysAdmin: boolean,
  userInfo: { managerId?: string }
): boolean {
  if (isSysAdmin) {
    return true;
  }

  if (gate.gate === "LINE_MANAGER" && userInfo.managerId) {
    return true;
  }

  if (gate.gate === "SYS_ADMIN") {
    return isSysAdmin;
  }

  return false;
}

export function useCanApprove(request: ApprovalRequestResponse | undefined) {
  const { userInfo, isSysAdmin } = useAuth();

  return useMemo(() => {
    if (!request || !userInfo) {
      return {
        canApprove: false,
        eligibleGates: [],
        reason: "Not authenticated",
      };
    }

    if (request.status !== "PENDING") {
      return {
        canApprove: false,
        eligibleGates: [],
        reason: "Request is not pending",
      };
    }

    const eligibleGates: string[] = [];
    const decisions = extractDecisions(request);

    if (request.required) {
      for (const gate of request.required) {
        if (isGateCompleted(gate, decisions)) {
          continue;
        }

        if (canUserApproveGate(gate, isSysAdmin, userInfo)) {
          eligibleGates.push(gate.gate || "Unknown Gate");
        }
      }
    }

    return {
      canApprove: eligibleGates.length > 0,
      eligibleGates,
      reason:
        eligibleGates.length === 0
          ? "No eligible gates for approval"
          : undefined,
    };
  }, [request, userInfo, isSysAdmin]);
}
