import React from "react";
import { ToggleButtonGroup, ToggleButton, Button } from "@mui/material";
import { DatePicker } from "@mui/x-date-pickers/DatePicker";
import { LocalizationProvider } from "@mui/x-date-pickers/LocalizationProvider";
import { AdapterDateFns } from "@mui/x-date-pickers/AdapterDateFns";
import { CalendarToday as CalendarIcon } from "@mui/icons-material";

export type TimeRange = "7d" | "30d" | "90d" | "custom";

interface TimeRangeSelectorProps {
  value: TimeRange;
  onChange: (range: TimeRange) => void;
  customStartDate?: Date | null;
  customEndDate?: Date | null;
  onCustomDateChange?: (start: Date | null, end: Date | null) => void;
}

export const TimeRangeSelector: React.FC<TimeRangeSelectorProps> = ({
  value,
  onChange,
  customStartDate,
  customEndDate,
  onCustomDateChange,
}) => {
  const handleRangeChange = (
    _event: React.MouseEvent<HTMLElement>,
    newRange: TimeRange | null
  ) => {
    if (newRange !== null) {
      onChange(newRange);
    }
  };

  return (
    <LocalizationProvider dateAdapter={AdapterDateFns}>
      <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
        <ToggleButtonGroup
          value={value}
          exclusive
          onChange={handleRangeChange}
          aria-label="time range selection"
          size="small"
        >
          <ToggleButton value="7d" aria-label="7 days">
            7 Days
          </ToggleButton>
          <ToggleButton value="30d" aria-label="30 days">
            30 Days
          </ToggleButton>
          <ToggleButton value="90d" aria-label="90 days">
            90 Days
          </ToggleButton>
        </ToggleButtonGroup>
        {value === "custom" && (
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <DatePicker
              label="Start Date"
              value={customStartDate}
              onChange={(newValue) => {
                onCustomDateChange?.(newValue, customEndDate || null);
              }}
              slotProps={{
                textField: {
                  size: "small",
                  sx: { width: 150 },
                },
              }}
            />
            <DatePicker
              label="End Date"
              value={customEndDate}
              onChange={(newValue) => {
                onCustomDateChange?.(customStartDate || null, newValue);
              }}
              slotProps={{
                textField: {
                  size: "small",
                  sx: { width: 150 },
                },
              }}
            />
          </div>
        )}
        <Button
          variant="outlined"
          size="small"
          startIcon={<CalendarIcon />}
          onClick={() => onChange("custom")}
          aria-label="Select custom date range"
        >
          Custom
        </Button>
      </div>
    </LocalizationProvider>
  );
};

