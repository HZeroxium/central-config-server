import { z } from 'zod';

// Resource level enum matching backend
export const ResourceLevelSchema = z.enum(['SERVICE', 'INSTANCE']);

// Grantee type enum matching backend  
export const GranteeTypeSchema = z.enum(['TEAM', 'USER']);

// Share permissions matching backend ServiceShare.SharePermission
export const SharePermissionSchema = z.enum([
  'VIEW_SERVICE',
  'VIEW_INSTANCE', 
  'VIEW_DRIFT',
  'EDIT_SERVICE',
  'EDIT_INSTANCE',
  'RESTART_INSTANCE'
]);

export const ServiceShareSchema = z.object({
  id: z.string(),
  resourceLevel: ResourceLevelSchema,
  serviceId: z.string(),
  grantToType: GranteeTypeSchema,
  grantToId: z.string(),
  permissions: z.array(SharePermissionSchema),
  environments: z.array(z.string()).optional(),
  grantedBy: z.string(),
  createdAt: z.string(),
  expiresAt: z.string().optional(),
});

export const CreateServiceShareSchema = z.object({
  serviceId: z.string().min(1, 'Service ID is required'),
  grantToType: GranteeTypeSchema,
  grantToId: z.string().min(1, 'Grant To ID is required'),
  permissions: z.array(SharePermissionSchema).min(1, 'At least one permission is required'),
  environments: z.array(z.string()).optional(),
  expiresAt: z.string().optional(),
});

export type ServiceShare = z.infer<typeof ServiceShareSchema>;
export type CreateServiceShareRequest = z.infer<typeof CreateServiceShareSchema>;

export interface ServiceShareQueryFilter {
  page?: number;
  size?: number;
  sort?: string;
  serviceId?: string;
  grantToType?: string;
  grantToId?: string;
  isActive?: boolean;
}
