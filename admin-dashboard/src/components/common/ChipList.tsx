import { useState, type MouseEvent } from "react";
import {
  Box,
  Chip,
  Popover,
  Stack,
  Typography,
  type ChipProps,
} from "@mui/material";

interface ChipListProps {
  readonly items: string[];
  readonly maxVisible?: number;
  readonly chipProps?: Partial<ChipProps>;
  readonly getChipColor?: (item: string) => ChipProps["color"];
  readonly variant?: ChipProps["variant"];
  readonly size?: ChipProps["size"];
}

/**
 * ChipList - Display a list of chips with expandable popover
 * Shows first N items, with "+X more" chip that opens popover with full list
 */
export function ChipList({
  items,
  maxVisible = 3,
  chipProps = {},
  getChipColor,
  variant = "outlined",
  size = "small",
}: ChipListProps) {
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);

  if (!items || items.length === 0) {
    return <span>-</span>;
  }

  const visibleItems = items.slice(0, maxVisible);
  const remainingItems = items.slice(maxVisible);
  const hasMore = remainingItems.length > 0;

  const handleClick = (event: MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const open = Boolean(anchorEl);
  const popoverId = open ? "chip-list-popover" : undefined;

  return (
    <Box
      sx={{ display: "flex", gap: 0.5, flexWrap: "wrap", alignItems: "center" }}
    >
      {visibleItems.map((item, index) => (
        <Chip
          key={`${item}-${index}`}
          label={item}
          size={size}
          variant={variant}
          color={getChipColor ? getChipColor(item) : undefined}
          {...chipProps}
        />
      ))}

      {hasMore && (
        <>
          <Chip
            label={`+${remainingItems.length} more`}
            size={size}
            variant="outlined"
            onClick={handleClick}
            aria-describedby={popoverId}
            sx={{
              cursor: "pointer",
              fontWeight: 600,
              "&:hover": {
                backgroundColor: (theme) =>
                  theme.palette.mode === "light"
                    ? "rgba(37, 99, 235, 0.08)"
                    : "rgba(96, 165, 250, 0.12)",
              },
            }}
          />
          <Popover
            id={popoverId}
            open={open}
            anchorEl={anchorEl}
            onClose={handleClose}
            anchorOrigin={{
              vertical: "bottom",
              horizontal: "left",
            }}
            transformOrigin={{
              vertical: "top",
              horizontal: "left",
            }}
            slotProps={{
              paper: {
                sx: {
                  maxWidth: 400,
                  maxHeight: 300,
                  overflow: "auto",
                },
              },
            }}
          >
            <Box sx={{ p: 2 }}>
              <Typography variant="subtitle2" sx={{ mb: 1.5, fontWeight: 600 }}>
                All Items ({items.length})
              </Typography>
              <Stack spacing={1} direction="row" flexWrap="wrap" useFlexGap>
                {items.map((item, index) => (
                  <Chip
                    key={`${item}-${index}`}
                    label={item}
                    size={size}
                    variant={variant}
                    color={getChipColor ? getChipColor(item) : undefined}
                    {...chipProps}
                  />
                ))}
              </Stack>
            </Box>
          </Popover>
        </>
      )}
    </Box>
  );
}

export default ChipList;
