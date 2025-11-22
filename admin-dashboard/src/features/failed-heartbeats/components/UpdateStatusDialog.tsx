import { useState } from "react";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from "@mui/material";
import type { UpdateStatusRequestStatus } from "@lib/api/models";

interface UpdateStatusDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: (status: UpdateStatusRequestStatus, notes?: string) => void;
  loading?: boolean;
  currentStatus?: string;
}

export function UpdateStatusDialog({
  open,
  onClose,
  onConfirm,
  loading = false,
}: UpdateStatusDialogProps) {
  const [status, setStatus] = useState<UpdateStatusRequestStatus>("INVESTIGATING");
  const [notes, setNotes] = useState("");

  const handleConfirm = () => {
    onConfirm(status, notes || undefined);
    setNotes("");
  };

  const handleClose = () => {
    setStatus("INVESTIGATING");
    setNotes("");
    onClose();
  };

  return (
    <Dialog
      open={open}
      onClose={handleClose}
      maxWidth="sm"
      fullWidth
      aria-labelledby="update-status-dialog-title"
    >
      <DialogTitle id="update-status-dialog-title">
        Update Failed Heartbeat Status
      </DialogTitle>
      <DialogContent>
        <FormControl fullWidth sx={{ mb: 2 }}>
          <InputLabel>Status</InputLabel>
          <Select
            value={status}
            onChange={(e) =>
              setStatus(e.target.value as UpdateStatusRequestStatus)
            }
            label="Status"
          >
            <MenuItem value="INVESTIGATING">Investigating</MenuItem>
            <MenuItem value="RESOLVED">Resolved</MenuItem>
            <MenuItem value="IGNORED">Ignored</MenuItem>
          </Select>
        </FormControl>

        <TextField
          fullWidth
          multiline
          rows={4}
          label="Notes (optional)"
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          placeholder="Add any notes or investigation summary..."
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClose} disabled={loading}>
          Cancel
        </Button>
        <Button
          onClick={handleConfirm}
          variant="contained"
          disabled={loading}
        >
          {loading ? "Updating..." : "Update"}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

