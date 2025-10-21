import React from 'react';
import { Skeleton, Box, Card, CardContent } from '@mui/material';
import Grid from '@mui/material/Grid';

interface SkeletonLoaderProps {
  variant?: 'table' | 'card' | 'list' | 'page';
  rows?: number;
  columns?: number;
}

export const SkeletonLoader: React.FC<SkeletonLoaderProps> = ({
  variant = 'card',
  rows = 3,
  columns = 1,
}) => {
  const renderTableSkeleton = () => (
    <Box>
      {/* Header */}
      <Box sx={{ display: 'flex', gap: 2, mb: 2 }}>
        {Array.from({ length: columns }).map((_, index) => (
          <Skeleton key={index} variant="rectangular" height={40} width={150} />
        ))}
      </Box>
      {/* Rows */}
      {Array.from({ length: rows }).map((_, rowIndex) => (
        <Box key={rowIndex} sx={{ display: 'flex', gap: 2, mb: 1 }}>
          {Array.from({ length: columns }).map((_, colIndex) => (
            <Skeleton key={colIndex} variant="text" height={20} width={150} />
          ))}
        </Box>
      ))}
    </Box>
  );

  const renderCardSkeleton = () => (
    <Grid container spacing={3}>
      {Array.from({ length: columns }).map((_, index) => (
        <Grid size={{ xs: 12, md: 6, lg: 4 }} key={index}>
          <Card>
            <CardContent>
              <Skeleton variant="text" height={32} width="60%" />
              <Skeleton variant="text" height={24} width="40%" />
              <Skeleton variant="text" height={20} width="80%" />
              <Skeleton variant="text" height={20} width="60%" />
            </CardContent>
          </Card>
        </Grid>
      ))}
    </Grid>
  );

  const renderListSkeleton = () => (
    <Box>
      {Array.from({ length: rows }).map((_, index) => (
        <Box key={index} sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
          <Skeleton variant="circular" width={40} height={40} sx={{ mr: 2 }} />
          <Box sx={{ flexGrow: 1 }}>
            <Skeleton variant="text" height={20} width="80%" />
            <Skeleton variant="text" height={16} width="60%" />
          </Box>
        </Box>
      ))}
    </Box>
  );

  const renderPageSkeleton = () => (
    <Box>
      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 8 }}>
          <Card>
            <CardContent>
              <Skeleton variant="text" height={32} width="30%" />
              <Skeleton variant="text" height={20} width="80%" />
              <Skeleton variant="text" height={20} width="60%" />
              <Skeleton variant="rectangular" height={200} />
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, md: 4 }}>
          <Card>
            <CardContent>
              <Skeleton variant="text" height={28} width="40%" />
              <Skeleton variant="text" height={20} width="70%" />
              <Skeleton variant="text" height={20} width="50%" />
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );

  switch (variant) {
    case 'table':
      return renderTableSkeleton();
    case 'card':
      return renderCardSkeleton();
    case 'list':
      return renderListSkeleton();
    case 'page':
      return renderPageSkeleton();
    default:
      return renderCardSkeleton();
  }
};

export default SkeletonLoader;