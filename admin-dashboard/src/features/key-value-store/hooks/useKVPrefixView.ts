/**
 * Hook for viewing KV prefix as structured document (JSON, YAML, Properties)
 */

import { useQuery } from "@tanstack/react-query";
import { customInstance } from "@lib/api/mutator";
import { handleApiError } from "@lib/api/errorHandler";

export type KVStructuredFormat = "json" | "yaml" | "properties";

export interface UseKVPrefixViewOptions {
  serviceId: string;
  prefix: string;
  format?: KVStructuredFormat;
  enabled?: boolean;
  consistent?: boolean;
  stale?: boolean;
}

/**
 * View a prefix as structured document
 */
export function useKVPrefixView(options: UseKVPrefixViewOptions) {
  const { serviceId, prefix, format = "json", enabled = true, consistent, stale } = options;

  return useQuery({
    queryKey: ["kv-view", serviceId, prefix, format, consistent, stale],
    queryFn: async () => {
      const params = new URLSearchParams();
      params.append("format", format);
      if (consistent) params.append("consistent", "true");
      if (stale) params.append("stale", "true");
      if (prefix) params.append("prefix", prefix);
      
      const url = `/api/application-services/${serviceId}/kv/view?${params.toString()}`;
      
      try {
        const response = await customInstance<string>({
          url,
          method: "GET",
        });
        return response;
      } catch (error) {
        handleApiError(error);
        throw error;
      }
    },
    enabled: enabled && !!serviceId,
    staleTime: 10_000,
  });
}

