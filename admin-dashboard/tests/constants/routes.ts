/**
 * Route paths and metadata for the application
 */

export interface RouteMetadata {
  path: string;
  label: string;
  requiresAdmin?: boolean;
  requiresRoute?: string;
}

/**
 * All routes in the application
 */
export const ROUTES = {
  DASHBOARD: '/dashboard',
  APPLICATION_SERVICES: '/application-services',
  APPLICATION_SERVICE_DETAIL: (id: string) => `/application-services/${id}`,
  SERVICE_INSTANCES: '/service-instances',
  SERVICE_INSTANCE_DETAIL: (serviceName: string, instanceId: string) =>
    `/service-instances/${serviceName}/${instanceId}`,
  REGISTRY: '/registry',
  REGISTRY_DETAIL: (serviceName: string) => `/registry/${serviceName}`,
  CONFIGS: '/configs',
  CONFIG_DETAIL: (application: string, profile: string) =>
    `/configs/${encodeURIComponent(application)}/${encodeURIComponent(profile)}`,
  KV_STORE: '/kv',
  KV_STORE_DETAIL: (serviceId: string) => `/kv/${serviceId}`,
  APPROVALS: '/approvals',
  APPROVAL_DETAIL: (id: string) => `/approvals/${id}`,
  APPROVAL_DECISIONS: '/approval-decisions',
  APPROVAL_DECISION_DETAIL: (id: string) => `/approval-decisions/${id}`,
  DRIFT_EVENTS: '/drift-events',
  DRIFT_EVENT_DETAIL: (id: string) => `/drift-events/${id}`,
  SERVICE_SHARES: '/service-shares',
  SERVICE_SHARE_DETAIL: (id: string) => `/service-shares/${id}`,
  IAM_USERS: '/iam/users',
  IAM_TEAMS: '/iam/teams',
  PROFILE: '/profile',
  UNAUTHORIZED: '/unauthorized',
  LOGIN_CALLBACK: '/login-callback',
} as const;

/**
 * Routes accessible by admin only
 */
export const ADMIN_ONLY_ROUTES: string[] = [
  ROUTES.IAM_USERS,
  ROUTES.IAM_TEAMS,
];

/**
 * Routes accessible by all authenticated users
 */
export const PUBLIC_ROUTES: string[] = [
  ROUTES.DASHBOARD,
  ROUTES.APPLICATION_SERVICES,
  ROUTES.SERVICE_INSTANCES,
  ROUTES.REGISTRY,
  ROUTES.CONFIGS,
  ROUTES.KV_STORE,
  ROUTES.APPROVALS,
  ROUTES.APPROVAL_DECISIONS,
  ROUTES.DRIFT_EVENTS,
  ROUTES.SERVICE_SHARES,
  ROUTES.PROFILE,
];

/**
 * All routes metadata
 */
export const ROUTE_METADATA: RouteMetadata[] = [
  { path: ROUTES.DASHBOARD, label: 'Dashboard' },
  { path: ROUTES.APPLICATION_SERVICES, label: 'Application Services' },
  { path: ROUTES.SERVICE_INSTANCES, label: 'Service Instances' },
  { path: ROUTES.REGISTRY, label: 'Service Registry' },
  { path: ROUTES.CONFIGS, label: 'Config Server' },
  { path: ROUTES.KV_STORE, label: 'Key-Value Store' },
  { path: ROUTES.APPROVALS, label: 'Approvals' },
  { path: ROUTES.APPROVAL_DECISIONS, label: 'Approval Decisions' },
  { path: ROUTES.DRIFT_EVENTS, label: 'Drift Events' },
  { path: ROUTES.SERVICE_SHARES, label: 'Service Shares' },
  { path: ROUTES.IAM_USERS, label: 'IAM Users', requiresAdmin: true },
  { path: ROUTES.IAM_TEAMS, label: 'IAM Teams', requiresAdmin: true },
  { path: ROUTES.PROFILE, label: 'Profile' },
];

/**
 * Check if route requires admin access
 */
export function isAdminOnlyRoute(path: string): boolean {
  return ADMIN_ONLY_ROUTES.some(route => path.startsWith(route));
}

/**
 * Get routes accessible by a specific role
 */
export function getAccessibleRoutes(isAdmin: boolean): string[] {
  if (isAdmin) {
    return [...PUBLIC_ROUTES, ...ADMIN_ONLY_ROUTES];
  }
  return PUBLIC_ROUTES;
}

