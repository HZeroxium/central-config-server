import React, { useState } from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  IconButton,
  Tooltip,
  Alert,
} from '@mui/material';
import { Share as ShareIcon, Delete as RevokeIcon } from '@mui/icons-material';
import { useFindAllServiceSharesForService1, useRevokeServiceShare } from '@lib/api/generated/service-shares/service-shares';
import { useAuth } from '@features/auth/authContext';
import { toast } from '@lib/toast/toast';
import { handleApiError } from '@lib/api/errorHandler';
import Loading from '@components/common/Loading';
import ConfirmDialog from '@components/common/ConfirmDialog';
import { GrantShareDrawer } from './GrantShareDrawer';

interface ServiceSharesTabProps {
  serviceId: string;
}

export const ServiceSharesTab: React.FC<ServiceSharesTabProps> = ({ serviceId }) => {
  const { isSysAdmin, permissions } = useAuth();
  const [shareDrawerOpen, setShareDrawerOpen] = useState(false);
  const [revokeDialogOpen, setRevokeDialogOpen] = useState(false);
  const [selectedShareId, setSelectedShareId] = useState<string | null>(null);

  // Fetch service shares
  const { data: sharesData, isLoading: sharesLoading, refetch: refetchShares } = useFindAllServiceSharesForService1({
    serviceId,
  }, {
    query: {
      staleTime: 30_000,
    },
  });

  // Revoke share mutation
  const revokeShareMutation = useRevokeServiceShare({
    mutation: {
      onSuccess: () => {
        toast.success('Service share revoked successfully');
        setRevokeDialogOpen(false);
        setSelectedShareId(null);
        refetchShares();
      },
      onError: (error) => {
        handleApiError(error);
      },
    },
  });

  const canManageShares = isSysAdmin || permissions?.actions?.['SERVICE_SHARE']?.includes('MANAGE');

  const handleRevokeShare = (shareId: string) => {
    setSelectedShareId(shareId);
    setRevokeDialogOpen(true);
  };

  const handleConfirmRevoke = () => {
    if (!selectedShareId) return;
    revokeShareMutation.mutate({ id: selectedShareId });
  };

  const handleCloseRevokeDialog = () => {
    setRevokeDialogOpen(false);
    setSelectedShareId(null);
  };

  const handleShareSuccess = () => {
    toast.success('Service share granted successfully');
    setShareDrawerOpen(false);
    refetchShares();
  };

  const shares = sharesData?.items || [];

  return (
    <>
      <Card>
        <CardContent>
          <Box
            sx={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              mb: 3,
            }}
          >
            <Typography variant="h6">Service Shares</Typography>
            {canManageShares && (
              <Button
                variant="contained"
                size="small"
                startIcon={<ShareIcon />}
                onClick={() => setShareDrawerOpen(true)}
              >
                Grant Share
              </Button>
            )}
          </Box>

          {sharesLoading ? (
            <Loading />
          ) : shares.length > 0 ? (
            <TableContainer component={Paper} variant="outlined">
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Granted To</TableCell>
                    <TableCell>Type</TableCell>
                    <TableCell>Permissions</TableCell>
                    <TableCell>Environments</TableCell>
                    <TableCell>Granted By</TableCell>
                    <TableCell>Created At</TableCell>
                    <TableCell>Expires At</TableCell>
                    {canManageShares && <TableCell align="right">Actions</TableCell>}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {shares.map((share: any) => (
                    <TableRow key={share.id} hover>
                      <TableCell>
                        <Box>
                          <Typography variant="body2" fontWeight="medium">
                            {share.grantToName || share.grantToId}
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            {share.grantToId}
                          </Typography>
                        </Box>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={share.grantToType}
                          size="small"
                          variant="outlined"
                          color={share.grantToType === 'TEAM' ? 'primary' : 'secondary'}
                        />
                      </TableCell>
                      <TableCell>
                        <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
                          {share.permissions?.map((perm: any) => (
                            <Chip
                              key={perm}
                              label={perm.replace('_', ' ')}
                              size="small"
                              color="primary"
                              variant="outlined"
                            />
                          ))}
                        </Box>
                      </TableCell>
                      <TableCell>
                        {share.environments && share.environments.length > 0 ? (
                          <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
                            {share.environments.map((env: any) => (
                              <Chip
                                key={env}
                                label={env.toUpperCase()}
                                size="small"
                                color={
                                  env === 'prod' ? 'error' : 
                                  env === 'staging' ? 'warning' : 
                                  'info'
                                }
                                variant="outlined"
                              />
                            ))}
                          </Box>
                        ) : (
                          <Typography variant="body2" color="text.secondary">
                            All
                          </Typography>
                        )}
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">
                          {share.createdBy || 'Unknown'}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">
                          {share.createdAt ? new Date(share.createdAt).toLocaleDateString() : '-'}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">
                          {share.expiresAt ? new Date(share.expiresAt).toLocaleDateString() : 'Never'}
                        </Typography>
                      </TableCell>
                      {canManageShares && (
                        <TableCell align="right">
                          <Tooltip title="Revoke Share">
                            <IconButton
                              size="small"
                              color="error"
                              onClick={() => handleRevokeShare(share.id)}
                            >
                              <RevokeIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        </TableCell>
                      )}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          ) : (
            <Alert severity="info">
              No shares found for this service. Grant shares to allow other teams or users to access this service.
            </Alert>
          )}
        </CardContent>
      </Card>

      {/* Grant Share Drawer */}
      <GrantShareDrawer
        open={shareDrawerOpen}
        serviceId={serviceId}
        onClose={() => setShareDrawerOpen(false)}
        onSuccess={handleShareSuccess}
      />

      {/* Revoke Share Confirmation Dialog */}
      <ConfirmDialog
        open={revokeDialogOpen}
        title="Revoke Service Share"
        message="Are you sure you want to revoke this service share? This action cannot be undone."
        confirmText="Revoke"
        cancelText="Cancel"
        confirmColor="error"
        onConfirm={handleConfirmRevoke}
        onCancel={handleCloseRevokeDialog}
        loading={revokeShareMutation.isPending}
      />
    </>
  );
};
