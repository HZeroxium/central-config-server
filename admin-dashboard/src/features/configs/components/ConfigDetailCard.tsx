import {
  Card,
  CardContent,
  Typography,
  Box,
  Button,
  Chip,
  IconButton,
  Tooltip,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  ToggleButton,
  ToggleButtonGroup,
  Link,
} from "@mui/material";
import {
  ContentCopy as Copy,
  Check,
  ExpandMore,
  ViewList,
  Code,
  GitHub,
  Edit,
  Save,
  Cancel,
} from "@mui/icons-material";
import { useState, useMemo } from "react";
import { Highlight, themes } from "prism-react-renderer";
import Editor from "@monaco-editor/react";
import * as yaml from "yaml";
import type {
  ConfigEnvironmentResponse,
  PropertySourceMap,
} from "@lib/api/types";
import ConfigMergeInfo from "./ConfigMergeInfo";
import ConfigStats from "./ConfigStats";

type ViewMode = "accordion" | "yaml";

export default function ConfigDetailCard({
  env,
}: Readonly<{
  env: ConfigEnvironmentResponse;
}>) {
  const [copiedProperty, setCopiedProperty] = useState<string | null>(null);
  const [viewMode, setViewMode] = useState<ViewMode>("accordion");
  const [isEditingFinal, setIsEditingFinal] = useState(false);
  const [editedFinalYaml, setEditedFinalYaml] = useState("");

  // Logic merge properties to create Final Source
  const finalProperties = useMemo(() => {
    if (!env.propertySources) return {};

    const merged: PropertySourceMap = {};

    // Merge from bottom to top (propertySources[0] has highest priority)
    // Spring Cloud Config merge order: application.yml -> application-{profile}.yml -> {service}/application.yml -> {service}/application-{profile}.yml
    for (let i = env.propertySources.length - 1; i >= 0; i--) {
      const source = env.propertySources[i];
      if (source.source) {
        Object.assign(merged, source.source);
      }
    }

    return merged;
  }, [env.propertySources]);

  const handleEditFinal = () => {
    setEditedFinalYaml(yaml.stringify(finalProperties, { indent: 2 }));
    setIsEditingFinal(true);
  };

  const handleSaveFinal = () => {
    // TODO: Implement save logic to backend
    console.log("Saving edited YAML:", editedFinalYaml);
    setIsEditingFinal(false);
  };

  const handleCancelEdit = () => {
    setIsEditingFinal(false);
    setEditedFinalYaml("");
  };

  const copyToClipboard = async (text: string, propertyKey?: string) => {
    try {
      await navigator.clipboard.writeText(text);
      if (propertyKey) {
        setCopiedProperty(propertyKey);
        setTimeout(() => setCopiedProperty(null), 2000);
      }
    } catch (err) {
      console.error("Failed to copy: ", err);
    }
  };

  const extractGitUrl = (sourceName: string): string | null => {
    // Try matching the pattern: https://github.com/OWNER/REPO.git + optional path
    // We use a regex with capture groups to split the base repo and the path.
    const regex = /^https:\/\/github\.com\/([^/]+)\/([^/]+)\.git(?:\/(.+))?$/;
    const match = new RegExp(regex).exec(sourceName);
    if (!match) {
      return null;
    }

    const owner = match[1]; // e.g. "hzeroxium"
    const repo = match[2]; // e.g. "ztf-spring-cloud-config-server"
    const restPath = match[3]; // e.g. "sample-service/application.yml", or undefined if no path

    // Build the blob URL with branch "master"
    if (restPath && restPath.length > 0) {
      // If there is a subpath after “.git/…”
      return `https://github.com/${owner}/${repo}/blob/master/${restPath}`;
    } else {
      // If no path part (just repo), link to root of repo
      return `https://github.com/${owner}/${repo}/tree/master`;
    }
  };

  const renderPropertyValue = (value: unknown, key: string) => {
    const jsonString = JSON.stringify(value, null, 2);
    const isCopied = copiedProperty === key;

    return (
      <Box
        sx={{ position: "relative", "&:hover .copy-button": { opacity: 1 } }}
      >
        <Highlight theme={themes.vsDark} code={jsonString} language="json">
          {({ className, style, tokens, getLineProps, getTokenProps }) => (
            <pre
              className={`${className} p-4 rounded-lg overflow-x-auto text-sm`}
              style={style}
            >
              {tokens.map((line, i) => (
                <div key={`${key}-line-${i}`} {...getLineProps({ line })}>
                  {line.map((token, tokenIdx) => (
                    <span
                      key={`${key}-token-${tokenIdx}`}
                      {...getTokenProps({ token })}
                    />
                  ))}
                </div>
              ))}
            </pre>
          )}
        </Highlight>

        <Tooltip title={isCopied ? "Copied!" : "Copy to clipboard"}>
          <IconButton
            size="small"
            className="copy-button"
            sx={{
              position: "absolute",
              top: 8,
              right: 8,
              opacity: 0,
              transition: "opacity 0.2s",
              bgcolor: "rgba(0,0,0,0.7)",
              "&:hover": {
                bgcolor: "rgba(0,0,0,0.85)",
              },
            }}
            onClick={() => copyToClipboard(jsonString, key)}
          >
            {isCopied ? (
              <Check sx={{ color: "success.main" }} />
            ) : (
              <Copy sx={{ color: "grey.300" }} />
            )}
          </IconButton>
        </Tooltip>
      </Box>
    );
  };

  return (
    <Box sx={{ display: "flex", flexDirection: "column", gap: 3 }}>
      <Card sx={{ border: 1, borderColor: "divider", boxShadow: 1 }}>
        <CardContent sx={{ p: 3 }}>
          <Box
            sx={{
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
              mb: 2,
            }}
          >
            <Typography
              variant="h6"
              sx={{ fontWeight: 600, color: "text.primary" }}
            >
              Configuration: {env.name}
            </Typography>
            <Button
              size="small"
              startIcon={
                copiedProperty === "full-config" ? <Check /> : <Copy />
              }
              onClick={() =>
                copyToClipboard(JSON.stringify(env, null, 2), "full-config")
              }
              sx={{
                color: "text.secondary",
                "&:hover": {
                  color: "text.primary",
                },
              }}
            >
              {copiedProperty === "full-config" ? "Copied!" : "Copy All"}
            </Button>
          </Box>

          <Box
            sx={{
              display: "grid",
              gridTemplateColumns: { xs: "1fr", sm: "1fr 1fr" },
              gap: 2,
            }}
          >
            <Box>
              <Typography
                variant="body2"
                sx={{ color: "text.secondary", mb: 1 }}
              >
                Profiles
              </Typography>
              <Box sx={{ display: "flex", flexWrap: "wrap", gap: 1 }}>
                {env.profiles?.map((profile) => (
                  <Chip
                    key={profile}
                    label={profile}
                    size="small"
                    color="primary"
                  />
                )) || <Chip label="default" size="small" />}
              </Box>
            </Box>

            <Box>
              <Typography
                variant="body2"
                sx={{ color: "text.secondary", mb: 1 }}
              >
                Label
              </Typography>
              <Typography
                variant="body1"
                sx={{ fontWeight: 500, color: "text.primary" }}
              >
                {env.label || "default"}
              </Typography>
            </Box>

            <Box>
              <Typography
                variant="body2"
                sx={{ color: "text.secondary", mb: 1 }}
              >
                Version
              </Typography>
              <Typography
                variant="body1"
                sx={{ fontWeight: 500, color: "text.primary" }}
              >
                {env.version || "-"}
              </Typography>
            </Box>

            <Box>
              <Typography
                variant="body2"
                sx={{ color: "text.secondary", mb: 1 }}
              >
                State
              </Typography>
              <Chip
                label={env.state || "unknown"}
                size="small"
                color={env.state ? "success" : "default"}
              />
            </Box>
          </Box>
        </CardContent>
      </Card>

      {/* Final Source Section */}
      <Card sx={{ border: 1, borderColor: "divider", boxShadow: 1 }}>
        <CardContent sx={{ p: 3 }}>
          <Box
            sx={{
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
              mb: 3,
            }}
          >
            <Box>
              <Typography
                variant="h6"
                sx={{ fontWeight: 600, color: "text.primary", mb: 0.5 }}
              >
                Final Configuration
              </Typography>
              <Typography variant="body2" sx={{ color: "text.secondary" }}>
                Merged properties with override precedence
              </Typography>
            </Box>
            <Box sx={{ display: "flex", gap: 1 }}>
              {!isEditingFinal ? (
                <Button
                  variant="outlined"
                  size="small"
                  startIcon={<Edit />}
                  onClick={handleEditFinal}
                >
                  Edit
                </Button>
              ) : (
                <>
                  <Button
                    variant="contained"
                    size="small"
                    startIcon={<Save />}
                    onClick={handleSaveFinal}
                    color="success"
                  >
                    Save
                  </Button>
                  <Button
                    variant="outlined"
                    size="small"
                    startIcon={<Cancel />}
                    onClick={handleCancelEdit}
                  >
                    Cancel
                  </Button>
                </>
              )}
              <Button
                size="small"
                startIcon={
                  copiedProperty === "final-config" ? <Check /> : <Copy />
                }
                onClick={() =>
                  copyToClipboard(
                    isEditingFinal
                      ? editedFinalYaml
                      : yaml.stringify(finalProperties, { indent: 2 }),
                    "final-config"
                  )
                }
                sx={{
                  color: "text.secondary",
                  "&:hover": {
                    color: "text.primary",
                  },
                }}
              >
                {copiedProperty === "final-config" ? "Copied!" : "Copy"}
              </Button>
            </Box>
          </Box>

          {isEditingFinal ? (
            <Box
              sx={{
                border: 1,
                borderColor: "divider",
                borderRadius: 1,
                overflow: "hidden",
              }}
            >
              <Editor
                height="400px"
                defaultLanguage="yaml"
                value={editedFinalYaml}
                onChange={(value) => setEditedFinalYaml(value || "")}
                theme="vs-dark"
                options={{
                  minimap: { enabled: false },
                  fontSize: 14,
                  lineNumbers: "on",
                  roundedSelection: false,
                  scrollBeyondLastLine: false,
                  automaticLayout: true,
                }}
              />
            </Box>
          ) : (
            <Box
              sx={{
                border: 1,
                borderColor: "divider",
                borderRadius: 1,
                overflow: "hidden",
              }}
            >
              <Highlight
                theme={themes.vsDark}
                code={yaml.stringify(finalProperties, { indent: 2 })}
                language="yaml"
              >
                {({
                  className,
                  style,
                  tokens,
                  getLineProps,
                  getTokenProps,
                }) => (
                  <pre
                    className={`${className} p-4 overflow-x-auto text-sm`}
                    style={style}
                  >
                    {tokens.map((line, i) => (
                      <div key={`final-line-${i}`} {...getLineProps({ line })}>
                        {line.map((token, tokenIdx) => (
                          <span
                            key={`final-token-${i}-${tokenIdx}`}
                            {...getTokenProps({ token })}
                          />
                        ))}
                      </div>
                    ))}
                  </pre>
                )}
              </Highlight>
            </Box>
          )}
        </CardContent>
      </Card>

      {/* Config Stats Section */}
      <ConfigStats env={env} />

      {/* Merge Info Section */}
      <ConfigMergeInfo env={env} />

      {/* Property Sources Section */}
      <Card sx={{ border: 1, borderColor: "divider", boxShadow: 1 }}>
        <CardContent sx={{ p: 3 }}>
          <Box
            sx={{
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
              mb: 3,
            }}
          >
            <Typography
              variant="h6"
              sx={{ fontWeight: 600, color: "text.primary" }}
            >
              Property Sources
            </Typography>
            <ToggleButtonGroup
              value={viewMode}
              exclusive
              onChange={(_, newMode) => newMode && setViewMode(newMode)}
              size="small"
            >
              <ToggleButton value="accordion">
                <ViewList sx={{ mr: 1 }} />
                Accordion
              </ToggleButton>
              <ToggleButton value="yaml">
                <Code sx={{ mr: 1 }} />
                YAML
              </ToggleButton>
            </ToggleButtonGroup>
          </Box>

          {viewMode === "accordion" ? (
            <Box sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
              {env.propertySources?.map((ps, index) => {
                const gitUrl = ps.name ? extractGitUrl(ps.name) : null;
                return (
                  <Accordion
                    key={ps.name}
                    sx={{
                      border: 1,
                      borderColor: "divider",
                      boxShadow: 1,
                      borderRadius: 2,
                      "&.Mui-expanded": { margin: 0 },
                    }}
                  >
                    <AccordionSummary
                      expandIcon={<ExpandMore />}
                      sx={{
                        "& .MuiAccordionSummary-content": { marginY: 1.5 },
                      }}
                    >
                      <Box
                        sx={{
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "space-between",
                          width: "100%",
                          mr: 2,
                        }}
                      >
                        <Box
                          sx={{
                            display: "flex",
                            alignItems: "center",
                            gap: 1,
                            flex: 1,
                            minWidth: 0,
                          }}
                        >
                          <Typography
                            variant="subtitle1"
                            sx={{
                              fontWeight: 500,
                              color: "text.primary",
                              flex: 1,
                              overflow: "hidden",
                              textOverflow: "ellipsis",
                            }}
                          >
                            {ps.name?.split("/").pop() || "Unknown"}
                          </Typography>
                          {gitUrl && (
                            <Tooltip title="Open in GitHub">
                              <IconButton
                                size="small"
                                component={Link}
                                href={gitUrl}
                                target="_blank"
                                rel="noopener noreferrer"
                                sx={{ color: "text.secondary" }}
                              >
                                <GitHub fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          )}
                        </Box>
                        <Button
                          size="small"
                          startIcon={
                            copiedProperty === `source-${index}` ? (
                              <Check />
                            ) : (
                              <Copy />
                            )
                          }
                          onClick={(e) => {
                            e.stopPropagation();
                            copyToClipboard(
                              JSON.stringify(ps.source, null, 2),
                              `source-${index}`
                            );
                          }}
                          sx={{
                            color: "text.secondary",
                            "&:hover": {
                              color: "text.primary",
                            },
                          }}
                        >
                          {copiedProperty === `source-${index}`
                            ? "Copied!"
                            : "Copy All"}
                        </Button>
                      </Box>
                    </AccordionSummary>
                    <AccordionDetails
                      sx={{ p: 2, borderTop: 1, borderColor: "divider" }}
                    >
                      <Box
                        sx={{
                          display: "flex",
                          flexDirection: "column",
                          gap: 2,
                        }}
                      >
                        {Object.entries(ps.source || {}).map(([key, value]) => (
                          <Box
                            key={key}
                            sx={{
                              border: 1,
                              borderColor: "divider",
                              borderRadius: 2,
                              overflow: "hidden",
                            }}
                          >
                            <Box
                              sx={{
                                bgcolor: "action.hover",
                                px: 2,
                                py: 1.5,
                                borderBottom: 1,
                                borderColor: "divider",
                              }}
                            >
                              <Typography
                                variant="body2"
                                sx={{ fontWeight: 500, color: "text.primary" }}
                              >
                                {key}
                              </Typography>
                            </Box>
                            <Box sx={{ position: "relative" }}>
                              {renderPropertyValue(value, `${ps.name}-${key}`)}
                            </Box>
                          </Box>
                        ))}
                      </Box>
                    </AccordionDetails>
                  </Accordion>
                );
              })}
            </Box>
          ) : (
            <Box sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
              {env.propertySources?.map((ps, index) => {
                const gitUrl = ps.name ? extractGitUrl(ps.name) : null;
                return (
                  <Card
                    key={ps.name}
                    sx={{ border: 1, borderColor: "divider", boxShadow: 1 }}
                  >
                    <CardContent sx={{ p: 2 }}>
                      <Box
                        sx={{
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "space-between",
                          mb: 2,
                        }}
                      >
                        <Box
                          sx={{
                            display: "flex",
                            alignItems: "center",
                            gap: 1,
                            flex: 1,
                            minWidth: 0,
                          }}
                        >
                          <Typography
                            variant="subtitle1"
                            sx={{
                              fontWeight: 500,
                              color: "text.primary",
                              overflow: "hidden",
                              textOverflow: "ellipsis",
                            }}
                          >
                            {ps.name?.split("/").pop() || "Unknown"}
                          </Typography>
                          {gitUrl && (
                            <Tooltip title="Open in GitHub">
                              <IconButton
                                size="small"
                                component={Link}
                                href={gitUrl}
                                target="_blank"
                                rel="noopener noreferrer"
                                sx={{ color: "text.secondary" }}
                              >
                                <GitHub fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          )}
                        </Box>
                        <Button
                          size="small"
                          startIcon={
                            copiedProperty === `source-yaml-${index}` ? (
                              <Check />
                            ) : (
                              <Copy />
                            )
                          }
                          onClick={() =>
                            copyToClipboard(
                              yaml.stringify(ps.source, { indent: 2 }),
                              `source-yaml-${index}`
                            )
                          }
                          sx={{
                            color: "text.secondary",
                            "&:hover": {
                              color: "text.primary",
                            },
                          }}
                        >
                          {copiedProperty === `source-yaml-${index}`
                            ? "Copied!"
                            : "Copy YAML"}
                        </Button>
                      </Box>
                      <Box
                        sx={{
                          border: 1,
                          borderColor: "divider",
                          borderRadius: 1,
                          overflow: "hidden",
                        }}
                      >
                        <Highlight
                          theme={themes.vsDark}
                          code={yaml.stringify(ps.source, { indent: 2 })}
                          language="yaml"
                        >
                          {({
                            className,
                            style,
                            tokens,
                            getLineProps,
                            getTokenProps,
                          }) => (
                            <pre
                              className={`${className} p-4 overflow-x-auto text-sm`}
                              style={style}
                            >
                              {tokens.map((line, i) => (
                                <div
                                  key={`source-${index}-line-${i}`}
                                  {...getLineProps({ line })}
                                >
                                  {line.map((token, tokenIdx) => (
                                    <span
                                      key={`source-${index}-token-${i}-${tokenIdx}`}
                                      {...getTokenProps({ token })}
                                    />
                                  ))}
                                </div>
                              ))}
                            </pre>
                          )}
                        </Highlight>
                      </Box>
                    </CardContent>
                  </Card>
                );
              })}
            </Box>
          )}
        </CardContent>
      </Card>
    </Box>
  );
}
