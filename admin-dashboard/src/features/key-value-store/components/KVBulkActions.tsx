/**
 * Bulk actions component for KV Store
 */

import {
  Box,
  Button,
  Checkbox,
  FormControlLabel,
  Typography,
  Stack,
} from "@mui/material";
import {
  Delete as DeleteIcon,
} from "@mui/icons-material";
import ConfirmDialog from "@components/common/ConfirmDialog";
import type { KVEntry } from "../types";
import { useState } from "react";

export interface KVBulkActionsProps {
  entries: KVEntry[];
  selectedPaths: Set<string>;
  onSelectionChange: (paths: Set<string>) => void;
  onBulkDelete: (paths: string[], recursive: boolean) => Promise<void>;
  isDeleting?: boolean;
}

export function KVBulkActions({
  entries,
  selectedPaths,
  onSelectionChange,
  onBulkDelete,
  isDeleting = false,
}: KVBulkActionsProps) {
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [recursiveDelete, setRecursiveDelete] = useState(false);

  const selectedCount = selectedPaths.size;
  const hasSelection = selectedCount > 0;

  const handleSelectAll = () => {
    const allPaths = new Set(entries.map((e) => e.path || "").filter(Boolean));
    onSelectionChange(allPaths);
  };

  const handleDeselectAll = () => {
    onSelectionChange(new Set());
  };

  const handleBulkDelete = async () => {
    const paths = Array.from(selectedPaths);
    await onBulkDelete(paths, recursiveDelete);
    setDeleteDialogOpen(false);
    setRecursiveDelete(false);
    onSelectionChange(new Set());
  };

  return (
    <Box
      sx={{
        display: "flex",
        justifyContent: "space-between",
        alignItems: "center",
        p: 2,
        borderBottom: 1,
        borderColor: "divider",
      }}
    >
      <Stack direction="row" spacing={2} alignItems="center">
        <FormControlLabel
          control={
            <Checkbox
              checked={hasSelection && selectedCount === entries.length}
              indeterminate={hasSelection && selectedCount < entries.length}
              onChange={(e) => {
                if (e.target.checked) {
                  handleSelectAll();
                } else {
                  handleDeselectAll();
                }
              }}
            />
          }
          label={
            <Typography variant="body2">
              {hasSelection
                ? `${selectedCount} selected`
                : "Select all"}
            </Typography>
          }
        />

        {hasSelection && (
          <>
            <Button
              size="small"
              startIcon={<DeleteIcon />}
              onClick={() => setDeleteDialogOpen(true)}
              color="error"
              variant="outlined"
            >
              Delete Selected
            </Button>
            <Button
              size="small"
              onClick={handleDeselectAll}
              variant="text"
            >
              Clear
            </Button>
          </>
        )}
      </Stack>

      <ConfirmDialog
        open={deleteDialogOpen}
        onConfirm={handleBulkDelete}
        onCancel={() => {
          setDeleteDialogOpen(false);
          setRecursiveDelete(false);
        }}
        title="Delete Selected Entries"
        message={
          <Box>
            <Typography variant="body1" sx={{ mb: 2 }}>
              Are you sure you want to delete {selectedCount} entry(ies)?
              This action cannot be undone.
            </Typography>
            <FormControlLabel
              control={
                <Checkbox
                  checked={recursiveDelete}
                  onChange={(e) => setRecursiveDelete(e.target.checked)}
                />
              }
              label="Recursive delete (delete all keys under prefix)"
            />
            {recursiveDelete && (
              <Typography variant="caption" color="error" sx={{ ml: 4, display: "block", mt: 1 }}>
                Warning: Recursive delete will remove all keys under the
                selected prefixes
              </Typography>
            )}
          </Box>
        }
        confirmText="Delete"
        cancelText="Cancel"
        confirmColor="error"
        loading={isDeleting}
      />
    </Box>
  );
}

