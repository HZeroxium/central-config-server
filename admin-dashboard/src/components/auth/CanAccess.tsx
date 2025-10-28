import type { ReactNode } from "react";
import { usePermissions } from "@features/auth/hooks/usePermissions";
import { useAuth } from "@features/auth/context";

export type PermissionType =
  | "view-service"
  | "edit-service"
  | "delete-service"
  | "share-service"
  | "approve-requests"
  | "manage-all-services"
  | "manage-application-services"
  | "view-drift-events"
  | "access-route"
  | "view-team-services"
  | "edit-team-services"
  | "sys-admin";

interface CanAccessProps {
  /** Type of permission to check */
  permission?: PermissionType;

  /** Service ID for service-specific permissions */
  serviceId?: string;

  /** Team ID for team-specific permissions */
  teamId?: string;

  /** Route path for route-based permissions */
  route?: string;

  /** Required role (alternative to permission) */
  role?: string;

  /** Custom permission check function */
  check?: () => boolean;

  /** Content to render if user has access */
  children: ReactNode;

  /** Content to render if user doesn't have access (default: null) */
  fallback?: ReactNode;

  /** If true, render children even without access but disable interactions */
  showDisabled?: boolean;
}

/**
 * Declarative permission wrapper component
 *
 * Usage examples:
 *
 * // Check service-specific permission
 * <CanAccess permission="edit-service" serviceId="payment-service">
 *   <Button>Edit Service</Button>
 * </CanAccess>
 *
 * // Check role
 * <CanAccess role="SYS_ADMIN">
 *   <Button>Delete Service</Button>
 * </CanAccess>
 *
 * // Check route access
 * <CanAccess permission="access-route" route="/admin">
 *   <MenuItem>Admin Panel</MenuItem>
 * </CanAccess>
 *
 * // Custom check
 * <CanAccess check={() => user.credits > 0}>
 *   <Button>Purchase</Button>
 * </CanAccess>
 *
 * // With fallback
 * <CanAccess permission="edit-service" serviceId="payment-service" fallback={<Tooltip title="No permission"><span><Button disabled>Edit</Button></span></Tooltip>}>
 *   <Button>Edit Service</Button>
 * </CanAccess>
 */
export default function CanAccess({
  permission,
  serviceId,
  teamId,
  route,
  role,
  check,
  children,
  fallback = null,
  showDisabled = false,
}: Readonly<CanAccessProps>) {
  const permissions = usePermissions();
  const { hasRole } = useAuth();

  let hasAccess = false;

  // Check custom function first
  if (check) {
    hasAccess = check();
  }
  // Check role
  else if (role) {
    hasAccess = hasRole(role);
  }
  // Check specific permissions
  else if (permission) {
    switch (permission) {
      case "view-service":
        hasAccess = serviceId ? !!permissions.canViewService(serviceId) : false;
        break;
      case "edit-service":
        hasAccess = serviceId ? !!permissions.canEditService(serviceId) : false;
        break;
      case "delete-service":
        hasAccess = serviceId
          ? !!permissions.canDeleteService(serviceId)
          : false;
        break;
      case "share-service":
        hasAccess = serviceId
          ? !!permissions.canShareService(serviceId)
          : false;
        break;
      case "approve-requests":
        hasAccess = permissions.canApproveRequests;
        break;
      case "manage-all-services":
        hasAccess = permissions.canManageAllServices;
        break;
      case "manage-application-services":
        hasAccess = permissions.canManageApplicationServices;
        break;
      case "view-drift-events":
        hasAccess = permissions.canViewDriftEvents;
        break;
      case "access-route":
        hasAccess = route ? permissions.canAccessRoute(route) : false;
        break;
      case "view-team-services":
        hasAccess = teamId ? permissions.canViewTeamServices(teamId) : false;
        break;
      case "edit-team-services":
        hasAccess = teamId ? permissions.canEditTeamServices(teamId) : false;
        break;
      case "sys-admin":
        hasAccess = permissions.isSysAdmin;
        break;
      default:
        hasAccess = false;
    }
  } else {
    // No permission check specified, default to allow
    hasAccess = true;
  }

  if (hasAccess) {
    return <>{children}</>;
  }

  if (showDisabled) {
    // Clone children and add disabled prop if possible
    return <>{children}</>;
  }

  return <>{fallback}</>;
}
