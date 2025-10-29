import {
  Box,
  Card,
  CardContent,
  Skeleton,
  Divider,
  Tabs,
  Tab,
} from "@mui/material";
import Grid from "@mui/material/Grid";

/**
 * Skeleton loader for detail pages
 * Matches the layout: page header, info cards, tabs, and content sections
 */
export const DetailPageSkeleton = () => {
  return (
    <Box>
      {/* Page Header Skeleton */}
      <Box
        sx={{
          mb: 3,
          display: "flex",
          justifyContent: "space-between",
          alignItems: "flex-start",
        }}
      >
        <Box sx={{ flexGrow: 1 }}>
          <Skeleton variant="text" height={40} width={300} />
          <Skeleton variant="text" height={24} width={500} sx={{ mt: 1 }} />
        </Box>
        <Box sx={{ display: "flex", gap: 1 }}>
          <Skeleton variant="rectangular" height={36} width={100} />
          <Skeleton variant="rectangular" height={36} width={100} />
        </Box>
      </Box>

      {/* Info Cards Grid */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        {[1, 2, 3, 4].map((index) => (
          <Grid size={{ xs: 12, sm: 6, md: 3 }} key={index}>
            <Card>
              <CardContent>
                <Skeleton
                  variant="text"
                  height={16}
                  width="60%"
                  sx={{ mb: 1 }}
                />
                <Skeleton variant="text" height={28} width="80%" />
              </CardContent>
            </Card>
          </Grid>
        ))}
      </Grid>

      {/* Tabs Skeleton */}
      <Box sx={{ borderBottom: 1, borderColor: "divider", mb: 3 }}>
        <Tabs value={0}>
          <Tab label={<Skeleton variant="text" width={100} />} />
          <Tab label={<Skeleton variant="text" width={100} />} />
          <Tab label={<Skeleton variant="text" width={100} />} />
        </Tabs>
      </Box>

      {/* Main Content Skeleton */}
      <Card>
        <CardContent>
          <Grid container spacing={3}>
            <Grid size={{ xs: 12, md: 8 }}>
              <Skeleton variant="text" height={28} width="40%" sx={{ mb: 2 }} />
              <Skeleton variant="rectangular" height={200} sx={{ mb: 2 }} />
              <Divider sx={{ my: 2 }} />
              <Skeleton variant="text" height={28} width="40%" sx={{ mb: 2 }} />
              {[1, 2, 3, 4].map((index) => (
                <Box key={index} sx={{ mb: 1 }}>
                  <Skeleton variant="text" height={20} width="30%" />
                  <Skeleton variant="text" height={20} width="70%" />
                </Box>
              ))}
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <Card variant="outlined">
                <CardContent>
                  <Skeleton
                    variant="text"
                    height={24}
                    width="50%"
                    sx={{ mb: 2 }}
                  />
                  {[1, 2, 3, 4].map((index) => (
                    <Box key={index} sx={{ mb: 2 }}>
                      <Skeleton variant="text" height={16} width="40%" />
                      <Skeleton variant="text" height={20} width="80%" />
                    </Box>
                  ))}
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        </CardContent>
      </Card>
    </Box>
  );
};
