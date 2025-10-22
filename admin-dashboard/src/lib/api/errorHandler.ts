import { AxiosError } from 'axios';
import type { ErrorResponse } from './types';

/**
 * Extract error message from API error response
 */
export function getErrorMessage(error: unknown): string {
  if (!error) return 'An unknown error occurred';

  // Axios error with response
  if (error instanceof AxiosError && error.response) {
    const data = error.response.data as ErrorResponse;
    
    // Use error message from response
    if (data?.message) {
      return data.message;
    }
    
    // Use validation errors if present
    if (data?.validationErrors && data.validationErrors.length > 0) {
      return data.validationErrors.map(ve => `${ve.field}: ${ve.message}`).join(', ');
    }
    
    // Fallback to status text
    return error.response.statusText || 'Request failed';
  }

  // Axios error without response (network error)
  if (error instanceof AxiosError) {
    if (error.code === 'ECONNABORTED') {
      return 'Request timeout - please try again';
    }
    if (error.code === 'ERR_NETWORK') {
      return 'Network error - please check your connection';
    }
    return error.message || 'Request failed';
  }

  // Generic error
  if (error instanceof Error) {
    return error.message;
  }

  // Unknown error type
  return String(error);
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
 * Handle API errors with appropriate actions
 * Returns true if error was handled (e.g., redirect for auth), false otherwise
 */
export function handleApiError(error: unknown): boolean {
  if (isAuthError(error)) {
    // Redirect to login
    console.warn('Authentication error detected, redirecting to login...');
    // Let Keycloak handle the redirect
    window.location.href = '/';
    return true;
  }

  if (isPermissionError(error)) {
    console.warn('Permission denied:', getErrorMessage(error));
    return false;
  }

  if (isServerError(error)) {
    console.error('Server error:', getErrorMessage(error));
    return false;
  }

  console.error('API error:', getErrorMessage(error));
  return false;
}
