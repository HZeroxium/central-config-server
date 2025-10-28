import { AxiosError } from "axios";
import type { ErrorResponse, ValidationError } from "./models";
import { toast } from "sonner";

/**
 * Transformed error information for UI display
 */
export interface TransformedError {
  title: string;
  message: string;
  statusCode?: number;
  validationErrors?: ValidationError[];
  type:
    | "auth"
    | "permission"
    | "validation"
    | "notFound"
    | "server"
    | "network"
    | "generic";
}

/**
 * Map HTTP status code to error type
 */
function getErrorTypeFromStatus(statusCode: number): TransformedError["type"] {
  if (statusCode === 401) return "auth";
  if (statusCode === 403) return "permission";
  if (statusCode === 404) return "notFound";
  if (statusCode === 400) return "validation";
  if (statusCode >= 500) return "server";
  return "generic";
}

/**
 * Transform Axios error with response
 */
function transformAxiosResponseError(error: AxiosError): TransformedError {
  const data = error.response!.data as ErrorResponse;
  const statusCode = error.response!.status;

  return {
    title: data?.title || error.response!.statusText || "Request Failed",
    message:
      data?.detail ||
      data?.title ||
      "An error occurred while processing your request",
    statusCode,
    validationErrors: data?.validationErrors,
    type: getErrorTypeFromStatus(statusCode),
  };
}

/**
 * Transform Axios network error (no response)
 */
function transformAxiosNetworkError(error: AxiosError): TransformedError {
  if (error.code === "ECONNABORTED") {
    return {
      title: "Request Timeout",
      message: "The request took too long to complete. Please try again.",
      type: "network",
    };
  }
  if (error.code === "ERR_NETWORK") {
    return {
      title: "Network Error",
      message:
        "Unable to connect to the server. Please check your internet connection.",
      type: "network",
    };
  }
  return {
    title: "Request Failed",
    message: error.message || "Failed to complete the request",
    type: "network",
  };
}

/**
 * Transform error into structured format for UI display
 */
export function transformError(error: unknown): TransformedError {
  if (!error) {
    return {
      title: "Unknown Error",
      message: "An unknown error occurred",
      type: "generic",
    };
  }

  if (error instanceof AxiosError && error.response) {
    return transformAxiosResponseError(error);
  }

  if (error instanceof AxiosError) {
    return transformAxiosNetworkError(error);
  }

  if (error instanceof Error) {
    return {
      title: "Error",
      message: error.message,
      type: "generic",
    };
  }

  return {
    title: "Unknown Error",
    message: typeof error === "object" ? JSON.stringify(error) : String(error),
    type: "generic",
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
export function getValidationErrors(
  error: unknown
): Array<{ field: string; message: string }> {
  const transformed = transformError(error);
  return (transformed.validationErrors || []).map((e) => ({
    field: e.field || "",
    message: e.message || "",
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
 * Show auth error and trigger redirect
 */
function handleAuthError(): void {
  toast.error("Authentication Required", {
    description: "Please log in to continue",
  });
  console.warn("Authentication error detected, redirecting to login...");
  setTimeout(() => {
    globalThis.location.href = "/";
  }, 1500);
}

/**
 * Show validation error with field details
 */
function handleValidationError(
  message: string,
  validationErrors?: ValidationError[]
): void {
  const errors = validationErrors || [];
  if (errors.length > 0) {
    const errorList = errors.map((e) => `${e.field}: ${e.message}`).join("\n");
    toast.error("Validation Error", { description: errorList });
  } else {
    toast.error("Validation Error", { description: message });
  }
  console.warn("Validation error:", message);
}

/**
 * Handle API errors with appropriate actions and toast notifications
 * Returns true if error requires special handling (e.g., auth redirect), false otherwise
 */
export function handleApiError(
  error: unknown,
  options?: { silent?: boolean; customMessage?: string }
): boolean {
  if (options?.silent) {
    return false;
  }

  const transformed = transformError(error);
  const message = options?.customMessage || transformed.message;

  if (transformed.type === "auth") {
    handleAuthError();
    return true;
  }

  if (transformed.type === "permission") {
    toast.error("Permission Denied", { description: message });
    console.warn("Permission denied:", message);
    return false;
  }

  if (transformed.type === "server") {
    toast.error("Server Error", {
      description: message || "An unexpected error occurred on the server",
    });
    console.error("Server error:", message);
    return false;
  }

  if (transformed.type === "validation") {
    handleValidationError(message, transformed.validationErrors);
    return false;
  }

  if (transformed.type === "notFound") {
    toast.error("Not Found", { description: message });
    console.warn("Not found error:", message);
    return false;
  }

  if (transformed.type === "network") {
    toast.error("Network Error", { description: message });
    console.error("Network error:", message);
    return false;
  }

  toast.error(transformed.title, { description: message });
  console.error("API error:", message);
  return false;
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
