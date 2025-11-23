/**
 * Hook to prefetch all config file profiles in parallel
 * Uses individual hooks for each profile to leverage React Query caching
 */

import { useGetConfigFile } from "@lib/api/hooks";
// import type { Profile } from "../types";

// const DEFAULT_PROFILES: Profile[] = ["dev", "prod", "staging", "test"];

export interface ConfigFilePrefetchResult {
  [profile: string]: {
    data: Awaited<ReturnType<typeof useGetConfigFile>>["data"];
    isLoading: boolean;
    error: Awaited<ReturnType<typeof useGetConfigFile>>["error"] | null;
  };
}

export function useConfigFilesPrefetch(serviceId: string) {
  // Fetch all profiles in parallel
  const devQuery = useGetConfigFile(serviceId, "dev", {
    query: {
      enabled: !!serviceId,
      staleTime: 10_000,
      retry: (failureCount, error) => {
        if (error && typeof error === "object" && "status" in error) {
          const status = (error as { status?: number }).status;
          if (status === 404) return false;
        }
        return failureCount < 2;
      },
    },
  });

  const prodQuery = useGetConfigFile(serviceId, "prod", {
    query: {
      enabled: !!serviceId,
      staleTime: 10_000,
      retry: (failureCount, error) => {
        if (error && typeof error === "object" && "status" in error) {
          const status = (error as { status?: number }).status;
          if (status === 404) return false;
        }
        return failureCount < 2;
      },
    },
  });

  const stagingQuery = useGetConfigFile(serviceId, "staging", {
    query: {
      enabled: !!serviceId,
      staleTime: 10_000,
      retry: (failureCount, error) => {
        if (error && typeof error === "object" && "status" in error) {
          const status = (error as { status?: number }).status;
          if (status === 404) return false;
        }
        return failureCount < 2;
      },
    },
  });

  const testQuery = useGetConfigFile(serviceId, "test", {
    query: {
      enabled: !!serviceId,
      staleTime: 10_000,
      retry: (failureCount, error) => {
        if (error && typeof error === "object" && "status" in error) {
          const status = (error as { status?: number }).status;
          if (status === 404) return false;
        }
        return failureCount < 2;
      },
    },
  });

  const result: ConfigFilePrefetchResult = {
    dev: {
      data: devQuery.data,
      isLoading: devQuery.isLoading,
      error: devQuery.error,
    },
    prod: {
      data: prodQuery.data,
      isLoading: prodQuery.isLoading,
      error: prodQuery.error,
    },
    staging: {
      data: stagingQuery.data,
      isLoading: stagingQuery.isLoading,
      error: stagingQuery.error,
    },
    test: {
      data: testQuery.data,
      isLoading: testQuery.isLoading,
      error: testQuery.error,
    },
  };

  const isLoading = devQuery.isLoading || prodQuery.isLoading || stagingQuery.isLoading || testQuery.isLoading;
  const hasErrors = [devQuery.error, prodQuery.error, stagingQuery.error, testQuery.error].some(
    (err) => err && (err as { status?: number })?.status !== 404
  );

  return {
    profiles: result,
    isLoading,
    hasErrors,
  };
}

