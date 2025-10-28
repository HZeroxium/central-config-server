import React from "react";
import { Box, Tabs, Tab, Badge } from "@mui/material";

interface ApprovalTabsProps {
  activeTab: string;
  onTabChange: (tab: string) => void;
  pendingCount?: number;
  approvedCount?: number;
  rejectedCount?: number;
  cancelledCount?: number;
}

export const ApprovalTabs: React.FC<ApprovalTabsProps> = ({
  activeTab,
  onTabChange,
  pendingCount = 0,
  approvedCount = 0,
  rejectedCount = 0,
  cancelledCount = 0,
}) => {
  const handleChange = (_event: React.SyntheticEvent, newValue: string) => {
    onTabChange(newValue);
  };

  const tabs = [
    { value: "PENDING", label: "Pending", count: pendingCount },
    { value: "APPROVED", label: "Approved", count: approvedCount },
    { value: "REJECTED", label: "Rejected", count: rejectedCount },
    { value: "CANCELLED", label: "Cancelled", count: cancelledCount },
  ];

  return (
    <Box sx={{ borderBottom: 1, borderColor: "divider", mb: 3 }}>
      <Tabs
        value={activeTab}
        onChange={handleChange}
        aria-label="approval tabs"
      >
        {tabs.map((tab) => (
          <Tab
            key={tab.value}
            label={
              <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                <span>{tab.label}</span>
                {tab.count > 0 && (
                  <Badge
                    badgeContent={tab.count}
                    color={tab.value === "PENDING" ? "warning" : "primary"}
                    sx={{
                      "& .MuiBadge-badge": {
                        fontSize: "0.75rem",
                        height: "18px",
                        minWidth: "18px",
                      },
                    }}
                  />
                )}
              </Box>
            }
            value={tab.value}
          />
        ))}
      </Tabs>
    </Box>
  );
};
