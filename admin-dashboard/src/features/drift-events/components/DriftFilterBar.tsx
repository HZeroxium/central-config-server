import React from 'react';
import {
  Box,
  TextField,
  InputAdornment,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Button,
  Chip,
} from '@mui/material';
import { Search as SearchIcon, Clear as ClearIcon } from '@mui/icons-material';

interface DriftFilterBarProps {
  serviceName: string;
  status: string;
  severity: string;
  environment: string;
  onServiceNameChange: (value: string) => void;
  onStatusChange: (value: string) => void;
  onSeverityChange: (value: string) => void;
  onEnvironmentChange: (value: string) => void;
  onClearFilters: () => void;
}

export const DriftFilterBar: React.FC<DriftFilterBarProps> = ({
  serviceName,
  status,
  severity,
  environment,
  onServiceNameChange,
  onStatusChange,
  onSeverityChange,
  onEnvironmentChange,
  onClearFilters,
}) => {
  const hasActiveFilters = serviceName || status || severity || environment;

  return (
    <Box sx={{ mb: 3, display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'center' }}>
      <TextField
        placeholder="Search by service name..."
        value={serviceName}
        onChange={(e) => onServiceNameChange(e.target.value)}
        InputProps={{
          startAdornment: (
            <InputAdornment position="start">
              <SearchIcon />
            </InputAdornment>
          ),
        }}
        sx={{ minWidth: 250 }}
      />
      
      <FormControl sx={{ minWidth: 120 }}>
        <InputLabel>Status</InputLabel>
        <Select
          value={status}
          onChange={(e) => onStatusChange(e.target.value)}
          label="Status"
        >
          <MenuItem value="">All</MenuItem>
          <MenuItem value="OPEN">Open</MenuItem>
          <MenuItem value="RESOLVED">Resolved</MenuItem>
          <MenuItem value="IGNORED">Ignored</MenuItem>
        </Select>
      </FormControl>

      <FormControl sx={{ minWidth: 120 }}>
        <InputLabel>Severity</InputLabel>
        <Select
          value={severity}
          onChange={(e) => onSeverityChange(e.target.value)}
          label="Severity"
        >
          <MenuItem value="">All</MenuItem>
          <MenuItem value="CRITICAL">Critical</MenuItem>
          <MenuItem value="HIGH">High</MenuItem>
          <MenuItem value="MEDIUM">Medium</MenuItem>
          <MenuItem value="LOW">Low</MenuItem>
        </Select>
      </FormControl>

      <FormControl sx={{ minWidth: 120 }}>
        <InputLabel>Environment</InputLabel>
        <Select
          value={environment}
          onChange={(e) => onEnvironmentChange(e.target.value)}
          label="Environment"
        >
          <MenuItem value="">All</MenuItem>
          <MenuItem value="dev">Dev</MenuItem>
          <MenuItem value="staging">Staging</MenuItem>
          <MenuItem value="prod">Prod</MenuItem>
          <MenuItem value="test">Test</MenuItem>
        </Select>
      </FormControl>

      {hasActiveFilters && (
        <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
          <Button
            variant="outlined"
            size="small"
            startIcon={<ClearIcon />}
            onClick={onClearFilters}
          >
            Clear Filters
          </Button>
          <Box sx={{ display: 'flex', gap: 1 }}>
            {serviceName && <Chip label={`Service: ${serviceName}`} size="small" onDelete={() => onServiceNameChange('')} />}
            {status && <Chip label={`Status: ${status}`} size="small" onDelete={() => onStatusChange('')} />}
            {severity && <Chip label={`Severity: ${severity}`} size="small" onDelete={() => onSeverityChange('')} />}
            {environment && <Chip label={`Env: ${environment}`} size="small" onDelete={() => onEnvironmentChange('')} />}
          </Box>
        </Box>
      )}
    </Box>
  );
};
