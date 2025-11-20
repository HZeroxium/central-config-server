/// <reference types="node" />
import * as path from 'node:path';
import { ROUTES } from '../constants/routes';
import type { ModuleGroup } from './types';

/**
 * Recording-specific constants
 */

export const RECORDING_OUTPUT_DIR = path.join(process.cwd(), 'test-results', 'recordings');
export const SCREENSHOTS_DIR = 'screenshots';
export const TUTORIALS_DIR = 'tutorials';

/**
 * Module groupings for organizing output
 */
export const MODULE_GROUPS: ModuleGroup[] = [
  {
    name: 'dashboard',
    label: 'Dashboard',
    modules: ['dashboard'],
  },
  {
    name: 'services',
    label: 'Services',
    modules: ['application-services', 'service-instances', 'registry'],
  },
  {
    name: 'configs',
    label: 'Configuration',
    modules: ['configs', 'kv'],
  },
  {
    name: 'governance',
    label: 'Governance',
    modules: ['approvals', 'approval-decisions', 'drift-events', 'service-shares'],
  },
  {
    name: 'iam',
    label: 'IAM',
    modules: ['iam-users', 'iam-teams'],
  },
  {
    name: 'profile',
    label: 'Profile',
    modules: ['profile'],
  },
];

/**
 * Module metadata with routes and descriptions
 */
export const MODULE_METADATA: Record<string, { route: string; label: string; description: string }> = {
  dashboard: {
    route: ROUTES.DASHBOARD,
    label: 'Dashboard',
    description: 'Overview dashboard with statistics and key metrics',
  },
  'application-services': {
    route: ROUTES.APPLICATION_SERVICES,
    label: 'Application Services',
    description: 'Manage application services, view details, and configure ownership',
  },
  'service-instances': {
    route: ROUTES.SERVICE_INSTANCES,
    label: 'Service Instances',
    description: 'View and manage service instances, check status and drift',
  },
  registry: {
    route: ROUTES.REGISTRY,
    label: 'Service Registry',
    description: 'Browse registered services in the discovery system',
  },
  configs: {
    route: ROUTES.CONFIGS,
    label: 'Config Server',
    description: 'Search and view configuration files for applications',
  },
  kv: {
    route: ROUTES.KV_STORE,
    label: 'Key-Value Store',
    description: 'Manage key-value pairs for services',
  },
  approvals: {
    route: ROUTES.APPROVALS,
    label: 'Approvals',
    description: 'View and manage approval requests for service ownership',
  },
  'approval-decisions': {
    route: ROUTES.APPROVAL_DECISIONS,
    label: 'Approval Decisions',
    description: 'View history of approval decisions',
  },
  'drift-events': {
    route: ROUTES.DRIFT_EVENTS,
    label: 'Drift Events',
    description: 'Monitor configuration drift events',
  },
  'service-shares': {
    route: ROUTES.SERVICE_SHARES,
    label: 'Service Shares',
    description: 'Manage service sharing permissions',
  },
  'iam-users': {
    route: ROUTES.IAM_USERS,
    label: 'IAM Users',
    description: 'Manage IAM users (Admin only)',
  },
  'iam-teams': {
    route: ROUTES.IAM_TEAMS,
    label: 'IAM Teams',
    description: 'Manage IAM teams (Admin only)',
  },
  profile: {
    route: ROUTES.PROFILE,
    label: 'Profile',
    description: 'View and manage user profile',
  },
};

/**
 * Recording delays (in milliseconds)
 */
export const RECORDING_DELAYS = {
  PAGE_LOAD: 2000,
  INTERACTION: 500,
  SCREENSHOT: 300,
  NAVIGATION: 1000,
} as const;

/**
 * Get output path for a specific role and module
 */
export function getModuleOutputPath(role: 'admin' | 'user', moduleName: string): string {
  return path.join(RECORDING_OUTPUT_DIR, role, moduleName);
}

/**
 * Get screenshots directory for a module
 */
export function getScreenshotsPath(role: 'admin' | 'user', moduleName: string): string {
  return path.join(getModuleOutputPath(role, moduleName), SCREENSHOTS_DIR);
}

