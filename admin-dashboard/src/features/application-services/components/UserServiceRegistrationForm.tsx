import {
  Box,
  Button,
  TextField,
  Typography,
  Divider,
  Stack,
  Alert,
} from "@mui/material";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Save as SaveIcon, Close as CloseIcon } from "@mui/icons-material";
import { useAuth } from "@features/auth/context";
import { useServiceRegistration } from "../hooks/useServiceRegistration";
import {
  userServiceRegistrationSchema,
  type UserServiceRegistrationInput,
} from "@lib/forms/schemas";

interface UserServiceRegistrationFormProps {
  onSuccess: (approvalRequestId: string, serviceId: string) => void;
  onCancel: () => void;
}

export function UserServiceRegistrationForm({
  onSuccess,
  onCancel,
}: UserServiceRegistrationFormProps) {
  const { userInfo } = useAuth();
  const { register, isRegistering, error } = useServiceRegistration();

  const {
    control,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<UserServiceRegistrationInput>({
    resolver: zodResolver(userServiceRegistrationSchema),
    defaultValues: {
      id: "",
      displayName: "",
    },
  });

  const onSubmit = async (data: UserServiceRegistrationInput) => {
    // Get user's team (first team if multiple)
    const targetTeamId = userInfo?.teamIds?.[0];
    if (!targetTeamId) {
      // This should not happen if user is authenticated, but handle gracefully
      return;
    }

    try {
      const result = await register(data.id, data.displayName, targetTeamId);
      onSuccess(result.approvalRequestId, result.serviceId);
    } catch (err) {
      // Error is already handled in the hook via toast
      // Don't call onSuccess if registration failed
    }
  };

  const isLoading = isRegistering || isSubmitting;

  return (
    <Box
      sx={{ p: 3, height: "100%", display: "flex", flexDirection: "column" }}
    >
      <Typography variant="h5" gutterBottom>
        Register Application Service
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Register a new application service. Your request will require admin
        approval before credentials are generated.
      </Typography>
      <Divider sx={{ mb: 3 }} />

      <Box
        component="form"
        onSubmit={handleSubmit(onSubmit)}
        sx={{ flex: 1, overflow: "auto" }}
      >
        <Stack spacing={3}>
          {/* Service ID */}
          <Controller
            name="id"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Service ID *"
                fullWidth
                error={!!errors.id}
                helperText={
                  errors.id?.message ||
                  "Unique identifier for the service (e.g., user-service). Only lowercase letters, numbers, and hyphens."
                }
                disabled={isLoading}
              />
            )}
          />

          {/* Display Name */}
          <Controller
            name="displayName"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                label="Display Name *"
                fullWidth
                error={!!errors.displayName}
                helperText={
                  errors.displayName?.message ||
                  "Human-readable name for the service"
                }
                disabled={isLoading}
              />
            )}
          />

          {error && (
            <Alert severity="error">
              {error instanceof Error
                ? error.message
                : typeof error === "object" && "detail" in error
                ? (error as { detail?: string }).detail || "Registration failed"
                : "Registration failed. Please try again."}
            </Alert>
          )}
        </Stack>
      </Box>

      {/* Actions */}
      <Box sx={{ mt: 3, pt: 2, borderTop: 1, borderColor: "divider" }}>
        <Stack direction="row" spacing={2} justifyContent="flex-end">
          <Button
            variant="outlined"
            onClick={onCancel}
            disabled={isLoading}
            startIcon={<CloseIcon />}
          >
            Cancel
          </Button>
          <Button
            variant="contained"
            onClick={handleSubmit(onSubmit)}
            disabled={isLoading}
            startIcon={<SaveIcon />}
          >
            {isLoading ? "Registering..." : "Register Service"}
          </Button>
        </Stack>
      </Box>
    </Box>
  );
}

