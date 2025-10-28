import { Box } from "@mui/material";
import { useSearchParams, useParams } from "react-router-dom";
import { useGetEnvironmentConfigServer } from "@lib/api/hooks";
import Loading from "@components/common/Loading";
import ErrorFallback from "@components/common/ErrorFallback";
import ConfigDetailCard from "@features/configs/components/ConfigDetailCard";

export default function ConfigDetailPage() {
  const { application = "", profile = "" } = useParams();
  const [sp] = useSearchParams();
  const label = sp.get("label") || undefined;
  const {
    data: configResponse,
    isLoading,
    error,
    refetch,
  } = useGetEnvironmentConfigServer(
    application,
    profile,
    { label },
    {
      query: {
        enabled: !!application && !!profile,
        staleTime: 30000,
      },
    }
  );

  const configData = configResponse;

  if (isLoading) return <Loading />;
  if (error || !configData)
    return (
      <ErrorFallback
        message={(error as Error)?.message || "Failed to load config"}
        onRetry={refetch}
      />
    );

  return (
    <Box sx={{ p: { xs: 2, sm: 3, md: 4 } }}>
      <ConfigDetailCard env={configData} />
    </Box>
  );
}
