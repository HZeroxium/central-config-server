import { QueryClient } from "@tanstack/react-query";

/**
 * Enhanced QueryClient with optimized caching strategies
 *
 * Cache Strategy:
 * - List queries: 30s staleTime (frequently updated)
 * - Detail queries: 60s staleTime (less frequently updated)
 * - Reference data: 5min staleTime (rarely changes)
 * - Dashboard queries: 60s staleTime (moderate update frequency)
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000, // 30 seconds default for list queries
      gcTime: 5 * 60 * 1000, // 5 minutes garbage collection (formerly cacheTime)
      retry: 1,
      refetchOnWindowFocus: false,
      refetchOnMount: true,
      refetchOnReconnect: true,
    },
    mutations: {
      retry: 0,
    },
  },
});
