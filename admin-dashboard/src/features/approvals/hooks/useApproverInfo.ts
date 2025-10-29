import { useMemo } from "react";
import { useQueries } from "@tanstack/react-query";
import { getFindByIdIamUserQueryOptions } from "@lib/api/generated/iam-users/iam-users";
import type { IamUserResponse } from "@lib/api/models";

interface UseApproverInfoResult {
  users: Map<string, IamUserResponse | undefined>;
  isLoading: boolean;
  errors: Map<string, Error | null>;
}

/**
 * Hook to fetch user information for multiple approver user IDs.
 * Handles deduplication and caching automatically via React Query.
 *
 * @param approverUserIds - Array of user IDs to fetch
 * @returns Map of userId -> IamUserResponse with loading and error states
 */
export function useApproverInfo(
  approverUserIds: (string | undefined)[]
): UseApproverInfoResult {
  // Deduplicate user IDs
  const uniqueUserIds = useMemo(() => {
    return Array.from(
      new Set(approverUserIds.filter((id): id is string => !!id))
    );
  }, [approverUserIds]);

  // Early return for empty arrays
  if (uniqueUserIds.length === 0) {
    return {
      users: new Map(),
      isLoading: false,
      errors: new Map(),
    };
  }

  // Use useQueries to fetch all users in parallel
  // React Query automatically handles deduplication and caching
  const queries = useQueries({
    queries: uniqueUserIds.map((userId) =>
      getFindByIdIamUserQueryOptions(userId, {
        query: {
          staleTime: 5 * 60 * 1000, // Cache for 5 minutes
          retry: 1,
        },
      })
    ),
  });

  // Aggregate results
  const result = useMemo(() => {
    const usersMap = new Map<string, IamUserResponse | undefined>();
    const errorsMap = new Map<string, Error | null>();
    let isLoading = false;

    uniqueUserIds.forEach((userId, index) => {
      const query = queries[index];
      if (query.isLoading) {
        isLoading = true;
      }
      if (query.error) {
        errorsMap.set(userId, query.error as Error);
      }
      if (query.data) {
        usersMap.set(userId, query.data);
      }
    });

    return {
      users: usersMap,
      isLoading,
      errors: errorsMap,
    };
  }, [uniqueUserIds, queries]);

  return result;
}
