import { Card, CardContent, Typography, Table, TableHead, TableRow, TableCell, TableBody, Chip } from '@mui/material'
import { useMemo } from 'react'
import StatusBadge from '@components/common/StatusBadge'
import { useListAllInstancesQuery } from '@features/services/api'

export default function ServiceListTable({ services, onSelect }: { services: Record<string, string[]>; onSelect: (name: string) => void }) {
  const { data: instanceCounts } = useListAllInstancesQuery()
  const rows = useMemo(() => Object.keys(services).sort(), [services])
  
  return (
    <Card sx={{ border: 1, borderColor: 'divider', boxShadow: 1 }}>
      <CardContent sx={{ p: 0 }}>
        <Table>
          <TableHead>
            <TableRow sx={{ bgcolor: 'action.hover' }}>
              <TableCell sx={{ fontWeight: 600, color: 'text.primary' }}>Service Name</TableCell>
              <TableCell sx={{ fontWeight: 600, color: 'text.primary' }}>Instances</TableCell>
              <TableCell sx={{ fontWeight: 600, color: 'text.primary' }}>Status</TableCell>
              <TableCell sx={{ fontWeight: 600, color: 'text.primary' }}>Tags</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((name) => {
              const instanceCount = instanceCounts?.[name] || 0
              const isHealthy = instanceCount > 0
              
              return (
                <TableRow 
                  key={name} 
                  hover 
                  sx={{ 
                    cursor: 'pointer',
                    transition: 'background-color 0.2s',
                    '&:hover': {
                      bgcolor: 'action.hover'
                    }
                  }}
                  onClick={() => onSelect(name)}
                >
                  <TableCell>
                    <Typography sx={{ 
                      color: 'primary.main',
                      fontWeight: 500,
                      transition: 'color 0.2s',
                      '&:hover': {
                        color: 'primary.dark'
                      }
                    }}>
                      {name}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Chip 
                      label={instanceCount}
                      size="small"
                      color={instanceCount > 0 ? 'primary' : 'default'}
                      sx={{ fontWeight: 500 }}
                    />
                  </TableCell>
                  <TableCell>
                    <StatusBadge 
                      status={isHealthy ? 'healthy' : 'unhealthy'} 
                      size="small"
                    />
                  </TableCell>
                  <TableCell>
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 4 }}>
                      {services[name]?.slice(0, 3).map((tag, index) => (
                        <Chip
                          key={index}
                          label={tag}
                          size="small"
                          variant="outlined"
                          sx={{ fontSize: '0.75rem' }}
                        />
                      ))}
                      {services[name]?.length > 3 && (
                        <Chip
                          label={`+${services[name].length - 3} more`}
                          size="small"
                          variant="outlined"
                          sx={{ fontSize: '0.75rem', color: 'text.secondary' }}
                        />
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              )
            })}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  )
}


