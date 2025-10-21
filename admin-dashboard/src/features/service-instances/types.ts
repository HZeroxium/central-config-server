import { z } from 'zod';

// Instance status enum matching backend ServiceInstance.InstanceStatus
export const InstanceStatusSchema = z.enum(['HEALTHY', 'UNHEALTHY', 'DRIFT', 'UNKNOWN']);

export const ServiceInstanceSchema = z.object({
  serviceName: z.string(),
  instanceId: z.string(),
  host: z.string(),
  port: z.number().int().positive(),
  environment: z.string(),
  version: z.string().optional(),
  configHash: z.string().optional(),
  lastAppliedHash: z.string().optional(),
  expectedHash: z.string().optional(),
  status: InstanceStatusSchema,
  lastSeenAt: z.string(),
  createdAt: z.string(),
  updatedAt: z.string(),
  metadata: z.record(z.string(), z.string()).optional(),
  hasDrift: z.boolean().optional(),
  driftDetectedAt: z.string().optional(),
});

export const UpdateServiceInstanceSchema = z.object({
  host: z.string().optional(),
  port: z.number().int().positive().optional(),
  environment: z.string().optional(),
  version: z.string().optional(),
  configHash: z.string().optional(),
  lastAppliedHash: z.string().optional(),
  expectedHash: z.string().optional(),
  hasDrift: z.boolean().optional(),
  status: InstanceStatusSchema.optional(),
  metadata: z.record(z.string(), z.string()).optional(),
});

export type ServiceInstance = z.infer<typeof ServiceInstanceSchema>;
export type UpdateServiceInstanceRequest = z.infer<typeof UpdateServiceInstanceSchema>;

export interface ServiceInstanceQueryFilter {
  page?: number;
  size?: number;
  sort?: string;
  serviceName?: string;
  environment?: string;
  status?: string;
  hasDrift?: boolean;
}
