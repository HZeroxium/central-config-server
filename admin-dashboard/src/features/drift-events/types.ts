import { z } from 'zod';

export const DriftEventSchema = z.object({
  id: z.uuid(),
  serviceName: z.string().min(1, 'Service Name is required'),
  instanceId: z.string().min(1, 'Instance ID is required'),
  severity: z.enum(['CRITICAL', 'HIGH', 'MEDIUM', 'LOW']),
  status: z.enum(['DETECTED', 'ACKNOWLEDGED', 'RESOLVING', 'RESOLVED', 'IGNORED']),
  detectedAt: z.iso.datetime(),
  resolvedAt: z.iso.datetime().optional(),
  expectedHash: z.string().optional(),
  appliedHash: z.string().optional(),
  configKey: z.string().optional(),
  environment: z.string().optional(),
  notes: z.string().optional(),
  resolvedBy: z.string().optional(),
  createdAt: z.iso.datetime(),
  updatedAt: z.iso.datetime(),
});

export const UpdateDriftEventSchema = z.object({
  status: z.enum(['DETECTED', 'ACKNOWLEDGED', 'RESOLVING', 'RESOLVED', 'IGNORED']).optional(),
  notes: z.string().optional(),
});

export type DriftEvent = z.infer<typeof DriftEventSchema>;
export type UpdateDriftEventRequest = z.infer<typeof UpdateDriftEventSchema>;

export interface DriftEventQueryFilter {
  page?: number;
  size?: number;
  sort?: string;
  serviceName?: string;
  status?: string;
  severity?: string;
  environment?: string;
}
