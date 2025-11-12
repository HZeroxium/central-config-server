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
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
} from "@mui/material";
import Grid from "@mui/material/Grid";
import {
  Add as AddIcon,
  ArrowBack as BackIcon,
  ArrowDropDown as ArrowDropDownIcon,
  InsertDriveFile as LeafIcon,
  List as ListIcon,
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
  KVListEditor,
  type KVCreateType,
} from "../components";
import { useKVStore, useKVPermissions, useKVTree, usePutKVList } from "../hooks";
import {
  normalizePath,
  encodePath,
  decodePath,
  getParentPath,
  isListPrefix,
  isFolderPrefix,
} from "../types";
import { useFindApplicationServiceById } from "@lib/api/hooks";
import type { KVPutRequest } from "@lib/api/models";
import { ToggleButton, ToggleButtonGroup } from "@mui/material";
import { ViewList as ViewListIcon, AccountTree as TreeIcon } from "@mui/icons-material";
import { useQueryClient } from "@tanstack/react-query";
import { showTransactionToast } from "../utils/errorHandling";
import { toGeneratedKVListItemArray, toKVListManifestMetadata } from "../utils/typeAdapters";
import type { UIListItem } from "../utils/typeAdapters";
import type { KVListWriteRequest, PutKVListParams } from "../hooks";
import { handleApiError } from "@lib/api/errorHandler";
import { getGetKVListQueryKey } from "@lib/api/generated/key-value-store/key-value-store";

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
  const [createType, setCreateType] = useState<KVCreateType | null>(null);
  const [createPrefix, setCreatePrefix] = useState("");
  const [createMenuAnchor, setCreateMenuAnchor] = useState<null | HTMLElement>(null);
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
  const queryClient = useQueryClient();

  // List mutations with error handling
  const putListMutation = usePutKVList({
    mutation: {
      onSuccess: (response, variables) => {
        showTransactionToast(response, "List saved successfully");
        // Invalidate related queries using generated query keys
        queryClient.invalidateQueries({
          queryKey: getGetKVListQueryKey(variables.serviceId, variables.params),
        });
        queryClient.invalidateQueries({
          queryKey: ["kv", serviceId],
        });
        refetchKeys();
      },
      onError: (error) => {
        handleApiError(error);
      },
    },
  });

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
      setCreateType(null);
      setCreatePrefix("");
    } else if (!propServiceId) {
      setSelectedPath(undefined);
      setCreateMode(false);
      setCreateType(null);
      setCreatePrefix("");
    }
  }, [pathParam, propServiceId]);

  // Navigate to folder or select file
  const handlePathSelect = (path: string, isFolder: boolean) => {
    const normalized = normalizePath(path);
    
    // Check if this is a List prefix - treat as single entity, don't navigate into it
    const isList = isListPrefix(normalized, keys);
    const isFolderType = isFolderPrefix(normalized, keys);
    
    if (isList) {
      // List: select to view/edit (don't navigate into internal structure)
      setSelectedPath(normalized);
      setCreateMode(false);
      setCreateType(null);
      setCreatePrefix("");
      if (!propServiceId) {
        const encoded = encodePath(normalized);
        navigate(`/kv/${serviceId}/${encoded}`, { replace: true });
      }
      return;
    }
    
    if (isFolderType || isFolder) {
      // FOLDER: navigate into folder (set currentPrefix)
      setCurrentPrefix(normalized);
      setSelectedPath(undefined);
      setCreateMode(false);
      setCreateType(null);
      setCreatePrefix("");
      return;
    }
    
    // Select file/leaf to view/edit
    setSelectedPath(normalized);
    setCreateMode(false);
    setCreateType(null);
    setCreatePrefix("");
    if (!propServiceId) {
      const encoded = encodePath(normalized);
      navigate(`/kv/${serviceId}/${encoded}`, { replace: true });
    }
  };

  // Navigate to prefix (for breadcrumb)
  const handlePrefixNavigate = (prefix: string) => {
    setCurrentPrefix(prefix);
    setSelectedPath(undefined);
    setCreateMode(false);
    setCreateType(null);
    setCreatePrefix("");
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

  // Handle edit (for leaf entries)
  const handleEdit = async (_path: string, data: KVPutRequest) => {
    if (!selectedPath) return;
    await putEntry(selectedPath, data);
    await refetchKeys();
  };

  // Handle edit list (used by detail panel)
  const handleEditListFromDetail = async (
    prefix: string,
    items: UIListItem[],
    manifest: { order?: string[]; version?: number; etag?: string | null; metadata?: Record<string, unknown> },
    deletes: string[]
  ) => {
    return handleEditList(prefix, items, manifest, deletes);
  };
  
  // Handle edit list
  const handleEditList = async (
    prefix: string,
    items: UIListItem[],
    manifest: { order?: string[]; version?: number; etag?: string | null; metadata?: Record<string, unknown> },
    deletes: string[]
  ) => {
    return new Promise<void>((resolve, reject) => {
      const params: PutKVListParams = { prefix };
      
      const requestData: KVListWriteRequest = {
        items: toGeneratedKVListItemArray(items),
        manifest: {
          order: manifest.order ?? [],
          version: manifest.version ?? 0,
          etag: manifest.etag ?? null,
          metadata: toKVListManifestMetadata(manifest.metadata),
        },
        deletes: deletes.length > 0 ? deletes : undefined,
      };

      putListMutation.mutate(
        {
          serviceId: serviceId || "",
          data: requestData,
          params,
        },
        {
          onSuccess: () => resolve(),
          onError: (error) => reject(error),
        }
      );
    });
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

  // Handle new entry menu
  const handleNewEntryMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setCreateMenuAnchor(event.currentTarget);
  };

  const handleNewEntryMenuClose = () => {
    setCreateMenuAnchor(null);
  };

  const handleNewEntryMenuSelect = (type: KVCreateType) => {
    setCreateMenuAnchor(null);
    
    if (type === "leaf") {
      // For leaf: open editor with path input (pre-filled with currentPrefix)
      const defaultPath = currentPrefix 
        ? `${currentPrefix}/new-key` 
        : "new-key";
      setCreateMode(true);
      setCreateType("leaf");
      setSelectedPath(defaultPath);
      setCreatePrefix("");
    } else {
      // For list: use currentPrefix directly, allow editing name part
      const defaultPrefix = currentPrefix 
        ? `${currentPrefix}/new-${type}` 
        : `new-${type}`;
      setCreateMode(true);
      setCreateType(type);
      setCreatePrefix(defaultPrefix);
      setSelectedPath(undefined);
    }
  };

  // Handle create list
  const handleCreateList = async (
    prefix: string,
    items: UIListItem[],
    manifest: { order?: string[]; version?: number; etag?: string | null; metadata?: Record<string, unknown> },
    deletes: string[]
  ) => {
    try {
      await handleEditList(prefix, items, manifest, deletes);
      // After success, navigate to view created list
      setCreateMode(false);
      setCreateType(null);
      setCreatePrefix("");
      // Wait for keys to refresh, then navigate
      await refetchKeys();
      // Select the prefix to view the created list
      handlePathSelect(prefix, false);
    } catch (error) {
      // Error is already handled by handleEditList
      throw error;
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
                    <>
                      <Button
                        size="small"
                        startIcon={<AddIcon />}
                        endIcon={<ArrowDropDownIcon />}
                        onClick={handleNewEntryMenuOpen}
                        variant="contained"
                        aria-label="Create new entry"
                        aria-haspopup="true"
                        aria-controls={createMenuAnchor ? "create-menu" : undefined}
                      >
                        New Entry
                      </Button>
                      <Menu
                        id="create-menu"
                        anchorEl={createMenuAnchor}
                        open={Boolean(createMenuAnchor)}
                        onClose={handleNewEntryMenuClose}
                        anchorOrigin={{
                          vertical: "bottom",
                          horizontal: "right",
                        }}
                        transformOrigin={{
                          vertical: "top",
                          horizontal: "right",
                        }}
                      >
                        <MenuItem
                          onClick={() => handleNewEntryMenuSelect("leaf")}
                          aria-label="Create leaf entry"
                        >
                          <ListItemIcon>
                            <LeafIcon fontSize="small" />
                          </ListItemIcon>
                          <ListItemText
                            primary="Leaf Entry"
                            secondary="Simple key-value entry"
                          />
                        </MenuItem>
                        <MenuItem
                          onClick={() => handleNewEntryMenuSelect("list")}
                          aria-label="Create list"
                        >
                          <ListItemIcon>
                            <ListIcon fontSize="small" />
                          </ListItemIcon>
                          <ListItemText
                            primary="List"
                            secondary="Ordered list of items"
                          />
                        </MenuItem>
                      </Menu>
                    </>
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
                    allKeys={keys}
                    onNavigateUp={handlePrefixNavigate}
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
                    allKeys={keys}
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
                {createType === "list" ? (
                  <KVListEditor
                    serviceId={serviceId}
                    prefix={createPrefix}
                    initialItems={[]}
                    initialManifest={undefined}
                    onSave={handleCreateList}
                    onCancel={() => {
                      setCreateMode(false);
                      setCreateType(null);
                      setCreatePrefix("");
                      setSelectedPath(undefined);
                    }}
                    isReadOnly={permissions.isReadOnly}
                    isSaving={putListMutation.isPending}
                    isCreateMode={true}
                  />
                ) : (
                  <KVEntryEditor
                    path={selectedPath || createPrefix}
                    currentPrefix={currentPrefix}
                    onSave={handleCreate}
                    onCancel={() => {
                      setCreateMode(false);
                      setCreateType(null);
                      setCreatePrefix("");
                      setSelectedPath(undefined);
                    }}
                    isReadOnly={permissions.isReadOnly}
                    isSaving={isPutting}
                  />
                )}
              </CardContent>
            </Card>
          ) : selectedPath ? (
            <KVDetailPanel
              entry={entry && typeof entry === "object" && "path" in entry ? entry : undefined}
              path={selectedPath}
              serviceId={serviceId}
              childKeys={keys.filter((key) => {
                const normalizedPath = normalizePath(selectedPath);
                const normalizedKey = normalizePath(key);
                return normalizedKey.startsWith(normalizedPath + "/");
              })}
              allKeys={keys}
              onEdit={handleEdit}
              onEditList={handleEditListFromDetail}
              onDelete={handleDelete}
              onRefresh={refetchEntry}
              isPrefix={keys.some((key) => {
                const normalizedPath = normalizePath(selectedPath);
                const normalizedKey = normalizePath(key);
                return normalizedKey.startsWith(normalizedPath + "/");
              })}
              isReadOnly={permissions.isReadOnly}
              isEditing={isPutting || putListMutation.isPending}
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

