/**
 * Error logging utility for SDK integration feature.
 */

interface ErrorLogEntry {
  timestamp: string;
  step: number;
  error: Error;
  context?: Record<string, unknown>;
  userId?: string;
}

/**
 * Logs errors for SDK integration wizard.
 */
export function logSdkIntegrationError(
  error: Error,
  step: number,
  context?: Record<string, unknown>
): void {
  const logEntry: ErrorLogEntry = {
    timestamp: new Date().toISOString(),
    step,
    error,
    context,
  };

  // Log to console in development
  if (import.meta.env.DEV) {
    console.error('[SDK Integration Error]', logEntry);
  }

  // In production, you could send to an error tracking service
  // Example: Sentry.captureException(error, { extra: logEntry });
}

/**
 * Logs API errors with context.
 */
export function logApiError(
  error: unknown,
  endpoint: string,
  step: number,
  requestData?: Record<string, unknown>
): void {
  const errorMessage = error instanceof Error ? error.message : 'Unknown error';
  const apiError = new Error(`API Error (${endpoint}): ${errorMessage}`);
  
  logSdkIntegrationError(apiError, step, {
    endpoint,
    requestData,
    errorType: 'API_ERROR',
  });
}

