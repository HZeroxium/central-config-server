import { useState, useEffect, useRef } from "react";
import { Box, Alert } from "@mui/material";
import Grid from "@mui/material/Grid";
import { ConfigEditor } from "./ConfigEditor";
import { ConfigFileTabs } from "./ConfigFileTabs";
import { GitHistoryViewer } from "./GitHistoryViewer";
import { useConfigFileOperations } from "../hooks/useConfigFile";
import { useCommitHistory } from "../hooks/useCommitHistory";
import type { Profile } from "../types";

interface ConfigFilesTabProps {
  serviceId: string;
}

const DEFAULT_PROFILES: Profile[] = ["dev", "prod", "staging", "test"];

export function ConfigFilesTab({ serviceId }: ConfigFilesTabProps) {
  const [selectedProfile, setSelectedProfile] = useState<Profile>("dev");
  const [editorContent, setEditorContent] = useState<string>("");
  const [saveError, setSaveError] = useState<string | null>(null);

  const {
    configFile,
    isLoading: configLoading,
    error: configError,
    refetch: refetchConfig,
    update,
    isUpdating,
  } = useConfigFileOperations(serviceId, selectedProfile);

  const {
    commits,
    isLoading: historyLoading,
    error: historyError,
  } = useCommitHistory(serviceId, selectedProfile);

  // Track the last content we loaded from server to detect external changes
  const lastLoadedContentRef = useRef<string | undefined>(undefined);

  // Update editor content when file is loaded from server (external change)
  useEffect(() => {
    const serverContent = configFile?.content;
    
    // Only update if this is a new file load (different from what we last loaded)
    // This prevents resetting user's edits when the same file is refetched
    if (serverContent !== undefined && serverContent !== lastLoadedContentRef.current) {
      setEditorContent(serverContent);
      lastLoadedContentRef.current = serverContent;
      setSaveError(null);
    } else if (serverContent === undefined && lastLoadedContentRef.current !== undefined) {
      // File was deleted or doesn't exist, allow user to create new file
      setEditorContent("");
      lastLoadedContentRef.current = undefined;
      setSaveError(null);
    }
  }, [configFile?.content]);

  // Reset editor content when profile changes (new file to load)
  useEffect(() => {
    // Reset the ref so the next file load will update editorContent
    lastLoadedContentRef.current = undefined;
    // The useEffect above will handle loading the new file content
  }, [selectedProfile]);

  const handleSave = async () => {
    setSaveError(null);
    try {
      await update(
        editorContent,
        `Update ${selectedProfile} config for ${serviceId}`,
        configFile?.sha
      );
      // After successful save, refetch to get updated content and SHA
      // This will trigger the useEffect above to update editorContent and originalValue
      await refetchConfig();
    } catch (error) {
      setSaveError(
        error instanceof Error
          ? error.message
          : "Failed to save config file"
      );
      // Don't update lastLoadedContentRef on error - keep current state
    }
  };

  const handleReset = () => {
    setEditorContent(configFile?.content || "");
    setSaveError(null);
  };

  if (configError && configError.status !== 404) {
    return (
      <Alert severity="error">
        Failed to load config file:{" "}
        {configError.detail || "Unknown error occurred"}
      </Alert>
    );
  }

  return (
    <Box>
      <ConfigFileTabs
        profiles={DEFAULT_PROFILES}
        currentProfile={selectedProfile}
        onProfileChange={(profile) => {
          setSelectedProfile(profile);
          setEditorContent("");
          setSaveError(null);
        }}
      />

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, lg: 8 }}>
          <ConfigEditor
            value={editorContent}
            onChange={setEditorContent}
            onSave={handleSave}
            onReset={handleReset}
            isLoading={configLoading}
            isSaving={isUpdating}
            error={saveError}
            readOnly={false}
            height={500}
            expectedSha={configFile?.sha}
          />
        </Grid>

        <Grid size={{ xs: 12, lg: 4 }}>
          <GitHistoryViewer
            commits={commits}
            isLoading={historyLoading}
            error={
              historyError
                ? new Error(historyError.detail || "Failed to load git history")
                : null
            }
            currentContent={editorContent}
          />
        </Grid>
      </Grid>
    </Box>
  );
}

