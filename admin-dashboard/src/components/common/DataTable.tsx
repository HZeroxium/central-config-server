import { useState, useMemo } from "react";
import {
  DataGrid,
  type GridColDef,
  GridToolbarContainer,
  GridToolbarColumnsButton,
  GridToolbarFilterButton,
  GridToolbarDensitySelector,
  GridToolbarExport,
  type GridRowsProp,
  type GridPaginationModel,
  type GridSortModel,
  type GridFilterModel,
} from "@mui/x-data-grid";
import {
  Box,
  TextField,
  InputAdornment,
  Menu,
  MenuItem,
  Checkbox,
  FormControlLabel,
  IconButton,
  Tooltip,
} from "@mui/material";
import SearchIcon from "@mui/icons-material/Search";
import ViewColumnIcon from "@mui/icons-material/ViewColumn";
import type { ReactNode } from "react";

interface CustomToolbarProps {
  readonly searchable?: boolean;
  readonly searchValue?: string;
  readonly onSearchChange?: (value: string) => void;
  readonly exportable?: boolean;
  readonly exportFilename?: string;
  readonly columns?: GridColDef[];
  readonly visibleColumns?: string[];
  readonly onColumnVisibilityChange?: (columns: string[]) => void;
  readonly customActions?: ReactNode;
}

function CustomToolbar({
  searchable,
  searchValue = "",
  onSearchChange,
  exportable,
  exportFilename = "data",
  columns = [],
  visibleColumns = [],
  onColumnVisibilityChange,
  customActions,
}: CustomToolbarProps) {
  const [columnMenuAnchor, setColumnMenuAnchor] = useState<null | HTMLElement>(
    null
  );

  const handleColumnToggle = (field: string) => {
    const newVisibleColumns = visibleColumns.includes(field)
      ? visibleColumns.filter((col) => col !== field)
      : [...visibleColumns, field];
    onColumnVisibilityChange?.(newVisibleColumns);
  };

  return (
    <GridToolbarContainer sx={{ p: 2, gap: 2, flexWrap: "wrap" }}>
      {searchable && (
        <TextField
          placeholder="Search..."
          value={searchValue}
          onChange={(e) => onSearchChange?.(e.target.value)}
          size="small"
          sx={{ minWidth: 250 }}
          slotProps={{
            input: {
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon />
                </InputAdornment>
              ),
            },
          }}
        />
      )}

      <Box sx={{ flexGrow: 1 }} />

      {customActions}

      <GridToolbarColumnsButton />
      <GridToolbarFilterButton />
      <GridToolbarDensitySelector />

      {exportable && (
        <GridToolbarExport
          csvOptions={{ fileName: exportFilename }}
          printOptions={{ disableToolbarButton: true }}
        />
      )}

      {columns.length > 0 && (
        <>
          <Tooltip title="Toggle columns">
            <IconButton
              size="small"
              onClick={(e) => setColumnMenuAnchor(e.currentTarget)}
            >
              <ViewColumnIcon />
            </IconButton>
          </Tooltip>
          <Menu
            anchorEl={columnMenuAnchor}
            open={Boolean(columnMenuAnchor)}
            onClose={() => setColumnMenuAnchor(null)}
          >
            {columns.map((col) => (
              <MenuItem key={col.field} dense>
                <FormControlLabel
                  control={
                    <Checkbox
                      checked={visibleColumns.includes(col.field)}
                      onChange={() => handleColumnToggle(col.field)}
                      size="small"
                    />
                  }
                  label={col.headerName || col.field}
                />
              </MenuItem>
            ))}
          </Menu>
        </>
      )}
    </GridToolbarContainer>
  );
}

interface DataTableProps {
  readonly rows: GridRowsProp;
  readonly columns: GridColDef[];
  readonly loading?: boolean;
  readonly searchable?: boolean;
  readonly exportable?: boolean;
  readonly exportFilename?: string;
  readonly onPaginationModelChange?: (model: GridPaginationModel) => void;
  readonly onSortModelChange?: (model: GridSortModel) => void;
  readonly onFilterModelChange?: (model: GridFilterModel) => void;
  readonly paginationModel?: GridPaginationModel;
  readonly rowCount?: number;
  readonly paginationMode?: "client" | "server";
  readonly customActions?: ReactNode;
  readonly autoHeight?: boolean;
  readonly getRowId?: (row: unknown) => string | number;
}

export default function DataTable({
  rows,
  columns,
  loading = false,
  searchable = true,
  exportable = true,
  exportFilename = "export",
  onPaginationModelChange,
  onSortModelChange,
  onFilterModelChange,
  paginationModel,
  rowCount,
  paginationMode = "client",
  customActions,
  autoHeight = false,
  getRowId,
}: DataTableProps) {
  const [searchValue, setSearchValue] = useState("");
  const [visibleColumns, setVisibleColumns] = useState<string[]>(
    columns.map((col) => col.field)
  );

  // Filter rows based on search
  const filteredRows = useMemo(() => {
    if (!searchValue || !searchable) return rows;

    return rows.filter((row) => {
      return columns.some((col) => {
        const value = row[col.field];
        return (
          value &&
          String(value).toLowerCase().includes(searchValue.toLowerCase())
        );
      });
    });
  }, [rows, searchValue, columns, searchable]);

  // Filter columns based on visibility
  const visibleColumnsConfig = useMemo(() => {
    return columns.map((col) => ({
      ...col,
      hide: !visibleColumns.includes(col.field),
    }));
  }, [columns, visibleColumns]);

  return (
    <Box sx={{ width: "100%", height: autoHeight ? "auto" : 600 }}>
      <DataGrid
        rows={filteredRows}
        columns={visibleColumnsConfig}
        loading={loading}
        pageSizeOptions={[10, 25, 50, 100]}
        paginationModel={paginationModel}
        onPaginationModelChange={onPaginationModelChange}
        paginationMode={paginationMode}
        rowCount={rowCount}
        onSortModelChange={onSortModelChange}
        onFilterModelChange={onFilterModelChange}
        disableRowSelectionOnClick
        autoHeight={autoHeight}
        getRowId={getRowId}
        slots={{
          toolbar:
            CustomToolbar as unknown as React.JSXElementConstructor<object>,
        }}
        slotProps={{
          toolbar: {
            searchable,
            searchValue,
            onSearchChange: setSearchValue,
            exportable,
            exportFilename,
            columns,
            visibleColumns,
            onColumnVisibilityChange: setVisibleColumns,
            customActions,
          } as unknown as object,
        }}
        sx={{
          "& .MuiDataGrid-cell": {
            display: "flex",
            alignItems: "center",
            py: 1.5,
          },
          "& .MuiDataGrid-cell:focus": {
            outline: "none",
          },
          "& .MuiDataGrid-cell:focus-within": {
            outline: "none",
          },
          "& .MuiDataGrid-row": {
            "&:hover": {
              backgroundColor: (theme) =>
                theme.palette.mode === "light"
                  ? "rgba(37, 99, 235, 0.04)"
                  : "rgba(96, 165, 250, 0.08)",
              cursor: "pointer",
            },
          },
          "& .MuiDataGrid-columnHeader": {
            backgroundColor: (theme) =>
              theme.palette.mode === "light"
                ? "rgba(37, 99, 235, 0.04)"
                : "rgba(96, 165, 250, 0.08)",
            fontWeight: 600,
          },
          "& .MuiDataGrid-columnHeaderTitle": {
            fontWeight: 600,
          },
        }}
      />
    </Box>
  );
}
