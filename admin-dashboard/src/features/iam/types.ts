export interface IamTeam {
  id: string;
  name: string;
  description?: string;
  memberCount: number;
  ownedServiceIds: string[];
  managerId?: string;
  managerName?: string;
  createdAt: string;
  updatedAt: string;
}

export interface IamUser {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  teamIds: string[];
  teamNames: string[];
  managerId?: string;
  managerName?: string;
  roles: string[];
  isActive: boolean;
  lastLoginAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface TeamStats {
  teamId: string;
  memberCount: number;
  ownedServiceCount: number;
  sharedServiceCount: number;
  activeDriftCount: number;
  pendingApprovalCount: number;
}

export interface UserStats {
  userId: string;
  teamCount: number;
  ownedServiceCount: number;
  sharedServiceCount: number;
  pendingApprovalCount: number;
  lastActivityAt?: string;
}

export interface IamFilter {
  teamId?: string;
  managerId?: string;
  role?: string;
  isActive?: boolean;
  search?: string;
}

export const USER_ROLES = {
  SYS_ADMIN: 'SYS_ADMIN',
  TEAM_LEAD: 'TEAM_LEAD',
  USER: 'USER',
} as const;

export type UserRole = typeof USER_ROLES[keyof typeof USER_ROLES];

export const ROLE_LABELS: Record<UserRole, string> = {
  [USER_ROLES.SYS_ADMIN]: 'System Administrator',
  [USER_ROLES.TEAM_LEAD]: 'Team Lead',
  [USER_ROLES.USER]: 'User',
};

export const ROLE_COLORS: Record<UserRole, string> = {
  [USER_ROLES.SYS_ADMIN]: 'error',
  [USER_ROLES.TEAM_LEAD]: 'warning',
  [USER_ROLES.USER]: 'primary',
};