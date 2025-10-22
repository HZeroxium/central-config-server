import React from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  IconButton,
  Box,
  Typography,
  Tooltip,
  LinearProgress,
} from '@mui/material';
import {
  Edit as EditIcon,
  Delete as DeleteIcon,
  Person as PersonIcon,
  Group as GroupIcon,
} from '@mui/icons-material';
import { format } from 'date-fns';
import type { ServiceShare } from '../types';
import { PERMISSION_LABELS, ENVIRONMENT_LABELS, ENVIRONMENT_COLORS } from '../types';

interface ServiceShareTableProps {
  shares: ServiceShare[];
  loading: boolean;
  onEdit?: (share: ServiceShare) => void;
  onRevoke?: (share: ServiceShare) => void;
}

export const ServiceShareTable: React.FC<ServiceShareTableProps> = ({
  shares,
  loading,
  onEdit,
  onRevoke,
}) => {
  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return 'success';
      case 'EXPIRED':
        return 'warning';
      case 'REVOKED':
        return 'error';
      default:
        return 'default';
    }
  };

  const formatExpiryDate = (expiresAt?: string) => {
    if (!expiresAt) return 'Never';
    return format(new Date(expiresAt), 'MMM dd, yyyy');
  };

  const isExpired = (expiresAt?: string) => {
    if (!expiresAt) return false;
    return new Date(expiresAt) < new Date();
  };

  if (loading) {
    return <LinearProgress />;
  }

  return (
    <TableContainer component={Paper}>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Service</TableCell>
            <TableCell>Granted To</TableCell>
            <TableCell>Permissions</TableCell>
            <TableCell>Environments</TableCell>
            <TableCell>Expires</TableCell>
            <TableCell>Status</TableCell>
            <TableCell>Created</TableCell>
            <TableCell align="right">Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {shares.length === 0 ? (
            <TableRow>
              <TableCell colSpan={8} align="center">
                <Typography variant="body2" color="text.secondary">
                  No service shares found
                </Typography>
              </TableCell>
            </TableRow>
          ) : (
            shares.map((share) => (
              <TableRow key={share.id} hover>
                <TableCell>
                  <Typography variant="body2" fontWeight="medium">
                    {share.serviceName}
                  </Typography>
                </TableCell>
                
                <TableCell>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    {share.grantedTo === 'TEAM' ? (
                      <GroupIcon fontSize="small" color="primary" />
                    ) : (
                      <PersonIcon fontSize="small" color="action" />
                    )}
                    <Box>
                      <Typography variant="body2" fontWeight="medium">
                        {share.grantedToName}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {share.grantedTo}
                      </Typography>
                    </Box>
                  </Box>
                </TableCell>

                <TableCell>
                  <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap', maxWidth: 200 }}>
                    {share.permissions.slice(0, 3).map((permission) => (
                      <Chip
                        key={permission}
                        label={PERMISSION_LABELS[permission as keyof typeof PERMISSION_LABELS] || permission}
                        size="small"
                        variant="outlined"
                      />
                    ))}
                    {share.permissions.length > 3 && (
                      <Tooltip title={share.permissions.slice(3).map(p => PERMISSION_LABELS[p as keyof typeof PERMISSION_LABELS] || p).join(', ')}>
                        <Chip
                          label={`+${share.permissions.length - 3}`}
                          size="small"
                          variant="outlined"
                        />
                      </Tooltip>
                    )}
                  </Box>
                </TableCell>

                <TableCell>
                  <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
                    {share.environments.map((env) => (
                      <Chip
                        key={env}
                        label={ENVIRONMENT_LABELS[env as keyof typeof ENVIRONMENT_LABELS] || env}
                        size="small"
                        sx={{
                          backgroundColor: ENVIRONMENT_COLORS[env as keyof typeof ENVIRONMENT_COLORS] || 'grey.300',
                          color: 'white',
                          fontWeight: 'medium',
                        }}
                      />
                    ))}
                  </Box>
                </TableCell>

                <TableCell>
                  <Typography
                    variant="body2"
                    color={isExpired(share.expiresAt) ? 'error' : 'text.primary'}
                  >
                    {formatExpiryDate(share.expiresAt)}
                  </Typography>
                </TableCell>

                <TableCell>
                  <Chip
                    label={share.status}
                    color={getStatusColor(share.status) as any}
                    size="small"
                  />
                </TableCell>

                <TableCell>
                  <Typography variant="body2" color="text.secondary">
                    {format(new Date(share.createdAt), 'MMM dd, yyyy')}
                  </Typography>
                </TableCell>

                <TableCell align="right">
                  <Box sx={{ display: 'flex', gap: 0.5 }}>
                    {onEdit && (
                      <Tooltip title="Edit">
                        <IconButton
                          size="small"
                          onClick={() => onEdit(share)}
                        >
                          <EditIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    )}
                    {onRevoke && (
                      <Tooltip title="Revoke">
                        <IconButton
                          size="small"
                          onClick={() => onRevoke(share)}
                          color="error"
                        >
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    )}
                  </Box>
                </TableCell>
              </TableRow>
            ))
          )}
        </TableBody>
      </Table>
    </TableContainer>
  );
};