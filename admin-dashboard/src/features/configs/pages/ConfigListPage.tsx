import {
  Card,
  CardContent,
  Stack,
  TextField,
  Button,
  Typography,
  Box,
  CircularProgress,
  Alert,
} from "@mui/material";
import Grid from "@mui/material/Grid";
import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import {
  Search,
  Refresh as RefreshIcon,
  HealthAndSafety as HealthIcon,
  Info as InfoIcon,
} from "@mui/icons-material";
import { PageHeader } from "@components/common/PageHeader";
import { useGetHealthConfigServer, useGetInfoConfigServer } from "@lib/api/hooks";
import { useConfigSearchHistory, type ConfigSearchEntry } from "@hooks/useConfigSearchHistory";
import { PopularApplications } from "../components/PopularApplications";
import { QuickFilterChips } from "../components/QuickFilterChips";
import { ConfigSearchHistory } from "../components/ConfigSearchHistory";

export default function ConfigListPage() {
  const [application, setApplication] = useState("sample-service");
  const [profile, setProfile] = useState("dev");
  const [label, setLabel] = useState("");
  const [validationError, setValidationError] = useState<string | null>(null);
  const navigate = useNavigate();

  const {
    history,
    addSearch,
    removeEntry,
    clearHistory,
    popularApplications,
  } = useConfigSearchHistory();

  // Fetch config server health and info
  const {
    data: healthResponse,
    isLoading: healthLoading,
    refetch: refetchHealth,
  } = useGetHealthConfigServer();
  const {
    data: infoResponse,
    isLoading: infoLoading,
    refetch: refetchInfo,
  } = useGetInfoConfigServer();

  const healthData = healthResponse;
  const infoData = infoResponse;

  // Validate form before navigation
  const validateAndGo = () => {
    setValidationError(null);

    if (!application.trim()) {
      setValidationError("Application name is required");
      return;
    }

    if (!profile.trim()) {
      setValidationError("Profile is required");
      return;
    }

    // Add to search history
    addSearch(application.trim(), profile.trim(), label.trim() || undefined);

    // Navigate
    const path = `/configs/${encodeURIComponent(application.trim())}/${encodeURIComponent(profile.trim())}`;
    const search = label.trim() ? `?label=${encodeURIComponent(label.trim())}` : "";
    navigate(path + search);
  };

  // Handle Enter key press
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Enter" && (event.ctrlKey || event.metaKey)) {
        event.preventDefault();
        validateAndGo();
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [application, profile, label]);

  const handleHistorySelect = (entry: ConfigSearchEntry) => {
    setApplication(entry.application);
    setProfile(entry.profile);
    setLabel(entry.label || "");
    setValidationError(null);
  };

  const handlePopularAppSelect = (app: string) => {
    setApplication(app);
    setValidationError(null);
  };

  const handleQuickFilterSelect = (prof: string) => {
    setProfile(prof);
    setValidationError(null);
  };

  const popularApps = popularApplications();

  return (
    <Box sx={{ p: { xs: 2, sm: 3, md: 4 } }}>
      <PageHeader
        title="Configuration Explorer"
        subtitle="Browse and manage application configurations"
        actions={
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={() => {
              refetchHealth()
              refetchInfo()
            }}
            disabled={healthLoading || infoLoading}
          >
            Refresh
          </Button>
        }
      />

      {/* Config Server Status */}
      <Box sx={{ mb: 3 }}>
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, sm: 6, md: 3 }}>
            <Card>
              <CardContent sx={{ textAlign: 'center' }}>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 1, mb: 1 }}>
                  <HealthIcon color={healthData?.status === 'UP' ? 'success' : 'error'} />
                  {healthLoading ? (
                    <CircularProgress size={20} />
                  ) : (
                    <Typography variant="h6">
                      {healthData?.status || 'Unknown'}
                    </Typography>
                  )}
                </Box>
                <Typography variant="body2" color="text.secondary">
                  Server Health
                </Typography>
              </CardContent>
            </Card>
          </Grid>

          <Grid size={{ xs: 12, sm: 6, md: 3 }}>
            <Card>
              <CardContent sx={{ textAlign: 'center' }}>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 1, mb: 1 }}>
                  <InfoIcon color="primary" />
                  {infoLoading ? (
                    <CircularProgress size={20} />
                  ) : (
                    <Typography variant="h6">
                      {infoData?.url || 'N/A'}
                    </Typography>
                  )}
                </Box>
                <Typography variant="body2" color="text.secondary">
                  Server URL
                </Typography>
              </CardContent>
            </Card>
          </Grid>

          <Grid size={{ xs: 12, sm: 6, md: 3 }}>
            <Card>
              <CardContent sx={{ textAlign: 'center' }}>
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 1, mb: 1 }}>
                  <InfoIcon color="info" />
                  {infoLoading ? (
                    <CircularProgress size={20} />
                  ) : (
                    <Typography variant="h6">
                      {infoData?.version || 'N/A'}
                    </Typography>
                  )}
                </Box>
                <Typography variant="body2" color="text.secondary">
                  Version
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </Box>
      
      <Grid container spacing={{ xs: 2, sm: 3 }}>
        <Grid size={{ xs: 12, lg: 8 }}>
          <Card sx={{ mb: { xs: 2, sm: 3 } }}>
            <CardContent sx={{ p: { xs: 2, sm: 3 } }}>
              <Typography
                variant="h6"
                sx={{ mb: 3, display: "flex", alignItems: "center" }}
              >
                <Search sx={{ mr: 1 }} />
                Search Configuration
              </Typography>

              {validationError && (
                <Alert severity="error" sx={{ mb: 2 }}>
                  {validationError}
                </Alert>
              )}

              <Stack spacing={3}>
                <Stack
                  direction={{ xs: "column", sm: "row" }}
                  spacing={2}
                >
                  <TextField
                    label="Application"
                    size="small"
                    value={application}
                    onChange={(e) => {
                      setApplication(e.target.value);
                      setValidationError(null);
                    }}
                    sx={{ flex: 1 }}
                    placeholder="e.g., sample-service"
                    required
                    error={!!validationError && !application.trim()}
                    aria-label="Application name"
                    aria-required="true"
                  />
                  <TextField
                    label="Profile"
                    size="small"
                    value={profile}
                    onChange={(e) => {
                      setProfile(e.target.value);
                      setValidationError(null);
                    }}
                    sx={{ flex: 1 }}
                    placeholder="e.g., dev"
                    required
                    error={!!validationError && !profile.trim()}
                    aria-label="Profile"
                    aria-required="true"
                  />
                  <TextField
                    label="Label (optional)"
                    size="small"
                    value={label}
                    onChange={(e) => {
                      setLabel(e.target.value);
                      setValidationError(null);
                    }}
                    sx={{ flex: 1 }}
                    placeholder="e.g., v1.2.3"
                    aria-label="Label (optional)"
                  />
                  <Button
                    variant="contained"
                    onClick={validateAndGo}
                    className="btn-primary"
                    aria-label="View configuration"
                  >
                    View Config
                  </Button>
                </Stack>

                {popularApps.length > 0 && (
                  <PopularApplications
                    applications={popularApps}
                    onSelect={handlePopularAppSelect}
                    selectedApplication={application}
                  />
                )}

                <QuickFilterChips
                  profiles={["dev", "staging", "prod"]}
                  selectedProfile={profile}
                  onSelect={handleQuickFilterSelect}
                />
              </Stack>
            </CardContent>
          </Card>
        </Grid>

        <Grid size={{ xs: 12, lg: 4 }}>
          <Card>
            <CardContent sx={{ p: { xs: 2, sm: 3 } }}>
              <ConfigSearchHistory
                history={history}
                onSelect={handleHistorySelect}
                onRemove={removeEntry}
                onClear={clearHistory}
              />
            </CardContent>
          </Card>
        </Grid>
      </Grid>
      
      {/* <Box sx={{ mt: { xs: 3, sm: 4 } }}>
        <Grid container spacing={{ xs: 2, sm: 3 }}>
          <Grid size={{ xs: 12, sm: 6 }}>
            <Card>
              <CardContent sx={{ p: { xs: 2, sm: 3 } }}>
                <Typography variant="h6" sx={{ mb: 3, fontWeight: 600 }}>
                  Quick Applications
                </Typography>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                  {quickApplications.map((app) => (
                    <Chip
                      key={app}
                      label={app}
                      size="small"
                      variant="outlined"
                      onClick={() => setApplication(app)}
                      sx={{
                        cursor: 'pointer',
                        transition: 'all 0.2s',
                        '&:hover': {
                          bgcolor: 'primary.50',
                          borderColor: 'primary.300'
                        }
                      }}
                    />
                  ))}
                </Box>
              </CardContent>
            </Card>
          </Grid>
          
          <Grid size={{ xs: 12, sm: 6 }}>
            <Card>
              <CardContent sx={{ p: { xs: 2, sm: 3 } }}>
                <Typography variant="h6" sx={{ mb: 3, fontWeight: 600 }}>
                  Quick Profiles
                </Typography>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                  {quickProfiles.map((prof) => (
                    <Chip
                      key={prof}
                      label={prof}
                      size="small"
                      variant="outlined"
                      onClick={() => setProfile(prof)}
                      sx={{
                        cursor: 'pointer',
                        transition: 'all 0.2s',
                        '&:hover': {
                          bgcolor: 'primary.50',
                          borderColor: 'primary.300'
                        }
                      }}
                    />
                  ))}
                </Box>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </Box> */}
    </Box>
  )
}