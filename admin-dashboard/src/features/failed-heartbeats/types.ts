/**
 * Failed Heartbeats Feature Types
 */

import type {
  FailedHeartbeatResponse,
  FailedHeartbeatResponseStatus,
} from "@lib/api/models";

export type FailedHeartbeatStatus = FailedHeartbeatResponseStatus;

export interface FailedHeartbeat extends FailedHeartbeatResponse {}

