import React from "react";
import {
  Box,
  Typography,
  Breadcrumbs,
  Link,
  Stack,
  type SxProps,
  type Theme,
} from "@mui/material";
import { NavigateNext as NavigateNextIcon } from "@mui/icons-material";

interface BreadcrumbItem {
  label: string;
  href?: string;
}

interface PageHeaderProps {
  title: string;
  subtitle?: string;
  breadcrumbs?: BreadcrumbItem[];
  actions?: React.ReactNode;
  sx?: SxProps<Theme>;
}

export const PageHeader: React.FC<PageHeaderProps> = ({
  title,
  subtitle,
  breadcrumbs,
  actions,
  sx = {},
}) => {
  return (
    <Box sx={{ mb: 3, ...sx }}>
      {breadcrumbs && breadcrumbs.length > 0 && (
        <Breadcrumbs
          separator={<NavigateNextIcon fontSize="small" />}
          sx={{ mb: 2 }}
        >
          {breadcrumbs.map((item, index) => (
            <React.Fragment key={index}>
              {item.href ? (
                <Link href={item.href} color="text.secondary" underline="hover">
                  {item.label}
                </Link>
              ) : (
                <Typography color="text.secondary">{item.label}</Typography>
              )}
            </React.Fragment>
          ))}
          <Typography color="text.primary" fontWeight={600}>
            {title}
          </Typography>
        </Breadcrumbs>
      )}

      <Stack
        direction="row"
        justifyContent="space-between"
        alignItems="flex-start"
        sx={{ mb: subtitle ? 1 : 0 }}
      >
        <Box>
          <Typography variant="h4" fontWeight={700} gutterBottom>
            {title}
          </Typography>
          {subtitle && (
            <Typography variant="body1" color="text.secondary">
              {subtitle}
            </Typography>
          )}
        </Box>
        {actions && (
          <Stack direction="row" spacing={2} sx={{ ml: 2 }}>
            {actions}
          </Stack>
        )}
      </Stack>
    </Box>
  );
};

export default PageHeader;
