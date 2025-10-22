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
  CheckCircle as ApproveIcon,
  Close as CancelIcon,
  Visibility as ViewIcon,
} from '@mui/icons-material';
import { format } from 'date-fns';
import type { ApprovalRequest } from '../types';
import { 
  REQUEST_TYPE_LABELS, 
  STATUS_LABELS, 
  PRIORITY_LABELS, 
  STATUS_COLORS, 
  PRIORITY_COLORS 
} from '../types';

interface ApprovalRequestTableProps {
  requests: ApprovalRequest[];
  loading: boolean;
  onDecision?: (request: ApprovalRequest) => void;
  onCancel?: (request: ApprovalRequest) => void;
  onView?: (request: ApprovalRequest) => void;
}

export const ApprovalRequestTable: React.FC<ApprovalRequestTableProps> = ({
  requests,
  loading,
  onDecision,
  onCancel,
  onView,
}) => {
  const getStatusColor = (status: string) => {
    return STATUS_COLORS[status as keyof typeof STATUS_COLORS] || 'default';
  };

  const getPriorityColor = (priority: string) => {
    return PRIORITY_COLORS[priority as keyof typeof PRIORITY_COLORS] || 'default';
  };

  const formatDate = (dateString: string) => {
    return format(new Date(dateString), 'MMM dd, yyyy HH:mm');
  };

  const isExpired = (expiresAt?: string) => {
    if (!expiresAt) return false;
    return new Date(expiresAt) < new Date();
  };

  const getApprovalProgress = (request: ApprovalRequest) => {
    const totalRequired = request.requiredGates.reduce((sum, gate) => sum + gate.minApprovals, 0);
    const currentApprovals = request.currentApprovals.length;
    return { current: currentApprovals, total: totalRequired };
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
            <TableCell>Type</TableCell>
            <TableCell>Requester</TableCell>
            <TableCell>Priority</TableCell>
            <TableCell>Status</TableCell>
            <TableCell>Progress</TableCell>
            <TableCell>Created</TableCell>
            <TableCell>Expires</TableCell>
            <TableCell align="right">Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {requests.length === 0 ? (
            <TableRow>
              <TableCell colSpan={9} align="center">
                <Typography variant="body2" color="text.secondary">
                  No approval requests found
                </Typography>
              </TableCell>
            </TableRow>
          ) : (
            requests.map((request) => {
              const progress = getApprovalProgress(request);
              const isExpiredRequest = isExpired(request.expiresAt);
              
              return (
                <TableRow key={request.id} hover>
                  <TableCell>
                    <Typography variant="body2" fontWeight="medium">
                      {request.serviceName}
                    </Typography>
                  </TableCell>
                  
                  <TableCell>
                    <Chip
                      label={REQUEST_TYPE_LABELS[request.requestType as keyof typeof REQUEST_TYPE_LABELS] || request.requestType}
                      size="small"
                      variant="outlined"
                    />
                  </TableCell>

                  <TableCell>
                    <Box>
                      <Typography variant="body2" fontWeight="medium">
                        {request.requestedBy}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {request.requestedByEmail}
                      </Typography>
                    </Box>
                  </TableCell>

                  <TableCell>
                    <Chip
                      label={PRIORITY_LABELS[request.priority as keyof typeof PRIORITY_LABELS] || request.priority}
                      color={getPriorityColor(request.priority) as any}
                      size="small"
                    />
                  </TableCell>

                  <TableCell>
                    <Chip
                      label={STATUS_LABELS[request.status as keyof typeof STATUS_LABELS] || request.status}
                      color={getStatusColor(request.status) as any}
                      size="small"
                    />
                  </TableCell>

                  <TableCell>
                    <Box sx={{ minWidth: 100 }}>
                      <Typography variant="body2" color="text.secondary">
                        {progress.current}/{progress.total}
                      </Typography>
                      <Box sx={{ 
                        width: '100%', 
                        height: 4, 
                        bgcolor: 'grey.200', 
                        borderRadius: 2,
                        overflow: 'hidden'
                      }}>
                        <Box
                          sx={{
                            width: `${(progress.current / progress.total) * 100}%`,
                            height: '100%',
                            bgcolor: progress.current >= progress.total ? 'success.main' : 'primary.main',
                            transition: 'width 0.3s ease',
                          }}
                        />
                      </Box>
                    </Box>
                  </TableCell>

                  <TableCell>
                    <Typography variant="body2" color="text.secondary">
                      {formatDate(request.createdAt)}
                    </Typography>
                  </TableCell>

                  <TableCell>
                    {request.expiresAt ? (
                      <Typography
                        variant="body2"
                        color={isExpiredRequest ? 'error' : 'text.secondary'}
                        fontWeight={isExpiredRequest ? 'medium' : 'normal'}
                      >
                        {formatDate(request.expiresAt)}
                        {isExpiredRequest && ' (Expired)'}
                      </Typography>
                    ) : (
                      <Typography variant="body2" color="text.secondary">
                        Never
                      </Typography>
                    )}
                  </TableCell>

                  <TableCell align="right">
                    <Box sx={{ display: 'flex', gap: 0.5 }}>
                      {onView && (
                        <Tooltip title="View Details">
                          <IconButton
                            size="small"
                            onClick={() => onView(request)}
                          >
                            <ViewIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      )}
                      
                      {onDecision && request.status === 'PENDING' && (
                        <Tooltip title="Make Decision">
                          <IconButton
                            size="small"
                            onClick={() => onDecision(request)}
                            color="primary"
                          >
                            <ApproveIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      )}
                      
                      {onCancel && request.status === 'PENDING' && (
                        <Tooltip title="Cancel Request">
                          <IconButton
                            size="small"
                            onClick={() => onCancel(request)}
                            color="error"
                          >
                            <CancelIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      )}
                    </Box>
                  </TableCell>
                </TableRow>
              );
            })
          )}
        </TableBody>
      </Table>
    </TableContainer>
  );
};