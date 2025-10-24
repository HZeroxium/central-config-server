import { Timeline, TimelineItem, TimelineSeparator, TimelineConnector, TimelineContent, TimelineDot, TimelineOppositeContent } from '@mui/lab';
import { Typography, Paper, Box, Chip } from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import { formatDistanceToNow } from 'date-fns';

interface GateDecision {
  gateName: string;
  decision: 'APPROVE' | 'REJECT';
  approverUserId: string;
  timestamp?: string;
  notes?: string;
}

interface DecisionTimelineProps {
  decisions: GateDecision[];
}

export default function DecisionTimeline({ decisions }: DecisionTimelineProps) {
  if (!decisions || decisions.length === 0) {
    return (
      <Box sx={{ py: 4, textAlign: 'center' }}>
        <Typography variant="body2" color="text.secondary">
          No decisions recorded yet
        </Typography>
      </Box>
    );
  }

  // Sort decisions by timestamp (most recent first)
  const sortedDecisions = [...decisions].sort((a, b) => {
    const dateA = a.timestamp ? new Date(a.timestamp).getTime() : 0;
    const dateB = b.timestamp ? new Date(b.timestamp).getTime() : 0;
    return dateB - dateA;
  });

  return (
    <Timeline position="right">
      {sortedDecisions.map((decision, index) => {
        const isApproved = decision.decision === 'APPROVE';
        const timestamp = decision.timestamp ? new Date(decision.timestamp) : null;

        return (
          <TimelineItem key={index}>
            <TimelineOppositeContent color="text.secondary" sx={{ flex: 0.3 }}>
              {timestamp && (
                <>
                  <Typography variant="caption" display="block">
                    {timestamp.toLocaleString()}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {formatDistanceToNow(timestamp, { addSuffix: true })}
                  </Typography>
                </>
              )}
            </TimelineOppositeContent>
            <TimelineSeparator>
              <TimelineDot color={isApproved ? 'success' : 'error'}>
                {isApproved ? <CheckCircleIcon fontSize="small" /> : <CancelIcon fontSize="small" />}
              </TimelineDot>
              {index < sortedDecisions.length - 1 && <TimelineConnector />}
            </TimelineSeparator>
            <TimelineContent>
              <Paper elevation={1} sx={{ p: 2, mb: 2 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                  <Chip 
                    label={decision.decision}
                    size="small"
                    color={isApproved ? 'success' : 'error'}
                  />
                  <Typography variant="body2" fontWeight="medium">
                    {decision.gateName}
                  </Typography>
                </Box>
                <Typography variant="body2" color="text.secondary" gutterBottom>
                  Approver: <strong>{decision.approverUserId}</strong>
                </Typography>
                {decision.notes && (
                  <Paper variant="outlined" sx={{ p: 1.5, mt: 1, bgcolor: 'background.default' }}>
                    <Typography variant="body2" color="text.secondary" sx={{ fontStyle: 'italic' }}>
                      "{decision.notes}"
                    </Typography>
                  </Paper>
                )}
              </Paper>
            </TimelineContent>
          </TimelineItem>
        );
      })}
    </Timeline>
  );
}

