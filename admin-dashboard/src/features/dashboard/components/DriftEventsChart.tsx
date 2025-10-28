import React from "react";
import { Card, CardContent, Typography, Box } from "@mui/material";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from "recharts";

interface DriftEventsData {
  date: string;
  critical: number;
  high: number;
  medium: number;
  low: number;
}

interface DriftEventsChartProps {
  data: DriftEventsData[];
}

export const DriftEventsChart: React.FC<DriftEventsChartProps> = ({ data }) => {
  return (
    <Card sx={{ height: "100%" }}>
      <CardContent>
        <Typography variant="h6" gutterBottom>
          Drift Events (Last 7 Days)
        </Typography>
        <Box sx={{ height: 300, width: "100%" }}>
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={data}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" />
              <YAxis />
              <Tooltip />
              <Legend />
              <Line
                type="monotone"
                dataKey="critical"
                stroke="#dc2626"
                strokeWidth={2}
              />
              <Line
                type="monotone"
                dataKey="high"
                stroke="#ea580c"
                strokeWidth={2}
              />
              <Line
                type="monotone"
                dataKey="medium"
                stroke="#d97706"
                strokeWidth={2}
              />
              <Line
                type="monotone"
                dataKey="low"
                stroke="#2563eb"
                strokeWidth={2}
              />
            </LineChart>
          </ResponsiveContainer>
        </Box>
      </CardContent>
    </Card>
  );
};
