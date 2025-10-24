import { useMemo } from 'react';
import { useFindAllApprovalRequests } from '@lib/api/hooks';
import { useAuth } from '@features/auth/authContext';

export function usePendingApprovalCount() {
  const { isSysAdmin, userInfo } = useAuth();

  const { data } = useFindAllApprovalRequests(
    {
      status: 'PENDING',
      page: 0,
      size: 100, // Fetch enough to get accurate count
    },
    {
      query: {
        staleTime: 30_000, // Cache for 30 seconds
        refetchInterval: 60_000, // Refetch every minute
      },
    }
  );

  const pendingCount = useMemo(() => {
    if (!data?.items) return 0;
    
    return data.items.filter((request) => {
      // SYS_ADMIN can approve everything
      if (isSysAdmin) return true;
      
      // LINE_MANAGER can approve if they are the manager of the requester
      if (request.snapshot?.managerId === userInfo?.userId) return true;
      
      return false;
    }).length;
  }, [data?.items, isSysAdmin, userInfo?.userId]);

  return pendingCount;
}

