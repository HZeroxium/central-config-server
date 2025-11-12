import React from "react";
import { Box, Chip, Typography } from "@mui/material";
import { FilterList as FilterListIcon } from "@mui/icons-material";

interface QuickFilterChipsProps {
  profiles: string[];
  selectedProfile?: string;
  onSelect: (profile: string) => void;
}

export const QuickFilterChips: React.FC<QuickFilterChipsProps> = ({
  profiles,
  selectedProfile,
  onSelect,
}) => {
  if (profiles.length === 0) {
    return null;
  }

  return (
    <Box>
      <Typography
        variant="subtitle2"
        sx={{ mb: 1.5, display: "flex", alignItems: "center", gap: 1 }}
        color="text.secondary"
      >
        <FilterListIcon sx={{ fontSize: 18 }} />
        Quick Filters
      </Typography>
      <Box sx={{ display: "flex", flexWrap: "wrap", gap: 1 }}>
        {profiles.map((profile) => {
          const isSelected = selectedProfile === profile;
          const getColor = (): "primary" | "error" | "warning" | "info" => {
            if (isSelected) return "primary";
            if (profile === "prod") return "error";
            if (profile === "staging") return "warning";
            return "info";
          };

          return (
            <Chip
              key={profile}
              label={profile.toUpperCase()}
              size="small"
              variant={isSelected ? "filled" : "outlined"}
              color={getColor()}
              onClick={() => onSelect(profile)}
              sx={{
                cursor: "pointer",
                transition: "all 0.2s",
                fontWeight: isSelected ? 600 : 400,
                "&:hover": {
                  bgcolor: `${getColor()}.light`,
                  color: `${getColor()}.contrastText`,
                },
              }}
              aria-label={`Filter by ${profile} profile`}
              aria-pressed={isSelected}
            />
          );
        })}
      </Box>
    </Box>
  );
};

