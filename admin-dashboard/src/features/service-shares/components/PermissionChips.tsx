import React from "react";
import { Chip, Box } from "@mui/material";

export type PermissionType = "VIEW" | "EDIT" | "DELETE";

interface PermissionChipsProps {
  permissions: PermissionType[];
}

const getPermissionColor = (
  permission: PermissionType
):
  | "default"
  | "primary"
  | "secondary"
  | "error"
  | "info"
  | "success"
  | "warning" => {
  switch (permission) {
    case "VIEW":
      return "info";
    case "EDIT":
      return "warning";
    case "DELETE":
      return "error";
    default:
      return "default";
  }
};

const getPermissionIcon = (permission: PermissionType): string => {
  switch (permission) {
    case "VIEW":
      return "👁️";
    case "EDIT":
      return "✏️";
    case "DELETE":
      return "🗑️";
    default:
      return "❓";
  }
};

export const PermissionChips: React.FC<PermissionChipsProps> = ({
  permissions,
}) => {
  return (
    <Box sx={{ display: "flex", gap: 0.5, flexWrap: "wrap" }}>
      {permissions.map((permission) => (
        <Chip
          key={permission}
          label={`${getPermissionIcon(permission)} ${permission}`}
          color={getPermissionColor(permission)}
          size="small"
        />
      ))}
    </Box>
  );
};
