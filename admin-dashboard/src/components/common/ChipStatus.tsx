import React from "react";
import { Chip, type ChipProps, Box } from "@mui/material";

export type StatusType =
  | "UP"
  | "DOWN"
  | "UNKNOWN"
  | "PENDING"
  | "APPROVED"
  | "REJECTED"
  | "CANCELLED"
  | "CRITICAL"
  | "HIGH"
  | "MEDIUM"
  | "LOW"
  | "ACTIVE"
  | "INACTIVE"
  | "HEALTHY"
  | "UNHEALTHY";

interface ChipStatusProps extends Omit<ChipProps, "color" | "label"> {
  status: StatusType;
  label?: string;
}

const getStatusColor = (status: StatusType): ChipProps["color"] => {
  const statusStr = status.toLowerCase();

  if (
    statusStr === "up" ||
    statusStr === "approved" ||
    statusStr === "healthy" ||
    statusStr === "active"
  ) {
    return "success";
  }

  if (
    statusStr === "down" ||
    statusStr === "rejected" ||
    statusStr === "critical" ||
    statusStr === "unhealthy"
  ) {
    return "error";
  }

  if (
    statusStr === "pending" ||
    statusStr === "medium" ||
    statusStr === "high"
  ) {
    return "warning";
  }

  if (
    statusStr === "unknown" ||
    statusStr === "cancelled" ||
    statusStr === "inactive"
  ) {
    return "default";
  }

  if (statusStr === "low") {
    return "info";
  }

  return "default";
};

export const ChipStatus: React.FC<ChipStatusProps> = ({
  status,
  label,
  sx,
  ...chipProps
}) => {
  const color = getStatusColor(status);
  const displayLabel = label || status;

  return (
    <Box
      sx={{
        display: "flex",
        alignItems: "center",
        justifyContent: "flex-start",
      }}
    >
      <Chip
        label={displayLabel}
        color={color}
        size="small"
        variant="outlined"
        sx={{
          fontWeight: 500,
          ...sx,
        }}
        {...chipProps}
      />
    </Box>
  );
};

export default ChipStatus;
