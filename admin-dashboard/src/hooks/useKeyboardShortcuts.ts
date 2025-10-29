import { useEffect, useCallback } from "react";

export interface KeyboardShortcut {
  /**
   * Keyboard key combination (e.g., 'ctrl+k', 'cmd+/', 'escape')
   */
  key: string;
  /**
   * Handler function
   */
  handler: (event: KeyboardEvent) => void;
  /**
   * Description for help dialog
   */
  description?: string;
  /**
   * Whether to prevent default behavior
   */
  preventDefault?: boolean;
  /**
   * Whether shortcut is enabled
   */
  enabled?: boolean;
}

interface UseKeyboardShortcutsOptions {
  /**
   * List of keyboard shortcuts
   */
  shortcuts: KeyboardShortcut[];
  /**
   * Whether shortcuts are enabled
   */
  enabled?: boolean;
}

/**
 * Hook for managing keyboard shortcuts
 *
 * @example
 * ```tsx
 * useKeyboardShortcuts({
 *   shortcuts: [
 *     {
 *       key: 'ctrl+k',
 *       handler: () => setSearchOpen(true),
 *       description: 'Open search',
 *     },
 *     {
 *       key: 'escape',
 *       handler: () => setSearchOpen(false),
 *       description: 'Close search',
 *     },
 *   ],
 * });
 * ```
 */
export function useKeyboardShortcuts({
  shortcuts,
  enabled = true,
}: UseKeyboardShortcutsOptions) {
  const handleKeyDown = useCallback(
    (event: KeyboardEvent) => {
      if (!enabled) return;

      // Check if user is typing in an input/textarea/contenteditable
      const target = event.target as HTMLElement;
      const isInputElement =
        target.tagName === "INPUT" ||
        target.tagName === "TEXTAREA" ||
        target.isContentEditable;

      // Don't trigger shortcuts when typing (except for Escape and special keys)
      if (isInputElement && !["Escape", "Tab"].includes(event.key)) {
        return;
      }

      // Check each shortcut
      for (const shortcut of shortcuts) {
        if (shortcut.enabled === false) continue;

        const key = shortcut.key.toLowerCase();
        const parts = key.split("+");
        const modifiers = {
          ctrl: parts.includes("ctrl") || parts.includes("cmd"),
          shift: parts.includes("shift"),
          alt: parts.includes("alt"),
          meta: parts.includes("meta"),
        };

        const keyPart = parts[parts.length - 1].toLowerCase();

        // Check modifiers
        const ctrlMatch = modifiers.ctrl
          ? event.ctrlKey || event.metaKey
          : !event.ctrlKey && !event.metaKey;
        const shiftMatch = modifiers.shift ? event.shiftKey : !event.shiftKey;
        const altMatch = modifiers.alt ? event.altKey : !event.altKey;

        // Check key
        const keyMatch =
          keyPart === event.key.toLowerCase() ||
          (keyPart === "space" && event.key === " ") ||
          (keyPart === "enter" && event.key === "Enter") ||
          (keyPart === "escape" && event.key === "Escape");

        if (ctrlMatch && shiftMatch && altMatch && keyMatch) {
          if (shortcut.preventDefault !== false) {
            event.preventDefault();
          }
          shortcut.handler(event);
          break; // Only trigger one shortcut
        }
      }
    },
    [shortcuts, enabled]
  );

  useEffect(() => {
    if (!enabled) return;

    window.addEventListener("keydown", handleKeyDown);
    return () => {
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [handleKeyDown, enabled]);
}

/**
 * Get readable key combination string
 */
export function getKeyDisplay(key: string): string {
  const isMac = navigator.platform.toUpperCase().indexOf("MAC") >= 0;
  const parts = key.split("+");

  return parts
    .map((part) => {
      switch (part.toLowerCase()) {
        case "ctrl":
          return isMac ? "⌃" : "Ctrl";
        case "cmd":
        case "meta":
          return isMac ? "⌘" : "Ctrl";
        case "shift":
          return isMac ? "⇧" : "Shift";
        case "alt":
          return isMac ? "⌥" : "Alt";
        case "space":
          return "Space";
        case "enter":
          return "Enter";
        case "escape":
          return "Esc";
        default:
          return part.toUpperCase();
      }
    })
    .join(isMac ? "" : "+");
}
