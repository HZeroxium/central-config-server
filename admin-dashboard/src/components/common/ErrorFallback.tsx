import {
  Alert,
  AlertTitle,
  Button,
  Stack,
  Typography,
  Box,
} from "@mui/material";
import { useState } from "react";
import { Refresh as RefreshIcon } from "@mui/icons-material";

interface ErrorFallbackProps {
  title?: string;
  message?: string;
  onRetry?: () => void | Promise<unknown>;
  /**
   * Maximum number of retry attempts
   */
  maxRetries?: number;
  /**
   * Enable exponential backoff
   */
  enableBackoff?: boolean;
  /**
   * Initial delay in milliseconds
   */
  initialDelay?: number;
}

export default function ErrorFallback({
  title = "Something went wrong",
  message,
  onRetry,
  maxRetries = 3,
  enableBackoff = true,
  initialDelay = 1000,
}: ErrorFallbackProps) {
  const [retryCount, setRetryCount] = useState(0);
  const [isRetrying, setIsRetrying] = useState(false);
  const [cooldownSeconds, setCooldownSeconds] = useState(0);

  // Calculate delay with exponential backoff
  const getDelay = (attempt: number): number => {
    if (!enableBackoff) return initialDelay;
    return initialDelay * Math.pow(2, attempt);
  };

  // Handle retry with cooldown
  const handleRetry = async () => {
    if (retryCount >= maxRetries) {
      return;
    }

    setIsRetrying(true);
    const delay = getDelay(retryCount);

    // Show cooldown timer
    let remainingSeconds = Math.ceil(delay / 1000);
    setCooldownSeconds(remainingSeconds);

    const intervalId = setInterval(() => {
      remainingSeconds -= 1;
      setCooldownSeconds(remainingSeconds);

      if (remainingSeconds <= 0) {
        clearInterval(intervalId);
      }
    }, 1000);

    // Wait for cooldown
    await new Promise((resolve) => setTimeout(resolve, delay));
    clearInterval(intervalId);
    setCooldownSeconds(0);

    // Execute retry
    try {
      await onRetry?.();
      setRetryCount(0); // Reset on success
    } catch {
      setRetryCount((prev) => prev + 1);
    } finally {
      setIsRetrying(false);
    }
  };

  const canRetry = retryCount < maxRetries && !isRetrying;
  const hasExceededRetries = retryCount >= maxRetries;

  return (
    <Stack spacing={2}>
      <Alert severity="error">
        <AlertTitle>{title}</AlertTitle>
        <Box>
          {message || "Please try again later."}
          {retryCount > 0 && (
            <Typography variant="caption" display="block" sx={{ mt: 1 }}>
              Retry attempt {retryCount} of {maxRetries}
            </Typography>
          )}
          {hasExceededRetries && (
            <Typography
              variant="caption"
              display="block"
              sx={{ mt: 1, fontWeight: 600 }}
            >
              Maximum retry attempts reached. Please refresh the page or contact
              support.
            </Typography>
          )}
        </Box>
      </Alert>

      {onRetry && canRetry && (
        <Stack direction="row" spacing={2} alignItems="center">
          <Button
            variant="contained"
            color="primary"
            onClick={handleRetry}
            disabled={isRetrying || cooldownSeconds > 0}
            startIcon={<RefreshIcon />}
            sx={{ alignSelf: "start" }}
          >
            {isRetrying
              ? "Retrying..."
              : cooldownSeconds > 0
              ? `Retry in ${cooldownSeconds}s`
              : "Retry"}
          </Button>
          {cooldownSeconds > 0 && enableBackoff && (
            <Typography variant="caption" color="text.secondary">
              Exponential backoff: {cooldownSeconds}s
            </Typography>
          )}
        </Stack>
      )}
    </Stack>
  );
}
