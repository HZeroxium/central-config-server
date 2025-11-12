/**
 * KV Store List Page - Service selection
 */

import { useState, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import {
  Box,
  Card,
  CardContent,
  TextField,
  Button,
  Typography,
  Alert,
  Chip,
  Stack,
  IconButton,
  InputAdornment,
  Divider,
  Paper,
} from "@mui/material";
import Grid from "@mui/material/Grid";
import {
  Search as SearchIcon,
  ArrowForward as ArrowForwardIcon,
  Clear as ClearIcon,
  Storage as StorageIcon,
  Apps as AppsIcon,
  AccessTime as TimeIcon,
  Tag as TagIcon,
  Public as PublicIcon,
  Code as CodeIcon,
} from "@mui/icons-material";
import { PageHeader } from "@components/common/PageHeader";
import { TableSkeleton } from "@components/common/skeletons";
import { useFindAllApplicationServices } from "@lib/api/hooks";
import { useDebounce } from "@hooks/useDebounce";
import type { ApplicationServiceResponse } from "@lib/api/models";

export default function KVStoreListPage() {
  const navigate = useNavigate();
  const [search, setSearch] = useState("");
  const debouncedSearch = useDebounce(search, 400);

  const { data, isLoading, error } = useFindAllApplicationServices(
    {
      search: debouncedSearch || undefined,
      page: 0,
      size: 100,
    },
    {
      query: {
        staleTime: 30_000,
      },
    }
  );

  const services = data?.items || [];
  const totalCount = data?.metadata?.totalElements || services.length;

  // Calculate statistics
  const stats = useMemo(() => {
    const now = Date.now();
    const oneDayAgo = now - 24 * 60 * 60 * 1000;
    
    const recentServices = services.filter((service) => {
      if (!service.updatedAt) return false;
      const updated = new Date(service.updatedAt).getTime();
      return updated > oneDayAgo;
    });

    const lifecycleCounts = services.reduce((acc, service) => {
      const lifecycle = service.lifecycle || "unknown";
      acc[lifecycle] = (acc[lifecycle] || 0) + 1;
      return acc;
    }, {} as Record<string, number>);

    return {
      total: totalCount,
      recent: recentServices.length,
      lifecycleCounts,
    };
  }, [services, totalCount]);

  const handleServiceSelect = (serviceId: string) => {
    navigate(`/kv/${serviceId}`);
  };

  const handleClearSearch = () => {
    setSearch("");
  };

  return (
    <Box sx={{ p: { xs: 2, sm: 3, md: 4 } }}>
      <PageHeader
        title="Key-Value Store"
        subtitle="Select a service to manage its key-value entries"
      />

      {/* Statistics Section */}
      {!isLoading && !error && (
        <Grid container spacing={2} sx={{ mt: 2, mb: 3 }}>
          <Grid size={{ xs: 12, sm: 6, md: 3 }}>
            <Paper
              elevation={0}
              sx={{
                p: 2,
                border: 1,
                borderColor: "divider",
                borderRadius: 2,
                bgcolor: "background.paper",
              }}
            >
              <Stack direction="row" spacing={2} alignItems="center">
                <Box
                  sx={{
                    p: 1.5,
                    borderRadius: 2,
                    bgcolor: "primary.main",
                    color: "primary.contrastText",
                  }}
                >
                  <StorageIcon />
                </Box>
                <Box>
                  <Typography variant="h4" fontWeight={700}>
                    {stats.total}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    Total Services
                  </Typography>
                </Box>
              </Stack>
            </Paper>
          </Grid>
          <Grid size={{ xs: 12, sm: 6, md: 3 }}>
            <Paper
              elevation={0}
              sx={{
                p: 2,
                border: 1,
                borderColor: "divider",
                borderRadius: 2,
                bgcolor: "background.paper",
              }}
            >
              <Stack direction="row" spacing={2} alignItems="center">
                <Box
                  sx={{
                    p: 1.5,
                    borderRadius: 2,
                    bgcolor: "secondary.main",
                    color: "secondary.contrastText",
                  }}
                >
                  <TimeIcon />
                </Box>
                <Box>
                  <Typography variant="h4" fontWeight={700}>
                    {stats.recent}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    Updated Today
                  </Typography>
                </Box>
              </Stack>
            </Paper>
          </Grid>
          {Object.keys(stats.lifecycleCounts).length > 0 && (
            <Grid size={{ xs: 12, sm: 6, md: 3 }}>
              <Paper
                elevation={0}
                sx={{
                  p: 2,
                  border: 1,
                  borderColor: "divider",
                  borderRadius: 2,
                  bgcolor: "background.paper",
                }}
              >
                <Stack direction="row" spacing={2} alignItems="center">
                  <Box
                    sx={{
                      p: 1.5,
                      borderRadius: 2,
                      bgcolor: "info.main",
                      color: "info.contrastText",
                    }}
                  >
                    <AppsIcon />
                  </Box>
                  <Box>
                    <Typography variant="h4" fontWeight={700}>
                      {Object.keys(stats.lifecycleCounts).length}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      Lifecycle Stages
                    </Typography>
                  </Box>
                </Stack>
              </Paper>
            </Grid>
          )}
        </Grid>
      )}

      <Card sx={{ mt: 3 }}>
        <CardContent>
          <Box sx={{ mb: 3 }}>
            <TextField
              fullWidth
              placeholder="Search services by name, ID, or description..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              size="small"
              slotProps={{
                input: {
                  startAdornment: (
                    <InputAdornment position="start">
                      <SearchIcon fontSize="small" color="action" />
                    </InputAdornment>
                  ),
                  endAdornment: search ? (
                    <InputAdornment position="end">
                      <IconButton
                        size="small"
                        onClick={handleClearSearch}
                        aria-label="Clear search"
                      >
                        <ClearIcon fontSize="small" />
                      </IconButton>
                    </InputAdornment>
                  ) : undefined,
                },
              }}
            />
          </Box>

          {isLoading && (
            <Grid container spacing={2}>
              {[1, 2, 3, 4, 5, 6].map((i) => (
                <Grid size={{ xs: 12, sm: 6, md: 4 }} key={i}>
                  <Card>
                    <CardContent>
                      <TableSkeleton rows={1} columns={1} />
                    </CardContent>
                  </Card>
                </Grid>
              ))}
            </Grid>
          )}

          {!isLoading && !error && services.length === 0 && (
            <Box sx={{ textAlign: "center", py: 6 }}>
              <StorageIcon
                sx={{
                  fontSize: 64,
                  color: "text.secondary",
                  mb: 2,
                  opacity: 0.5,
                }}
              />
              <Typography variant="h6" color="text.secondary" gutterBottom>
                {search
                  ? "No services found"
                  : "No services available"}
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                {search
                  ? "Try adjusting your search criteria or clear the search to see all services."
                  : "Services will appear here once they are registered in the system."}
              </Typography>
              {search && (
                <Button
                  variant="outlined"
                  startIcon={<ClearIcon />}
                  onClick={handleClearSearch}
                >
                  Clear Search
                </Button>
              )}
            </Box>
          )}

          {error && (
            <Box sx={{ textAlign: "center", py: 4 }}>
              <Alert
                severity="error"
                sx={{ mb: 2 }}
                action={
                  <Button
                    color="inherit"
                    size="small"
                    onClick={() => window.location.reload()}
                  >
                    Retry
                  </Button>
                }
              >
                Failed to load services:{" "}
                {error && typeof error === "object" && "detail" in error
                  ? (error as { detail?: string }).detail
                  : "Unknown error"}
              </Alert>
            </Box>
          )}

          {!isLoading && !error && services.length > 0 && (
            <Grid container spacing={2}>
              {services.map((service: ApplicationServiceResponse) => {
                const displayName = service.attributes?.displayName || service.displayName || service.id;
                const serviceId = service.id || "";
                const hasTags = service.tags && service.tags.length > 0;
                const hasEnvironments = service.environments && service.environments.length > 0;
                const isOrphan = !service.ownerTeamId;

                return (
                  <Grid size={{ xs: 12, sm: 6, md: 4 }} key={service.id}>
                    <Card
                      sx={{
                        cursor: "pointer",
                        transition: "all 0.2s ease-in-out",
                        height: "100%",
                        display: "flex",
                        flexDirection: "column",
                        "&:hover": {
                          boxShadow: 4,
                          transform: "translateY(-4px)",
                        },
                      }}
                      onClick={() => handleServiceSelect(serviceId)}
                    >
                      <CardContent sx={{ flex: 1, display: "flex", flexDirection: "column" }}>
                        <Box
                          sx={{
                            display: "flex",
                            justifyContent: "space-between",
                            alignItems: "start",
                            mb: 2,
                          }}
                        >
                          <Box sx={{ flex: 1, minWidth: 0 }}>
                            <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
                              <StorageIcon fontSize="small" color="primary" />
                              <Typography
                                variant="h6"
                                sx={{
                                  fontWeight: 600,
                                  overflow: "hidden",
                                  textOverflow: "ellipsis",
                                  whiteSpace: "nowrap",
                                }}
                              >
                                {displayName}
                              </Typography>
                            </Stack>
                            <Typography
                              variant="caption"
                              color="text.secondary"
                              sx={{
                                fontFamily: "monospace",
                                display: "block",
                                mb: 1,
                                wordBreak: "break-all",
                              }}
                            >
                              {serviceId}
                            </Typography>
                          </Box>
                          <Button
                            size="small"
                            variant="contained"
                            endIcon={<ArrowForwardIcon />}
                            onClick={(e) => {
                              e.stopPropagation();
                              handleServiceSelect(serviceId);
                            }}
                            sx={{ ml: 1, flexShrink: 0 }}
                          >
                            Open
                          </Button>
                        </Box>

                        {service.attributes?.description && (
                          <Typography
                            variant="body2"
                            color="text.secondary"
                            sx={{
                              mb: 2,
                              display: "-webkit-box",
                              WebkitLineClamp: 2,
                              WebkitBoxOrient: "vertical",
                              overflow: "hidden",
                              flex: 1,
                            }}
                          >
                            {service.attributes.description}
                          </Typography>
                        )}

                        <Divider sx={{ my: 1.5 }} />

                        <Stack spacing={1}>
                          {service.lifecycle && (
                            <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                              <AppsIcon fontSize="small" color="action" />
                              <Chip
                                label={service.lifecycle}
                                size="small"
                                variant="outlined"
                                color="primary"
                              />
                            </Box>
                          )}

                          {isOrphan && (
                            <Chip
                              label="Orphan"
                              size="small"
                              color="warning"
                              variant="outlined"
                              icon={<PublicIcon fontSize="small" />}
                            />
                          )}

                          {hasTags && (
                            <Box sx={{ display: "flex", alignItems: "center", gap: 0.5, flexWrap: "wrap" }}>
                              <TagIcon fontSize="small" color="action" />
                              {service.tags?.slice(0, 3).map((tag) => (
                                <Chip
                                  key={tag}
                                  label={tag}
                                  size="small"
                                  variant="outlined"
                                  sx={{ height: 20, fontSize: "0.65rem" }}
                                />
                              ))}
                              {service.tags && service.tags.length > 3 && (
                                <Typography variant="caption" color="text.secondary">
                                  +{service.tags.length - 3} more
                                </Typography>
                              )}
                            </Box>
                          )}

                          {hasEnvironments && (
                            <Box sx={{ display: "flex", alignItems: "center", gap: 0.5, flexWrap: "wrap" }}>
                              <CodeIcon fontSize="small" color="action" />
                              {service.environments?.slice(0, 2).map((env) => (
                                <Chip
                                  key={env}
                                  label={env}
                                  size="small"
                                  variant="outlined"
                                  color="info"
                                  sx={{ height: 20, fontSize: "0.65rem" }}
                                />
                              ))}
                              {service.environments && service.environments.length > 2 && (
                                <Typography variant="caption" color="text.secondary">
                                  +{service.environments.length - 2} more
                                </Typography>
                              )}
                            </Box>
                          )}

                          {service.repoUrl && (
                            <Typography
                              variant="caption"
                              color="text.secondary"
                              sx={{
                                display: "flex",
                                alignItems: "center",
                                gap: 0.5,
                                overflow: "hidden",
                                textOverflow: "ellipsis",
                                whiteSpace: "nowrap",
                              }}
                            >
                              <CodeIcon fontSize="small" />
                              {service.repoUrl}
                            </Typography>
                          )}
                        </Stack>
                      </CardContent>
                    </Card>
                  </Grid>
                );
              })}
            </Grid>
          )}
        </CardContent>
      </Card>
    </Box>
  );
}

