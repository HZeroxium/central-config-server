/**
 * KV Store Page - Main KV store interface
 */

import { useState, useEffect } from "react";
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
  KVBreadcrumb,
  KVFlatListView,
} from "../components";
import { useKVStore, useKVPermissions, useKVTree } from "../hooks";
import {
  normalizePath,
  encodePath,
  decodePath,
  getParentPath,
} from "../types";
import { useFindApplicationServiceById } from "@lib/api/hooks";
import type { KVPutRequest } from "@lib/api/models";
import { ToggleButton, ToggleButtonGroup } from "@mui/material";
import { ViewList as ViewListIcon, AccountTree as TreeIcon } from "@mui/icons-material";

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
  const [currentPrefix, setCurrentPrefix] = useState("");
  const [selectedPath, setSelectedPath] = useState<string | undefined>(
    !propServiceId && pathParam ? decodePath(pathParam) : undefined
  );
  const [createMode, setCreateMode] = useState(false);
  const [viewMode, setViewMode] = useState<"tree" | "flat">("flat");

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

  // KV Store operations - optimized queries
  const {
    keys,
    keysLoading,
    keysError,
    refetchKeys,
    entry,
    entryLoading,
    refetchEntry,
    putEntry,
    deleteEntry,
    isPutting,
    isDeleting,
  } = useKVStore({
    serviceId: serviceId || "",
    prefix: currentPrefix,
    selectedPath: selectedPath && !createMode ? selectedPath : undefined,
    getParams: {
      raw: false,
    },
  });

  // Build tree from keys
  const {
    tree,
    expandedNodes,
    toggleNode,
  } = useKVTree({
    keys,
    prefix: currentPrefix,
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

  // Navigate to folder or select file
  const handlePathSelect = (path: string, isFolder: boolean) => {
    const normalized = normalizePath(path);
    
    if (isFolder) {
      // Navigate into folder
      setCurrentPrefix(normalized);
      setSelectedPath(undefined);
      setCreateMode(false);
    } else {
      // Select file to view/edit
      setSelectedPath(normalized);
      setCreateMode(false);
      if (!propServiceId) {
        const encoded = encodePath(normalized);
        navigate(`/kv/${serviceId}/${encoded}`, { replace: true });
      }
    }
  };

  // Navigate to prefix (for breadcrumb)
  const handlePrefixNavigate = (prefix: string) => {
    setCurrentPrefix(prefix);
    setSelectedPath(undefined);
    setCreateMode(false);
  };

  // Navigate back (go to parent folder)
  const handleBack = () => {
    const parentPrefix = getParentPath(currentPrefix);
    handlePrefixNavigate(parentPrefix);
  };

  // Handle create
  const handleCreate = async (path: string, data: KVPutRequest) => {
    await putEntry(path, data);
    setCreateMode(false);
    // Refresh keys and navigate to the created entry
    await refetchKeys();
    handlePathSelect(path, false);
  };

  // Handle edit
  const handleEdit = async (_path: string, data: KVPutRequest) => {
    if (!selectedPath) return;
    await putEntry(selectedPath, data);
    await refetchKeys();
  };

  // Handle delete
  const handleDelete = async () => {
    if (!selectedPath) return;
    await deleteEntry(selectedPath);
    // Refresh keys and clear selection
    await refetchKeys();
    setSelectedPath(undefined);
    setCreateMode(false);
  };

  // Handle new entry
  const handleNewEntry = () => {
    setCreateMode(true);
    setSelectedPath(undefined);
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

      {keysError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          Failed to load keys:{" "}
          {keysError && typeof keysError === "object" && "detail" in keysError
            ? (keysError.detail as string)
            : "Unknown error"}
        </Alert>
      )}

      {permissions.isReadOnly && (
        <Alert severity="info" sx={{ mb: 2 }}>
          You have read-only access to this service's KV store
        </Alert>
      )}

      <Grid container spacing={2} sx={{ mt: 1 }}>
        {/* Left Panel - Navigation */}
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

                {/* Breadcrumb */}
                <Box sx={{ mb: 2 }}>
                  <KVBreadcrumb
                    prefix={currentPrefix}
                    onNavigate={handlePrefixNavigate}
                    onBack={handleBack}
                    showBackButton={!!currentPrefix}
                  />
                </Box>

                {/* View mode toggle */}
                <Box sx={{ mb: 2, display: "flex", justifyContent: "flex-end" }}>
                  <ToggleButtonGroup
                    value={viewMode}
                    exclusive
                    onChange={(_, newMode) => newMode && setViewMode(newMode)}
                    size="small"
                    aria-label="view mode"
                  >
                    <ToggleButton value="flat" aria-label="flat list">
                      <ViewListIcon fontSize="small" />
                    </ToggleButton>
                    <ToggleButton value="tree" aria-label="tree view">
                      <TreeIcon fontSize="small" />
                    </ToggleButton>
                  </ToggleButtonGroup>
                </Box>

                <KVSearchBar
                  value={searchQuery}
                  onChange={setSearchQuery}
                  placeholder="Search by prefix..."
                />
              </Box>

              <Box sx={{ maxHeight: "calc(100vh - 500px)", overflow: "auto" }}>
                {viewMode === "flat" ? (
                  <KVFlatListView
                    keys={keys}
                    prefix={currentPrefix}
                    selectedPath={selectedPath}
                    onSelect={handlePathSelect}
                    isLoading={keysLoading}
                  />
                ) : (
                  <KVTreeView
                    tree={tree}
                    selectedPath={selectedPath}
                    onSelect={handlePathSelect}
                    expandedNodes={expandedNodes}
                    onToggleNode={toggleNode}
                    isLoading={keysLoading}
                    searchQuery={searchQuery}
                    onFolderNavigate={handlePrefixNavigate}
                  />
                )}
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
                  currentPrefix={currentPrefix}
                  onSave={handleCreate}
                  onCancel={() => {
                    setCreateMode(false);
                    setSelectedPath(undefined);
                  }}
                  isReadOnly={permissions.isReadOnly}
                  isSaving={isPutting}
                />
              </CardContent>
            </Card>
          ) : selectedPath ? (
            <KVDetailPanel
              entry={entry && typeof entry === "object" && "path" in entry ? entry : undefined}
              path={selectedPath}
              onEdit={handleEdit}
              onDelete={handleDelete}
              onRefresh={refetchEntry}
              isReadOnly={permissions.isReadOnly}
              isEditing={isPutting}
              isDeleting={isDeleting}
              isLoading={entryLoading}
            />
          ) : (
            <Card>
              <CardContent>
                <Box sx={{ textAlign: "center", py: 4 }}>
                  <Typography variant="body2" color="text.secondary">
                    Select an entry to view details, or create a new entry
                  </Typography>
                </Box>
              </CardContent>
            </Card>
          )}
        </Grid>
      </Grid>
    </Box>
  );
}

