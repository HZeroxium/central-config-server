import React, { useState } from 'react';
import { Box, TextField, InputAdornment, Alert } from '@mui/material';
import { Search as SearchIcon } from '@mui/icons-material';
import { PageHeader } from '@components/common/PageHeader';
import { DataTable } from '@components/common/DataTable';
import { useGetIamTeamsQuery } from '../api';
import { usePermissions } from '@features/auth/hooks/usePermissions';
import { type GridColDef } from '@mui/x-data-grid';

const IamTeamListPage: React.FC = () => {
  const [page] = useState(0);
  const [pageSize] = useState(10);
  const [search, setSearch] = useState('');

  const { isSysAdmin } = usePermissions();

  const { data, isLoading } = useGetIamTeamsQuery({
    page,
    size: pageSize,
    search: search || undefined,
  });

  const columns: GridColDef[] = [
    { 
      field: 'name', 
      headerName: 'Team Name', 
      width: 200,
      renderCell: (params) => (
        <strong>{params.value}</strong>
      ),
    },
    { 
      field: 'description', 
      headerName: 'Description', 
      width: 300,
      renderCell: (params) => params.value || 'No description',
    },
    {
      field: 'members',
      headerName: 'Members',
      width: 120,
      renderCell: (params) => {
        const members = params.value || [];
        return `${members.length} member${members.length !== 1 ? 's' : ''}`;
      },
    },
    {
      field: 'ownerId',
      headerName: 'Owner ID',
      width: 200,
      renderCell: (params) => params.value || 'N/A',
    },
    {
      field: 'createdAt',
      headerName: 'Created At',
      width: 150,
      type: 'dateTime',
      valueFormatter: (value: any) => {
        if (!value) return '';
        return new Date(value).toLocaleDateString();
      },
    },
    {
      field: 'updatedAt',
      headerName: 'Updated At',
      width: 150,
      type: 'dateTime',
      valueFormatter: (value: any) => {
        if (!value) return '';
        return new Date(value).toLocaleDateString();
      },
    },
  ];

  if (!isSysAdmin) {
    return (
      <Box>
        <PageHeader title="IAM Teams" />
        <Alert severity="warning">
          You don't have permission to view IAM teams. This feature is only available to system administrators.
        </Alert>
      </Box>
    );
  }

  return (
    <Box>
      <PageHeader title="IAM Teams" />
      
      <Box sx={{ mb: 3, display: 'flex', gap: 2, flexWrap: 'wrap' }}>
        <TextField
          placeholder="Search by team name or description..."
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
      </Box>

      <DataTable 
        rows={data?.content || []} 
        columns={columns} 
        loading={isLoading} 
        getRowId={(row) => row.id}
        noRowsMessage="No teams found"
      />
    </Box>
  );
};

export default IamTeamListPage;
