import { Box, Skeleton } from '@mui/material'

interface SkeletonLoaderProps {
  variant?: 'text' | 'rectangular' | 'circular'
  width?: string | number
  height?: string | number
  lines?: number
  className?: string
}

export default function SkeletonLoader({ 
  variant = 'rectangular',
  width = '100%',
  height = 20,
  lines = 1,
  className = ''
}: SkeletonLoaderProps) {
  if (lines > 1) {
    return (
      <Box className={className}>
        {Array.from({ length: lines }).map((_, index) => (
          <Skeleton
            key={index}
            variant={variant}
            width={width}
            height={height}
            className="mb-2"
            animation="wave"
          />
        ))}
      </Box>
    )
  }

  return (
    <Skeleton
      variant={variant}
      width={width}
      height={height}
      className={className}
      animation="wave"
    />
  )
}
