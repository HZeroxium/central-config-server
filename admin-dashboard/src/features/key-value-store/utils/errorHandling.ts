/**
 * Error handling utilities for KV operations
 */

import type { KVTransactionResponse } from "@lib/api/models";
import { toast } from "@lib/toast/toast";

/**
 * Parse transaction response and extract error information
 */
export interface TransactionErrorInfo {
  success: boolean;
  failedOperations: Array<{
    path: string;
    message: string;
  }>;
  errorMessage?: string;
}

/**
 * Parse a transaction response to extract error information
 */
export function parseTransactionResponse(response: KVTransactionResponse): TransactionErrorInfo {
  const failedOperations: Array<{ path: string; message: string }> = [];
  
  if (response.results) {
    for (const result of response.results) {
      if (!result.success) {
        failedOperations.push({
          path: result.path ?? "unknown",
          message: result.message ?? "Operation failed",
        });
      }
    }
  }
  
  return {
    success: response.success ?? false,
    failedOperations,
    errorMessage: response.error ?? undefined,
  };
}

/**
 * Format user-friendly error message from transaction response
 */
export function formatTransactionErrorMessage(response: KVTransactionResponse): string {
  const errorInfo = parseTransactionResponse(response);
  
  if (errorInfo.failedOperations.length === 0) {
    return errorInfo.errorMessage ?? "Transaction failed";
  }
  
  if (errorInfo.failedOperations.length === 1) {
    const op = errorInfo.failedOperations[0];
    return `${op.path}: ${op.message}`;
  }
  
  // Multiple failures
  const failures = errorInfo.failedOperations.map(op => `${op.path}: ${op.message}`).join("\n");
  return `${errorInfo.failedOperations.length} operations failed:\n${failures}`;
}

/**
 * Show toast notification for transaction result
 */
export function showTransactionToast(response: KVTransactionResponse, successMessage: string = "Operation completed successfully"): void {
  const errorInfo = parseTransactionResponse(response);
  
  if (errorInfo.success && errorInfo.failedOperations.length === 0) {
    toast.success(successMessage);
    return;
  }
  
  // Partial or complete failure
  const errorMessage = formatTransactionErrorMessage(response);
  
  if (errorInfo.failedOperations.length > 0) {
    // Show detailed error for each failed operation
    if (errorInfo.failedOperations.length === 1) {
      const op = errorInfo.failedOperations[0];
      toast.error("Operation Failed", `${op.path}: ${op.message}`);
    } else {
      // Show summary for multiple failures
      toast.error(
        "Operation Failed",
        `${errorInfo.failedOperations.length} operations failed. ${errorMessage}`
      );
    }
  } else if (errorInfo.errorMessage) {
    toast.error("Operation Failed", errorInfo.errorMessage);
  } else {
    toast.error("Operation Failed", "An error occurred during the operation");
  }
}

/**
 * Check if transaction response indicates success
 */
export function isTransactionSuccess(response: KVTransactionResponse): boolean {
  const errorInfo = parseTransactionResponse(response);
  return errorInfo.success && errorInfo.failedOperations.length === 0;
}

/**
 * Get failed operation paths from transaction response
 */
export function getFailedOperationPaths(response: KVTransactionResponse): string[] {
  const errorInfo = parseTransactionResponse(response);
  return errorInfo.failedOperations.map(op => op.path);
}

