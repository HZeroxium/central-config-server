import { useState, useEffect, useCallback, useRef } from "react";
import { useDebounce } from "./useDebounce";

export interface UseSearchWithToggleOptions {
  /** localStorage key for persisting real-time toggle state */
  storageKey: string;
  /** Default value for real-time toggle */
  defaultRealtimeEnabled?: boolean;
  /** Debounce delay in milliseconds (default: 800ms) */
  debounceDelay?: number;
  /** Initial search value */
  initialSearch?: string;
  /** Callback when debounce starts (for visual feedback) */
  onDebounceStart?: () => void;
  /** Callback when debounce completes */
  onDebounceComplete?: () => void;
}

export interface UseSearchWithToggleReturn {
  /** Current search input value */
  search: string;
  /** Set search input value */
  setSearch: (value: string) => void;
  /** Debounced search value (used when real-time is enabled) */
  debouncedSearch: string;
  /** Manual search value (used when real-time is disabled) */
  manualSearch: string;
  /** Whether real-time search is enabled */
  realtimeEnabled: boolean;
  /** Toggle real-time search */
  setRealtimeEnabled: (enabled: boolean) => void;
  /** Trigger manual search */
  handleManualSearch: () => void;
  /** Reset search state */
  handleReset: () => void;
  /** Get the effective search value (debounced if real-time, manual otherwise) */
  effectiveSearch: string;
  /** Whether search is currently active */
  isSearching: boolean;
  /** Whether debouncing is in progress (for visual feedback) */
  isDebouncing: boolean;
}

/**
 * Hook to manage search with real-time toggle functionality
 * Supports localStorage persistence and debouncing
 */
export function useSearchWithToggle(
  options: UseSearchWithToggleOptions
): UseSearchWithToggleReturn {
  const {
    storageKey,
    defaultRealtimeEnabled = true,
    debounceDelay = 800,
    initialSearch = "",
    onDebounceStart,
    onDebounceComplete,
  } = options;

  // Load real-time toggle state from localStorage
  const [realtimeEnabled, setRealtimeEnabledState] = useState<boolean>(() => {
    try {
      const stored = localStorage.getItem(storageKey);
      return stored !== null ? stored === "true" : defaultRealtimeEnabled;
    } catch {
      return defaultRealtimeEnabled;
    }
  });

  // Search state
  const [search, setSearch] = useState(initialSearch);
  // Initialize manualSearch based on real-time state
  // If real-time is disabled initially, use initialSearch; otherwise empty
  const [manualSearch, setManualSearch] = useState(() => {
    // If real-time is disabled, initialize with initialSearch so API call works on page load
    try {
      const stored = localStorage.getItem(storageKey);
      const isRealtimeEnabled = stored !== null ? stored === "true" : defaultRealtimeEnabled;
      return isRealtimeEnabled ? "" : initialSearch;
    } catch {
      return defaultRealtimeEnabled ? "" : initialSearch;
    }
  });

  // Track debouncing state
  const [isDebouncing, setIsDebouncing] = useState(false);
  const debounceTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const previousSearchRef = useRef(search);

  // Debounce search when real-time is enabled
  const debouncedSearch = useDebounce(
    search,
    realtimeEnabled ? debounceDelay : 0
  );

  // Track debouncing state for visual feedback
  useEffect(() => {
    if (!realtimeEnabled) {
      setIsDebouncing(false);
      return;
    }

    // If search value changed, we're starting to debounce
    if (search !== previousSearchRef.current) {
      setIsDebouncing(true);
      onDebounceStart?.();

      // Clear existing timeout
      if (debounceTimeoutRef.current) {
        clearTimeout(debounceTimeoutRef.current);
      }

      // Set timeout to mark debouncing as complete
      debounceTimeoutRef.current = setTimeout(() => {
        setIsDebouncing(false);
        onDebounceComplete?.();
      }, debounceDelay);

      previousSearchRef.current = search;
    }

    return () => {
      if (debounceTimeoutRef.current) {
        clearTimeout(debounceTimeoutRef.current);
      }
    };
  }, [search, realtimeEnabled, debounceDelay, onDebounceStart, onDebounceComplete]);

  // When debounced value changes, mark debouncing as complete
  useEffect(() => {
    if (realtimeEnabled && debouncedSearch === search && search !== "") {
      setIsDebouncing(false);
    }
  }, [debouncedSearch, search, realtimeEnabled]);

  // Update localStorage when real-time toggle changes
  useEffect(() => {
    try {
      localStorage.setItem(storageKey, String(realtimeEnabled));
    } catch {
      // Ignore localStorage errors (e.g., private browsing)
    }
  }, [realtimeEnabled, storageKey]);

  // Get effective search value
  // When real-time is enabled: use debounced search value
  // When real-time is disabled: use manual search value (set when user clicks search)
  const effectiveSearch = realtimeEnabled ? debouncedSearch : manualSearch;

  // Toggle real-time search
  const setRealtimeEnabled = useCallback((enabled: boolean) => {
    setRealtimeEnabledState(enabled);
    setIsDebouncing(false);
    // When switching from real-time to manual, keep current search value as manual
    if (!enabled) {
      setManualSearch(search);
    }
    // When switching from manual to real-time, use current manual search
    if (enabled && manualSearch) {
      setSearch(manualSearch);
    }
  }, [search, manualSearch]);

  // Trigger manual search
  const handleManualSearch = useCallback(() => {
    if (!realtimeEnabled) {
      setManualSearch(search);
    }
  }, [realtimeEnabled, search]);

  // Reset search state
  const handleReset = useCallback(() => {
    setSearch("");
    setManualSearch("");
    setIsDebouncing(false);
  }, []);

  return {
    search,
    setSearch,
    debouncedSearch,
    manualSearch,
    realtimeEnabled,
    setRealtimeEnabled,
    handleManualSearch,
    handleReset,
    effectiveSearch,
    isSearching: effectiveSearch !== "",
    isDebouncing,
  };
}

