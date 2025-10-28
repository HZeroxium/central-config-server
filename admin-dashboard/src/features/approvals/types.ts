export interface ApprovalRequest {
  id: string;
  serviceId: string;
  serviceName: string;
  requestedBy: string;
  requestedByEmail: string;
  requestType:
    | "SERVICE_SHARE"
    | "CONFIG_CHANGE"
    | "DRIFT_RESOLUTION"
    | "SERVICE_DELETION";
  description: string;
  status: "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";
  priority: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
  createdAt: string;
  updatedAt: string;
  expiresAt?: string;
  requiredGates: ApprovalGate[];
  currentApprovals: Approval[];
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  metadata: Record<string, any>;
}

export interface ApprovalGate {
  gate: "SYS_ADMIN" | "LINE_MANAGER" | "TEAM_LEAD" | "SERVICE_OWNER";
  minApprovals: number;
  description: string;
}

export interface Approval {
  id: string;
  requestId: string;
  gate: string;
  approverId: string;
  approverName: string;
  approverEmail: string;
  decision: "APPROVED" | "REJECTED";
  note?: string;
  createdAt: string;
}

export interface ApprovalDecision {
  requestId: string;
  gate: string;
  decision: "APPROVED" | "REJECTED";
  note?: string;
}

export interface ApprovalRequestFilter {
  status?: "PENDING" | "APPROVED" | "REJECTED" | "CANCELLED";
  requestType?:
    | "SERVICE_SHARE"
    | "CONFIG_CHANGE"
    | "DRIFT_RESOLUTION"
    | "SERVICE_DELETION";
  priority?: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
  requestedBy?: string;
  serviceId?: string;
}

export const REQUEST_TYPES = {
  SERVICE_SHARE: "SERVICE_SHARE",
  CONFIG_CHANGE: "CONFIG_CHANGE",
  DRIFT_RESOLUTION: "DRIFT_RESOLUTION",
  SERVICE_DELETION: "SERVICE_DELETION",
} as const;

export const REQUEST_STATUSES = {
  PENDING: "PENDING",
  APPROVED: "APPROVED",
  REJECTED: "REJECTED",
  CANCELLED: "CANCELLED",
} as const;

export const PRIORITIES = {
  LOW: "LOW",
  MEDIUM: "MEDIUM",
  HIGH: "HIGH",
  CRITICAL: "CRITICAL",
} as const;

export const APPROVAL_GATES = {
  SYS_ADMIN: "SYS_ADMIN",
  LINE_MANAGER: "LINE_MANAGER",
  TEAM_LEAD: "TEAM_LEAD",
  SERVICE_OWNER: "SERVICE_OWNER",
} as const;

export type RequestType = (typeof REQUEST_TYPES)[keyof typeof REQUEST_TYPES];
export type RequestStatus =
  (typeof REQUEST_STATUSES)[keyof typeof REQUEST_STATUSES];
export type Priority = (typeof PRIORITIES)[keyof typeof PRIORITIES];
export type ApprovalGateType =
  (typeof APPROVAL_GATES)[keyof typeof APPROVAL_GATES];

export const REQUEST_TYPE_LABELS: Record<RequestType, string> = {
  [REQUEST_TYPES.SERVICE_SHARE]: "Service Share",
  [REQUEST_TYPES.CONFIG_CHANGE]: "Configuration Change",
  [REQUEST_TYPES.DRIFT_RESOLUTION]: "Drift Resolution",
  [REQUEST_TYPES.SERVICE_DELETION]: "Service Deletion",
};

export const STATUS_LABELS: Record<RequestStatus, string> = {
  [REQUEST_STATUSES.PENDING]: "Pending",
  [REQUEST_STATUSES.APPROVED]: "Approved",
  [REQUEST_STATUSES.REJECTED]: "Rejected",
  [REQUEST_STATUSES.CANCELLED]: "Cancelled",
};

export const PRIORITY_LABELS: Record<Priority, string> = {
  [PRIORITIES.LOW]: "Low",
  [PRIORITIES.MEDIUM]: "Medium",
  [PRIORITIES.HIGH]: "High",
  [PRIORITIES.CRITICAL]: "Critical",
};

export const GATE_LABELS: Record<ApprovalGateType, string> = {
  [APPROVAL_GATES.SYS_ADMIN]: "System Administrator",
  [APPROVAL_GATES.LINE_MANAGER]: "Line Manager",
  [APPROVAL_GATES.TEAM_LEAD]: "Team Lead",
  [APPROVAL_GATES.SERVICE_OWNER]: "Service Owner",
};

export const STATUS_COLORS: Record<RequestStatus, string> = {
  [REQUEST_STATUSES.PENDING]: "warning",
  [REQUEST_STATUSES.APPROVED]: "success",
  [REQUEST_STATUSES.REJECTED]: "error",
  [REQUEST_STATUSES.CANCELLED]: "default",
};

export const PRIORITY_COLORS: Record<Priority, string> = {
  [PRIORITIES.LOW]: "info",
  [PRIORITIES.MEDIUM]: "warning",
  [PRIORITIES.HIGH]: "error",
  [PRIORITIES.CRITICAL]: "error",
};
