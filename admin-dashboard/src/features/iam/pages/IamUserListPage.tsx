import React, { useState } from 'react';
import { Box, TextField, InputAdornment, FormControl, InputLabel, Select, MenuItem, Alert } from '@mui/material';
import { Search as SearchIcon } from '@mui/icons-material';
import { PageHeader } from '@components/common/PageHeader';
import { DataTable } from '@components/common/DataTable';
import { ChipStatus } from '@components/common/ChipStatus';
import { useGetIamUsersQuery } from '../api';
import { usePermissions } from '@features/auth/hooks/usePermissions';
import { type GridColDef } from '@mui/x-data-grid';

const IamUserListPage: React.FC = () => {
  const [page] = useState(0);
  const [pageSize] = useState(10);
  const [search, setSearch] = useState('');
  const [enabledFilter, setEnabledFilter] = useState('');

  const { isSysAdmin } = usePermissions();

  const { data, isLoading } = useGetIamUsersQuery({
    page,
    size: pageSize,
    search: search || undefined,
    enabled: enabledFilter === 'enabled' ? true : enabledFilter === 'disabled' ? false : undefined,
  });

  const columns: GridColDef[] = [
    { 
      field: 'username', 
      headerName: 'Username', 
      width: 200,
      renderCell: (params) => (
        <strong>{params.value}</strong>
      ),
    },
    { 
      field: 'email', 
      headerName: 'Email', 
      width: 250,
    },
    { 
      field: 'firstName', 
      headerName: 'First Name', 
      width: 150,
      renderCell: (params) => params.value || 'N/A',
    },
    { 
      field: 'lastName', 
      headerName: 'Last Name', 
      width: 150,
      renderCell: (params) => params.value || 'N/A',
    },
    {
      field: 'enabled',
      headerName: 'Status',
      width: 120,
      renderCell: (params) => <ChipStatus status={params.value ? 'ACTIVE' : 'INACTIVE'} />,
    },
    {
      field: 'emailVerified',
      headerName: 'Email Verified',
      width: 140,
      renderCell: (params) => <ChipStatus status={params.value ? 'VERIFIED' : 'UNVERIFIED'} />,
    },
    {
      field: 'roles',
      headerName: 'Roles',
      width: 200,
      renderCell: (params) => {
        const roles = params.value || [];
        return (
          <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
            {roles.map((role: string) => (
              <ChipStatus key={role} status={role} />
            ))}
          </div>
        );
      },
    },
    {
      field: 'lastLoginAt',
      headerName: 'Last Login',
      width: 150,
      type: 'dateTime',
      valueFormatter: (value: any) => {
        if (!value) return 'Never';
        return new Date(value).toLocaleDateString();
      },
    },
  ];

  if (!isSysAdmin) {
    return (
      <Box>
        <PageHeader title="IAM Users" />
        <Alert severity="warning">
          You don't have permission to view IAM users. This feature is only available to system administrators.
        </Alert>
      </Box>
    );
  }

  return (
    <Box>
      <PageHeader title="IAM Users" />
      
      <Box sx={{ mb: 3, display: 'flex', gap: 2, flexWrap: 'wrap' }}>
        <TextField
          placeholder="Search by username or email..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon />
              </InputAdornment>
            ),
          }}
          sx={{ minWidth: 300 }}
        />
        
        <FormControl sx={{ minWidth: 120 }}>
          <InputLabel>Status</InputLabel>
          <Select
            value={enabledFilter}
            onChange={(e) => setEnabledFilter(e.target.value)}
            label="Status"
          >
            <MenuItem value="">All</MenuItem>
            <MenuItem value="enabled">Enabled</MenuItem>
            <MenuItem value="disabled">Disabled</MenuItem>
          </Select>
        </FormControl>
      </Box>

      <DataTable 
        rows={data?.content || []} 
        columns={columns} 
        loading={isLoading} 
        getRowId={(row) => row.id}
        noRowsMessage="No users found"
      />
    </Box>
  );
};

export default IamUserListPage;
