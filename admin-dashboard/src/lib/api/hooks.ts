// src/lib/api/hooks.ts

/**
 * Central export file for all API hooks.
 * Re-exports generated hooks from orval with convenient aliases.
 */

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
} from "./generated/application-services/application-services";

// Service Instances - Runtime instance tracking
export {
  useFindAllServiceInstances,
  useUpdateServiceInstance,
  useDeleteServiceInstance,
  useFindByIdServiceInstance,
} from "./generated/service-instances/service-instances";

// Drift Events - Configuration drift detection
export {
  useFindAllDriftEvents,
  useCreateDriftEvent,
  useUpdateDriftEvent,
  useFindDriftEventById,
} from "./generated/drift-events/drift-events";

// Approval Requests - Multi-gate approval workflow
export {
  useFindAllApprovalRequests,
  useCreateApprovalRequest,
  useSubmitApprovalDecision,
  useFindApprovalRequestById,
  useCancelApprovalRequest,
} from "./generated/approval-requests/approval-requests";

// Service Shares - Rename for better API
export {
  useFindAllServiceShares,
  useGrantServiceShare,
  useRevokeServiceShare,
  useFindByIdServiceShare,
} from "./generated/service-shares/service-shares";

// Config Server
export {
  useGetEnvironmentConfigServer,
  useGetHealthConfigServer,
  useGetInfoConfigServer,
} from "./generated/config-server/config-server";

// Service Registry - Rename for consistency
export {
  useListServiceRegistryServices as useFindAllServiceRegistry,
  useGetServiceRegistryServiceInstances,
} from "./generated/service-registry/service-registry";

// User Management - Current user info and permissions
export {
  useFindCurrentUserInformation,
  useFindCurrentUserPermissions,
} from "./generated/user-management/user-management";

// IAM Teams - Team management (read-only, synced from Keycloak)
export {
  useFindAllIamTeams,
  useFindByIdIamTeam,
  useFindByMemberIamTeam,
  useGetStatsIamTeam,
} from "./generated/iam-teams/iam-teams";

// IAM Users - User management (read-only, synced from Keycloak)
export {
  useFindAllIamUsers,
  useFindByIdIamUser,
  useFindByTeamIamUser,
  useFindByManagerIamUser,
  useGetStatsIamUser,
} from "./generated/iam-users/iam-users";
