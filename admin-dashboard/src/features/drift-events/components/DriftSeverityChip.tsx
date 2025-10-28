import React from "react";
import { Chip, type ChipProps } from "@mui/material";

export type DriftSeverityType = "CRITICAL" | "HIGH" | "MEDIUM" | "LOW";

interface DriftSeverityChipProps extends ChipProps {
  severity: DriftSeverityType;
}

const getSeverityColor = (severity: DriftSeverityType): ChipProps["color"] => {
  switch (severity) {
    case "CRITICAL":
      return "error";
    case "HIGH":
      return "error";
    case "MEDIUM":
      return "warning";
    case "LOW":
      return "info";
    default:
      return "default";
  }
};

const getSeverityIcon = (severity: DriftSeverityType) => {
  switch (severity) {
    case "CRITICAL":
      return "🚨";
    case "HIGH":
      return "⚠️";
    case "MEDIUM":
      return "⚡";
    case "LOW":
      return "ℹ️";
    default:
      return "⚪";
  }
};

export const DriftSeverityChip: React.FC<DriftSeverityChipProps> = ({
  severity,
  ...props
}) => {
  const color = getSeverityColor(severity);
  const icon = getSeverityIcon(severity);

  return (
    <Chip label={`${icon} ${severity}`} color={color} size="small" {...props} />
  );
};
