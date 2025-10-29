import { Box, Card, CardContent, Skeleton } from "@mui/material";
import Grid from "@mui/material/Grid";

/**
 * Skeleton loader for the dashboard page
 * Matches the layout: stats cards, charts, and activity list
 */
export const DashboardSkeleton = () => {
  return (
    <Box>
      {/* Page Header Skeleton */}
      <Box sx={{ mb: 3 }}>
        <Skeleton variant="text" height={40} width={200} />
        <Skeleton variant="text" height={24} width={400} sx={{ mt: 1 }} />
      </Box>

      {/* Stats Cards Skeleton */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        {[1, 2, 3, 4].map((index) => (
          <Grid size={{ xs: 12, sm: 6, md: 3 }} key={index}>
            <Card>
              <CardContent>
                <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
                  <Skeleton variant="circular" width={48} height={48} />
                  <Box sx={{ flexGrow: 1 }}>
                    <Skeleton variant="text" height={20} width="60%" />
                    <Skeleton
                      variant="text"
                      height={32}
                      width="40%"
                      sx={{ mt: 1 }}
                    />
                  </Box>
                </Box>
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      {/* Charts Skeleton */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid size={{ xs: 12, md: 6 }}>
          <Card>
            <CardContent>
              <Skeleton variant="text" height={28} width="40%" sx={{ mb: 2 }} />
              <Skeleton variant="rectangular" height={300} />
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, md: 6 }}>
          <Card>
            <CardContent>
              <Skeleton variant="text" height={28} width="40%" sx={{ mb: 2 }} />
              <Skeleton variant="rectangular" height={300} />
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Bottom Section */}
      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 8 }}>
          <Card>
            <CardContent>
              <Skeleton variant="text" height={28} width="40%" sx={{ mb: 2 }} />
              <Skeleton variant="rectangular" height={300} />
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, md: 4 }}>
          <Card>
            <CardContent>
              <Skeleton variant="text" height={28} width="50%" sx={{ mb: 2 }} />
              {[1, 2, 3, 4, 5].map((index) => (
                <Box key={index} sx={{ mb: 2 }}>
                  <Skeleton variant="text" height={20} width="80%" />
                  <Skeleton
                    variant="text"
                    height={16}
                    width="60%"
                    sx={{ mt: 0.5 }}
                  />
                </Box>
              ))}
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};
