import { Tabs, Tab, Box } from "@mui/material";
import type { Profile } from "../types";
import type { ReactNode } from "react";

interface ConfigFileTabsProps {
  profiles: Profile[];
  currentProfile: Profile;
  onProfileChange: (profile: Profile) => void;
  renderTabLabel?: (profile: Profile) => ReactNode;
}

export function ConfigFileTabs({
  profiles,
  currentProfile,
  onProfileChange,
  renderTabLabel,
}: ConfigFileTabsProps) {
  const handleChange = (_event: React.SyntheticEvent, newValue: number) => {
    onProfileChange(profiles[newValue]);
  };

  const currentIndex = profiles.indexOf(currentProfile);

  return (
    <Box sx={{ borderBottom: 1, borderColor: "divider", mb: 2 }}>
      <Tabs
        value={currentIndex >= 0 ? currentIndex : 0}
        onChange={handleChange}
        aria-label="config file profile tabs"
      >
        {profiles.map((profile) => (
          <Tab
            key={profile}
            label={renderTabLabel ? renderTabLabel(profile) : profile.toUpperCase()}
            value={profiles.indexOf(profile)}
          />
        ))}
      </Tabs>
    </Box>
  );
}

