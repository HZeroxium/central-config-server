import {
  Breadcrumbs as MuiBreadcrumbs,
  Link,
  Typography,
  Skeleton,
} from "@mui/material";
import { useLocation, Link as RouterLink, useParams } from "react-router-dom";
import { NavigateNext } from "@mui/icons-material";
import { useMemo, useCallback } from "react";
import { useDynamicBreadcrumbLabel } from "@hooks/useDynamicBreadcrumbLabel";

interface BreadcrumbItem {
  label: string | React.ReactNode;
  href?: string;
  isLoading?: boolean;
}

interface BreadcrumbsProps {
  /**
   * Custom breadcrumb items to override automatic generation
   */
  items?: BreadcrumbItem[];
  /**
   * Custom labels for specific routes
   */
  customLabels?: Record<string, string>;
  /**
   * Whether to fetch dynamic labels for detail pages (e.g., service names)
   */
  enableDynamicLabels?: boolean;
}

/**
 * Route name to readable label mapping
 */
const ROUTE_LABELS: Record<string, string> = {
  dashboard: "Dashboard",
  "application-services": "Application Services",
  "service-instances": "Service Instances",
  approvals: "Approvals",
  "approval-decisions": "Approval Decisions",
  "drift-events": "Drift Events",
  "service-shares": "Service Shares",
  configs: "Configuration",
  registry: "Service Registry",
  iam: "IAM",
  users: "Users",
  teams: "Teams",
  profile: "Profile",
};

/**
 * Routes that support dynamic labels (detail pages)
 */
const DYNAMIC_LABEL_ROUTES: Record<
  string,
  (params: Record<string, string>) => Promise<string | null>
> = {
  "application-services/:id": async () => {
    // For now, return null to use ID fallback
    // In Phase 4.2, we'll fetch the actual service name
    return null;
  },
  "service-instances/:serviceName/:instanceId": async (params) => {
    // Use instanceId or serviceName as label
    return params.instanceId || params.serviceName || null;
  },
  "approvals/:id": async () => {
    // In Phase 4.2, we'll fetch the actual request info
    return null;
  },
  "drift-events/:id": async () => {
    // In Phase 4.2, we'll fetch the actual drift event info
    return null;
  },
  "service-shares/:id": async () => {
    // In Phase 4.2, we'll fetch the actual share info
    return null;
  },
  "configs/:application/:profile": async (params) => {
    // Use application and profile as label
    return `${params.application} / ${params.profile}`;
  },
  "registry/:serviceName": async (params) => {
    return params.serviceName || null;
  },
};

/**
 * Get readable label for a route segment
 */
function getRouteLabel(
  segment: string,
  customLabels?: Record<string, string>
): string {
  // Check custom labels first
  if (customLabels && customLabels[segment]) {
    return customLabels[segment];
  }

  // Check route labels mapping
  if (ROUTE_LABELS[segment]) {
    return ROUTE_LABELS[segment];
  }

  // Fallback: convert kebab-case to Title Case
  return segment
    .split("-")
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(" ");
}

export default function Breadcrumbs({
  items,
  customLabels,
  enableDynamicLabels = false,
}: BreadcrumbsProps = {}) {
  const location = useLocation();
  const params = useParams();

  // Get dynamic label for the last breadcrumb if it's a detail page
  const pathnames = location.pathname.split("/").filter((x) => x);
  const pathSegments = pathnames.map((_, index) =>
    pathnames.slice(0, index + 1).join("/")
  );
  const lastRoutePattern = pathSegments[pathSegments.length - 1];
  const isDetailPage =
    enableDynamicLabels && lastRoutePattern in DYNAMIC_LABEL_ROUTES;

  const { label: dynamicLabel, isLoading: isLabelLoading } =
    useDynamicBreadcrumbLabel(
      lastRoutePattern || "",
      (params || {}) as Record<string, string | undefined>
    );

  const generateBreadcrumbs = useCallback((): BreadcrumbItem[] => {
    // If custom items provided, use them
    if (items) {
      return items;
    }

    const breadcrumbs: BreadcrumbItem[] = [{ label: "Dashboard", href: "/" }];

    // Build breadcrumb path incrementally
    const segments: string[] = [];

    pathnames.forEach((name, index) => {
      segments.push(name);
      const routeTo = `/${segments.join("/")}`;
      const isLast = index === pathnames.length - 1;

      // Check if this is a dynamic route parameter
      const isParam =
        Object.keys(params).includes(name) ||
        Object.values(params).includes(name);

      let label: string | React.ReactNode = getRouteLabel(name, customLabels);

      // For dynamic labels on detail pages
      if (isLast && isDetailPage && isParam) {
        if (isLabelLoading) {
          label = <Skeleton variant="text" width={100} height={20} />;
        } else if (dynamicLabel) {
          label = dynamicLabel;
        } else {
          // Fallback to param value or ID
          label = params[name as keyof typeof params] || name;
        }
      } else if (isLast && !isDetailPage && isParam) {
        // Use the param value or ID as fallback
        label = params[name as keyof typeof params] || name;
      }

      if (isLast) {
        breadcrumbs.push({ label, isLoading: isDetailPage && isParam });
      } else {
        breadcrumbs.push({ label, href: routeTo });
      }
    });

    return breadcrumbs;
  }, [
    params,
    items,
    customLabels,
    isDetailPage,
    dynamicLabel,
    isLabelLoading,
    pathnames,
  ]);

  const breadcrumbs = useMemo(
    () => generateBreadcrumbs(),
    [generateBreadcrumbs]
  );

  return (
    <MuiBreadcrumbs
      separator={<NavigateNext fontSize="small" />}
      sx={{ mb: 2 }}
      aria-label="breadcrumb navigation"
    >
      {breadcrumbs.map((breadcrumb, index) => {
        const isLast = index === breadcrumbs.length - 1;
        const key =
          typeof breadcrumb.label === "string"
            ? breadcrumb.label
            : `breadcrumb-${index}`;

        if (isLast) {
          return (
            <Typography
              key={key}
              color="text.primary"
              fontWeight={600}
              component="span"
            >
              {breadcrumb.label}
            </Typography>
          );
        }

        return (
          <Link
            key={key}
            component={RouterLink}
            to={breadcrumb.href || "#"}
            sx={{
              color: "text.secondary",
              textDecoration: "none",
              transition: "color 0.2s",
              "&:hover": {
                color: "primary.main",
              },
            }}
            aria-label={`Navigate to ${
              typeof breadcrumb.label === "string" ? breadcrumb.label : "page"
            }`}
          >
            {breadcrumb.label}
          </Link>
        );
      })}
    </MuiBreadcrumbs>
  );
}
