import React from 'react';
import { type GridColDef, GridActionsCellItem } from '@mui/x-data-grid';
import {
  Visibility as ViewIcon,
  CheckCircle as ResolveIcon,
} from '@mui/icons-material';
import { DataTable } from '@components/common/DataTable';
import { ChipStatus } from '@components/common/ChipStatus';
import { DriftSeverityChip } from './DriftSeverityChip';
import type { DriftEvent } from '../types';
import { usePermissions } from '@features/auth/hooks/usePermissions';

interface DriftEventTableProps {
  events: DriftEvent[];
  loading?: boolean;
  onView: (id: string) => void;
  onResolve: (event: DriftEvent) => void;
}

export const DriftEventTable: React.FC<DriftEventTableProps> = ({
  events,
  loading,
  onView,
  onResolve,
}) => {
  const { canViewDriftEvents } = usePermissions();

  const columns: GridColDef[] = [
    { 
      field: 'serviceName', 
      headerName: 'Service Name', 
      width: 200,
      renderCell: (params) => (
        <strong>{params.value}</strong>
      ),
    },
    { 
      field: 'instanceId', 
      headerName: 'Instance ID', 
      width: 150,
      renderCell: (params) => (
        <span style={{ fontFamily: 'monospace', fontSize: '0.8rem' }}>
          {params.value.substring(0, 8)}...
        </span>
      ),
    },
    { 
      field: 'environment', 
      headerName: 'Environment', 
      width: 120,
      renderCell: (params) => {
        const env = params.value;
        return env ? (
          <span style={{ textTransform: 'uppercase', fontWeight: 500 }}>
            {env}
          </span>
        ) : 'N/A';
      },
    },
    {
      field: 'severity',
      headerName: 'Severity',
      width: 120,
      renderCell: (params) => <DriftSeverityChip severity={params.value} />,
    },
    {
      field: 'status',
      headerName: 'Status',
      width: 120,
      renderCell: (params) => <ChipStatus status={params.value} />,
    },
    {
      field: 'detectedAt',
      headerName: 'Detected At',
      width: 150,
      type: 'dateTime',
      valueFormatter: (value: any) => {
        if (!value) return '';
        return new Date(value).toLocaleDateString();
      },
    },
    {
      field: 'resolvedAt',
      headerName: 'Resolved At',
      width: 150,
      type: 'dateTime',
      valueFormatter: (value: any) => {
        if (!value) return '';
        return new Date(value).toLocaleDateString();
      },
    },
    {
      field: 'actions',
      type: 'actions',
      headerName: 'Actions',
      width: 120,
      getActions: (params) => {
        const event = params.row as DriftEvent;
        const canResolve = canViewDriftEvents && event.status === 'OPEN';
        
        const actions = [
          <GridActionsCellItem
            key="view"
            icon={<ViewIcon />}
            label="View"
            onClick={() => onView(params.id as string)}
            showInMenu
          />,
        ];

        if (canResolve) {
          actions.push(
            <GridActionsCellItem
              key="resolve"
              icon={<ResolveIcon />}
              label="Resolve"
              onClick={() => onResolve(event)}
              showInMenu
            />
          );
        }

        return actions;
      },
    },
  ];

  return <DataTable rows={events} columns={columns} loading={loading} getRowId={(row) => row.id} />;
};
