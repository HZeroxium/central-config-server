import { useEffect, useRef, useMemo } from "react";
import { useSearchParams } from "react-router-dom";

export interface UseDebouncedUrlSyncOptions {
  /** Values to sync to URL */
  values: Record<string, string | number | boolean | null | undefined>;
  /** Debounce delay in milliseconds (default: 300ms) */
  debounceDelay?: number;
  /** Whether to enable URL sync (default: true) */
  enabled?: boolean;
}

/**
 * Hook to debounce URL parameter synchronization
 * Prevents blocking UI thread during rapid filter changes
 * 
 * The key insight: URL sync should be debounced separately from search debounce
 * to prevent blocking the input field updates.
 * 
 * @example
 * ```tsx
 * useDebouncedUrlSync({
 *   values: {
 *     search: effectiveSearch,
 *     page: page,
 *     size: pageSize,
 *   },
 *   debounceDelay: 300,
 * });
 * ```
 */
export function useDebouncedUrlSync({
  values,
  debounceDelay = 300,
  enabled = true,
}: UseDebouncedUrlSyncOptions): void {
  const [, setSearchParams] = useSearchParams();
  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const previousValuesRef = useRef<string>("");

  // Memoize serialized values to prevent unnecessary effect runs
  const valuesStr = useMemo(() => JSON.stringify(values), [values]);

  useEffect(() => {
    if (!enabled) return;

    // Only sync if values actually changed
    if (valuesStr === previousValuesRef.current) {
      return;
    }

    // Clear existing timeout
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current);
    }

    // Capture current values to avoid stale closure
    const currentValuesStr = valuesStr;
    const currentValues = { ...values };

    // Debounce URL sync to prevent blocking UI thread
    // Use setTimeout to defer URL update to next event loop tick
    timeoutRef.current = setTimeout(() => {
      // Double-check values haven't changed while waiting
      if (currentValuesStr !== previousValuesRef.current) {
        const params = new URLSearchParams();

        // Update params based on captured values
        Object.entries(currentValues).forEach(([key, value]) => {
          if (value === null || value === undefined || value === "" || value === false) {
            // Don't set empty values
            return;
          }
          params.set(key, String(value));
        });

        // Use functional update to avoid dependency on searchParams
        setSearchParams((prev) => {
          const prevStr = prev.toString();
          const newStr = params.toString();
          
          // Only update if params actually changed
          if (prevStr !== newStr) {
            return params;
          }
          return prev;
        });

        previousValuesRef.current = currentValuesStr;
      }
    }, debounceDelay);

    return () => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }
    };
  }, [valuesStr, values, debounceDelay, enabled, setSearchParams]);
}

