// src/lib/api/hooks.ts

/**
 * Central export file for all API hooks.
 * Re-exports generated hooks from orval with convenient aliases.
 */

// Import hooks from generated files
import { 
  useFindAllApplicationServices,
  useCreateApplicationService,
  useUpdateApplicationService,
  useDeleteApplicationService,
  useFindApplicationServiceById,
} from './generated/application-services/application-services';

import { 
  useFindAllServiceInstances,
  useUpdateServiceInstance,
  useDeleteServiceInstance,
  useFindByIdServiceInstance,
} from './generated/service-instances/service-instances';

import { 
  useFindAllDriftEvents,
  useCreateDriftEvent,
  useUpdateDriftEvent,
  useFindDriftEventById,
} from './generated/drift-events/drift-events';

import { 
  useFindAllApprovalRequests,
  useCreateApprovalRequest,
  useSubmitApprovalDecision,
  useFindApprovalRequestById,
  useCancelApprovalRequest,
} from './generated/approval-requests/approval-requests';

import { 
  useFindAllServiceSharesForService1,
  useGrantServiceShare,
  useRevokeServiceShare,
  useFindByIdServiceShare,
} from './generated/service-shares/service-shares';

import { 
  useGetEnvironmentConfigServer,
  useGetHealthConfigServer,
  useGetInfoConfigServer,
} from './generated/config-server/config-server';

import { 
  useListServiceRegistryServices,
  useGetServiceRegistryServiceInstances,
} from './generated/service-registry/service-registry';

import { 
  useFindCurrentUserInformation,
  useFindCurrentUserPermissions,
} from './generated/user-management/user-management';

import {
  useFindAllIamTeams,
  useFindByIdIamTeam,
  useFindByMemberIamTeam,
  useGetStatsIamTeam,
} from './generated/iam-teams/iam-teams';

import {
  useFindAllIamUsers,
  useFindByIdIamUser,
  useFindByTeamIamUser,
  useFindByManagerIamUser,
  useGetStatsIamUser,
} from './generated/iam-users/iam-users';

// ============================================================================
// Re-export all hooks with documentation
// ============================================================================

// Application Services - Core service management
export { 
  useFindAllApplicationServices,
  useCreateApplicationService,
  useUpdateApplicationService,
  useDeleteApplicationService,
  useFindApplicationServiceById,
};

// Service Instances - Runtime instance tracking
export { 
  useFindAllServiceInstances,
  useUpdateServiceInstance,
  useDeleteServiceInstance,
  useFindByIdServiceInstance,
};

// Drift Events - Configuration drift detection
export { 
  useFindAllDriftEvents,
  useCreateDriftEvent,
  useUpdateDriftEvent,
  useFindDriftEventById,
};

// Approval Requests - Multi-gate approval workflow
export { 
  useFindAllApprovalRequests,
  useCreateApprovalRequest,
  useSubmitApprovalDecision,
  useFindApprovalRequestById,
  useCancelApprovalRequest,
};

// Service Shares - Rename for better API
export { 
  useFindAllServiceSharesForService1 as useFindAllServiceShares,
  useGrantServiceShare,
  useRevokeServiceShare,
  useFindByIdServiceShare,
};

// Config Server
export { 
  useGetEnvironmentConfigServer,
  useGetHealthConfigServer,
  useGetInfoConfigServer,
};

// Service Registry - Rename for consistency
export { 
  useListServiceRegistryServices as useFindAllServiceRegistry,
  useGetServiceRegistryServiceInstances,
};

// User Management - Current user info and permissions
export { 
  useFindCurrentUserInformation,
  useFindCurrentUserPermissions,
};

// IAM Teams - Team management (read-only, synced from Keycloak)
export {
  useFindAllIamTeams,
  useFindByIdIamTeam,
  useFindByMemberIamTeam,
  useGetStatsIamTeam,
};

// IAM Users - User management (read-only, synced from Keycloak)
export {
  useFindAllIamUsers,
  useFindByIdIamUser,
  useFindByTeamIamUser,
  useFindByManagerIamUser,
  useGetStatsIamUser,
};
