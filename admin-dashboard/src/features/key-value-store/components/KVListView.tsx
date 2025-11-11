/**
 * Read-only view component for KV List structure
 */

import { useMemo } from "react";
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  Stack,
  Alert,
  CircularProgress,
  IconButton,
  Tooltip,
  Skeleton,
} from "@mui/material";
import {
  Edit as EditIcon,
  Refresh as RefreshIcon,
  List as ListIcon,
} from "@mui/icons-material";
import { useGetKVList, type GetKVListParams } from "../hooks";
import { fromGeneratedKVListItemArray, fromKVListManifestMetadata } from "../utils/typeAdapters";
import type { UIListItem } from "../utils/typeAdapters";

export interface KVListViewProps {
  serviceId: string;
  prefix: string;
  /** Callback to switch to edit mode */
  onEdit?: () => void;
  /** Callback to refresh data */
  onRefresh?: () => void;
  /** Whether this is read-only */
  isReadOnly?: boolean;
  /** Loading state override */
  isLoading?: boolean;
  /** Error state override */
  error?: Error | null;
}

export function KVListView({
  serviceId,
  prefix,
  onEdit,
  onRefresh,
  isReadOnly = false,
  isLoading: providedLoading,
  error: providedError,
}: KVListViewProps) {
  const params: GetKVListParams | undefined = serviceId && prefix
    ? { prefix, consistent: false, stale: false }
    : undefined;

  const {
    data: listData,
    isLoading: isLoadingList,
    error: listError,
    refetch,
  } = useGetKVList(
    serviceId,
    params,
    {
      query: {
        enabled: !!serviceId && !!prefix,
        staleTime: 10_000,
      },
    }
  );

  const isLoading = providedLoading !== undefined ? providedLoading : isLoadingList;
  const error = providedError || listError;

  const handleRefresh = () => {
    refetch();
    onRefresh?.();
  };

  // Convert loaded data to UI representation
  const items: UIListItem[] = useMemo(() => {
    if (!listData?.items) return [];
    return fromGeneratedKVListItemArray(listData.items);
  }, [listData?.items]);

  const manifest = useMemo(() => {
    if (!listData?.manifest) return null;
    return {
      order: listData.manifest.order ?? [],
      version: listData.manifest.version ?? 0,
      etag: listData.manifest.etag ?? null,
      metadata: listData.manifest.metadata
        ? fromKVListManifestMetadata(listData.manifest.metadata)
        : undefined,
    };
  }, [listData?.manifest]);

  // Get items in order (if manifest.order exists)
  const orderedItems = useMemo(() => {
    if (!manifest?.order || manifest.order.length === 0) {
      return items;
    }
    // Sort items according to manifest order
    const itemMap = new Map(items.map((item) => [item.id, item]));
    const ordered: UIListItem[] = [];
    // Add items in manifest order
    manifest.order.forEach((id) => {
      const item = itemMap.get(id);
      if (item) {
        ordered.push(item);
        itemMap.delete(id);
      }
    });
    // Add remaining items (not in manifest order)
    itemMap.forEach((item) => ordered.push(item));
    return ordered;
  }, [items, manifest]);

  // Loading skeleton
  if (isLoading) {
    return (
      <Card>
        <CardContent>
          {/* Header skeleton */}
          <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 3 }}>
            <Stack direction="row" spacing={2} alignItems="center">
              <Skeleton variant="text" width={120} height={32} />
              <Skeleton variant="rectangular" width={60} height={24} sx={{ borderRadius: 1 }} />
              <Skeleton variant="rectangular" width={80} height={24} sx={{ borderRadius: 1 }} />
            </Stack>
            <Stack direction="row" spacing={1}>
              <Skeleton variant="circular" width={32} height={32} />
              <Skeleton variant="rectangular" width={80} height={32} sx={{ borderRadius: 1 }} />
            </Stack>
          </Box>
          
          {/* Metadata skeleton */}
          <Box sx={{ mb: 3 }}>
            <Skeleton variant="text" width={80} height={20} sx={{ mb: 1 }} />
            <Stack direction="row" spacing={2}>
              <Skeleton variant="text" width={100} height={24} />
              <Skeleton variant="text" width={150} height={24} />
            </Stack>
          </Box>

          {/* Table skeleton */}
          <TableContainer component={Paper} variant="outlined">
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>
                    <Skeleton variant="text" width={60} height={20} />
                  </TableCell>
                  <TableCell>
                    <Skeleton variant="text" width={80} height={20} />
                  </TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {[1, 2, 3, 4, 5].map((row) => (
                  <TableRow key={row}>
                    <TableCell>
                      <Skeleton variant="text" width={120} height={20} />
                    </TableCell>
                    <TableCell>
                      <Skeleton variant="rectangular" width="100%" height={60} sx={{ borderRadius: 1 }} />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>
    );
  }

  // Error state with retry
  if (error) {
    const errorMessage = error instanceof Error 
      ? error.message 
      : typeof error === "object" && error !== null && "message" in error
      ? String(error.message)
      : "Unknown error occurred";
    
    const isNotFound = errorMessage.toLowerCase().includes("not found") || 
                      errorMessage.toLowerCase().includes("404");
    
    return (
      <Card>
        <CardContent>
          <Alert 
            severity={isNotFound ? "warning" : "error"} 
            sx={{ mb: 2 }}
            action={
              <Button 
                color="inherit" 
                size="small" 
                onClick={handleRefresh}
                startIcon={<RefreshIcon />}
              >
                Retry
              </Button>
            }
          >
            <Typography variant="body2" fontWeight={600} gutterBottom>
              {isNotFound ? "List not found" : "Failed to load list"}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {errorMessage}
            </Typography>
          </Alert>
          <Button 
            variant="outlined" 
            onClick={handleRefresh}
            startIcon={<RefreshIcon />}
            fullWidth
          >
            Retry Loading
          </Button>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardContent>
        <Box
          sx={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            mb: 3,
          }}
        >
          <Stack direction="row" spacing={2} alignItems="center">
            <Typography variant="h6">List View</Typography>
            <Chip label="LIST" size="small" color="secondary" />
            {manifest && (
              <>
                <Chip
                  label={`${items.length} items`}
                  size="small"
                  variant="outlined"
                />
                {manifest.version !== undefined && (
                  <Chip
                    label={`v${manifest.version}`}
                    size="small"
                    variant="outlined"
                  />
                )}
              </>
            )}
          </Stack>
          <Stack direction="row" spacing={1}>
            <Tooltip title="Refresh">
              <IconButton 
                size="small" 
                onClick={handleRefresh} 
                aria-label="Refresh"
                disabled={isLoadingList}
              >
                {isLoadingList ? (
                  <CircularProgress size={16} />
                ) : (
                  <RefreshIcon fontSize="small" />
                )}
              </IconButton>
            </Tooltip>
            {!isReadOnly && onEdit && (
              <Button
                variant="contained"
                startIcon={<EditIcon />}
                onClick={onEdit}
                size="small"
              >
                Edit
              </Button>
            )}
          </Stack>
        </Box>

        {/* Metadata */}
        {manifest && (
          <Box sx={{ mb: 3 }}>
            <Typography variant="subtitle2" color="text.secondary" gutterBottom>
              Metadata
            </Typography>
            <Stack direction="row" spacing={2} flexWrap="wrap">
              <Box>
                <Typography variant="caption" color="text.secondary">
                  Version
                </Typography>
                <Typography variant="body2">{manifest.version ?? 0}</Typography>
              </Box>
              {manifest.etag && (
                <Box>
                  <Typography variant="caption" color="text.secondary">
                    ETag
                  </Typography>
                  <Typography variant="body2" sx={{ fontFamily: "monospace", fontSize: "0.75rem" }}>
                    {manifest.etag}
                  </Typography>
                </Box>
              )}
              {manifest.metadata && Object.keys(manifest.metadata).length > 0 && (
                <Box>
                  <Typography variant="caption" color="text.secondary">
                    Custom Metadata
                  </Typography>
                  <Typography variant="body2" sx={{ fontFamily: "monospace", fontSize: "0.75rem" }}>
                    {JSON.stringify(manifest.metadata, null, 2)}
                  </Typography>
                </Box>
              )}
            </Stack>
          </Box>
        )}

        {/* Items Table */}
        {items.length === 0 ? (
          <Alert 
            severity="info" 
            icon={<ListIcon />}
            sx={{ 
              py: 3,
              "& .MuiAlert-message": {
                width: "100%",
              },
            }}
          >
            <Typography variant="body1" fontWeight={600} gutterBottom>
              Empty List
            </Typography>
            <Typography variant="body2" color="text.secondary">
              This list structure exists but contains no items. Click "Edit" to add items.
            </Typography>
          </Alert>
        ) : (
          <TableContainer 
            component={Paper} 
            variant="outlined"
            sx={{
              maxHeight: "calc(100vh - 400px)",
              overflow: "auto",
            }}
          >
            <Table size="small" stickyHeader>
              <TableHead>
                <TableRow>
                  <TableCell sx={{ fontWeight: 600, minWidth: 150 }}>ID</TableCell>
                  <TableCell sx={{ fontWeight: 600 }}>Data</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {orderedItems.map((item) => (
                  <TableRow 
                    key={item.id} 
                    hover
                    sx={{
                      "&:last-child td": { borderBottom: 0 },
                    }}
                  >
                    <TableCell>
                      <Typography
                        variant="body2"
                        sx={{
                          fontFamily: "monospace",
                          fontWeight: 500,
                          wordBreak: "break-word",
                        }}
                      >
                        {item.id}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Box
                        sx={{
                          maxWidth: "100%",
                          overflow: "auto",
                        }}
                      >
                        <Typography
                          variant="body2"
                          component="pre"
                          sx={{
                            fontFamily: "monospace",
                            fontSize: "0.75rem",
                            whiteSpace: "pre-wrap",
                            wordBreak: "break-word",
                            m: 0,
                            p: 1.5,
                            bgcolor: "background.default",
                            borderRadius: 1,
                            border: 1,
                            borderColor: "divider",
                          }}
                        >
                          {JSON.stringify(item.data, null, 2)}
                        </Typography>
                      </Box>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </CardContent>
    </Card>
  );
}

