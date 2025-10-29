import {
  Box,
  Card,
  CardContent,
  Typography,
  Avatar,
  Grid,
  Divider,
  Chip,
  Button,
  Stack,
} from "@mui/material";
import PersonIcon from "@mui/icons-material/Person";
import EmailIcon from "@mui/icons-material/Email";
import BusinessIcon from "@mui/icons-material/Business";
import SupervisorAccountIcon from "@mui/icons-material/SupervisorAccount";
import AppsIcon from "@mui/icons-material/Apps";
import ShareIcon from "@mui/icons-material/Share";
import { useAuth } from "../context";
import { useFindCurrentUserInformation } from "@lib/api/hooks";
import PageHeader from "@components/common/PageHeader";
import { DetailPageSkeleton } from "@components/common/skeletons";
import ErrorFallback from "@components/common/ErrorFallback";
import { getErrorMessage } from "@lib/api/errorHandler";

export default function ProfilePage() {
  const { hasRole } = useAuth();
  const {
    data: userInfo,
    isLoading,
    error,
    refetch,
  } = useFindCurrentUserInformation();

  if (isLoading) return <DetailPageSkeleton />;
  if (error)
    return <ErrorFallback message={getErrorMessage(error)} onRetry={refetch} />;

  const handleChangePassword = () => {
    // Keycloak account management would be handled here
    window.open("/account", "_blank");
  };

  return (
    <Box>
      <PageHeader
        title="My Profile"
        subtitle="View your account information and permissions"
      />

      <Stack spacing={3}>
        {/* User Overview Card */}
        <Card>
          <CardContent>
            <Box sx={{ display: "flex", alignItems: "center", mb: 3 }}>
              <Avatar
                sx={{
                  width: 80,
                  height: 80,
                  bgcolor: "primary.main",
                  mr: 3,
                  fontSize: "2rem",
                }}
              >
                {userInfo?.username?.charAt(0).toUpperCase() || "U"}
              </Avatar>
              <Box>
                <Typography variant="h5" gutterBottom>
                  {userInfo?.username || "Unknown User"}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  @{userInfo?.username}
                </Typography>
              </Box>
            </Box>

            <Divider sx={{ my: 2 }} />

            <Grid container spacing={3}>
              <Grid size={{ xs: 12, md: 6 }}>
                <Box sx={{ display: "flex", alignItems: "center", mb: 2 }}>
                  <EmailIcon sx={{ mr: 1, color: "text.secondary" }} />
                  <Box>
                    <Typography
                      variant="caption"
                      color="text.secondary"
                      display="block"
                    >
                      Email
                    </Typography>
                    <Typography variant="body1">
                      {userInfo?.email || "N/A"}
                    </Typography>
                  </Box>
                </Box>
              </Grid>

              {userInfo?.managerId && (
                <Grid size={{ xs: 12, md: 6 }}>
                  <Box sx={{ display: "flex", alignItems: "center", mb: 2 }}>
                    <SupervisorAccountIcon
                      sx={{ mr: 1, color: "text.secondary" }}
                    />
                    <Box>
                      <Typography
                        variant="caption"
                        color="text.secondary"
                        display="block"
                      >
                        Manager
                      </Typography>
                      <Typography variant="body1">
                        {userInfo.managerId}
                      </Typography>
                    </Box>
                  </Box>
                </Grid>
              )}

              <Grid size={{ xs: 12, md: 6 }}>
                <Box sx={{ display: "flex", alignItems: "center" }}>
                  <BusinessIcon sx={{ mr: 1, color: "text.secondary" }} />
                  <Box>
                    <Typography
                      variant="caption"
                      color="text.secondary"
                      display="block"
                    >
                      Teams
                    </Typography>
                    <Box
                      sx={{
                        display: "flex",
                        gap: 1,
                        flexWrap: "wrap",
                        mt: 0.5,
                      }}
                    >
                      {userInfo?.teamIds && userInfo.teamIds.length > 0 ? (
                        userInfo.teamIds.map((teamId: string) => (
                          <Chip key={teamId} label={teamId} size="small" />
                        ))
                      ) : (
                        <Typography variant="body2" color="text.secondary">
                          No teams
                        </Typography>
                      )}
                    </Box>
                  </Box>
                </Box>
              </Grid>

              <Grid size={{ xs: 12, md: 6 }}>
                <Box sx={{ display: "flex", alignItems: "center" }}>
                  <PersonIcon sx={{ mr: 1, color: "text.secondary" }} />
                  <Box>
                    <Typography
                      variant="caption"
                      color="text.secondary"
                      display="block"
                    >
                      Roles
                    </Typography>
                    <Box
                      sx={{
                        display: "flex",
                        gap: 1,
                        flexWrap: "wrap",
                        mt: 0.5,
                      }}
                    >
                      {hasRole("SYS_ADMIN") && (
                        <Chip label="System Admin" size="small" color="error" />
                      )}
                      {hasRole("TEAM_LEAD") && (
                        <Chip label="Team Lead" size="small" color="primary" />
                      )}
                      {hasRole("DEVELOPER") && (
                        <Chip label="Developer" size="small" color="default" />
                      )}
                      {!hasRole("SYS_ADMIN") &&
                        !hasRole("TEAM_LEAD") &&
                        !hasRole("DEVELOPER") && (
                          <Typography variant="body2" color="text.secondary">
                            No roles
                          </Typography>
                        )}
                    </Box>
                  </Box>
                </Box>
              </Grid>
            </Grid>

            <Box sx={{ mt: 3 }}>
              <Button variant="outlined" onClick={handleChangePassword}>
                Manage Account (Keycloak)
              </Button>
            </Box>
          </CardContent>
        </Card>

        {/* Services Summary */}
        <Grid container spacing={3}>
          <Grid size={{ xs: 12, md: 4 }}>
            <Card>
              <CardContent>
                <Box sx={{ display: "flex", alignItems: "center", mb: 2 }}>
                  <AppsIcon sx={{ mr: 1, color: "primary.main" }} />
                  <Typography variant="h6">Owned Services</Typography>
                </Box>
                <Typography variant="h3" color="primary.main">
                  0
                </Typography>
                <Typography
                  variant="body2"
                  color="text.secondary"
                  sx={{ mt: 1 }}
                >
                  Services you own
                </Typography>
              </CardContent>
            </Card>
          </Grid>

          <Grid size={{ xs: 12, md: 4 }}>
            <Card>
              <CardContent>
                <Box sx={{ display: "flex", alignItems: "center", mb: 2 }}>
                  <ShareIcon sx={{ mr: 1, color: "success.main" }} />
                  <Typography variant="h6">Shared Services</Typography>
                </Box>
                <Typography variant="h3" color="success.main">
                  0
                </Typography>
                <Typography
                  variant="body2"
                  color="text.secondary"
                  sx={{ mt: 1 }}
                >
                  Services shared with you
                </Typography>
              </CardContent>
            </Card>
          </Grid>

          <Grid size={{ xs: 12, md: 4 }}>
            <Card>
              <CardContent>
                <Box sx={{ display: "flex", alignItems: "center", mb: 2 }}>
                  <BusinessIcon sx={{ mr: 1, color: "info.main" }} />
                  <Typography variant="h6">Team Services</Typography>
                </Box>
                <Typography variant="h3" color="info.main">
                  0
                </Typography>
                <Typography
                  variant="body2"
                  color="text.secondary"
                  sx={{ mt: 1 }}
                >
                  Services in your teams
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </Stack>
    </Box>
  );
}
