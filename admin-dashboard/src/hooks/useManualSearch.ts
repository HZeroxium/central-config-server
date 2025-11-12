import { useState, useCallback, useTransition } from "react";

export interface UseManualSearchOptions {
  /** Initial search value */
  initialSearch?: string;
  /** Callback when search is submitted */
  onSearchChange?: (search: string) => void;
}

export interface UseManualSearchReturn {
  /** Current search input value */
  search: string;
  /** Set search input value */
  setSearch: (value: string) => void;
  /** Submitted search value (used for API calls) */
  submittedSearch: string;
  /** Trigger search */
  handleSearch: () => void;
  /** Reset search state */
  handleReset: () => void;
  /** Handle key press (Enter key to submit) */
  handleKeyPress: (e: React.KeyboardEvent<HTMLInputElement>) => void;
  /** Whether input update is pending (for visual feedback) */
  isPending: boolean;
}

/**
 * Hook to manage manual search functionality
 * No debouncing - search is triggered only on button click or Enter key
 * Uses useTransition to mark input updates as non-urgent for better performance
 */
export function useManualSearch(
  options: UseManualSearchOptions = {}
): UseManualSearchReturn {
  const { initialSearch = "", onSearchChange } = options;

  const [search, setSearchState] = useState(initialSearch);
  const [submittedSearch, setSubmittedSearch] = useState(initialSearch);
  const [isPending, startTransition] = useTransition();

  // Optimized setSearch using useTransition for non-urgent updates
  const setSearch = useCallback((value: string) => {
    startTransition(() => {
      setSearchState(value);
    });
  }, []);

  // Trigger search (urgent update)
  const handleSearch = useCallback(() => {
    setSubmittedSearch(search);
    if (onSearchChange) {
      onSearchChange(search);
    }
  }, [search, onSearchChange]);

  // Reset search state
  const handleReset = useCallback(() => {
    setSearchState("");
    setSubmittedSearch("");
    if (onSearchChange) {
      onSearchChange("");
    }
  }, [onSearchChange]);

  // Handle key press (Enter key to submit)
  const handleKeyPress = useCallback(
    (e: React.KeyboardEvent<HTMLInputElement>) => {
      if (e.key === "Enter") {
        e.preventDefault();
        handleSearch();
      }
    },
    [handleSearch]
  );

  return {
    search,
    setSearch,
    submittedSearch,
    handleSearch,
    handleReset,
    handleKeyPress,
    isPending,
  };
}

