import { Chip, Tooltip } from "@mui/material";
import type { ServiceCredentialResponseStatus } from "@lib/api/models";

interface CredentialsStatusBadgeProps {
  status: ServiceCredentialResponseStatus;
}

export function CredentialsStatusBadge({ status }: CredentialsStatusBadgeProps) {
  const getStatusConfig = (status: ServiceCredentialResponseStatus) => {
    switch (status) {
      case "PENDING":
        return {
          color: "warning" as const,
          label: "Pending",
          tooltip: "Credentials created but not yet activated. Config files must be ready before activation.",
        };
      case "ACTIVE":
        return {
          color: "success" as const,
          label: "Active",
          tooltip: "Credentials are active and can be used for M2M authentication.",
        };
      case "REVOKED":
        return {
          color: "error" as const,
          label: "Revoked",
          tooltip: "Credentials have been revoked. New tokens cannot be issued.",
        };
      case "EXPIRED":
        return {
          color: "default" as const,
          label: "Expired",
          tooltip: "Credentials have expired and are no longer valid.",
        };
      default:
        return {
          color: "default" as const,
          label: status,
          tooltip: `Credential status: ${status}`,
        };
    }
  };

  const config = getStatusConfig(status);

  return (
    <Tooltip title={config.tooltip} arrow>
      <Chip
        label={config.label}
        color={config.color}
        size="small"
        sx={{ fontWeight: 500 }}
      />
    </Tooltip>
  );
}

