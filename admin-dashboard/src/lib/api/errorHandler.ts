import { AxiosError } from 'axios';
import type { ErrorResponse, ValidationError } from './models';
import { toast } from 'sonner';

/**
 * Transformed error information for UI display
 */
export interface TransformedError {
  title: string;
  message: string;
  statusCode?: number;
  validationErrors?: ValidationError[];
  type: 'auth' | 'permission' | 'validation' | 'notFound' | 'server' | 'network' | 'generic';
}

/**
 * Transform error into structured format for UI display
 */
export function transformError(error: unknown): TransformedError {
  if (!error) {
    return {
      title: 'Unknown Error',
      message: 'An unknown error occurred',
      type: 'generic'
    };
  }

  // Axios error with response
  if (error instanceof AxiosError && error.response) {
    const data = error.response.data as ErrorResponse;
    const statusCode = error.response.status;
    
    let type: TransformedError['type'] = 'generic';
    if (statusCode === 401) type = 'auth';
    else if (statusCode === 403) type = 'permission';
    else if (statusCode === 404) type = 'notFound';
    else if (statusCode === 400) type = 'validation';
    else if (statusCode >= 500) type = 'server';

    return {
      title: data?.title || error.response.statusText || 'Request Failed',
      message: data?.detail || data?.title || 'An error occurred while processing your request',
      statusCode,
      validationErrors: data?.validationErrors,
      type
    };
  }

  // Axios error without response (network error)
  if (error instanceof AxiosError) {
    if (error.code === 'ECONNABORTED') {
      return {
        title: 'Request Timeout',
        message: 'The request took too long to complete. Please try again.',
        type: 'network'
      };
    }
    if (error.code === 'ERR_NETWORK') {
      return {
        title: 'Network Error',
        message: 'Unable to connect to the server. Please check your internet connection.',
        type: 'network'
      };
    }
    return {
      title: 'Request Failed',
      message: error.message || 'Failed to complete the request',
      type: 'network'
    };
  }

  // Generic error
  if (error instanceof Error) {
    return {
      title: 'Error',
      message: error.message,
      type: 'generic'
    };
  }

  // Unknown error type
  return {
    title: 'Unknown Error',
    message: String(error),
    type: 'generic'
  };
}

/**
 * Extract error message from API error response
 */
export function getErrorMessage(error: unknown): string {
  const transformed = transformError(error);
  return transformed.message;
}

/**
 * Get validation errors as array of field-message pairs
 */
export function getValidationErrors(error: unknown): Array<{ field: string; message: string }> {
  const transformed = transformError(error);
  return (transformed.validationErrors || []).map(e => ({
    field: e.field || '',
    message: e.message || ''
  }));
}

/**
 * Check if error is an authentication error (401)
 */
export function isAuthError(error: unknown): boolean {
  if (error instanceof AxiosError && error.response) {
    return error.response.status === 401;
  }
  return false;
}

/**
 * Check if error is a permission error (403)
 */
export function isPermissionError(error: unknown): boolean {
  if (error instanceof AxiosError && error.response) {
    return error.response.status === 403;
  }
  return false;
}

/**
 * Check if error is a not found error (404)
 */
export function isNotFoundError(error: unknown): boolean {
  if (error instanceof AxiosError && error.response) {
    return error.response.status === 404;
  }
  return false;
}

/**
 * Check if error is a validation error (400)
 */
export function isValidationError(error: unknown): boolean {
  if (error instanceof AxiosError && error.response) {
    return error.response.status === 400;
  }
  return false;
}

/**
 * Check if error is a server error (5xx)
 */
export function isServerError(error: unknown): boolean {
  if (error instanceof AxiosError && error.response) {
    return error.response.status >= 500;
  }
  return false;
}

/**
 * Handle API errors with appropriate actions and toast notifications
 * Returns true if error was handled (e.g., redirect for auth), false otherwise
 */
export function handleApiError(error: unknown, options?: { silent?: boolean; customMessage?: string }): boolean {
  const transformed = transformError(error);
  const message = options?.customMessage || transformed.message;

  if (!options?.silent) {
    if (transformed.type === 'auth') {
      toast.error('Authentication Required', {
        description: 'Please log in to continue',
      });
      console.warn('Authentication error detected, redirecting to login...');
      // Let Keycloak handle the redirect
      setTimeout(() => {
        window.location.href = '/';
      }, 1500);
      return true;
    }

    if (transformed.type === 'permission') {
      toast.error('Permission Denied', {
        description: message,
      });
      console.warn('Permission denied:', message);
      return true;
    }

    if (transformed.type === 'server') {
      toast.error('Server Error', {
        description: message || 'An unexpected error occurred on the server',
      });
      console.error('Server error:', message);
      return true;
    }

    if (transformed.type === 'validation') {
      const validationErrors = transformed.validationErrors || [];
      if (validationErrors.length > 0) {
        const errorList = validationErrors.map(e => `${e.field}: ${e.message}`).join('\n');
        toast.error('Validation Error', {
          description: errorList,
        });
      } else {
        toast.error('Validation Error', {
          description: message,
        });
      }
      console.warn('Validation error:', message);
      return true;
    }

    if (transformed.type === 'notFound') {
      toast.error('Not Found', {
        description: message,
      });
      console.warn('Not found error:', message);
      return true;
    }

    if (transformed.type === 'network') {
      toast.error('Network Error', {
        description: message,
      });
      console.error('Network error:', message);
      return true;
    }

    // Generic error
    toast.error(transformed.title, {
      description: message,
    });
    console.error('API error:', message);
  }
  
  return true;
}

/**
 * Show success toast notification
 */
export function showSuccess(title: string, message?: string): void {
  toast.success(title, {
    description: message,
  });
}

/**
 * Show info toast notification
 */
export function showInfo(title: string, message?: string): void {
  toast.info(title, {
    description: message,
  });
}

/**
 * Show warning toast notification
 */
export function showWarning(title: string, message?: string): void {
  toast.warning(title, {
    description: message,
  });
}
