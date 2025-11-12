import { useEffect, useRef, useCallback } from "react";

export interface UseGlobalSearchShortcutOptions {
  /** Whether the shortcut is enabled */
  enabled?: boolean;
  /** Callback when Ctrl+K is pressed */
  onShortcut?: () => void;
  /** Selector for search field to focus (default: 'input[type="text"][aria-label*="Search"]') */
  searchFieldSelector?: string;
}

/**
 * Hook to handle global Ctrl+K keyboard shortcut for focusing search fields
 * Supports multiple search fields on the same page
 */
export function useGlobalSearchShortcut(
  options: UseGlobalSearchShortcutOptions = {}
): void {
  const {
    enabled = true,
    onShortcut,
    searchFieldSelector = 'input[type="text"][aria-label*="Search"], input[type="text"][placeholder*="Search"], input[type="text"][placeholder*="search"]',
  } = options;

  const handlerRef = useRef<(e: KeyboardEvent) => void>();

  useEffect(() => {
    if (!enabled) return;

    handlerRef.current = (e: KeyboardEvent) => {
      // Check for Ctrl+K (Windows/Linux) or Cmd+K (Mac)
      if ((e.ctrlKey || e.metaKey) && e.key === "k") {
        e.preventDefault();

        // If custom callback provided, use it
        if (onShortcut) {
          onShortcut();
          return;
        }

        // Otherwise, find and focus the first search field
        const searchFields = document.querySelectorAll<HTMLInputElement>(
          searchFieldSelector
        );

        if (searchFields.length > 0) {
          // Focus the first visible search field
          for (const field of Array.from(searchFields)) {
            if (field.offsetParent !== null) {
              // Field is visible
              field.focus();
              field.select(); // Select text for easy replacement
              break;
            }
          }
        }
      }
    };

    window.addEventListener("keydown", handlerRef.current);

    return () => {
      if (handlerRef.current) {
        window.removeEventListener("keydown", handlerRef.current);
      }
    };
  }, [enabled, onShortcut, searchFieldSelector]);
}

/**
 * Hook to register a search field for Ctrl+K focus
 * Returns ref to attach to input element
 */
export function useSearchFieldRef(): React.RefObject<HTMLInputElement> {
  const inputRef = useRef<HTMLInputElement>(null);

  const focusHandler = useCallback(() => {
    if (inputRef.current) {
      inputRef.current.focus();
      inputRef.current.select();
    }
  }, []);

  useEffect(() => {
    if (!inputRef.current) return;

    // Add data attribute for easy selection
    inputRef.current.setAttribute("data-search-field", "true");

    // Listen for custom focus event
    const handleFocusEvent = () => {
      focusHandler();
    };

    inputRef.current.addEventListener("focus-search", handleFocusEvent);

    return () => {
      if (inputRef.current) {
        inputRef.current.removeEventListener("focus-search", handleFocusEvent);
      }
    };
  }, [focusHandler]);

  return inputRef;
}

