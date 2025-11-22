import { useGetCommitHistory } from "@lib/api/hooks";
import type { CommitResponse } from "@lib/api/models";

export function useCommitHistory(serviceId: string, profile: string) {
  const {
    data: commitHistory,
    isLoading,
    error,
    refetch,
  } = useGetCommitHistory(serviceId, profile, {
    query: {
      enabled: !!serviceId && !!profile,
      staleTime: 30_000, // Commit history doesn't change frequently
    },
  });

  // API returns single CommitResponse, but backend comment says "up to 50 commits"
  // Handle both cases: if it's an array, use it; if single, wrap in array
  const commits: CommitResponse[] = Array.isArray(commitHistory)
    ? commitHistory
    : commitHistory
    ? [commitHistory]
    : [];

  return {
    commits,
    isLoading,
    error,
    refetch,
  };
}

