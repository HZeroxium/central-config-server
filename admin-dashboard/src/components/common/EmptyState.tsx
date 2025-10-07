import { Box, Typography, Button } from '@mui/material'
import type { ReactNode } from 'react'

interface EmptyStateProps {
  icon?: ReactNode
  title: string
  description?: string
  action?: {
    label: string
    onClick: () => void
  }
  className?: string
}

export default function EmptyState({ 
  icon, 
  title, 
  description, 
  action,
  className = ''
}: EmptyStateProps) {
  return (
    <Box sx={{ 
      display: 'flex', 
      flexDirection: 'column', 
      alignItems: 'center', 
      justifyContent: 'center', 
      py: 12, 
      px: 6, 
      textAlign: 'center' 
    }} className={className}>
      {icon && (
        <Box sx={{ mb: 4, color: 'text.disabled' }}>
          {icon}
        </Box>
      )}
      
      <Typography variant="h6" sx={{ color: 'text.primary', mb: 2, fontWeight: 500 }}>
        {title}
      </Typography>
      
      {description && (
        <Typography variant="body2" sx={{ color: 'text.secondary', mb: 6, maxWidth: 500 }}>
          {description}
        </Typography>
      )}
      
      {action && (
        <Button 
          variant="contained" 
          color="primary"
          onClick={action.onClick}
        >
          {action.label}
        </Button>
      )}
    </Box>
  )
}
