export interface ServiceShare {
  id: string;
  serviceId: string;
  serviceName: string;
  grantedTo: "TEAM" | "USER";
  grantedToId: string;
  grantedToName: string;
  permissions: string[];
  environments: string[];
  expiresAt?: string;
  createdAt: string;
  createdBy: string;
  status: "ACTIVE" | "EXPIRED" | "REVOKED";
}

export interface CreateServiceShareRequest {
  serviceId: string;
  grantedTo: "TEAM" | "USER";
  grantedToId: string;
  permissions: string[];
  environments: string[];
  expiresAt?: string;
  note?: string;
}

export interface UpdateServiceShareRequest {
  permissions?: string[];
  environments?: string[];
  expiresAt?: string;
  note?: string;
}

export interface ServiceShareFilter {
  serviceId?: string;
  grantedTo?: "TEAM" | "USER";
  grantedToId?: string;
  status?: "ACTIVE" | "EXPIRED" | "REVOKED";
  environment?: string;
  permission?: string;
}

export const PERMISSIONS = {
  VIEW_INSTANCE: "VIEW_INSTANCE",
  EDIT_INSTANCE: "EDIT_INSTANCE",
  VIEW_DRIFT: "VIEW_DRIFT",
  RESOLVE_DRIFT: "RESOLVE_DRIFT",
  VIEW_CONFIG: "VIEW_CONFIG",
  EDIT_CONFIG: "EDIT_CONFIG",
  VIEW_LOGS: "VIEW_LOGS",
  MANAGE_SHARES: "MANAGE_SHARES",
} as const;

export const ENVIRONMENTS = {
  DEV: "dev",
  STAGING: "staging",
  PROD: "prod",
  TEST: "test",
} as const;

export type Permission = (typeof PERMISSIONS)[keyof typeof PERMISSIONS];
export type Environment = (typeof ENVIRONMENTS)[keyof typeof ENVIRONMENTS];

export const PERMISSION_LABELS: Record<Permission, string> = {
  [PERMISSIONS.VIEW_INSTANCE]: "View Instances",
  [PERMISSIONS.EDIT_INSTANCE]: "Edit Instances",
  [PERMISSIONS.VIEW_DRIFT]: "View Drift Events",
  [PERMISSIONS.RESOLVE_DRIFT]: "Resolve Drift Events",
  [PERMISSIONS.VIEW_CONFIG]: "View Configuration",
  [PERMISSIONS.EDIT_CONFIG]: "Edit Configuration",
  [PERMISSIONS.VIEW_LOGS]: "View Logs",
  [PERMISSIONS.MANAGE_SHARES]: "Manage Shares",
};

export const ENVIRONMENT_LABELS: Record<Environment, string> = {
  [ENVIRONMENTS.DEV]: "Development",
  [ENVIRONMENTS.STAGING]: "Staging",
  [ENVIRONMENTS.PROD]: "Production",
  [ENVIRONMENTS.TEST]: "Test",
};

export const ENVIRONMENT_COLORS: Record<Environment, string> = {
  [ENVIRONMENTS.DEV]: "#4caf50",
  [ENVIRONMENTS.STAGING]: "#ff9800",
  [ENVIRONMENTS.PROD]: "#f44336",
  [ENVIRONMENTS.TEST]: "#9c27b0",
};
