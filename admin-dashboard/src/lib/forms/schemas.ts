import { z } from "zod";

/**
 * Application Service Schemas
 */
export const applicationServiceCreateSchema = z.object({
  id: z
    .string()
    .min(1, "Service ID is required")
    .max(100, "Service ID must not exceed 100 characters"),
  displayName: z
    .string()
    .min(1, "Display name is required")
    .max(200, "Display name must not exceed 200 characters"),
  ownerTeamId: z
    .string()
    .min(1, "Owner team is required")
    .max(100, "Owner team ID must not exceed 100 characters"),
  environments: z
    .array(z.string())
    .min(1, "At least one environment must be specified"),
  tags: z.array(z.string()).optional(),
  repoUrl: z
    .union([
      z
        .url({ message: "Must be a valid URL" })
        .max(500, "Repository URL must not exceed 500 characters"),
      z.literal(""),
    ])
    .optional(),
  attributes: z.record(z.string(), z.string()).optional(),
});

export const applicationServiceUpdateSchema = z.object({
  displayName: z
    .string()
    .max(200, "Display name must not exceed 200 characters")
    .optional(),
  lifecycle: z.enum(["ACTIVE", "DEPRECATED", "RETIRED"]).optional(),
  tags: z.array(z.string()).optional(),
  repoUrl: z
    .union([
      z
        .url({ message: "Must be a valid URL" })
        .max(500, "Repository URL must not exceed 500 characters"),
      z.literal(""),
    ])
    .optional(),
  attributes: z.record(z.string(), z.string()).optional(),
});

/**
 * Approval Request Schemas
 */
export const approvalRequestCreateSchema = z.object({
  serviceId: z
    .string()
    .min(1, "Service ID is required")
    .max(100, "Service ID must not exceed 100 characters"),
  targetTeamId: z
    .string()
    .min(1, "Target team is required")
    .max(100, "Target team ID must not exceed 100 characters"),
  note: z.string().max(500, "Note must not exceed 500 characters").optional(),
});

export const approvalDecisionSchema = z.object({
  decision: z.enum(["APPROVE", "REJECT"]),
  note: z.string().max(500, "Note must not exceed 500 characters").optional(),
});

/**
 * Service Share Schemas
 */
export const serviceShareCreateSchema = z.object({
  serviceId: z
    .string()
    .min(1, "Service ID is required")
    .max(100, "Service ID must not exceed 100 characters"),
  grantToType: z.enum(["TEAM", "USER"]),
  grantToId: z
    .string()
    .min(1, "Grantee ID is required")
    .max(100, "Grantee ID must not exceed 100 characters"),
  permissions: z
    .array(z.string())
    .min(1, "At least one permission must be specified"),
  environments: z.array(z.string()).optional(),
  expiresAt: z.iso.datetime().optional(),
});

/**
 * Drift Event Schemas
 */
export const driftEventUpdateSchema = z.object({
  status: z.enum(["DETECTED", "RESOLVED", "IGNORED"]).optional(),
  resolvedBy: z.string().optional(),
  notes: z.string().optional(),
});

/**
 * Service Instance Schemas
 */
export const serviceInstanceUpdateSchema = z.object({
  host: z.string().optional(),
  port: z.number().positive("Port must be a positive number").optional(),
  environment: z.enum(["dev", "staging", "prod"]).optional(),
  version: z.string().optional(),
  configHash: z.string().optional(),
  lastAppliedHash: z.string().optional(),
  expectedHash: z.string().optional(),
  hasDrift: z.boolean().optional(),
  status: z.enum(["HEALTHY", "UNHEALTHY", "DRIFT", "UNKNOWN"]).optional(),
  metadata: z.record(z.string(), z.string()).optional(),
});

/**
 * Type inference helpers
 */
export type ApplicationServiceCreateInput = z.infer<
  typeof applicationServiceCreateSchema
>;
export type ApplicationServiceUpdateInput = z.infer<
  typeof applicationServiceUpdateSchema
>;
export type ApprovalRequestCreateInput = z.infer<
  typeof approvalRequestCreateSchema
>;
export type ApprovalDecisionInput = z.infer<typeof approvalDecisionSchema>;
export type ServiceShareCreateInput = z.infer<typeof serviceShareCreateSchema>;
export type DriftEventUpdateInput = z.infer<typeof driftEventUpdateSchema>;
export type ServiceInstanceUpdateInput = z.infer<
  typeof serviceInstanceUpdateSchema
>;
