import { Card, CardContent, Typography, Box, alpha } from '@mui/material'
import type { ReactNode } from 'react'

interface StatCardProps {
  title: string
  value: string | number
  icon?: ReactNode
  trend?: {
    value: number
    isPositive: boolean
  }
  subtitle?: string
  color?: 'primary' | 'success' | 'warning' | 'error' | 'info'
}

export default function StatCard({ 
  title, 
  value, 
  icon, 
  trend, 
  subtitle,
  color = 'primary' 
}: StatCardProps) {
  return (
    <Card sx={{ 
      border: 1, 
      borderColor: 'divider',
      boxShadow: 1,
      transition: 'box-shadow 0.2s',
      '&:hover': {
        boxShadow: 3
      }
    }}>
      <CardContent sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
          <Box sx={{ 
            p: 1.5, 
            borderRadius: 2,
            bgcolor: (theme) => alpha(theme.palette[color].main, 0.1),
            color: `${color}.main`,
            display: 'flex',
            alignItems: 'center'
          }}>
            {icon}
          </Box>
          {trend && (
            <Box sx={{ 
              display: 'flex', 
              alignItems: 'center',
              fontSize: '0.875rem',
              color: trend.isPositive ? 'success.main' : 'error.main',
              fontWeight: 500
            }}>
              <span style={{ marginRight: 4 }}>
                {trend.isPositive ? '↗' : '↘'}
              </span>
              {Math.abs(trend.value)}%
            </Box>
          )}
        </Box>
        
        <Typography variant="h4" sx={{ fontWeight: 700, color: 'text.primary', mb: 0.5 }}>
          {value}
        </Typography>
        
        <Typography variant="body2" sx={{ color: 'text.secondary', mb: 0.5, fontWeight: 500 }}>
          {title}
        </Typography>
        
        {subtitle && (
          <Typography variant="caption" sx={{ color: 'text.secondary' }}>
            {subtitle}
          </Typography>
        )}
      </CardContent>
    </Card>
  )
}
