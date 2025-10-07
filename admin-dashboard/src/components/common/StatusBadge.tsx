import { Chip } from '@mui/material'

interface StatusBadgeProps {
  status: string
  size?: 'small' | 'medium'
  className?: string
}

export default function StatusBadge({ status, size = 'small', className = '' }: StatusBadgeProps) {
  const getStatusConfig = (status: string) => {
    const normalizedStatus = status?.toLowerCase() || 'unknown'
    
    switch (normalizedStatus) {
      case 'healthy':
      case 'up':
      case 'running':
        return {
          color: 'success' as const,
          label: 'Healthy'
        }
      case 'unhealthy':
      case 'down':
      case 'stopped':
        return {
          color: 'error' as const,
          label: 'Unhealthy'
        }
      case 'drift':
      case 'warning':
        return {
          color: 'warning' as const,
          label: 'Drift'
        }
      case 'pending':
      case 'loading':
        return {
          color: 'info' as const,
          label: 'Pending'
        }
      default:
        return {
          color: 'default' as const,
          label: status || 'Unknown'
        }
    }
  }

  const config = getStatusConfig(status)

  return (
    <Chip
      label={config.label}
      size={size}
      color={config.color}
      sx={{ fontWeight: 500 }}
      className={className}
    />
  )
}
