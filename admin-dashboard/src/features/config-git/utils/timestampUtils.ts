/**
 * Timestamp utility functions for Git history
 */

/**
 * Parse Unix timestamp (seconds) to Date object
 * Handles both string and number formats
 * 
 * @param timestamp - Unix timestamp in seconds (string or number)
 * @returns Date object
 */
export function parseUnixTimestamp(timestamp: string | number | undefined): Date | null {
  if (!timestamp) return null;
  
  // Convert to number if string
  const numTimestamp = typeof timestamp === "string" ? parseFloat(timestamp) : timestamp;
  
  // Check if it's a valid number
  if (isNaN(numTimestamp)) return null;
  
  // If timestamp is less than a reasonable date (year 2000), assume it's in seconds
  // Otherwise, assume it's already in milliseconds
  const threshold = 946684800000; // Jan 1, 2000 in milliseconds
  
  // If timestamp is less than threshold when treated as milliseconds, it's likely in seconds
  if (numTimestamp < threshold) {
    // Convert seconds to milliseconds
    return new Date(numTimestamp * 1000);
  }
  
  // Already in milliseconds
  return new Date(numTimestamp);
}

