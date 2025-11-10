/**
 * Breadcrumb navigation component for KV Store
 */

import { useMemo } from "react";
import {
  Breadcrumbs as MuiBreadcrumbs,
  Link,
  Typography,
  IconButton,
  Box,
} from "@mui/material";
import {
  NavigateNext,
  ArrowBack,
} from "@mui/icons-material";
import { normalizePath, getPathSegments } from "../types";

export interface KVBreadcrumbProps {
  /** Current prefix path */
  prefix: string;
  /** Callback when breadcrumb item is clicked */
  onNavigate: (prefix: string) => void;
  /** Callback for back button */
  onBack?: () => void;
  /** Show back button */
  showBackButton?: boolean;
}

export function KVBreadcrumb({
  prefix,
  onNavigate,
  onBack,
  showBackButton = true,
}: KVBreadcrumbProps) {
  const segments = useMemo(() => {
    const normalized = normalizePath(prefix);
    if (!normalized) return [];
    return getPathSegments(normalized);
  }, [prefix]);

  const breadcrumbItems = useMemo(() => {
    const items: Array<{ label: string; prefix: string }> = [
      { label: "Root", prefix: "" },
    ];

    let currentPath = "";
    segments.forEach((segment) => {
      currentPath = currentPath ? `${currentPath}/${segment}` : segment;
      items.push({ label: segment, prefix: currentPath });
    });

    return items;
  }, [segments]);

  const hasParent = prefix && normalizePath(prefix).length > 0;

  return (
    <Box
      sx={{
        display: "flex",
        alignItems: "center",
        gap: 1,
        py: 1,
      }}
    >
      {showBackButton && hasParent && (
        <IconButton
          size="small"
          onClick={onBack}
          aria-label="Go back"
          sx={{ mr: 1 }}
        >
          <ArrowBack fontSize="small" />
        </IconButton>
      )}
      <MuiBreadcrumbs
        separator={<NavigateNext fontSize="small" />}
        aria-label="breadcrumb navigation"
      >
        {breadcrumbItems.map((item, index) => {
          const isLast = index === breadcrumbItems.length - 1;
          if (isLast) {
            return (
              <Typography
                key={item.prefix}
                color="text.primary"
                variant="body2"
                sx={{ fontWeight: 500 }}
              >
                {item.label}
              </Typography>
            );
          }
          return (
            <Link
              key={item.prefix}
              component="button"
              variant="body2"
              onClick={() => onNavigate(item.prefix)}
              sx={{
                color: "primary.main",
                textDecoration: "none",
                "&:hover": {
                  textDecoration: "underline",
                },
                cursor: "pointer",
                border: "none",
                background: "none",
                padding: 0,
              }}
            >
              {item.label}
            </Link>
          );
        })}
      </MuiBreadcrumbs>
    </Box>
  );
}

