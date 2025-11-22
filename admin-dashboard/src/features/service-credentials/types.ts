/**
 * Service Credentials Feature Types
 */

import type { ServiceCredentialResponse } from "@lib/api/models";

export type CredentialStatus = ServiceCredentialResponse["status"];

export interface CredentialsDisplayData {
  clientId: string;
  clientSecret: string;
  tokenEndpoint: string;
  status: CredentialStatus;
  expiresAt?: string;
}

