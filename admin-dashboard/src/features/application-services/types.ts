import { z } from 'zod';

// Zod schemas matching backend DTOs
export const CreateApplicationServiceSchema = z.object({
  id: z.string().min(1, 'Service ID is required').max(100, 'Service ID must not exceed 100 characters'),
  displayName: z.string().min(1, 'Display name is required').max(200, 'Display name must not exceed 200 characters'),
  ownerTeamId: z.string().min(1, 'Owner team ID is required').max(100, 'Owner team ID must not exceed 100 characters'),
  environments: z.array(z.string()).min(1, 'At least one environment must be specified'),
  tags: z.array(z.string()).optional(),
  repoUrl: z.string().max(500, 'Repository URL must not exceed 500 characters').optional(),
  attributes: z.record(z.string(), z.string()).optional(),
});

export const UpdateApplicationServiceSchema = z.object({
  displayName: z.string().max(200, 'Display name must not exceed 200 characters').optional(),
  lifecycle: z.string().max(50, 'Lifecycle must not exceed 50 characters').optional(),
  tags: z.array(z.string()).optional(),
  repoUrl: z.string().max(500, 'Repository URL must not exceed 500 characters').optional(),
  attributes: z.record(z.string(), z.string()).optional(),
});

export const QueryFilterSchema = z.object({
  ownerTeamId: z.string().max(100, 'Owner team ID must not exceed 100 characters').optional(),
  lifecycle: z.string().max(50, 'Lifecycle must not exceed 50 characters').optional(),
  tags: z.array(z.string()).optional(),
  search: z.string().max(200, 'Search term must not exceed 200 characters').optional(),
});

// TypeScript interfaces
export interface ApplicationService {
  id: string;
  displayName: string;
  ownerTeamId: string;
  environments: string[];
  tags: string[];
  repoUrl?: string;
  lifecycle?: string;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  attributes: Record<string, string>;
}

export interface CreateApplicationServiceRequest {
  id: string;
  displayName: string;
  ownerTeamId: string;
  environments: string[];
  tags?: string[];
  repoUrl?: string;
  attributes?: Record<string, string>;
}

export interface UpdateApplicationServiceRequest {
  displayName?: string;
  lifecycle?: string;
  tags?: string[];
  repoUrl?: string;
  attributes?: Record<string, string>;
}

export interface ApplicationServiceQueryFilter {
  ownerTeamId?: string;
  lifecycle?: string;
  tags?: string[];
  search?: string;
}

export type CreateApplicationServiceFormData = z.infer<typeof CreateApplicationServiceSchema>;
export type UpdateApplicationServiceFormData = z.infer<typeof UpdateApplicationServiceSchema>;
export type ApplicationServiceQueryFilterData = z.infer<typeof QueryFilterSchema>;
