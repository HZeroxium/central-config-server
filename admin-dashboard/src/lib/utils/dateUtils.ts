import { format, formatDistanceToNow } from "date-fns";

/**
 * Parses a timestamp value that can be either:
 * - Unix timestamp in seconds (number < 1e12, e.g., 1763030547.0655098)
 * - Unix timestamp in milliseconds (number >= 1e12, e.g., 1763030547065)
 * - ISO 8601 string (e.g., "2024-01-15T14:30:45.123Z")
 * - Numeric string representing Unix timestamp
 *
 * @param value - The timestamp value to parse
 * @returns Parsed Date object or null if invalid
 */
export function parseTimestamp(
  value: string | number | null | undefined
): Date | null {
  if (value === null || value === undefined) {
    return null;
  }

  // Handle number (Unix timestamp)
  if (typeof value === "number") {
    // Check if it's a valid number
    if (isNaN(value) || !isFinite(value)) {
      return null;
    }

    // Determine if it's in seconds or milliseconds
    // Unix timestamps in seconds are typically < 1e12 (year ~2001 in milliseconds)
    // Unix timestamps in milliseconds are >= 1e12
    // Current year (2025) in seconds: ~1735689600
    // Current year (2025) in milliseconds: ~1735689600000
    const ms = value < 1e12 ? value * 1000 : value;
    
    // Validate the resulting timestamp is reasonable (between year 1970 and 2100)
    const minMs = 0; // Jan 1, 1970
    const maxMs = 4102444800000; // Jan 1, 2100
    if (ms < minMs || ms > maxMs) {
      console.warn("Timestamp out of reasonable range:", value, "->", ms, "ms");
      return null;
    }

    const date = new Date(ms);
    // Double-check the date is valid
    if (isNaN(date.getTime())) {
      return null;
    }
    return date;
  }

  // Handle string
  if (typeof value === "string") {
    const trimmed = value.trim();
    if (!trimmed) {
      return null;
    }

    // Check if it's a numeric string (Unix timestamp)
    // Use a more precise check to avoid false positives
    const num = Number(trimmed);
    if (!isNaN(num) && isFinite(num) && trimmed === String(num)) {
      // Recursively parse as number
      return parseTimestamp(num);
    }

    // Otherwise parse as ISO 8601 string
    const date = new Date(trimmed);
    if (isNaN(date.getTime())) {
      return null;
    }
    return date;
  }

  return null;
}

/**
 * Formats a timestamp value using date-fns format string.
 *
 * @param value - The timestamp value to format
 * @param formatStr - date-fns format string (e.g., "MMM dd, yyyy HH:mm")
 * @param fallback - Fallback string if value is invalid (default: "-")
 * @returns Formatted date string or fallback
 */
export function formatTimestamp(
  value: string | number | null | undefined,
  formatStr: string,
  fallback: string = "-"
): string {
  const date = parseTimestamp(value);
  if (!date) {
    return fallback;
  }

  try {
    return format(date, formatStr);
  } catch (error) {
    console.warn("Failed to format timestamp:", value, error);
    return fallback;
  }
}

/**
 * Formats a timestamp as relative time (e.g., "2 hours ago", "3 days ago").
 *
 * @param value - The timestamp value to format
 * @param fallback - Fallback string if value is invalid (default: "-")
 * @returns Relative time string or fallback
 */
export function formatRelativeTime(
  value: string | number | null | undefined,
  fallback: string = "-"
): string {
  const date = parseTimestamp(value);
  if (!date) {
    return fallback;
  }

  try {
    return formatDistanceToNow(date, { addSuffix: true });
  } catch (error) {
    console.warn("Failed to format relative time:", value, error);
    return fallback;
  }
}

