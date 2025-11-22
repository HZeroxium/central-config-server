/**
 * Config Git Feature Types
 */

import type { ConfigFileResponse } from "@lib/api/models";

export type Profile = "dev" | "prod" | "staging" | "test";

export interface ConfigFileData extends ConfigFileResponse {
  profile: Profile;
}

