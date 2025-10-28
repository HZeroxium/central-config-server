import { Box, Typography, Button, Stack, Divider } from "@mui/material";
import { Close as CloseIcon } from "@mui/icons-material";

interface ServiceShareDrawerProps {
  readonly serviceId: string;
  readonly onClose: () => void;
}

export function ServiceShareDrawer({
  serviceId,
  onClose,
}: ServiceShareDrawerProps) {
  return (
    <Box
      sx={{ p: 3, height: "100%", display: "flex", flexDirection: "column" }}
    >
      <Typography variant="h5" gutterBottom>
        Share Service
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Grant permissions to teams or users for service: {serviceId}
      </Typography>
      <Divider sx={{ mb: 3 }} />

      <Box sx={{ flex: 1, overflow: "auto" }}>
        <Typography variant="body1" color="text.secondary">
          Service sharing functionality - coming soon
        </Typography>
      </Box>

      {/* Actions */}
      <Box sx={{ mt: 3, pt: 2, borderTop: 1, borderColor: "divider" }}>
        <Stack direction="row" spacing={2} justifyContent="flex-end">
          <Button
            variant="outlined"
            onClick={onClose}
            startIcon={<CloseIcon />}
          >
            Close
          </Button>
        </Stack>
      </Box>
    </Box>
  );
}
