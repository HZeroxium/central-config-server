import { Chip, Card, CardContent, Table, TableHead, TableRow, TableCell, TableBody } from '@mui/material'
import type { ServiceInstanceSummary } from '@lib/api/types'

function StatusChip({ status }: { status?: string }) {
  const color = status === 'HEALTHY' ? 'success' : status === 'DRIFT' ? 'warning' : status === 'UNHEALTHY' ? 'error' : 'default'
  return <Chip label={status || 'UNKNOWN'} color={color as any} size="small" />
}

export default function InstanceListTable({ instances }: { instances: ServiceInstanceSummary[] }) {
  return (
    <Card sx={{ border: 1, borderColor: 'divider', boxShadow: 1 }}>
      <CardContent sx={{ p: 0 }}>
        <Table>
          <TableHead>
            <TableRow sx={{ bgcolor: 'action.hover' }}>
              <TableCell sx={{ fontWeight: 600, color: 'text.primary' }}>Instance ID</TableCell>
              <TableCell sx={{ fontWeight: 600, color: 'text.primary' }}>Host</TableCell>
              <TableCell sx={{ fontWeight: 600, color: 'text.primary' }}>Port</TableCell>
              <TableCell sx={{ fontWeight: 600, color: 'text.primary' }}>Status</TableCell>
              <TableCell sx={{ fontWeight: 600, color: 'text.primary' }}>URI</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {instances.map((it) => (
              <TableRow 
                key={it.instanceId} 
                hover
                sx={{
                  transition: 'background-color 0.2s',
                  '&:hover': {
                    bgcolor: 'action.hover'
                  }
                }}
              >
                <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.875rem' }}>{it.instanceId}</TableCell>
                <TableCell>{it.host || '-'}</TableCell>
                <TableCell>{it.port || '-'}</TableCell>
                <TableCell><StatusChip status={it.status} /></TableCell>
                <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.875rem', color: 'text.secondary' }}>
                  {it.uri || '-'}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  )
}


