import {
  Box,
  Button,
  // TextField, // TODO: For future form implementation
  // FormControl, // TODO: For future form implementation
  // InputLabel, // TODO: For future form implementation
  // Select, // TODO: For future form implementation
  // MenuItem, // TODO: For future form implementation
  // Chip, // TODO: For future form implementation
  Typography,
  Divider,
  Stack,
  // Autocomplete, // TODO: For future form implementation
  Drawer,
} from '@mui/material';
import { Save as SaveIcon, Close as CloseIcon } from '@mui/icons-material';

interface ShareFormDrawerProps {
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export function ShareFormDrawer({ open, onClose, onSuccess }: ShareFormDrawerProps) {
  return (
    <Drawer
      anchor="right"
      open={open}
      onClose={onClose}
      PaperProps={{
        sx: { width: { xs: '100%', sm: 600 } },
      }}
    >
      <Box sx={{ p: 3, height: '100%', display: 'flex', flexDirection: 'column' }}>
        <Typography variant="h5" gutterBottom>
          Create Service Share
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Grant service access to teams or users
        </Typography>
        <Divider sx={{ mb: 3 }} />

        <Box sx={{ flex: 1, overflow: 'auto' }}>
          <Typography variant="body1" color="text.secondary">
            Service share form - coming soon
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
            This feature will allow you to:
          </Typography>
          <ul>
            <li>Select a service to share</li>
            <li>Choose team or user to grant access</li>
            <li>Select permissions (VIEW, EDIT, DELETE)</li>
            <li>Choose environments (dev, staging, prod)</li>
            <li>Set expiration date (optional)</li>
          </ul>
        </Box>

        {/* Actions */}
        <Box sx={{ mt: 3, pt: 2, borderTop: 1, borderColor: 'divider' }}>
          <Stack direction="row" spacing={2} justifyContent="flex-end">
            <Button variant="outlined" onClick={onClose} startIcon={<CloseIcon />}>
              Cancel
            </Button>
            <Button
              variant="contained"
              onClick={() => {
                onSuccess();
              }}
              startIcon={<SaveIcon />}
            >
              Create Share
            </Button>
          </Stack>
        </Box>
      </Box>
    </Drawer>
  );
}
