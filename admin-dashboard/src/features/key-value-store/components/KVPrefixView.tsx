/**
 * Component for viewing a KV prefix as a formatted document (JSON, YAML, Properties)
 */

import { useState } from "react";
import {
  Box,
  Card,
  CardContent,
  Typography,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Button,
  Stack,
  Alert,
  CircularProgress,
} from "@mui/material";
import {
  Download as DownloadIcon,
  ContentCopy as CopyIcon,
} from "@mui/icons-material";
import SyntaxHighlighter from "react-syntax-highlighter";
import { vscDarkPlus } from "react-syntax-highlighter/dist/esm/styles/prism";
import { toast } from "@lib/toast/toast";
import { useKVPrefixView } from "../hooks";

export type KVStructuredFormat = "json" | "yaml" | "properties";

export interface KVPrefixViewProps {
  serviceId: string;
  prefix: string;
  /** Initial format */
  initialFormat?: KVStructuredFormat;
  /** Content to display (if provided, won't fetch) */
  content?: string;
  /** Loading state */
  isLoading?: boolean;
  /** Error state */
  error?: Error | null;
  /** Callback to fetch content (if not provided, component handles fetching) */
  onFetch?: (format: KVStructuredFormat) => Promise<string | null>;
}

export function KVPrefixView({
  serviceId,
  prefix,
  initialFormat = "json",
  content: providedContent,
  isLoading: providedLoading,
  error: providedError,
  onFetch,
}: KVPrefixViewProps) {
  const [format, setFormat] = useState<KVStructuredFormat>(initialFormat);

  // Auto-fetch if onFetch is not provided
  const shouldAutoFetch = !onFetch;
  const {
    data: fetchedContent,
    isLoading: isLoadingFetched,
    error: fetchError,
  } = useKVPrefixView({
    serviceId,
    prefix,
    format,
    enabled: shouldAutoFetch && !!serviceId,
  });

  const handleFormatChange = async (newFormat: KVStructuredFormat) => {
    setFormat(newFormat);
    if (onFetch) {
      // Use custom fetch handler if provided
      try {
        await onFetch(newFormat);
        // Note: We don't update state here since onFetch is expected to handle it
      } catch (err) {
        // Error handling is up to the caller
      }
    }
    // Note: When format changes, the query key changes (format is in the key),
    // so React Query will automatically refetch with the new format
  };

  const handleCopy = () => {
    if (displayContent) {
      navigator.clipboard.writeText(displayContent);
      toast.success("Content copied to clipboard");
    }
  };

  const handleDownload = () => {
    if (!displayContent) return;

    const extension = format === "yaml" ? "yml" : format;
    const mimeType =
      format === "json"
        ? "application/json"
        : format === "yaml"
        ? "application/x-yaml"
        : "text/plain";

    const blob = new Blob([displayContent], { type: mimeType });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${prefix.replace(/\//g, "_")}.${extension}`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    toast.success("File downloaded");
  };

  const getLanguage = () => {
    switch (format) {
      case "json":
        return "json";
      case "yaml":
        return "yaml";
      case "properties":
        return "properties";
      default:
        return "text";
    }
  };

  const displayContent = providedContent || (shouldAutoFetch ? fetchedContent || null : null);
  const displayLoading = providedLoading !== undefined ? providedLoading : (shouldAutoFetch ? isLoadingFetched : false);
  const displayError = providedError || (shouldAutoFetch ? fetchError : null);

  return (
    <Card>
      <CardContent>
        <Box
          sx={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            mb: 2,
          }}
        >
          <Typography variant="h6">Prefix View</Typography>
          <Stack direction="row" spacing={1}>
            <FormControl size="small" sx={{ minWidth: 120 }}>
              <InputLabel>Format</InputLabel>
              <Select
                value={format}
                label="Format"
                onChange={(e) =>
                  handleFormatChange(e.target.value as KVStructuredFormat)
                }
              >
                <MenuItem value="json">JSON</MenuItem>
                <MenuItem value="yaml">YAML</MenuItem>
                <MenuItem value="properties">Properties</MenuItem>
              </Select>
            </FormControl>
            {displayContent && (
              <>
                <Button
                  size="small"
                  startIcon={<CopyIcon />}
                  onClick={handleCopy}
                  variant="outlined"
                >
                  Copy
                </Button>
                <Button
                  size="small"
                  startIcon={<DownloadIcon />}
                  onClick={handleDownload}
                  variant="outlined"
                >
                  Download
                </Button>
              </>
            )}
          </Stack>
        </Box>

        {displayError && (
          <Alert severity="error" sx={{ mb: 2 }}>
            Failed to load content: {displayError.message}
          </Alert>
        )}

        {displayLoading && (
          <Box sx={{ textAlign: "center", py: 4 }}>
            <CircularProgress size={24} />
            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
              Loading...
            </Typography>
          </Box>
        )}

        {!displayLoading && !displayError && displayContent && (
          <Box
            sx={{
              border: 1,
              borderColor: "divider",
              borderRadius: 1,
              overflow: "hidden",
            }}
          >
            <SyntaxHighlighter
              language={getLanguage()}
              style={vscDarkPlus}
              customStyle={{
                margin: 0,
                borderRadius: 0,
                fontSize: "0.875rem",
              }}
            >
              {displayContent}
            </SyntaxHighlighter>
          </Box>
        )}

        {!displayLoading && !displayError && !displayContent && (
          <Box sx={{ textAlign: "center", py: 4 }}>
            <Typography variant="body2" color="text.secondary">
              No content available
            </Typography>
          </Box>
        )}
      </CardContent>
    </Card>
  );
}

