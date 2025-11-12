import React from "react";
import { Box, Typography } from "@mui/material";
import {
  TrendingUp as TrendingUpIcon,
  TrendingDown as TrendingDownIcon,
  Remove as RemoveIcon,
} from "@mui/icons-material";

interface TrendIndicatorProps {
  value: number;
  isPositive?: boolean;
  showIcon?: boolean;
  size?: "small" | "medium";
  className?: string;
}

export const TrendIndicator: React.FC<TrendIndicatorProps> = ({
  value,
  isPositive = true,
  showIcon = true,
  size = "small",
  className,
}) => {
  if (value === 0) {
    return (
      <Box
        sx={{
          display: "flex",
          alignItems: "center",
          gap: 0.5,
        }}
        className={className}
        aria-label="No change"
      >
        {showIcon && <RemoveIcon sx={{ fontSize: size === "small" ? 14 : 16, color: "text.disabled" }} />}
        <Typography
          variant="caption"
          color="text.disabled"
          fontWeight={500}
          sx={{ fontSize: size === "small" ? "0.7rem" : "0.75rem" }}
        >
          {Math.abs(value)}%
        </Typography>
      </Box>
    );
  }

  const isPositiveTrend = value > 0 ? isPositive : !isPositive;
  const color = isPositiveTrend ? "success.main" : "error.main";
  const Icon = isPositiveTrend ? TrendingUpIcon : TrendingDownIcon;

  return (
    <Box
      sx={{
        display: "flex",
        alignItems: "center",
        gap: 0.5,
      }}
      className={className}
      aria-label={`${isPositiveTrend ? "Increase" : "Decrease"} of ${Math.abs(value)}%`}
    >
      {showIcon && (
        <Icon
          sx={{
            fontSize: size === "small" ? 14 : 16,
            color,
          }}
        />
      )}
      <Typography
        variant="caption"
        color={color}
        fontWeight={500}
        sx={{ fontSize: size === "small" ? "0.7rem" : "0.75rem" }}
      >
        {Math.abs(value)}%
      </Typography>
    </Box>
  );
};

