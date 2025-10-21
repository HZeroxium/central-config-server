import { z } from 'zod';

export const IamUserSchema = z.object({
  userId: z.string(),
  username: z.string(),
  email: z.string(),
  firstName: z.string().optional(),
  lastName: z.string().optional(),
  teamIds: z.array(z.string()).optional(),
  managerId: z.string().optional(),
  roles: z.array(z.string()).optional(),
  createdAt: z.string(),
  updatedAt: z.string(),
  syncedAt: z.string(),
});

export const IamTeamSchema = z.object({
  teamId: z.string(),
  displayName: z.string(),
  members: z.array(z.string()).optional(), // User IDs
  createdAt: z.string(),
  updatedAt: z.string(),
  syncedAt: z.string(),
});

export type IamUser = z.infer<typeof IamUserSchema>;
export type IamTeam = z.infer<typeof IamTeamSchema>;

export interface IamUserQueryFilter {
  page?: number;
  size?: number;
  sort?: string;
  search?: string;
  enabled?: boolean;
}

export interface IamTeamQueryFilter {
  page?: number;
  size?: number;
  sort?: string;
  search?: string;
}
