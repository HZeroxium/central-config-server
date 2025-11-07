/**
 * KV Store Page - Main KV store interface
 */

import { useState, useEffect, useMemo } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  Box,
  Card,
  CardContent,
  Typography,
  Alert,
  Button,
} from "@mui/material";
import Grid from "@mui/material/Grid";
import {
  Add as AddIcon,
  ArrowBack as BackIcon,
} from "@mui/icons-material";
import { PageHeader } from "@components/common/PageHeader";
import { DetailPageSkeleton } from "@components/common/skeletons";
import ErrorFallback from "@components/common/ErrorFallback";
import {
  KVTreeView,
  KVDetailPanel,
  KVEntryEditor,
  KVSearchBar,
  KVBulkActions,
} from "../components";
import { useKVStore, useKVPermissions, useKVTree } from "../hooks";
import { normalizePath, encodePath, decodePath } from "../types";
import { useFindApplicationServiceById } from "@lib/api/hooks";
import type { KVEntry } from "../types";
import type { KVPutRequest } from "@lib/api/models";

export interface KVStorePageProps {
  /** Service ID (when used as tab, overrides URL param) */
  serviceId?: string;
  /** Hide page header (when used as tab) */
  hideHeader?: boolean;
}

export default function KVStorePage({
  serviceId: propServiceId,
  hideHeader = false,
}: KVStorePageProps = {}) {
  const { serviceId: urlServiceId, "*": pathParam } = useParams<{
    serviceId: string;
    "*"?: string;
  }>();
  const serviceId = propServiceId || urlServiceId || "";
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedPath, setSelectedPath] = useState<string | undefined>(
    !propServiceId && pathParam ? decodePath(pathParam) : undefined
  );
  const [createMode, setCreateMode] = useState(false);
  const [selectedPaths, setSelectedPaths] = useState<Set<string>>(new Set());
  const [prefix] = useState("");

  // Fetch service info
  const {
    data: service,
    isLoading: serviceLoading,
    error: serviceError,
  } = useFindApplicationServiceById(serviceId || "", {
    query: {
      enabled: !!serviceId,
      staleTime: 30_000,
    },
  });

  // Check permissions
  const permissions = useKVPermissions(serviceId || "");

  // KV Store operations
  const {
    entries: listData,
    entriesLoading,
    entriesError,
    refetchEntries,
    entry,
    entryLoading,
    refetchEntry,
    putEntry,
    deleteEntry,
    isPutting,
    isDeleting,
  } = useKVStore({
    serviceId: serviceId || "",
    path: selectedPath,
    listParams: {
      prefix,
      recurse: true,
      keysOnly: false,
    },
    getParams: {
      raw: false,
    },
  });

  // Determine entries from list response
  const entries: KVEntry[] = useMemo(() => {
    if (!listData) return [];
    // Check if it's KVListResponse or KVKeysResponse
    if ("items" in listData && Array.isArray(listData.items)) {
      return listData.items;
    }
    return [];
  }, [listData]);

  // Build tree
  const {
    tree,
    expandedNodes,
    toggleNode,
  } = useKVTree({
    entries,
    prefix,
    searchQuery,
  });

  // Update selected path when URL changes (only when not using prop serviceId)
  useEffect(() => {
    if (!propServiceId && pathParam) {
      const decoded = decodePath(pathParam);
      setSelectedPath(decoded);
      setCreateMode(false);
    } else if (!propServiceId) {
      setSelectedPath(undefined);
      setCreateMode(false);
    }
  }, [pathParam, propServiceId]);

  // Navigate to path
  const handlePathSelect = (path: string) => {
    const normalized = normalizePath(path);
    if (propServiceId) {
      // When used as tab, update state only (no URL navigation)
      setSelectedPath(normalized);
      setCreateMode(false);
    } else {
      // When used standalone, navigate URL
      const encoded = encodePath(normalized);
      navigate(`/kv/${serviceId}/${encoded}`, { replace: true });
      setSelectedPath(normalized);
      setCreateMode(false);
    }
  };

  // Handle create
  const handleCreate = async (data: KVPutRequest) => {
    if (!selectedPath) return;
    await putEntry(selectedPath, data);
    setCreateMode(false);
    // Navigate to the created entry
    handlePathSelect(selectedPath);
  };

  // Handle edit
  const handleEdit = async (data: KVPutRequest) => {
    if (!selectedPath) return;
    await putEntry(selectedPath, data);
  };

  // Handle delete
  const handleDelete = async () => {
    if (!selectedPath) return;
    await deleteEntry(selectedPath);
    // Navigate back to root
    if (propServiceId) {
      setSelectedPath(undefined);
    } else {
      navigate(`/kv/${serviceId}`, { replace: true });
      setSelectedPath(undefined);
    }
  };

  // Handle bulk delete
  const handleBulkDelete = async (paths: string[], recursive: boolean) => {
    for (const path of paths) {
      await deleteEntry(path, { recurse: recursive });
    }
    setSelectedPaths(new Set());
    refetchEntries();
  };

  // Handle new entry
  const handleNewEntry = () => {
    const newPath = prefix ? `${prefix}/new-key` : "new-key";
    if (propServiceId) {
      setSelectedPath(newPath);
      setCreateMode(true);
    } else {
      const encoded = encodePath(newPath);
      navigate(`/kv/${serviceId}/${encoded}`, { replace: true });
      setSelectedPath(newPath);
      setCreateMode(true);
    }
  };

  if (serviceLoading) {
    return <DetailPageSkeleton />;
  }

  if (serviceError || !serviceId) {
    return (
      <ErrorFallback
        message={
          serviceError && typeof serviceError === "object" && "message" in serviceError
            ? (serviceError.message as string)
            : "Service not found"
        }
        onRetry={() => window.location.reload()}
      />
    );
  }

  if (!permissions.canView) {
    return (
      <Box sx={{ p: { xs: 2, sm: 3, md: 4 } }}>
        <Alert severity="error">
          You do not have permission to view this service's KV store
        </Alert>
      </Box>
    );
  }

  return (
    <Box sx={{ p: { xs: 2, sm: 3, md: 4 } }}>
      {!hideHeader && (
        <PageHeader
          title={`Key-Value Store: ${service?.attributes?.displayName || serviceId}`}
          subtitle="Manage key-value entries for this service"
          actions={
            <Button
              variant="outlined"
              startIcon={<BackIcon />}
              onClick={() => navigate("/kv")}
            >
              Back to Services
            </Button>
          }
        />
      )}

      {entriesError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          Failed to load entries:{" "}
          {entriesError && typeof entriesError === "object" && "detail" in entriesError
            ? (entriesError.detail as string)
            : "Unknown error"}
        </Alert>
      )}

      {permissions.isReadOnly && (
        <Alert severity="info" sx={{ mb: 2 }}>
          You have read-only access to this service's KV store
        </Alert>
      )}

      <Grid container spacing={2} sx={{ mt: 1 }}>
        {/* Left Panel - Tree View */}
        <Grid size={{ xs: 12, md: 4 }}>
          <Card>
            <CardContent sx={{ p: 0 }}>
              <Box sx={{ p: 2, borderBottom: 1, borderColor: "divider" }}>
                <Box
                  sx={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    mb: 2,
                  }}
                >
                  <Typography variant="h6">Navigation</Typography>
                  {!permissions.isReadOnly && (
                    <Button
                      size="small"
                      startIcon={<AddIcon />}
                      onClick={handleNewEntry}
                      variant="contained"
                    >
                      New Entry
                    </Button>
                  )}
                </Box>
                <KVSearchBar
                  value={searchQuery}
                  onChange={setSearchQuery}
                  placeholder="Search by prefix..."
                />
              </Box>

              <KVBulkActions
                entries={entries}
                selectedPaths={selectedPaths}
                onSelectionChange={setSelectedPaths}
                onBulkDelete={handleBulkDelete}
                isDeleting={isDeleting}
              />

              <Box sx={{ maxHeight: "calc(100vh - 400px)", overflow: "auto" }}>
                <KVTreeView
                  tree={tree}
                  selectedPath={selectedPath}
                  onSelect={handlePathSelect}
                  expandedNodes={expandedNodes}
                  onToggleNode={toggleNode}
                  isLoading={entriesLoading}
                  searchQuery={searchQuery}
                />
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* Right Panel - Detail View */}
        <Grid size={{ xs: 12, md: 8 }}>
          {createMode ? (
            <Card>
              <CardContent>
                <KVEntryEditor
                  path={selectedPath || ""}
                  onSave={handleCreate}
                  onCancel={() => {
                    setCreateMode(false);
                    if (!propServiceId) {
                      navigate(`/kv/${serviceId}`, { replace: true });
                    }
                  }}
                  isReadOnly={permissions.isReadOnly}
                  isSaving={isPutting}
                />
              </CardContent>
            </Card>
          ) : (
            <KVDetailPanel
              entry={entry && typeof entry === "object" && "path" in entry ? entry : undefined}
              path={selectedPath || ""}
              onEdit={handleEdit}
              onDelete={handleDelete}
              onRefresh={refetchEntry}
              isReadOnly={permissions.isReadOnly}
              isEditing={isPutting}
              isDeleting={isDeleting}
              isLoading={entryLoading}
            />
          )}
        </Grid>
      </Grid>
    </Box>
  );
}

