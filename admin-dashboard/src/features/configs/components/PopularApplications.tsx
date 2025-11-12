import React from "react";
import { Box, Chip, Typography } from "@mui/material";
import { Apps as AppsIcon } from "@mui/icons-material";

interface PopularApplicationsProps {
  applications: Array<{ application: string; count: number }>;
  onSelect: (application: string) => void;
  selectedApplication?: string;
}

export const PopularApplications: React.FC<PopularApplicationsProps> = ({
  applications,
  onSelect,
  selectedApplication,
}) => {
  if (applications.length === 0) {
    return null;
  }

  return (
    <Box>
      <Typography
        variant="subtitle2"
        sx={{ mb: 1.5, display: "flex", alignItems: "center", gap: 1 }}
        color="text.secondary"
      >
        <AppsIcon sx={{ fontSize: 18 }} />
        Popular Applications
      </Typography>
      <Box sx={{ display: "flex", flexWrap: "wrap", gap: 1 }}>
        {applications.map(({ application, count }) => (
          <Chip
            key={application}
            label={`${application} (${count})`}
            size="small"
            variant={selectedApplication === application ? "filled" : "outlined"}
            color={selectedApplication === application ? "primary" : "default"}
            onClick={() => onSelect(application)}
            sx={{
              cursor: "pointer",
              transition: "all 0.2s",
              "&:hover": {
                bgcolor: "primary.light",
                color: "primary.contrastText",
              },
            }}
            aria-label={`Select ${application}, searched ${count} times`}
          />
        ))}
      </Box>
    </Box>
  );
};

