import React, { useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { Box, Card, CardContent, Typography, Alert } from "@mui/material";
import { useFindAllApprovalRequests } from "@lib/api/hooks";
import { TableSkeleton } from "@components/common/skeletons";
import { ApprovalRequestTable } from "@features/approvals/components/ApprovalRequestTable";
import type { ApprovalRequestResponse } from "@lib/api/models";

interface ApprovalsTabProps {
  serviceId: string;
}

export const ApprovalsTab: React.FC<ApprovalsTabProps> = ({ serviceId }) => {
  const navigate = useNavigate();

  // Fetch all approval requests (API doesn't support filtering by serviceId)
  const { data: requestsData, isLoading: requestsLoading } =
    useFindAllApprovalRequests(
      {
        page: 0,
        size: 100, // Fetch more to ensure we get all requests for this service
      },
      {
        query: {
          staleTime: 30_000,
        },
      }
    );

  // Filter requests by serviceId client-side
  const filteredRequests = useMemo(() => {
    const requests = requestsData?.items || [];
    return requests.filter(
      (request: ApprovalRequestResponse) =>
        request.target?.serviceId === serviceId
    );
  }, [requestsData?.items, serviceId]);

  const handleRequestClick = (requestId: string) => {
    navigate(`/approvals/${requestId}`);
  };

  return (
    <Card>
      <CardContent>
        <Box
          sx={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            mb: 3,
          }}
        >
          <Typography variant="h6">Approval Requests</Typography>
        </Box>

        {requestsLoading ? (
          <TableSkeleton rows={5} columns={7} />
        ) : filteredRequests.length > 0 ? (
          <ApprovalRequestTable
            requests={filteredRequests}
            loading={requestsLoading}
            page={0}
            pageSize={filteredRequests.length}
            totalElements={filteredRequests.length}
            onPageChange={() => {}}
            onPageSizeChange={() => {}}
            onRowClick={handleRequestClick}
          />
        ) : (
          <Alert severity="info">
            No approval requests found for this service.
          </Alert>
        )}
      </CardContent>
    </Card>
  );
};
