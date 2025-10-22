import { useSnackbar } from 'notistack';
import { useCallback } from 'react';
import { getErrorMessage, isAuthError, isPermissionError, isServerError } from '@lib/api/errorHandler';

/**
 * Custom hook for handling errors with toast notifications
 */
export function useErrorHandler() {
  const { enqueueSnackbar } = useSnackbar();

  const handleError = useCallback((error: unknown, customMessage?: string) => {
    const message = customMessage || getErrorMessage(error);

    if (isAuthError(error)) {
      enqueueSnackbar('Authentication required. Please log in.', {
        variant: 'error',
        autoHideDuration: 5000,
      });
      return;
    }

    if (isPermissionError(error)) {
      enqueueSnackbar(`Permission denied: ${message}`, {
        variant: 'error',
        autoHideDuration: 5000,
      });
      return;
    }

    if (isServerError(error)) {
      enqueueSnackbar(`Server error: ${message}`, {
        variant: 'error',
        autoHideDuration: 7000,
      });
      return;
    }

    // Generic error
    enqueueSnackbar(message, {
      variant: 'error',
      autoHideDuration: 5000,
    });
  }, [enqueueSnackbar]);

  const showSuccess = useCallback((message: string) => {
    enqueueSnackbar(message, {
      variant: 'success',
      autoHideDuration: 3000,
    });
  }, [enqueueSnackbar]);

  const showInfo = useCallback((message: string) => {
    enqueueSnackbar(message, {
      variant: 'info',
      autoHideDuration: 4000,
    });
  }, [enqueueSnackbar]);

  const showWarning = useCallback((message: string) => {
    enqueueSnackbar(message, {
      variant: 'warning',
      autoHideDuration: 4000,
    });
  }, [enqueueSnackbar]);

  return {
    handleError,
    showSuccess,
    showInfo,
    showWarning,
  };
}

