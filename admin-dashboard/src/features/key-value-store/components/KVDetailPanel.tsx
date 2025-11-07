/**
 * Detail panel component for KV entries
 */

import { useState } from "react";
import {
  Box,
  Card,
  CardContent,
  Tabs,
  Tab,
  Typography,
  Button,
  IconButton,
  Tooltip,
  Alert,
  Stack,
} from "@mui/material";
import {
  Edit as EditIcon,
  Delete as DeleteIcon,
  Download as DownloadIcon,
  Refresh as RefreshIcon,
} from "@mui/icons-material";
import { TabPanel } from "@components/common/TabPanel";
import { KVEntryEditor } from "./KVEntryEditor";
import type { KVEntry } from "../types";
import { decodeBase64 } from "../types";
import type { KVPutRequest } from "@lib/api/models";

export interface KVDetailPanelProps {
  entry?: KVEntry;
  path: string;
  onEdit: (data: KVPutRequest) => Promise<void>;
  onDelete: () => Promise<void>;
  onRefresh: () => void;
  isReadOnly?: boolean;
  isEditing?: boolean;
  isDeleting?: boolean;
  isLoading?: boolean;
}

export function KVDetailPanel({
  entry,
  path,
  onEdit,
  onDelete,
  onRefresh,
  isReadOnly = false,
  isEditing = false,
  isDeleting = false,
  isLoading = false,
}: KVDetailPanelProps) {
  const [tabValue, setTabValue] = useState(0);
  const [editMode, setEditMode] = useState(false);

  const handleEdit = async (data: KVPutRequest) => {
    await onEdit(data);
    setEditMode(false);
  };

  const handleCancel = () => {
    setEditMode(false);
  };

  const handleDownload = () => {
    if (!entry?.valueBase64) return;

    try {
      const decoded = decodeBase64(entry.valueBase64);
      const blob = new Blob([decoded], { type: "application/octet-stream" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = path.split("/").pop() || "kv-entry";
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (err) {
      console.error("Failed to download:", err);
    }
  };

  if (editMode) {
    return (
      <Card>
        <CardContent>
          <KVEntryEditor
            entry={entry}
            path={path}
            onSave={handleEdit}
            onCancel={handleCancel}
            isReadOnly={isReadOnly}
            isSaving={isEditing}
          />
        </CardContent>
      </Card>
    );
  }

  if (isLoading) {
    return (
      <Card>
        <CardContent>
          <Box sx={{ textAlign: "center", py: 4 }}>
            <Typography variant="body2" color="text.secondary">
              Loading...
            </Typography>
          </Box>
        </CardContent>
      </Card>
    );
  }

  if (!entry) {
    return (
      <Card>
        <CardContent>
          <Box sx={{ textAlign: "center", py: 4 }}>
            <Typography variant="body2" color="text.secondary">
              Select an entry to view details
            </Typography>
          </Box>
        </CardContent>
      </Card>
    );
  }

  const decodedValue = entry.valueBase64
    ? decodeBase64(entry.valueBase64)
    : "";

  return (
    <Card>
      <CardContent>
        <Box
          sx={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            mb: 2,
          }}
        >
          <Typography variant="h6">Entry Details</Typography>
          <Stack direction="row" spacing={1}>
            <Tooltip title="Refresh">
              <IconButton size="small" onClick={onRefresh} aria-label="Refresh">
                <RefreshIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            {!isReadOnly && (
              <>
                <Tooltip title="Edit">
                  <IconButton
                    size="small"
                    onClick={() => setEditMode(true)}
                    aria-label="Edit"
                  >
                    <EditIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
                <Tooltip title="Delete">
                  <IconButton
                    size="small"
                    onClick={onDelete}
                    disabled={isDeleting}
                    aria-label="Delete"
                    color="error"
                  >
                    <DeleteIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
              </>
            )}
          </Stack>
        </Box>

        {isReadOnly && (
          <Alert severity="info" sx={{ mb: 2 }}>
            You have read-only access to this service
          </Alert>
        )}

        <Tabs value={tabValue} onChange={(_, v) => setTabValue(v)}>
          <Tab label="Metadata" />
          <Tab label="Raw Value" />
        </Tabs>

        <TabPanel value={tabValue} index={0}>
          <Stack spacing={2}>
            <Box>
              <Typography variant="caption" color="text.secondary">
                Path
              </Typography>
              <Typography variant="body1" sx={{ wordBreak: "break-word" }}>
                {path}
              </Typography>
            </Box>

            <Box>
              <Typography variant="caption" color="text.secondary">
                Modify Index
              </Typography>
              <Typography variant="body1">
                {entry.modifyIndex ?? "N/A"}
              </Typography>
            </Box>

            <Box>
              <Typography variant="caption" color="text.secondary">
                Create Index
              </Typography>
              <Typography variant="body1">
                {entry.createIndex ?? "N/A"}
              </Typography>
            </Box>

            <Box>
              <Typography variant="caption" color="text.secondary">
                Flags
              </Typography>
              <Typography variant="body1">{entry.flags ?? 0}</Typography>
            </Box>

            <Box>
              <Typography variant="caption" color="text.secondary">
                Value (Decoded)
              </Typography>
              <Box
                sx={{
                  mt: 1,
                  p: 2,
                  bgcolor: "background.default",
                  borderRadius: 1,
                  border: 1,
                  borderColor: "divider",
                  maxHeight: 300,
                  overflow: "auto",
                }}
              >
                <Typography
                  variant="body2"
                  component="pre"
                  sx={{
                    fontFamily: "monospace",
                    whiteSpace: "pre-wrap",
                    wordBreak: "break-word",
                    m: 0,
                  }}
                >
                  {decodedValue || "(empty)"}
                </Typography>
              </Box>
            </Box>
          </Stack>
        </TabPanel>

        <TabPanel value={tabValue} index={1}>
          <Stack spacing={2}>
            <Box
              sx={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
              }}
            >
              <Typography variant="body2" color="text.secondary">
                Raw Value (Base64)
              </Typography>
              <Button
                size="small"
                startIcon={<DownloadIcon />}
                onClick={handleDownload}
                variant="outlined"
              >
                Download
              </Button>
            </Box>

            <Box
              sx={{
                p: 2,
                bgcolor: "background.default",
                borderRadius: 1,
                border: 1,
                borderColor: "divider",
                maxHeight: 400,
                overflow: "auto",
              }}
            >
              <Typography
                variant="body2"
                component="pre"
                sx={{
                  fontFamily: "monospace",
                  whiteSpace: "pre-wrap",
                  wordBreak: "break-word",
                  m: 0,
                }}
              >
                {entry.valueBase64 || "(empty)"}
              </Typography>
            </Box>
          </Stack>
        </TabPanel>
      </CardContent>
    </Card>
  );
}

