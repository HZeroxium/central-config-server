/**
 * KV Store List Page - Service selection
 */

import { useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  Box,
  Card,
  CardContent,
  TextField,
  Button,
  Typography,
  Alert,
} from "@mui/material";
import Grid from "@mui/material/Grid";
import {
  Search as SearchIcon,
  ArrowForward as ArrowForwardIcon,
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

  const handleServiceSelect = (serviceId: string) => {
    navigate(`/kv/${serviceId}`);
  };

  return (
    <Box sx={{ p: { xs: 2, sm: 3, md: 4 } }}>
      <PageHeader
        title="Key-Value Store"
        subtitle="Select a service to manage its key-value entries"
      />

      <Card sx={{ mt: 3 }}>
        <CardContent>
          <Box sx={{ mb: 3 }}>
            <TextField
              fullWidth
              placeholder="Search services..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              size="small"
              slotProps={{
                input: {
                  startAdornment: (
                    <Box sx={{ mr: 1, display: "flex", alignItems: "center" }}>
                      <SearchIcon fontSize="small" color="action" />
                    </Box>
                  ),
                },
              }}
            />
          </Box>

          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              Failed to load services:{" "}
              {error && typeof error === "object" && "detail" in error
                ? (error as { detail?: string }).detail
                : "Unknown error"}
            </Alert>
          )}

          {isLoading && <TableSkeleton rows={5} columns={3} />}

          {!isLoading && !error && services.length === 0 && (
            <Alert severity="info">
              {search
                ? "No services found matching your search"
                : "No services available"}
            </Alert>
          )}

          {!isLoading && !error && services.length > 0 && (
            <Grid container spacing={2}>
              {services.map((service: ApplicationServiceResponse) => (
                <Grid size={{ xs: 12, sm: 6, md: 4 }} key={service.id}>
                  <Card
                    sx={{
                      cursor: "pointer",
                      transition: "all 0.2s",
                      "&:hover": {
                        boxShadow: 3,
                        transform: "translateY(-2px)",
                      },
                    }}
                    onClick={() => handleServiceSelect(service.id || "")}
                  >
                    <CardContent>
                      <Box
                        sx={{
                          display: "flex",
                          justifyContent: "space-between",
                          alignItems: "start",
                        }}
                      >
                        <Box sx={{ flex: 1 }}>
                          <Typography variant="h6" sx={{ mb: 1 }}>
                            {service.attributes?.displayName || service.id}
                          </Typography>
                          <Typography
                            variant="body2"
                            color="text.secondary"
                            sx={{ mb: 1 }}
                          >
                            ID: {service.id}
                          </Typography>
                          {service.attributes?.description && (
                            <Typography
                              variant="body2"
                              color="text.secondary"
                              sx={{
                                display: "-webkit-box",
                                WebkitLineClamp: 2,
                                WebkitBoxOrient: "vertical",
                                overflow: "hidden",
                              }}
                            >
                              {service.attributes.description}
                            </Typography>
                          )}
                        </Box>
                        <Button
                          size="small"
                          endIcon={<ArrowForwardIcon />}
                          onClick={(e) => {
                            e.stopPropagation();
                            handleServiceSelect(service.id || "");
                          }}
                        >
                          Open
                        </Button>
                      </Box>
                    </CardContent>
                  </Card>
                </Grid>
              ))}
            </Grid>
          )}
        </CardContent>
      </Card>
    </Box>
  );
}

