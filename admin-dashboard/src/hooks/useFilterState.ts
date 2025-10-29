import { useSearchParams } from "react-router-dom";
import { useCallback, useMemo } from "react";

/**
 * Hook to manage filter state synchronized with URL query parameters
 * @param defaultValues - Default filter values
 * @returns Filter state and update functions
 */
export function useFilterState<
  T extends Record<string, string | number | boolean | undefined>
>(
  defaultValues: T
): {
  filters: T;
  updateFilter: <K extends keyof T>(key: K, value: T[K]) => void;
  resetFilters: () => void;
  clearFilter: (key: keyof T) => void;
} {
  const [searchParams, setSearchParams] = useSearchParams();

  // Parse filters from URL params
  const filters = useMemo(() => {
    const parsed: Partial<T> = {};
    for (const key in defaultValues) {
      const urlValue = searchParams.get(key);
      if (urlValue !== null) {
        const defaultValue = defaultValues[key];
        if (typeof defaultValue === "boolean") {
          parsed[key] = (urlValue === "true") as T[Extract<keyof T, string>];
        } else if (typeof defaultValue === "number") {
          parsed[key] = Number(urlValue) as T[Extract<keyof T, string>];
        } else {
          parsed[key] = urlValue as T[Extract<keyof T, string>];
        }
      } else {
        parsed[key] = defaultValues[key];
      }
    }
    return parsed as T;
  }, [searchParams, defaultValues]);

  const updateFilter = useCallback(
    <K extends keyof T>(key: K, value: T[K]) => {
      setSearchParams((prev) => {
        const newParams = new URLSearchParams(prev);
        if (value === undefined || value === "" || value === null) {
          newParams.delete(key as string);
        } else {
          newParams.set(key as string, String(value));
        }
        // Reset page when filters change
        newParams.delete("page");
        return newParams;
      });
    },
    [setSearchParams]
  );

  const resetFilters = useCallback(() => {
    setSearchParams({});
  }, [setSearchParams]);

  const clearFilter = useCallback(
    (key: keyof T) => {
      updateFilter(key, undefined as T[Extract<keyof T, string>]);
    },
    [updateFilter]
  );

  return {
    filters,
    updateFilter,
    resetFilters,
    clearFilter,
  };
}
