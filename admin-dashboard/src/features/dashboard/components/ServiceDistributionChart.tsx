import React from "react";
import { Card, CardContent, Typography, Box } from "@mui/material";
import {
  PieChart,
  Pie,
  Cell,
  ResponsiveContainer,
  Legend,
  Tooltip,
} from "recharts";

interface ServiceDistributionData {
  name: string;
  value: number;
  color: string;
}

interface ServiceDistributionChartProps {
  data: ServiceDistributionData[];
}

const COLORS = ["#2563eb", "#60a5fa", "#93c5fd", "#dbeafe", "#f3f4f6"];

export const ServiceDistributionChart: React.FC<
  ServiceDistributionChartProps
> = ({ data }) => {
  return (
    <Card sx={{ height: "100%" }}>
      <CardContent>
        <Typography variant="h6" gutterBottom>
          Services by Team
        </Typography>
        <Box sx={{ height: 300, width: "100%" }}>
          <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Pie
                data={data}
                cx="50%"
                cy="50%"
                labelLine={false}
                label={({ name, percent }) =>
                  `${name} ${(percent * 100).toFixed(0)}%`
                }
                outerRadius={80}
                fill="#8884d8"
                dataKey="value"
              >
                {data.map((entry) => (
                  <Cell
                    key={`cell-${entry.name}`}
                    fill={
                      entry.color || COLORS[data.indexOf(entry) % COLORS.length]
                    }
                  />
                ))}
              </Pie>
              <Tooltip />
              <Legend />
            </PieChart>
          </ResponsiveContainer>
        </Box>
      </CardContent>
    </Card>
  );
};
