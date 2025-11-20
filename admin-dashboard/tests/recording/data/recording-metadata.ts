import { ROUTES } from '../../constants/routes';
import type { RecordingStep } from '../types';

/**
 * Metadata about recording steps for each module
 */

export interface ModuleRecordingPlan {
  moduleName: string;
  steps: Omit<RecordingStep, 'stepNumber' | 'timestamp' | 'screenshotPath'>[];
}

/**
 * Recording plans for admin role
 */
export const ADMIN_RECORDING_PLANS: ModuleRecordingPlan[] = [
  {
    moduleName: 'dashboard',
    steps: [
      {
        name: 'dashboard-overview',
        description: 'View dashboard overview with statistics',
        module: 'dashboard',
        route: ROUTES.DASHBOARD,
        interactions: ['view-stats', 'view-charts'],
      },
    ],
  },
  {
    moduleName: 'application-services',
    steps: [
      {
        name: 'services-list',
        description: 'View application services list',
        module: 'application-services',
        route: ROUTES.APPLICATION_SERVICES,
        interactions: ['view-table'],
      },
      {
        name: 'services-search',
        description: 'Search for services',
        module: 'application-services',
        route: ROUTES.APPLICATION_SERVICES,
        interactions: ['search', 'filter'],
      },
      {
        name: 'service-detail',
        description: 'View service detail page',
        module: 'application-services',
        route: ROUTES.APPLICATION_SERVICES,
        interactions: ['navigate-to-detail'],
      },
    ],
  },
  {
    moduleName: 'service-instances',
    steps: [
      {
        name: 'instances-list',
        description: 'View service instances list',
        module: 'service-instances',
        route: ROUTES.SERVICE_INSTANCES,
        interactions: ['view-table'],
      },
      {
        name: 'instances-filter',
        description: 'Filter instances by status',
        module: 'service-instances',
        route: ROUTES.SERVICE_INSTANCES,
        interactions: ['filter-by-status'],
      },
      {
        name: 'instance-detail',
        description: 'View instance detail',
        module: 'service-instances',
        route: ROUTES.SERVICE_INSTANCES,
        interactions: ['navigate-to-detail'],
      },
    ],
  },
  {
    moduleName: 'registry',
    steps: [
      {
        name: 'registry-overview',
        description: 'View service registry',
        module: 'registry',
        route: ROUTES.REGISTRY,
        interactions: ['view-registry'],
      },
    ],
  },
  {
    moduleName: 'configs',
    steps: [
      {
        name: 'configs-search',
        description: 'Search for configuration',
        module: 'configs',
        route: ROUTES.CONFIGS,
        interactions: ['search-config'],
      },
      {
        name: 'config-detail',
        description: 'View configuration detail',
        module: 'configs',
        route: ROUTES.CONFIGS,
        interactions: ['view-config-detail'],
      },
    ],
  },
  {
    moduleName: 'kv',
    steps: [
      {
        name: 'kv-store-list',
        description: 'View key-value store list',
        module: 'kv',
        route: ROUTES.KV_STORE,
        interactions: ['view-kv-list'],
      },
    ],
  },
  {
    moduleName: 'approvals',
    steps: [
      {
        name: 'approvals-list',
        description: 'View approvals list',
        module: 'approvals',
        route: ROUTES.APPROVALS,
        interactions: ['view-table'],
      },
      {
        name: 'approval-detail',
        description: 'View approval detail',
        module: 'approvals',
        route: ROUTES.APPROVALS,
        interactions: ['navigate-to-detail'],
      },
    ],
  },
  {
    moduleName: 'approval-decisions',
    steps: [
      {
        name: 'decisions-list',
        description: 'View approval decisions list',
        module: 'approval-decisions',
        route: ROUTES.APPROVAL_DECISIONS,
        interactions: ['view-table'],
      },
    ],
  },
  {
    moduleName: 'drift-events',
    steps: [
      {
        name: 'drift-events-list',
        description: 'View drift events list',
        module: 'drift-events',
        route: ROUTES.DRIFT_EVENTS,
        interactions: ['view-table'],
      },
      {
        name: 'drift-filter',
        description: 'Filter drift events',
        module: 'drift-events',
        route: ROUTES.DRIFT_EVENTS,
        interactions: ['filter-by-status'],
      },
    ],
  },
  {
    moduleName: 'service-shares',
    steps: [
      {
        name: 'shares-list',
        description: 'View service shares list',
        module: 'service-shares',
        route: ROUTES.SERVICE_SHARES,
        interactions: ['view-table'],
      },
    ],
  },
  {
    moduleName: 'iam-users',
    steps: [
      {
        name: 'iam-users-list',
        description: 'View IAM users list (Admin only)',
        module: 'iam-users',
        route: ROUTES.IAM_USERS,
        interactions: ['view-table'],
      },
    ],
  },
  {
    moduleName: 'iam-teams',
    steps: [
      {
        name: 'iam-teams-list',
        description: 'View IAM teams list (Admin only)',
        module: 'iam-teams',
        route: ROUTES.IAM_TEAMS,
        interactions: ['view-table'],
      },
    ],
  },
  {
    moduleName: 'profile',
    steps: [
      {
        name: 'profile-view',
        description: 'View user profile',
        module: 'profile',
        route: ROUTES.PROFILE,
        interactions: ['view-profile'],
      },
    ],
  },
];

/**
 * Recording plans for user role (public routes only)
 */
export const USER_RECORDING_PLANS: ModuleRecordingPlan[] = [
  {
    moduleName: 'dashboard',
    steps: [
      {
        name: 'dashboard-overview',
        description: 'View dashboard overview',
        module: 'dashboard',
        route: ROUTES.DASHBOARD,
        interactions: ['view-stats'],
      },
    ],
  },
  {
    moduleName: 'application-services',
    steps: [
      {
        name: 'services-list',
        description: 'View application services list (public)',
        module: 'application-services',
        route: ROUTES.APPLICATION_SERVICES,
        interactions: ['view-table'],
      },
    ],
  },
  {
    moduleName: 'service-instances',
    steps: [
      {
        name: 'instances-list',
        description: 'View service instances list',
        module: 'service-instances',
        route: ROUTES.SERVICE_INSTANCES,
        interactions: ['view-table'],
      },
    ],
  },
  {
    moduleName: 'configs',
    steps: [
      {
        name: 'configs-search',
        description: 'Search for configuration',
        module: 'configs',
        route: ROUTES.CONFIGS,
        interactions: ['search-config'],
      },
    ],
  },
  {
    moduleName: 'profile',
    steps: [
      {
        name: 'profile-view',
        description: 'View user profile',
        module: 'profile',
        route: ROUTES.PROFILE,
        interactions: ['view-profile'],
      },
    ],
  },
];

