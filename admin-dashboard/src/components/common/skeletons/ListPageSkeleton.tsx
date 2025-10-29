import { Box, Card, CardContent, Skeleton, Stack } from "@mui/material";

/**
 * Skeleton loader for list pages
 * Matches the layout: page header, filters, and table
 */
export const ListPageSkeleton = () => {
  return (
    <Box>
      {/* Page Header Skeleton */}
      <Box sx={{ mb: 3 }}>
        <Skeleton variant="text" height={40} width={200} />
        <Skeleton variant="text" height={24} width={400} sx={{ mt: 1 }} />
      </Box>

      {/* Filters Skeleton */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
            <Skeleton variant="rectangular" height={40} width="100%" />
            <Skeleton variant="rectangular" height={40} width="100%" />
            <Skeleton variant="rectangular" height={40} width="100%" />
            <Skeleton variant="rectangular" height={40} width={120} />
          </Stack>
        </CardContent>
      </Card>

      {/* Table Skeleton */}
      <Card>
        <CardContent>
          {/* Table Header */}
          <Box sx={{ display: "flex", gap: 2, mb: 2 }}>
            {[1, 2, 3, 4, 5].map((index) => (
              <Skeleton
                key={index}
                variant="rectangular"
                height={40}
                width={150}
              />
            ))}
          </Box>
          {/* Table Rows */}
          {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map((rowIndex) => (
            <Box
              key={rowIndex}
              sx={{ display: "flex", gap: 2, mb: 1, alignItems: "center" }}
            >
              <Skeleton variant="text" height={40} width={150} />
              <Skeleton variant="text" height={40} width={150} />
              <Skeleton variant="text" height={40} width={150} />
              <Skeleton variant="text" height={40} width={150} />
              <Skeleton variant="text" height={40} width={150} />
              <Skeleton variant="circular" width={32} height={32} />
            </Box>
          ))}
        </CardContent>
      </Card>
    </Box>
  );
};
