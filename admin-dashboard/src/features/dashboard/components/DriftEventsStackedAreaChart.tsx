import React from "react";
import { Card, CardContent, Typography, Box } from "@mui/material";
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from "recharts";
import EmptyState from "@components/common/EmptyState";
import { BarChart as BarChartIcon } from "@mui/icons-material";

interface DriftEventsData {
  date: string;
  critical: number;
  high: number;
  medium: number;
  low: number;
}

interface DriftEventsStackedAreaChartProps {
  data: DriftEventsData[];
  loading?: boolean;
  title?: string;
}

export const DriftEventsStackedAreaChart: React.FC<
  DriftEventsStackedAreaChartProps
> = ({ data, loading = false, title = "Drift Events by Severity" }) => {
  // Validate data
  const isValidData =
    Array.isArray(data) &&
    data.length > 0 &&
    data.every(
      (item) =>
        typeof item === "object" &&
        item !== null &&
        typeof item.date === "string" &&
        typeof item.critical === "number" &&
        typeof item.high === "number" &&
        typeof item.medium === "number" &&
        typeof item.low === "number"
    );

  // Check if all values are zero (no drift events)
  const hasData =
    isValidData &&
    data.some((item) => item.critical + item.high + item.medium + item.low > 0);

  if (loading) {
    return (
      <Card sx={{ height: "100%" }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            {title}
          </Typography>
          <Box
            sx={{
              height: 300,
              width: "100%",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
            }}
          >
            <Typography variant="body2" color="text.secondary">
              Loading chart data...
            </Typography>
          </Box>
        </CardContent>
      </Card>
    );
  }

  if (!isValidData || !hasData) {
    return (
      <Card sx={{ height: "100%" }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            {title}
          </Typography>
          <Box sx={{ height: 300, width: "100%" }}>
            <EmptyState
              icon={<BarChartIcon sx={{ fontSize: 48 }} />}
              title="No drift events data available"
              description="No configuration drift events were detected during this period."
            />
          </Box>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card sx={{ height: "100%" }}>
      <CardContent>
        <Typography variant="h6" gutterBottom>
          {title}
        </Typography>
        <Box
          sx={{ height: 300, width: "100%" }}
          role="img"
          aria-label={`Stacked area chart showing ${title}`}
        >
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart
              data={data}
              margin={{ top: 10, right: 30, left: 0, bottom: 0 }}
            >
              <defs>
                <linearGradient id="colorCritical" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#dc2626" stopOpacity={0.8} />
                  <stop offset="95%" stopColor="#dc2626" stopOpacity={0.1} />
                </linearGradient>
                <linearGradient id="colorHigh" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#ea580c" stopOpacity={0.8} />
                  <stop offset="95%" stopColor="#ea580c" stopOpacity={0.1} />
                </linearGradient>
                <linearGradient id="colorMedium" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#d97706" stopOpacity={0.8} />
                  <stop offset="95%" stopColor="#d97706" stopOpacity={0.1} />
                </linearGradient>
                <linearGradient id="colorLow" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#2563eb" stopOpacity={0.8} />
                  <stop offset="95%" stopColor="#2563eb" stopOpacity={0.1} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis
                dataKey="date"
                tick={{ fontSize: 12 }}
                label={{ value: "Date", position: "insideBottom", offset: -5 }}
              />
              <YAxis
                tick={{ fontSize: 12 }}
                label={{ value: "Count", angle: -90, position: "insideLeft" }}
              />
              <Tooltip
                contentStyle={{
                  backgroundColor: "rgba(255, 255, 255, 0.95)",
                  border: "1px solid #ccc",
                  borderRadius: "4px",
                }}
              />
              <Legend />
              <Area
                type="monotone"
                dataKey="critical"
                stackId="1"
                stroke="#dc2626"
                fill="url(#colorCritical)"
                name="Critical"
              />
              <Area
                type="monotone"
                dataKey="high"
                stackId="1"
                stroke="#ea580c"
                fill="url(#colorHigh)"
                name="High"
              />
              <Area
                type="monotone"
                dataKey="medium"
                stackId="1"
                stroke="#d97706"
                fill="url(#colorMedium)"
                name="Medium"
              />
              <Area
                type="monotone"
                dataKey="low"
                stackId="1"
                stroke="#2563eb"
                fill="url(#colorLow)"
                name="Low"
              />
            </AreaChart>
          </ResponsiveContainer>
        </Box>
      </CardContent>
    </Card>
  );
};

