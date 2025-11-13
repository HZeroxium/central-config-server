import { useCallback, useRef, useEffect, memo } from "react";
import {
  TextField,
  InputAdornment,
  IconButton,
  FormControlLabel,
  Switch,
  Tooltip,
  Box,
  CircularProgress,
  useTheme,
} from "@mui/material";
import {
  Search as SearchIcon,
  Clear as ClearIcon,
  Refresh as RefreshIcon,
} from "@mui/icons-material";
import { SearchAnnouncer } from "./SearchAnnouncer";
import { useSearchFieldRef } from "@hooks/useGlobalSearchShortcut";

export interface SearchFieldWithToggleProps {
  /** Current search value */
  value: string;
  /** Callback when search value changes */
  onChange: (value: string) => void;
  /** Callback when search is triggered (manual search) */
  onSearch: () => void;
  /** Label for the search field */
  label: string;
  /** Placeholder text */
  placeholder?: string;
  /** Whether real-time search is enabled */
  realtimeEnabled: boolean;
  /** Callback when real-time toggle changes */
  onRealtimeToggle: (enabled: boolean) => void;
  /** Whether search is loading */
  loading?: boolean;
  /** Whether the field is disabled */
  disabled?: boolean;
  /** Helper text */
  helperText?: string;
  /** Full width */
  fullWidth?: boolean;
  /** Aria label for accessibility */
  "aria-label"?: string;
  /** Whether debouncing is in progress (for visual feedback) */
  isDebouncing?: boolean;
  /** Search result count for ARIA announcement */
  resultCount?: number;
  /** Whether to show debounce indicator */
  showDebounceIndicator?: boolean;
}

/**
 * Search field with real-time toggle functionality
 * Supports both real-time (debounced) and manual search modes
 * Optimized with React.memo and visual feedback for debouncing
 */
export const SearchFieldWithToggle = memo(function SearchFieldWithToggle({
  value,
  onChange,
  onSearch,
  label,
  placeholder,
  realtimeEnabled,
  onRealtimeToggle,
  loading = false,
  disabled = false,
  helperText,
  fullWidth = true,
  "aria-label": ariaLabel,
  isDebouncing = false,
  resultCount,
  showDebounceIndicator = true,
}: SearchFieldWithToggleProps) {
  const theme = useTheme();
  const inputRef = useSearchFieldRef();
  const announcementRef = useRef<string>("");

  // Generate ARIA announcement message
  useEffect(() => {
    if (loading) {
      announcementRef.current = `Searching for ${value || "results"}...`;
    } else if (resultCount !== undefined && value) {
      announcementRef.current = `Found ${resultCount} result${resultCount !== 1 ? "s" : ""} for "${value}"`;
    } else if (value && !isDebouncing) {
      announcementRef.current = `Search completed for "${value}"`;
    } else {
      announcementRef.current = "";
    }
  }, [value, loading, resultCount, isDebouncing]);

  // Memoize onChange handler to prevent re-renders when parent passes new function
  const handleChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      onChange(e.target.value);
    },
    [onChange]
  );

  const handleClear = useCallback(() => {
    onChange("");
    // In real-time mode, clearing automatically triggers search via debounce
    // In manual mode, user needs to click search button after clearing
  }, [onChange]);

  const handleKeyPress = useCallback(
    (e: React.KeyboardEvent<HTMLInputElement>) => {
      if (e.key === "Enter" && !realtimeEnabled) {
        e.preventDefault();
        onSearch();
      } else if (e.key === "Escape") {
        e.preventDefault();
        onChange("");
      }
    },
    [onSearch, onChange, realtimeEnabled]
  );

  // Visual feedback for debouncing: subtle border color change
  const debounceBorderColor = isDebouncing && showDebounceIndicator
    ? theme.palette.primary.light
    : undefined;

  return (
    <Box sx={{ display: "flex", flexDirection: "column", gap: 1, position: "relative" }}>
      <Box sx={{ display: "flex", gap: 1, alignItems: "flex-start", width: "100%" }}>
        <Box sx={{ flex: 1 }}>
          <TextField
            inputRef={inputRef}
            fullWidth={fullWidth}
            label={label}
            value={value}
            onChange={handleChange}
            onKeyPress={handleKeyPress}
            placeholder={placeholder}
            disabled={disabled || loading}
            helperText={helperText}
            slotProps={{
              input: {
                startAdornment: (
                  <InputAdornment position="start">
                    {loading ? (
                      <CircularProgress size={20} />
                    ) : (
                      <SearchIcon />
                    )}
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
              "& .MuiOutlinedInput-root": {
                transition: "border-color 0.3s ease-in-out",
                ...(debounceBorderColor && {
                  "& fieldset": {
                    borderColor: debounceBorderColor,
                    borderWidth: "2px",
                  },
                  "&:hover fieldset": {
                    borderColor: debounceBorderColor,
                  },
                  "&.Mui-focused fieldset": {
                    borderColor: theme.palette.primary.main,
                  },
                }),
              },
            }}
          />
        </Box>
        {!realtimeEnabled && (
          <Tooltip title="Click to search">
            <span>
              <IconButton
                onClick={onSearch}
                disabled={disabled || loading || !value}
                color="primary"
                aria-label="Search"
                sx={{ mt: 1, minWidth: 48 }}
              >
                {loading ? (
                  <CircularProgress size={24} />
                ) : (
                  <RefreshIcon />
                )}
              </IconButton>
            </span>
          </Tooltip>
        )}
      </Box>
      <FormControlLabel
        control={
          <Switch
            checked={realtimeEnabled}
            onChange={(e) => onRealtimeToggle(e.target.checked)}
            disabled={disabled}
            size="small"
          />
        }
        label={
          <Box component="span" sx={{ fontSize: "0.875rem" }}>
            Real-time search
          </Box>
        }
        sx={{ m: 0 }}
      />
      {/* ARIA live region for screen readers */}
      <SearchAnnouncer
        message={announcementRef.current}
        priority="polite"
        announce={!!announcementRef.current}
      />
    </Box>
  );
});

