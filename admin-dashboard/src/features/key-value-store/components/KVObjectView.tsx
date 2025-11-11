/**
 * Read-only view component for KV Object structure
 */

import { useMemo, useState } from "react";
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  Stack,
  Alert,
  CircularProgress,
  IconButton,
  Tooltip,
  Tabs,
  Tab,
  Skeleton,
} from "@mui/material";
import {
  Edit as EditIcon,
  Refresh as RefreshIcon,
  Code as ObjectIcon,
} from "@mui/icons-material";
import SyntaxHighlighter from "react-syntax-highlighter";
import { vscDarkPlus } from "react-syntax-highlighter/dist/esm/styles/prism";
import { useGetKVObject, type GetKVObjectParams } from "../hooks";
import { fromKVObjectResponseData } from "../utils/typeAdapters";
import { TabPanel } from "@components/common/TabPanel";

export interface KVObjectViewProps {
  serviceId: string;
  prefix: string;
  /** Callback to switch to edit mode */
  onEdit?: () => void;
  /** Callback to refresh data */
  onRefresh?: () => void;
  /** Whether this is read-only */
  isReadOnly?: boolean;
  /** Loading state override */
  isLoading?: boolean;
  /** Error state override */
  error?: Error | null;
}

export function KVObjectView({
  serviceId,
  prefix,
  onEdit,
  onRefresh,
  isReadOnly = false,
  isLoading: providedLoading,
  error: providedError,
}: KVObjectViewProps) {
  const [tabValue, setTabValue] = useState(0);

  const params: GetKVObjectParams | undefined = serviceId && prefix
    ? { prefix, consistent: false, stale: false }
    : undefined;

  const {
    data: objectData,
    isLoading: isLoadingObject,
    error: objectError,
    refetch,
  } = useGetKVObject(
    serviceId,
    params,
    {
      query: {
        enabled: !!serviceId && !!prefix,
        staleTime: 10_000,
      },
    }
  );

  const isLoading = providedLoading !== undefined ? providedLoading : isLoadingObject;
  const error = providedError || objectError;

  const handleRefresh = () => {
    refetch();
    onRefresh?.();
  };

  // Convert loaded data to UI representation
  const objectDataRecord: Record<string, unknown> = useMemo(() => {
    if (!objectData?.data) return {};
    return fromKVObjectResponseData(objectData.data);
  }, [objectData?.data]);

  const jsonString = useMemo(() => {
    return JSON.stringify(objectDataRecord, null, 2);
  }, [objectDataRecord]);

  const entries = useMemo(() => {
    return Object.entries(objectDataRecord).map(([key, value]) => ({
      key,
      value,
      valueString: typeof value === "string" ? value : JSON.stringify(value),
    }));
  }, [objectDataRecord]);

  // Loading skeleton
  if (isLoading) {
    return (
      <Card>
        <CardContent>
          {/* Header skeleton */}
          <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 3 }}>
            <Stack direction="row" spacing={2} alignItems="center">
              <Skeleton variant="text" width={120} height={32} />
              <Skeleton variant="rectangular" width={70} height={24} sx={{ borderRadius: 1 }} />
              <Skeleton variant="rectangular" width={100} height={24} sx={{ borderRadius: 1 }} />
            </Stack>
            <Stack direction="row" spacing={1}>
              <Skeleton variant="circular" width={32} height={32} />
              <Skeleton variant="rectangular" width={80} height={32} sx={{ borderRadius: 1 }} />
            </Stack>
          </Box>

          {/* Tabs skeleton */}
          <Box sx={{ mb: 2 }}>
            <Skeleton variant="rectangular" width={200} height={48} sx={{ borderRadius: 1 }} />
          </Box>

          {/* Table skeleton */}
          <TableContainer component={Paper} variant="outlined">
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>
                    <Skeleton variant="text" width={60} height={20} />
                  </TableCell>
                  <TableCell>
                    <Skeleton variant="text" width={80} height={20} />
                  </TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {[1, 2, 3, 4, 5].map((row) => (
                  <TableRow key={row}>
                    <TableCell>
                      <Skeleton variant="text" width={150} height={20} />
                    </TableCell>
                    <TableCell>
                      <Skeleton variant="rectangular" width="100%" height={40} sx={{ borderRadius: 1 }} />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </CardContent>
      </Card>
    );
  }

  // Error state with retry
  if (error) {
    const errorMessage = error instanceof Error 
      ? error.message 
      : typeof error === "object" && error !== null && "message" in error
      ? String(error.message)
      : "Unknown error occurred";
    
    const isNotFound = errorMessage.toLowerCase().includes("not found") || 
                      errorMessage.toLowerCase().includes("404");
    
    return (
      <Card>
        <CardContent>
          <Alert 
            severity={isNotFound ? "warning" : "error"} 
            sx={{ mb: 2 }}
            action={
              <Button 
                color="inherit" 
                size="small" 
                onClick={handleRefresh}
                startIcon={<RefreshIcon />}
              >
                Retry
              </Button>
            }
          >
            <Typography variant="body2" fontWeight={600} gutterBottom>
              {isNotFound ? "Object not found" : "Failed to load object"}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {errorMessage}
            </Typography>
          </Alert>
          <Button 
            variant="outlined" 
            onClick={handleRefresh}
            startIcon={<RefreshIcon />}
            fullWidth
          >
            Retry Loading
          </Button>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardContent>
        <Box
          sx={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            mb: 3,
          }}
        >
          <Stack direction="row" spacing={2} alignItems="center">
            <Typography variant="h6">Object View</Typography>
            <Chip label="OBJECT" size="small" color="primary" />
            {entries.length > 0 && (
              <Chip
                label={`${entries.length} properties`}
                size="small"
                variant="outlined"
              />
            )}
          </Stack>
          <Stack direction="row" spacing={1}>
            <Tooltip title="Refresh">
              <IconButton 
                size="small" 
                onClick={handleRefresh} 
                aria-label="Refresh"
                disabled={isLoadingObject}
              >
                {isLoadingObject ? (
                  <CircularProgress size={16} />
                ) : (
                  <RefreshIcon fontSize="small" />
                )}
              </IconButton>
            </Tooltip>
            {!isReadOnly && onEdit && (
              <Button
                variant="contained"
                startIcon={<EditIcon />}
                onClick={onEdit}
                size="small"
              >
                Edit
              </Button>
            )}
          </Stack>
        </Box>

        {entries.length === 0 ? (
          <Alert 
            severity="info" 
            icon={<ObjectIcon />}
            sx={{ 
              py: 3,
              "& .MuiAlert-message": {
                width: "100%",
              },
            }}
          >
            <Typography variant="body1" fontWeight={600} gutterBottom>
              Empty Object
            </Typography>
            <Typography variant="body2" color="text.secondary">
              This object structure exists but contains no properties. Click "Edit" to add properties.
            </Typography>
          </Alert>
        ) : (
          <>
            <Tabs value={tabValue} onChange={(_, v) => setTabValue(v)} sx={{ mb: 2 }}>
              <Tab label="Key-Value Table" />
              <Tab label="JSON Preview" />
            </Tabs>

            <TabPanel value={tabValue} index={0}>
              <TableContainer 
                component={Paper} 
                variant="outlined"
                sx={{
                  maxHeight: "calc(100vh - 400px)",
                  overflow: "auto",
                }}
              >
                <Table size="small" stickyHeader>
                  <TableHead>
                    <TableRow>
                      <TableCell sx={{ fontWeight: 600, minWidth: 200, width: "30%" }}>Key</TableCell>
                      <TableCell sx={{ fontWeight: 600 }}>Value</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {entries.map((entry) => (
                      <TableRow 
                        key={entry.key} 
                        hover
                        sx={{
                          "&:last-child td": { borderBottom: 0 },
                        }}
                      >
                        <TableCell>
                          <Typography
                            variant="body2"
                            sx={{
                              fontFamily: "monospace",
                              fontWeight: 500,
                              wordBreak: "break-word",
                            }}
                          >
                            {entry.key}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Box
                            sx={{
                              maxWidth: "100%",
                              overflow: "auto",
                            }}
                          >
                            {typeof entry.value === "string" ? (
                              <Typography
                                variant="body2"
                                sx={{
                                  wordBreak: "break-word",
                                }}
                              >
                                {entry.value}
                              </Typography>
                            ) : (
                              <Typography
                                variant="body2"
                                component="pre"
                                sx={{
                                  fontFamily: "monospace",
                                  fontSize: "0.75rem",
                                  whiteSpace: "pre-wrap",
                                  wordBreak: "break-word",
                                  m: 0,
                                  p: 1.5,
                                  bgcolor: "background.default",
                                  borderRadius: 1,
                                  border: 1,
                                  borderColor: "divider",
                                }}
                              >
                                {entry.valueString}
                              </Typography>
                            )}
                          </Box>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            </TabPanel>

            <TabPanel value={tabValue} index={1}>
              <Box
                sx={{
                  border: 1,
                  borderColor: "divider",
                  borderRadius: 1,
                  overflow: "hidden",
                }}
              >
                <SyntaxHighlighter
                  language="json"
                  style={vscDarkPlus}
                  customStyle={{
                    margin: 0,
                    borderRadius: 0,
                    fontSize: "0.875rem",
                  }}
                >
                  {jsonString}
                </SyntaxHighlighter>
              </Box>
            </TabPanel>
          </>
        )}
      </CardContent>
    </Card>
  );
}

