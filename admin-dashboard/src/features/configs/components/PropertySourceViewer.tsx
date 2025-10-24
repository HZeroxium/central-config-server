import { Box, Typography, TextField, InputAdornment, Chip, IconButton, Tooltip, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper } from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import CheckIcon from '@mui/icons-material/Check';
import { useState, useMemo } from 'react';

interface PropertySource {
  name?: string;
  source?: Record<string, unknown>;
}

interface PropertySourceViewerProps {
  propertySource: PropertySource;
}

export default function PropertySourceViewer({ propertySource }: PropertySourceViewerProps) {
  const [searchTerm, setSearchTerm] = useState('');
  const [copiedKey, setCopiedKey] = useState<string | null>(null);

  const properties = useMemo(() => {
    if (!propertySource.source) return [];
    return Object.entries(propertySource.source);
  }, [propertySource.source]);

  const filteredProperties = useMemo(() => {
    if (!searchTerm) return properties;
    return properties.filter(([key, value]) => 
      key.toLowerCase().includes(searchTerm.toLowerCase()) ||
      String(value).toLowerCase().includes(searchTerm.toLowerCase())
    );
  }, [properties, searchTerm]);

  const handleCopy = async (key: string, value: unknown) => {
    try {
      await navigator.clipboard.writeText(`${key}=${String(value)}`);
      setCopiedKey(key);
      setTimeout(() => setCopiedKey(null), 2000);
    } catch (err) {
      console.error('Failed to copy:', err);
    }
  };

  const getValueType = (value: unknown): string => {
    if (value === null) return 'null';
    if (Array.isArray(value)) return 'array';
    return typeof value;
  };

  const getValueColor = (type: string): 'primary' | 'secondary' | 'success' | 'error' | 'warning' | 'info' => {
    switch (type) {
      case 'string': return 'primary';
      case 'number': return 'success';
      case 'boolean': return 'warning';
      case 'object': return 'secondary';
      case 'array': return 'info';
      default: return 'error';
    }
  };

  return (
    <Box>
      <Box sx={{ mb: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="body2" color="text.secondary">
          {filteredProperties.length} {filteredProperties.length === 1 ? 'property' : 'properties'}
        </Typography>
        <TextField
          placeholder="Search properties..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          size="small"
          sx={{ minWidth: 300 }}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon />
              </InputAdornment>
            ),
          }}
        />
      </Box>

      <TableContainer component={Paper} variant="outlined">
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell sx={{ fontWeight: 'bold' }}>Property Key</TableCell>
              <TableCell sx={{ fontWeight: 'bold' }}>Value</TableCell>
              <TableCell sx={{ fontWeight: 'bold', width: 100 }}>Type</TableCell>
              <TableCell sx={{ fontWeight: 'bold', width: 80 }}>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {filteredProperties.length === 0 ? (
              <TableRow>
                <TableCell colSpan={4} align="center" sx={{ py: 3 }}>
                  <Typography variant="body2" color="text.secondary">
                    {searchTerm ? 'No properties match your search' : 'No properties found'}
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              filteredProperties.map(([key, value]) => {
                const valueType = getValueType(value);
                const displayValue = valueType === 'object' || valueType === 'array' 
                  ? JSON.stringify(value, null, 2) 
                  : String(value);

                return (
                  <TableRow key={key} hover>
                    <TableCell>
                      <Typography variant="body2" sx={{ fontFamily: 'monospace', wordBreak: 'break-word' }}>
                        {key}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Typography 
                        variant="body2" 
                        sx={{ 
                          fontFamily: 'monospace', 
                          maxWidth: 400,
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          whiteSpace: valueType === 'object' || valueType === 'array' ? 'pre-wrap' : 'nowrap'
                        }}
                      >
                        {displayValue}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Chip 
                        label={valueType} 
                        size="small" 
                        color={getValueColor(valueType)}
                      />
                    </TableCell>
                    <TableCell>
                      <Tooltip title={copiedKey === key ? 'Copied!' : 'Copy property'}>
                        <IconButton 
                          size="small" 
                          onClick={() => handleCopy(key, value)}
                          color={copiedKey === key ? 'success' : 'default'}
                        >
                          {copiedKey === key ? <CheckIcon fontSize="small" /> : <ContentCopyIcon fontSize="small" />}
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                );
              })
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );
}

