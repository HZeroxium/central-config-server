import { Box, Skeleton } from "@mui/material";

interface TableSkeletonProps {
  rows?: number;
  columns?: number;
}

/**
 * Skeleton loader for DataGrid tables
 * Shows a loading state that matches the table structure
 */
export const TableSkeleton = ({
  rows = 10,
  columns = 5,
}: TableSkeletonProps) => {
  return (
    <Box sx={{ p: 2 }}>
      {/* Table Header */}
      <Box sx={{ display: "flex", gap: 2, mb: 2, alignItems: "center" }}>
        {Array.from({ length: columns }).map((_, index) => (
          <Skeleton key={index} variant="rectangular" height={40} width={150} />
        ))}
      </Box>
      {/* Table Rows */}
      {Array.from({ length: rows }).map((_, rowIndex) => (
        <Box
          key={rowIndex}
          sx={{
            display: "flex",
            gap: 2,
            mb: 1,
            alignItems: "center",
            py: 1,
          }}
        >
          {Array.from({ length: columns }).map((_, colIndex) => (
            <Skeleton key={colIndex} variant="text" height={40} width={150} />
          ))}
        </Box>
      ))}
    </Box>
  );
};
