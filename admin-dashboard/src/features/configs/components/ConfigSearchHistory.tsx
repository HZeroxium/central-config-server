import React from "react";
import {
  Box,
  Typography,
  IconButton,
  Stack,
  Divider,
  Tooltip,
} from "@mui/material";
import {
  History as HistoryIcon,
  Delete as DeleteIcon,
  ClearAll as ClearAllIcon,
} from "@mui/icons-material";
import { format } from "date-fns";
import type { ConfigSearchEntry } from "@hooks/useConfigSearchHistory";

interface ConfigSearchHistoryProps {
  history: ConfigSearchEntry[];
  onSelect: (entry: ConfigSearchEntry) => void;
  onRemove: (index: number) => void;
  onClear: () => void;
}

export const ConfigSearchHistory: React.FC<ConfigSearchHistoryProps> = ({
  history,
  onSelect,
  onRemove,
  onClear,
}) => {
  if (history.length === 0) {
    return (
      <Box sx={{ textAlign: "center", py: 4 }}>
        <HistoryIcon sx={{ fontSize: 48, color: "text.disabled", mb: 1 }} />
        <Typography variant="body2" color="text.secondary">
          No search history yet
        </Typography>
      </Box>
    );
  }

  return (
    <Box>
      <Box
        sx={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          mb: 2,
        }}
      >
        <Typography
          variant="h6"
          sx={{ display: "flex", alignItems: "center", gap: 1 }}
        >
          <HistoryIcon />
          Recent Searches
        </Typography>
        {history.length > 0 && (
          <Tooltip title="Clear all history">
            <IconButton
              size="small"
              onClick={onClear}
              aria-label="Clear all search history"
            >
              <ClearAllIcon />
            </IconButton>
          </Tooltip>
        )}
      </Box>

      <Stack spacing={1} divider={<Divider />}>
        {history.map((entry, index) => (
          <Box
            key={`${entry.application}-${entry.profile}-${entry.label}-${entry.timestamp}`}
            sx={{
              p: 1.5,
              border: 1,
              borderColor: "divider",
              borderRadius: 1,
              cursor: "pointer",
              transition: "all 0.2s",
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              "&:hover": {
                bgcolor: "action.hover",
                borderColor: "primary.main",
              },
            }}
            onClick={() => onSelect(entry)}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => {
              if (e.key === "Enter" || e.key === " ") {
                e.preventDefault();
                onSelect(entry);
              }
            }}
            aria-label={`Search config for ${entry.application} ${entry.profile}${entry.label ? ` ${entry.label}` : ""}`}
          >
            <Box sx={{ flex: 1 }}>
              <Typography variant="body2" sx={{ fontWeight: 500, mb: 0.5 }}>
                {entry.application}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Profile: {entry.profile}
                {entry.label && ` • Label: ${entry.label}`}
              </Typography>
              <Typography variant="caption" color="text.disabled" sx={{ display: "block", mt: 0.5 }}>
                {format(new Date(entry.timestamp), "MMM dd, yyyy HH:mm")}
              </Typography>
            </Box>
            <IconButton
              size="small"
              onClick={(e) => {
                e.stopPropagation();
                onRemove(index);
              }}
              aria-label={`Remove ${entry.application} ${entry.profile} from history`}
            >
              <DeleteIcon fontSize="small" />
            </IconButton>
          </Box>
        ))}
      </Stack>
    </Box>
  );
};

