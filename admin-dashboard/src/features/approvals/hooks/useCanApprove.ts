import { useMemo } from 'react';
import { useAuth } from '@features/auth/authContext';
import type { ApprovalRequestResponse } from '@lib/api/models';

interface ApprovalDecision {
  gate?: string;
  decision: 'APPROVED' | 'REJECTED' | string;
  decidedBy?: string;
  note?: string;
  decidedAt?: string;
}

export function useCanApprove(request: ApprovalRequestResponse | undefined) {
  const { userInfo, isSysAdmin } = useAuth();

  return useMemo(() => {
    if (!request || !userInfo) {
      return {
        canApprove: false,
        eligibleGates: [],
        reason: 'Not authenticated',
      };
    }

    // Can't approve if request is not pending
    if (request.status !== 'PENDING') {
      return {
        canApprove: false,
        eligibleGates: [],
        reason: 'Request is not pending',
      };
    }

    const eligibleGates: string[] = [];
    
    // Extract decisions from request metadata or empty array
    const decisions: ApprovalDecision[] = (request && typeof request === 'object' && 'decisions' in request 
      ? (request as { decisions?: ApprovalDecision[] }).decisions 
      : []) || [];

    // Check each required gate
    if (request.required) {
      for (const gate of request.required) {
        const gateDecisions = decisions.filter((d: ApprovalDecision) => d.gate === gate.gate) || [];
        const currentApprovals = gateDecisions.filter((d: ApprovalDecision) => d.decision === 'APPROVED').length;
        const hasRejection = gateDecisions.some((d: ApprovalDecision) => d.decision === 'REJECTED');
        
        // Skip if already rejected or completed
        if (hasRejection || currentApprovals >= (gate.minApprovals || 1)) {
          continue;
        }

        // Check if user can approve this gate
        let canApproveGate = false;

        // SYS_ADMIN can approve any gate
        if (isSysAdmin) {
          canApproveGate = true;
        }
        // Check if user is the manager of the requester
        else if (gate.gate === 'LINE_MANAGER' && userInfo.managerId) {
          // This would need to be implemented based on your business logic
          // For now, we'll assume any authenticated user can approve LINE_MANAGER gates
          canApproveGate = true;
        }
        // Add other gate-specific logic here
        else if (gate.gate === 'SYS_ADMIN') {
          canApproveGate = isSysAdmin;
        }

        if (canApproveGate) {
          eligibleGates.push(gate.gate || 'Unknown Gate');
        }
      }
    }

    return {
      canApprove: eligibleGates.length > 0,
      eligibleGates,
      reason: eligibleGates.length === 0 ? 'No eligible gates for approval' : undefined,
    };
  }, [request, userInfo, isSysAdmin]);
}
