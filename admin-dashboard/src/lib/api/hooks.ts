// src/lib/api/hooks.ts

// Import hooks first before re-exporting
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
  useGetServiceRegistryServiceHealth,
  useGetServiceRegistryService,
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

// Re-export all hooks
export { 
  useFindAllApplicationServices,
  useCreateApplicationService,
  useUpdateApplicationService,
  useDeleteApplicationService,
  useFindApplicationServiceById,
};

export { 
  useFindAllServiceInstances,
  useUpdateServiceInstance,
  useDeleteServiceInstance,
  useFindByIdServiceInstance,
};

export { 
  useFindAllDriftEvents,
  useCreateDriftEvent,
  useUpdateDriftEvent,
  useFindDriftEventById,
};

export { 
  useFindAllApprovalRequests,
  useCreateApprovalRequest,
  useSubmitApprovalDecision,
  useFindApprovalRequestById,
  useCancelApprovalRequest,
};

export { 
  useFindAllServiceSharesForService1 as useFindAllServiceShares,
  useGrantServiceShare,
  useRevokeServiceShare,
  useFindByIdServiceShare,
};

export { 
  useGetEnvironmentConfigServer,
  useGetHealthConfigServer,
  useGetInfoConfigServer,
};

export { 
  useListServiceRegistryServices as useFindAllServiceRegistry,
  useGetServiceRegistryServiceInstances,
  useGetServiceRegistryServiceHealth,
  useGetServiceRegistryService,
};

export { 
  useFindCurrentUserInformation,
  useFindCurrentUserPermissions,
};

export {
  useFindAllIamTeams,
  useFindByIdIamTeam,
  useFindByMemberIamTeam,
  useGetStatsIamTeam,
};

export {
  useFindAllIamUsers,
  useFindByIdIamUser,
  useFindByTeamIamUser,
  useFindByManagerIamUser,
  useGetStatsIamUser,
};
