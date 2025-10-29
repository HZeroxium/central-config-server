import {
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Chip,
  Box,
  OutlinedInput,
  Checkbox,
  ListItemText,
  IconButton,
  Typography,
} from "@mui/material";
import type { SelectChangeEvent } from "@mui/material";
import { Clear as ClearIcon } from "@mui/icons-material";

export interface MultiSelectFilterProps<T = string> {
  /**
   * Label for the filter
   */
  label: string;
  /**
   * Selected values
   */
  value: T[];
  /**
   * Available options
   */
  options: Array<{ value: T; label: string }>;
  /**
   * Callback when selection changes
   */
  onChange: (value: T[]) => void;
  /**
   * Whether the filter is disabled
   */
  disabled?: boolean;
  /**
   * Helper text to display
   */
  helperText?: string;
  /**
   * Maximum number of selections (undefined = unlimited)
   */
  maxSelections?: number;
}

/**
 * Multi-select filter component
 */
export function MultiSelectFilter<T extends string | number>({
  label,
  value,
  options,
  onChange,
  disabled = false,
  helperText,
  maxSelections,
}: MultiSelectFilterProps<T>) {
  const handleChange = (event: SelectChangeEvent<T[]>) => {
    const selected = event.target.value as T[];

    // Enforce max selections if specified
    if (maxSelections && selected.length > maxSelections) {
      return;
    }

    onChange(selected);
  };

  const handleClear = () => {
    onChange([]);
  };

  return (
    <Box>
      <FormControl fullWidth size="small" disabled={disabled}>
        <InputLabel id={`multiselect-${label}-label`}>{label}</InputLabel>
        <Select
          labelId={`multiselect-${label}-label`}
          multiple
          value={value}
          onChange={handleChange}
          input={<OutlinedInput label={label} />}
          renderValue={(selected) => (
            <Box sx={{ display: "flex", flexWrap: "wrap", gap: 0.5 }}>
              {selected.map((val) => {
                const option = options.find((opt) => opt.value === val);
                return (
                  <Chip
                    key={String(val)}
                    label={option?.label || String(val)}
                    size="small"
                  />
                );
              })}
            </Box>
          )}
          endAdornment={
            value.length > 0 && !disabled ? (
              <IconButton
                size="small"
                onClick={(e) => {
                  e.stopPropagation();
                  handleClear();
                }}
                sx={{ mr: 1 }}
                aria-label="Clear selection"
              >
                <ClearIcon fontSize="small" />
              </IconButton>
            ) : undefined
          }
        >
          {options.map((option) => (
            <MenuItem key={String(option.value)} value={option.value}>
              <Checkbox checked={value.includes(option.value)} />
              <ListItemText primary={option.label} />
            </MenuItem>
          ))}
        </Select>
      </FormControl>
      {helperText && (
        <Typography
          variant="caption"
          color="text.secondary"
          sx={{ mt: 0.5, display: "block" }}
        >
          {helperText}
        </Typography>
      )}
      {maxSelections && value.length >= maxSelections && (
        <Typography
          variant="caption"
          color="warning.main"
          sx={{ mt: 0.5, display: "block" }}
        >
          Maximum {maxSelections} selections allowed
        </Typography>
      )}
    </Box>
  );
}

export default MultiSelectFilter;
