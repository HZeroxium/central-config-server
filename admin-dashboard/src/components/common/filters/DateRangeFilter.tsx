import { Box, Stack, IconButton, Typography } from "@mui/material";
import { DatePicker } from "@mui/x-date-pickers/DatePicker";
import { LocalizationProvider } from "@mui/x-date-pickers/LocalizationProvider";
import { AdapterDateFns } from "@mui/x-date-pickers/AdapterDateFns";
import { Clear as ClearIcon } from "@mui/icons-material";

export interface DateRangeFilterProps {
  /**
   * Label for the date range filter
   */
  label: string;
  /**
   * Start date value
   */
  startDate: Date | null;
  /**
   * End date value
   */
  endDate: Date | null;
  /**
   * Callback when dates change
   */
  onChange: (startDate: Date | null, endDate: Date | null) => void;
  /**
   * Whether the filter is disabled
   */
  disabled?: boolean;
  /**
   * Helper text to display
   */
  helperText?: string;
}

/**
 * Date Range Picker component for filtering
 */
export function DateRangeFilter({
  label,
  startDate,
  endDate,
  onChange,
  disabled = false,
  helperText,
}: DateRangeFilterProps) {
  const handleStartDateChange = (newDate: Date | null) => {
    onChange(newDate, endDate);
  };

  const handleEndDateChange = (newDate: Date | null) => {
    onChange(startDate, newDate);
  };

  const handleClear = () => {
    onChange(null, null);
  };

  return (
    <LocalizationProvider dateAdapter={AdapterDateFns}>
      <Box>
        <Typography
          variant="caption"
          color="text.secondary"
          sx={{ mb: 1, display: "block" }}
        >
          {label}
        </Typography>
        <Stack direction="row" spacing={1} alignItems="center">
          <DatePicker
            label="From"
            value={startDate}
            onChange={handleStartDateChange}
            disabled={disabled}
            slotProps={{
              textField: {
                size: "small",
                fullWidth: true,
              },
            }}
          />
          <DatePicker
            label="To"
            value={endDate}
            onChange={handleEndDateChange}
            disabled={disabled}
            minDate={startDate || undefined}
            slotProps={{
              textField: {
                size: "small",
                fullWidth: true,
              },
            }}
          />
          {(startDate || endDate) && (
            <IconButton
              size="small"
              onClick={handleClear}
              disabled={disabled}
              aria-label="Clear date range"
            >
              <ClearIcon fontSize="small" />
            </IconButton>
          )}
        </Stack>
        {helperText && (
          <Typography
            variant="caption"
            color="text.secondary"
            sx={{ mt: 0.5, display: "block" }}
          >
            {helperText}
          </Typography>
        )}
      </Box>
    </LocalizationProvider>
  );
}

export default DateRangeFilter;
