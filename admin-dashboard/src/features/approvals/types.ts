import { z } from 'zod';

// ApprovalTarget schema matching backend
export const ApprovalTargetSchema = z.object({
  serviceId: z.string(),
  teamId: z.string(),
});

// ApprovalGate schema matching backend
export const ApprovalGateSchema = z.object({
  gate: z.string(),
  minApprovals: z.number(),
});

// RequesterSnapshot schema matching backend
export const RequesterSnapshotSchema = z.object({
  teamIds: z.array(z.string()),
  managerId: z.string().optional(),
  roles: z.array(z.string()),
});

export const ApprovalRequestSchema = z.object({
  id: z.string(),
  requesterUserId: z.string(),
  requestType: z.enum(['ASSIGN_SERVICE_TO_TEAM', 'SERVICE_OWNERSHIP_TRANSFER']),
  target: ApprovalTargetSchema,
  required: z.array(ApprovalGateSchema),
  status: z.enum(['PENDING', 'APPROVED', 'REJECTED', 'CANCELLED']),
  snapshot: RequesterSnapshotSchema,
  counts: z.record(z.string(), z.number()),
  createdAt: z.string(),
  updatedAt: z.string(),
  note: z.string().optional(),
  cancelReason: z.string().optional(),
});

export const CreateApprovalRequestSchema = z.object({
  serviceId: z.string().min(1, 'Service ID is required'),
  targetTeamId: z.string().min(1, 'Target team ID is required'),
  note: z.string().optional(),
});

export const DecisionRequestSchema = z.object({
  decision: z.enum(['APPROVE', 'REJECT']),
  note: z.string().optional(),
});

export type ApprovalRequest = z.infer<typeof ApprovalRequestSchema>;
export type CreateApprovalRequestRequest = z.infer<typeof CreateApprovalRequestSchema>;
export type DecisionRequest = z.infer<typeof DecisionRequestSchema>;

export interface ApprovalRequestQueryFilter {
  page?: number;
  size?: number;
  sort?: string;
  status?: string;
  requestType?: string;
  requesterUserId?: string;
}
