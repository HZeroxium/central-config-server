import { type FetchBaseQueryError } from '@reduxjs/toolkit/query';
import { type SerializedError } from '@reduxjs/toolkit';

export interface ApiErrorResponse {
  status: string;
  message: string;
  errors?: string[];
  timestamp?: number;
  traceId?: string;
}

export const getErrorMessage = (
  error: FetchBaseQueryError | SerializedError | undefined
): string => {
  if (!error) return 'An unexpected error occurred';

  // Handle RTK Query fetch errors
  if ('status' in error) {
    if (typeof error.status === 'number') {
      switch (error.status) {
        case 400:
          return 'Bad request. Please check your input.';
        case 401:
          return 'Authentication required. Please log in.';
        case 403:
          return 'You do not have permission to perform this action.';
        case 404:
          return 'The requested resource was not found.';
        case 409:
          return 'Conflict. The resource already exists or has been modified.';
        case 422:
          return 'Validation error. Please check your input.';
        case 500:
          return 'Internal server error. Please try again later.';
        default:
          return `Error ${error.status}: Unknown error`;
      }
    }

    // Handle API response errors
    if ('data' in error && error.data) {
      const apiError = error.data as ApiErrorResponse;
      return apiError.message || 'An error occurred';
    }
  }

  // Handle serialized errors
  if ('message' in error && error.message) {
    return error.message;
  }

  return 'An unexpected error occurred';
};

export const isNetworkError = (error: FetchBaseQueryError | SerializedError | undefined): boolean => {
  if (!error) return false;
  
  if ('status' in error) {
    return typeof error.status === 'string' && error.status === 'FETCH_ERROR';
  }
  
  return false;
};

export const isAuthError = (error: FetchBaseQueryError | SerializedError | undefined): boolean => {
  if (!error) return false;
  
  if ('status' in error) {
    return error.status === 401 || error.status === 403;
  }
  
  return false;
};
