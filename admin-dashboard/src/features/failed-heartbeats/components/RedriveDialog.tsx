import { useState } from "react";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  Button,
  Checkbox,
  FormControlLabel,
  Typography,
  Box,
} from "@mui/material";

interface RedriveDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: (force: boolean) => void;
  loading?: boolean;
  serviceName?: string;
  instanceId?: string;
}

export function RedriveDialog({
  open,
  onClose,
  onConfirm,
  loading = false,
  serviceName,
  instanceId,
}: RedriveDialogProps) {
  const [force, setForce] = useState(false);

  const handleConfirm = () => {
    onConfirm(force);
    setForce(false);
  };

  const handleClose = () => {
    setForce(false);
    onClose();
  };

  return (
    <Dialog
      open={open}
      onClose={handleClose}
      maxWidth="sm"
      fullWidth
      aria-labelledby="redrive-dialog-title"
    >
      <DialogTitle id="redrive-dialog-title">
        Re-drive Failed Heartbeat
      </DialogTitle>
      <DialogContent>
        <DialogContentText component="div">
          <Typography variant="body1" gutterBottom>
            Are you sure you want to re-drive this failed heartbeat back to the
            main topic for reprocessing?
          </Typography>
          {serviceName && (
            <Box sx={{ mt: 2 }}>
              <Typography variant="body2" component="div">
                <strong>Service:</strong> {serviceName}
              </Typography>
              {instanceId && (
                <Typography variant="body2" component="div">
                  <strong>Instance:</strong> {instanceId}
                </Typography>
              )}
            </Box>
          )}
          <FormControlLabel
            control={
              <Checkbox
                checked={force}
                onChange={(e) => setForce(e.target.checked)}
              />
            }
            label="Force re-drive (ignore age restrictions)"
            sx={{ mt: 2 }}
          />
        </DialogContentText>
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
          {loading ? "Re-driving..." : "Re-drive"}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

