import { useCallback, useRef, useEffect, memo } from "react";
import {
  TextField,
  InputAdornment,
  IconButton,
  Tooltip,
  Box,
  CircularProgress,
} from "@mui/material";
import {
  Search as SearchIcon,
  Clear as ClearIcon,
  Refresh as RefreshIcon,
} from "@mui/icons-material";
import { SearchAnnouncer } from "./SearchAnnouncer";
import { useSearchFieldRef } from "@hooks/useGlobalSearchShortcut";

export interface ManualSearchFieldProps {
  /** Current search value */
  value: string;
  /** Callback when search value changes */
  onChange: (value: string) => void;
  /** Callback when search is triggered */
  onSearch: () => void;
  /** Label for the search field */
  label: string;
  /** Placeholder text */
  placeholder?: string;
  /** Whether search is loading */
  loading?: boolean;
  /** Whether the field is disabled */
  disabled?: boolean;
  /** Helper text */
  helperText?: string;
  /** Tooltip text explaining the search requirement */
  tooltipText?: string;
  /** Full width */
  fullWidth?: boolean;
  /** Aria label for accessibility */
  "aria-label"?: string;
  /** Handle key press event (for Enter key) */
  onKeyPress?: (e: React.KeyboardEvent<HTMLInputElement>) => void;
  /** Whether input update is pending (from useTransition) */
  isPending?: boolean;
  /** Search result count for ARIA announcement */
  resultCount?: number;
}

/**
 * Manual search field component
 * Requires explicit button click or Enter key to trigger search
 * Useful for ID-based searches that require exact matches
 * Optimized with React.memo and useTransition for better performance
 */
export const ManualSearchField = memo(function ManualSearchField({
  value,
  onChange,
  onSearch,
  label,
  placeholder,
  loading = false,
  disabled = false,
  helperText,
  tooltipText,
  fullWidth = true,
  "aria-label": ariaLabel,
  onKeyPress,
  isPending = false,
  resultCount,
}: ManualSearchFieldProps) {
  const inputRef = useSearchFieldRef();
  const announcementRef = useRef<string>("");

  // Generate ARIA announcement message
  useEffect(() => {
    if (loading) {
      announcementRef.current = `Searching for ${value || "results"}...`;
    } else if (resultCount !== undefined && value) {
      announcementRef.current = `Found ${resultCount} result${resultCount !== 1 ? "s" : ""} for "${value}"`;
    } else if (value && !loading) {
      announcementRef.current = `Search completed for "${value}"`;
    } else {
      announcementRef.current = "";
    }
  }, [value, loading, resultCount]);

  const handleClear = useCallback(() => {
    onChange("");
    // Clear field only - don't trigger search automatically
  }, [onChange]);

  const handleKeyPressInternal = useCallback(
    (e: React.KeyboardEvent<HTMLInputElement>) => {
      if (e.key === "Enter") {
        e.preventDefault();
        onSearch();
      }
      // Also call external handler if provided
      if (onKeyPress) {
        onKeyPress(e);
      }
    },
    [onSearch, onKeyPress]
  );

  // Visual feedback for pending state: subtle opacity change
  const pendingOpacity = isPending ? 0.7 : 1;

  const searchField = (
    <TextField
      inputRef={inputRef}
      fullWidth={fullWidth}
      label={label}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      onKeyPress={handleKeyPressInternal}
      placeholder={placeholder}
      disabled={disabled || loading}
      helperText={helperText || tooltipText}
      slotProps={{
        input: {
          startAdornment: (
            <InputAdornment position="start">
              {loading ? <CircularProgress size={20} /> : <SearchIcon />}
            </InputAdornment>
          ),
          endAdornment: value ? (
            <InputAdornment position="end">
              <IconButton
                size="small"
                onClick={handleClear}
                disabled={disabled || loading}
                aria-label="Clear search"
                edge="end"
              >
                <ClearIcon fontSize="small" />
              </IconButton>
            </InputAdornment>
          ) : undefined,
          "aria-label": ariaLabel || label,
        },
      }}
      sx={{
        opacity: pendingOpacity,
        transition: "opacity 0.2s ease-in-out",
      }}
    />
  );

  return (
    <Box sx={{ display: "flex", gap: 1, alignItems: "flex-start", width: "100%", position: "relative" }}>
      <Box sx={{ flex: 1 }}>
        {tooltipText ? (
          <Tooltip title={tooltipText} arrow placement="top">
            <Box>{searchField}</Box>
          </Tooltip>
        ) : (
          searchField
        )}
      </Box>
      <Tooltip title="Click to search">
        <span>
          <IconButton
            onClick={onSearch}
            disabled={disabled || loading || !value}
            color="primary"
            aria-label="Search"
            sx={{ mt: 1, minWidth: 48 }}
          >
            {loading ? <CircularProgress size={24} /> : <RefreshIcon />}
          </IconButton>
        </span>
      </Tooltip>
      {/* ARIA live region for screen readers */}
      <SearchAnnouncer
        message={announcementRef.current}
        priority="polite"
        announce={!!announcementRef.current}
      />
    </Box>
  );
});

