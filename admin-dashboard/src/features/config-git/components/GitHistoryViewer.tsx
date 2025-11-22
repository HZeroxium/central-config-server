import { useState } from "react";
import {
  Box,
  Card,
  CardContent,
  Typography,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction,
  IconButton,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Divider,
  Alert,
  CircularProgress,
} from "@mui/material";
import {
  History as HistoryIcon,
  Code as DiffIcon,
} from "@mui/icons-material";
import { format } from "date-fns";
import { DiffViewer } from "./DiffViewer";
import type { CommitResponse } from "@lib/api/models";

interface GitHistoryViewerProps {
  commits: CommitResponse[];
  isLoading?: boolean;
  error?: Error | null;
  currentContent?: string;
}

export function GitHistoryViewer({
  commits,
  isLoading = false,
  error = null,
  currentContent,
}: GitHistoryViewerProps) {
  const [selectedCommit, setSelectedCommit] = useState<CommitResponse | null>(null);
  const [showDiffDialog, setShowDiffDialog] = useState(false);
  const [diffContent, setDiffContent] = useState<string>("");

  const handleViewDiff = (commit: CommitResponse) => {
    // For now, we'll show the commit message. In a real implementation,
    // we'd fetch the file content at that commit SHA
    setDiffContent(commit.message || "No diff available");
    setSelectedCommit(commit);
    setShowDiffDialog(true);
  };

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" p={2}>
        <CircularProgress size={24} />
      </Box>
    );
  }

  if (error) {
    return (
      <Alert severity="error">
        Failed to load commit history: {error.message}
      </Alert>
    );
  }

  if (commits.length === 0) {
    return (
      <Alert severity="info">
        No commit history available for this config file.
      </Alert>
    );
  }

  return (
    <Card variant="outlined">
      <CardContent>
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            gap: 1,
            mb: 2,
          }}
        >
          <HistoryIcon />
          <Typography variant="h6">Commit History</Typography>
          <Chip label={commits.length} size="small" />
        </Box>

        <List>
          {commits.map((commit, index) => (
            <Box key={commit.sha || index}>
              <ListItem>
                <ListItemText
                  primary={
                    <Typography variant="body1" fontWeight={500}>
                      {commit.message || "No message"}
                    </Typography>
                  }
                  secondary={
                    <Box>
                      <Typography variant="body2" color="text.secondary">
                        {commit.author && `By ${commit.author}`}
                        {commit.timestamp &&
                          ` • ${format(
                            new Date(commit.timestamp),
                            "PPpp"
                          )}`}
                      </Typography>
                      {commit.sha && (
                        <Typography
                          variant="caption"
                          fontFamily="monospace"
                          color="text.secondary"
                        >
                          {commit.sha.substring(0, 8)}
                        </Typography>
                      )}
                    </Box>
                  }
                />
                <ListItemSecondaryAction>
                  <IconButton
                    edge="end"
                    onClick={() => handleViewDiff(commit)}
                    size="small"
                    aria-label="View diff"
                  >
                    <DiffIcon />
                  </IconButton>
                </ListItemSecondaryAction>
              </ListItem>
              {index < commits.length - 1 && <Divider />}
            </Box>
          ))}
        </List>

        <Dialog
          open={showDiffDialog}
          onClose={() => setShowDiffDialog(false)}
          maxWidth="lg"
          fullWidth
        >
          <DialogTitle>
            Commit Diff - {selectedCommit?.message || "Unknown"}
          </DialogTitle>
          <DialogContent>
            {selectedCommit && currentContent && (
              <DiffViewer
                original={diffContent}
                modified={currentContent}
                height={400}
              />
            )}
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setShowDiffDialog(false)}>Close</Button>
          </DialogActions>
        </Dialog>
      </CardContent>
    </Card>
  );
}

