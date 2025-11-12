import {
  Box,
  Card,
  CardContent,
  Typography,
  Alert,
  TextField,
  InputAdornment,
  MenuItem,
  Select,
  FormControl,
  InputLabel,
} from "@mui/material";
import SearchIcon from "@mui/icons-material/Search";
import { useListServiceRegistryServices } from "@lib/api/generated/service-registry/service-registry";
import PageHeader from "@components/common/PageHeader";
import { TableSkeleton } from "@components/common/skeletons";
import ConsulServiceTable from "../components/ConsulServiceTable";
import { getErrorMessage } from "@lib/api/errorHandler";
import { useState, useMemo } from "react";

export default function ServiceRegistryListPage() {
  const [searchTerm, setSearchTerm] = useState("");
  const [tagFilter, setTagFilter] = useState<string>("");

  const { data, isLoading, error } = useListServiceRegistryServices({
    query: {
      staleTime: 30_000, // 30 seconds
      refetchInterval: 30000, // Refresh every 30 seconds
      refetchIntervalInBackground: false,
    },
  });

  // Extract all unique tags for filter dropdown
  const allTags = useMemo(() => {
    if (!data?.services) return [];
    const tagSet = new Set<string>();
    Object.values(data.services).forEach((tags) => {
      if (Array.isArray(tags)) {
        tags.forEach((tag) => tagSet.add(tag));
      }
    });
    return Array.from(tagSet).sort();
  }, [data]);

  // Calculate filtered count (table handles filtering internally)
  const totalServiceCount = Object.keys(data?.services || {}).length;

  return (
    <Box>
      <PageHeader
        title="Service Registry (Consul)"
        subtitle="View services registered in Consul service discovery"
      />

      <Card>
        <CardContent>
          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              Failed to load services from Consul: {getErrorMessage(error)}
            </Alert>
          )}

          {!error && (
            <>
              <Box
                sx={{
                  mb: 3,
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  flexWrap: "wrap",
                  gap: 2,
                }}
              >
                <Typography variant="body2" color="text.secondary">
                  Total Services: {totalServiceCount}
                </Typography>
                <Box sx={{ display: "flex", gap: 2, flexWrap: "wrap" }}>
                  <TextField
                    placeholder="Search services..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    size="small"
                    sx={{ minWidth: 250 }}
                    slotProps={{
                      input: {
                        startAdornment: (
                          <InputAdornment position="start">
                            <SearchIcon />
                          </InputAdornment>
                        ),
                      },
                    }}
                    aria-label="Search services"
                  />
                  {allTags.length > 0 && (
                    <FormControl size="small" sx={{ minWidth: 200 }}>
                      <InputLabel id="tag-filter-label">Filter by Tag</InputLabel>
                      <Select
                        labelId="tag-filter-label"
                        value={tagFilter}
                        label="Filter by Tag"
                        onChange={(e) => setTagFilter(e.target.value)}
                        aria-label="Filter services by tag"
                      >
                        <MenuItem value="">
                          <em>All Tags</em>
                        </MenuItem>
                        {allTags.map((tag) => (
                          <MenuItem key={tag} value={tag}>
                            {tag}
                          </MenuItem>
                        ))}
                      </Select>
                    </FormControl>
                  )}
                </Box>
              </Box>

              {isLoading && <TableSkeleton rows={10} columns={4} />}

              {!isLoading && data && (
                <ConsulServiceTable
                  servicesData={data}
                  loading={isLoading}
                  searchTerm={searchTerm}
                  tagFilter={tagFilter || undefined}
                />
              )}
            </>
          )}
        </CardContent>
      </Card>
    </Box>
  );
}
