import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import {
  Box,
  Button,
  Card,
  CardContent,
  Tabs,
  Tab,
  Typography,
  Chip,
  Divider,
  Alert,
  Drawer,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  IconButton,
  Tooltip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  TextField,
} from "@mui/material";
import Grid from "@mui/material/Grid";
import {
  ArrowBack as BackIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Share as ShareIcon,
  Visibility as ViewIcon,
  Assignment as ClaimIcon,
  SwapHoriz as TransferIcon,
} from "@mui/icons-material";
import PageHeader from "@components/common/PageHeader";
import Loading from "@components/common/Loading";
import ConfirmDialog from "@components/common/ConfirmDialog";
import {
  useFindApplicationServiceById,
  useDeleteApplicationService,
  useFindAllServiceInstances,
  useFindAllServiceShares,
  useRevokeServiceShare,
  useCreateApprovalRequest,
} from "@lib/api/hooks";
import { useAuth } from "@features/auth/context";
import { toast } from "@lib/toast/toast";
import { handleApiError } from "@lib/api/errorHandler";
import { ApplicationServiceForm } from "../components/ApplicationServiceForm";
import { ServiceShareDrawer } from "../components/ServiceShareDrawer";
import { ServiceSharesTab } from "../components/ServiceSharesTab";
import type { FindAllServiceInstancesEnvironment } from "@lib/api/models";

export default function ApplicationServiceDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { isSysAdmin, permissions, userInfo } = useAuth();

  const [tabValue, setTabValue] = useState(0);
  const [selectedEnvironment, setSelectedEnvironment] = useState<
    FindAllServiceInstancesEnvironment | ""
  >("");
  const [editDrawerOpen, setEditDrawerOpen] = useState(false);
  const [shareDrawerOpen, setShareDrawerOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [revokeShareDialogOpen, setRevokeShareDialogOpen] = useState(false);
  const [claimOwnershipDialogOpen, setClaimOwnershipDialogOpen] =
    useState(false);
  const [transferOwnershipDialogOpen, setTransferOwnershipDialogOpen] =
    useState(false);
  const [selectedShareId, setSelectedShareId] = useState<string | null>(null);
  const [ownershipNote, setOwnershipNote] = useState("");

  const {
    data: service,
    isLoading,
    error,
    refetch,
  } = useFindApplicationServiceById(id!, {
    query: {
      enabled: !!id,
      staleTime: 10_000,
    },
  });

  // Fetch service instances
  const { data: instancesData, isLoading: instancesLoading } =
    useFindAllServiceInstances(
      {
        serviceId: id,
        environment: selectedEnvironment || undefined,
        page: 0,
        size: 100,
      },
      {
        query: {
          enabled: !!id && tabValue === 1,
          staleTime: 15_000,
        },
      }
    );

  // Fetch service shares
  const { data: _sharesDataRaw, refetch: refetchShares } =
    useFindAllServiceShares(
      {
        serviceId: id,
        page: 0,
        size: 100,
      },
      {
        query: {
          enabled: !!id && tabValue === 2,
          staleTime: 15_000,
        },
      }
    );

  // Type assertion for shares data (unused for now - ServiceSharesTab fetches its own)
  // const sharesData = (
  //   typeof sharesDataRaw === "object" ? sharesDataRaw : undefined
  // ) as
  //   | {
  //       items?: Array<{
  //         id?: string;
  //         grantToType?: string;
  //         grantToId?: string;
  //         permissions?: string[];
  //         environments?: string[];
  //         createdAt?: string;
  //       }>;
  //     }
  //   | undefined;

  const deleteMutation = useDeleteApplicationService();
  const revokeShareMutation = useRevokeServiceShare();
  const createApprovalRequestMutation = useCreateApprovalRequest();

  const handleBack = () => {
    navigate("/application-services");
  };

  const canEdit =
    isSysAdmin || (id && permissions?.ownedServiceIds?.includes(id));

  const isOrphan = !service?.ownerTeamId;
  const canClaim = isOrphan && userInfo?.teamIds && userInfo.teamIds.length > 0;
  const canTransfer = canEdit && !isOrphan;

  const handleDelete = async () => {
    if (!id) return;

    deleteMutation.mutate(
      { id },
      {
        onSuccess: () => {
          toast.success("Service deleted successfully");
          navigate("/application-services");
        },
        onError: (error) => {
          handleApiError(error);
        },
      }
    );
  };

  const handleEditSuccess = () => {
    toast.success("Service updated successfully");
    setEditDrawerOpen(false);
    refetch();
  };

  const _handleRevokeShare = (shareId: string) => {
    setSelectedShareId(shareId);
    setRevokeShareDialogOpen(true);
  };

  const handleConfirmRevokeShare = async () => {
    if (!selectedShareId) return;

    revokeShareMutation.mutate(
      { id: selectedShareId },
      {
        onSuccess: () => {
          toast.success("Service share revoked successfully");
          setRevokeShareDialogOpen(false);
          setSelectedShareId(null);
          refetchShares();
        },
        onError: (error) => {
          handleApiError(error);
        },
      }
    );
  };

  const handleClaimOwnership = () => {
    setOwnershipNote("");
    setClaimOwnershipDialogOpen(true);
  };

  const handleSubmitClaimOwnership = async () => {
    if (!id || !userInfo?.teamIds?.[0]) {
      toast.error("Cannot submit ownership request: No team found");
      return;
    }

    createApprovalRequestMutation.mutate(
      {
        serviceId: id,
        data: {
          serviceId: id,
          targetTeamId: userInfo.teamIds[0],
          note: ownershipNote || undefined,
        },
      },
      {
        onSuccess: () => {
          toast.success("Ownership claim request submitted successfully");
          setClaimOwnershipDialogOpen(false);
          setOwnershipNote("");
          navigate("/approvals");
        },
        onError: (error) => {
          handleApiError(error);
        },
      }
    );
  };

  const handleTransferOwnership = () => {
    setOwnershipNote("");
    setTransferOwnershipDialogOpen(true);
  };

  const handleSubmitTransferOwnership = async () => {
    if (!id || !userInfo?.teamIds?.[0]) {
      toast.error("Cannot submit transfer request: No team found");
      return;
    }

    createApprovalRequestMutation.mutate(
      {
        serviceId: id,
        data: {
          serviceId: id,
          targetTeamId: userInfo.teamIds[0],
          note: ownershipNote || undefined,
        },
      },
      {
        onSuccess: () => {
          toast.success("Ownership transfer request submitted successfully");
          setTransferOwnershipDialogOpen(false);
          setOwnershipNote("");
          navigate("/approvals");
        },
        onError: (error) => {
          handleApiError(error);
        },
      }
    );
  };

  const handleViewInstance = (instanceId: string) => {
    navigate(`/service-instances/${id}/${instanceId}`);
  };

  const handleShareSuccess = () => {
    toast.success("Service share granted successfully");
    setShareDrawerOpen(false);
    refetchShares();
  };

  if (isLoading) {
    return <Loading />;
  }

  // Handle errors (404, 403, or network errors)
  // 404 from backend could mean service doesn't exist OR user lacks permission
  if (error || !service) {
    const errorMessage = error
      ? error.detail || "Service not found or access denied."
      : "Service not found or access denied.";

    const isUnauthorized =
      error && (error.status === 403 || error.status === 404);

    return (
      <Box>
        <PageHeader
          title="Application Service Details"
          actions={
            <Button
              variant="outlined"
              startIcon={<BackIcon />}
              onClick={handleBack}
            >
              Back to Services
            </Button>
          }
        />
        <Alert
          severity={isUnauthorized ? "warning" : "error"}
          action={
            <Button color="inherit" size="small" onClick={handleBack}>
              Go Back
            </Button>
          }
        >
          {isUnauthorized ? (
            <>
              <strong>Access Denied</strong>
              <br />
              You don't have permission to view this service. You can only view
              orphaned services, services owned by your teams, or services
              shared to your teams.
            </>
          ) : (
            <>
              <strong>Service Not Found</strong>
              <br />
              {errorMessage}
            </>
          )}
        </Alert>
      </Box>
    );
  }

  return (
    <Box>
      <PageHeader
        title={service.displayName || service.id || "Application Service"}
        subtitle={`Service ID: ${service.id}`}
        actions={
          <>
            <Button
              variant="outlined"
              startIcon={<BackIcon />}
              onClick={handleBack}
            >
              Back
            </Button>
            {canClaim && (
              <Button
                variant="contained"
                color="primary"
                startIcon={<ClaimIcon />}
                onClick={handleClaimOwnership}
              >
                Claim Ownership
              </Button>
            )}
            {canTransfer && (
              <Button
                variant="outlined"
                startIcon={<TransferIcon />}
                onClick={handleTransferOwnership}
              >
                Transfer Ownership
              </Button>
            )}
            {canEdit && (
              <>
                <Button
                  variant="outlined"
                  startIcon={<ShareIcon />}
                  onClick={() => setShareDrawerOpen(true)}
                >
                  Share
                </Button>
                <Button
                  variant="outlined"
                  startIcon={<EditIcon />}
                  onClick={() => setEditDrawerOpen(true)}
                >
                  Edit
                </Button>
                <Button
                  variant="outlined"
                  color="error"
                  startIcon={<DeleteIcon />}
                  onClick={() => setDeleteDialogOpen(true)}
                >
                  Delete
                </Button>
              </>
            )}
          </>
        }
      />

      {/* Tabs */}
      <Box sx={{ borderBottom: 1, borderColor: "divider", mb: 3 }}>
        <Tabs
          value={tabValue}
          onChange={(_, newValue) => setTabValue(newValue)}
        >
          <Tab label="Overview" />
          <Tab label="Instances" />
          <Tab label="Shares" />
          <Tab label="Approvals" />
        </Tabs>
      </Box>

      {/* Overview Tab */}
      {tabValue === 0 && (
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Service Information
            </Typography>
            <Divider sx={{ mb: 2 }} />

            <Grid container spacing={3}>
              <Grid size={{ xs: 12, md: 6 }}>
                <Typography
                  variant="subtitle2"
                  color="text.secondary"
                  gutterBottom
                >
                  Service ID
                </Typography>
                <Typography
                  variant="body1"
                  fontFamily="monospace"
                  fontWeight={600}
                >
                  {service.id}
                </Typography>
              </Grid>

              <Grid size={{ xs: 12, md: 6 }}>
                <Typography
                  variant="subtitle2"
                  color="text.secondary"
                  gutterBottom
                >
                  Display Name
                </Typography>
                <Typography variant="body1">{service.displayName}</Typography>
              </Grid>

              <Grid size={{ xs: 12, md: 6 }}>
                <Typography
                  variant="subtitle2"
                  color="text.secondary"
                  gutterBottom
                >
                  Owner Team
                </Typography>
                <Chip label={service.ownerTeamId} variant="outlined" />
              </Grid>

              <Grid size={{ xs: 12, md: 6 }}>
                <Typography
                  variant="subtitle2"
                  color="text.secondary"
                  gutterBottom
                >
                  Lifecycle
                </Typography>
                <Chip
                  label={service.lifecycle}
                  color={
                    service.lifecycle === "ACTIVE"
                      ? "success"
                      : service.lifecycle === "DEPRECATED"
                      ? "warning"
                      : "error"
                  }
                />
              </Grid>

              <Grid size={{ xs: 12, md: 6 }}>
                <Typography
                  variant="subtitle2"
                  color="text.secondary"
                  gutterBottom
                >
                  Environments
                </Typography>
                <Box sx={{ display: "flex", gap: 1, flexWrap: "wrap" }}>
                  {service.environments?.map((env) => (
                    <Chip
                      key={env}
                      label={env.toUpperCase()}
                      size="small"
                      variant="outlined"
                      color={
                        env === "prod"
                          ? "error"
                          : env === "staging"
                          ? "warning"
                          : "info"
                      }
                    />
                  )) || "N/A"}
                </Box>
              </Grid>

              <Grid size={{ xs: 12, md: 6 }}>
                <Typography
                  variant="subtitle2"
                  color="text.secondary"
                  gutterBottom
                >
                  Tags
                </Typography>
                <Box sx={{ display: "flex", gap: 1, flexWrap: "wrap" }}>
                  {service.tags && service.tags.length > 0 ? (
                    service.tags.map((tag) => (
                      <Chip key={tag} label={tag} size="small" />
                    ))
                  ) : (
                    <Typography variant="body2" color="text.secondary">
                      No tags
                    </Typography>
                  )}
                </Box>
              </Grid>

              {service.repoUrl && (
                <Grid size={{ xs: 12 }}>
                  <Typography
                    variant="subtitle2"
                    color="text.secondary"
                    gutterBottom
                  >
                    Repository URL
                  </Typography>
                  <Typography
                    variant="body2"
                    component="a"
                    href={service.repoUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    sx={{ color: "primary.main", textDecoration: "none" }}
                  >
                    {service.repoUrl}
                  </Typography>
                </Grid>
              )}

              {service.attributes &&
                Object.keys(service.attributes).length > 0 && (
                  <Grid size={{ xs: 12 }}>
                    <Typography
                      variant="subtitle2"
                      color="text.secondary"
                      gutterBottom
                    >
                      Attributes
                    </Typography>
                    <Box
                      component="pre"
                      sx={{
                        bgcolor: "grey.100",
                        p: 2,
                        borderRadius: 1,
                        overflow: "auto",
                        fontSize: "0.875rem",
                      }}
                    >
                      {JSON.stringify(service.attributes, null, 2)}
                    </Box>
                  </Grid>
                )}
            </Grid>
          </CardContent>
        </Card>
      )}

      {/* Instances Tab */}
      {tabValue === 1 && (
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
              <Typography variant="h6">Service Instances</Typography>
            </Box>

            {/* Environment Filter */}
            <Box sx={{ mb: 3, display: "flex", gap: 1, flexWrap: "wrap" }}>
              <Chip
                label="All"
                onClick={() => setSelectedEnvironment("")}
                color={selectedEnvironment === "" ? "primary" : "default"}
                variant={selectedEnvironment === "" ? "filled" : "outlined"}
              />
              {service.environments?.map((env) => (
                <Chip
                  key={env}
                  label={env.toUpperCase()}
                  onClick={() =>
                    setSelectedEnvironment(
                      env as FindAllServiceInstancesEnvironment
                    )
                  }
                  color={selectedEnvironment === env ? "primary" : "default"}
                  variant={selectedEnvironment === env ? "filled" : "outlined"}
                />
              ))}
            </Box>

            {instancesLoading ? (
              <Loading />
            ) : instancesData?.items && instancesData.items.length > 0 ? (
              <TableContainer component={Paper} variant="outlined">
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Instance ID</TableCell>
                      <TableCell>Environment</TableCell>
                      <TableCell>Host</TableCell>
                      <TableCell>Port</TableCell>
                      <TableCell>Version</TableCell>
                      <TableCell>Status</TableCell>
                      <TableCell>Drift</TableCell>
                      <TableCell align="right">Actions</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {instancesData.items.map((instance) => (
                      <TableRow key={instance.instanceId} hover>
                        <TableCell sx={{ fontFamily: "monospace" }}>
                          {instance.instanceId}
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={instance.environment?.toUpperCase() || "N/A"}
                            size="small"
                            color={
                              instance.environment === "prod"
                                ? "error"
                                : instance.environment === "staging"
                                ? "warning"
                                : "info"
                            }
                          />
                        </TableCell>
                        <TableCell>{instance.host || "N/A"}</TableCell>
                        <TableCell>{instance.port || "N/A"}</TableCell>
                        <TableCell>{instance.version || "N/A"}</TableCell>
                        <TableCell>
                          <Chip
                            label={instance.status || "UNKNOWN"}
                            size="small"
                            color={
                              instance.status === "HEALTHY"
                                ? "success"
                                : instance.status === "DRIFT"
                                ? "warning"
                                : "error"
                            }
                          />
                        </TableCell>
                        <TableCell>
                          {instance.hasDrift ? (
                            <Chip
                              label="Drift Detected"
                              size="small"
                              color="warning"
                            />
                          ) : (
                            <Chip
                              label="In Sync"
                              size="small"
                              color="success"
                            />
                          )}
                        </TableCell>
                        <TableCell align="right">
                          <Tooltip title="View Details">
                            <IconButton
                              size="small"
                              onClick={() =>
                                handleViewInstance(instance.instanceId || "")
                              }
                            >
                              <ViewIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            ) : (
              <Alert severity="info">
                No instances found for this service.
              </Alert>
            )}
          </CardContent>
        </Card>
      )}

      {/* Shares Tab */}
      {tabValue === 2 && <ServiceSharesTab serviceId={id || ""} />}

      {/* Approvals Tab - Placeholder */}
      {tabValue === 3 && (
        <Card>
          <CardContent>
            <Typography variant="body1" color="text.secondary">
              Approval requests view - coming soon
            </Typography>
          </CardContent>
        </Card>
      )}

      {/* Edit Service Drawer */}
      <Drawer
        anchor="right"
        open={editDrawerOpen}
        onClose={() => setEditDrawerOpen(false)}
        PaperProps={{
          sx: { width: { xs: "100%", sm: 600 } },
        }}
      >
        <ApplicationServiceForm
          mode="edit"
          initialData={service}
          onSuccess={handleEditSuccess}
          onCancel={() => setEditDrawerOpen(false)}
        />
      </Drawer>

      {/* Share Service Drawer */}
      <Drawer
        anchor="right"
        open={shareDrawerOpen}
        onClose={() => setShareDrawerOpen(false)}
        PaperProps={{
          sx: { width: { xs: "100%", sm: 600 } },
        }}
      >
        <ServiceShareDrawer
          serviceId={service.id || ""}
          onSuccess={handleShareSuccess}
          onClose={() => setShareDrawerOpen(false)}
        />
      </Drawer>

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        open={deleteDialogOpen}
        title="Delete Application Service"
        message={`Are you sure you want to delete ${
          service.displayName || service.id
        }? This action cannot be undone and will affect all associated instances and shares.`}
        confirmText="Delete"
        cancelText="Cancel"
        onConfirm={handleDelete}
        onCancel={() => setDeleteDialogOpen(false)}
        loading={deleteMutation.isPending}
      />

      {/* Revoke Share Confirmation Dialog */}
      <ConfirmDialog
        open={revokeShareDialogOpen}
        title="Revoke Service Share"
        message="Are you sure you want to revoke this share? The grantee will lose access immediately."
        confirmText="Revoke"
        cancelText="Cancel"
        onConfirm={handleConfirmRevokeShare}
        onCancel={() => {
          setRevokeShareDialogOpen(false);
          setSelectedShareId(null);
        }}
        loading={revokeShareMutation.isPending}
      />

      {/* Claim Ownership Dialog */}
      <Dialog
        open={claimOwnershipDialogOpen}
        onClose={() => setClaimOwnershipDialogOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Claim Ownership</DialogTitle>
        <DialogContent>
          <DialogContentText sx={{ mb: 2 }}>
            You are requesting ownership of this orphan service. Your request
            will require approval from system administrators.
          </DialogContentText>
          <TextField
            fullWidth
            multiline
            rows={3}
            label="Note (Optional)"
            value={ownershipNote}
            onChange={(e) => setOwnershipNote(e.target.value)}
            placeholder="Provide a reason for claiming this service..."
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setClaimOwnershipDialogOpen(false)}>
            Cancel
          </Button>
          <Button
            variant="contained"
            onClick={handleSubmitClaimOwnership}
            disabled={createApprovalRequestMutation.isPending}
          >
            {createApprovalRequestMutation.isPending
              ? "Submitting..."
              : "Submit Request"}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Transfer Ownership Dialog */}
      <Dialog
        open={transferOwnershipDialogOpen}
        onClose={() => setTransferOwnershipDialogOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Transfer Ownership</DialogTitle>
        <DialogContent>
          <DialogContentText sx={{ mb: 2 }}>
            You are requesting to transfer ownership of this service. Your
            request will require approval.
          </DialogContentText>
          <TextField
            fullWidth
            multiline
            rows={3}
            label="Note (Optional)"
            value={ownershipNote}
            onChange={(e) => setOwnershipNote(e.target.value)}
            placeholder="Provide a reason for transferring ownership..."
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setTransferOwnershipDialogOpen(false)}>
            Cancel
          </Button>
          <Button
            variant="contained"
            onClick={handleSubmitTransferOwnership}
            disabled={createApprovalRequestMutation.isPending}
          >
            {createApprovalRequestMutation.isPending
              ? "Submitting..."
              : "Submit Request"}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
