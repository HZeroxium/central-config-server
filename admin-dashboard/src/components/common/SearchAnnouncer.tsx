import { useEffect, useRef } from "react";
import { Box } from "@mui/material";

export interface SearchAnnouncerProps {
  /** Message to announce */
  message: string;
  /** Priority: 'polite' (default) or 'assertive' */
  priority?: "polite" | "assertive";
  /** Whether to announce immediately */
  announce?: boolean;
}

/**
 * Component to announce search results to screen readers
 * Uses ARIA live regions for accessibility
 */
export function SearchAnnouncer({
  message,
  priority = "polite",
  announce = false,
}: SearchAnnouncerProps) {
  const previousMessageRef = useRef<string>("");

  useEffect(() => {
    if (announce && message && message !== previousMessageRef.current) {
      previousMessageRef.current = message;
    }
  }, [message, announce]);

  if (!announce || !message) {
    return null;
  }

  return (
    <Box
      component="div"
      role="status"
      aria-live={priority}
      aria-atomic="true"
      sx={{
        position: "absolute",
        left: "-10000px",
        width: "1px",
        height: "1px",
        overflow: "hidden",
      }}
    >
      {message}
    </Box>
  );
}

