import { Breadcrumbs as MuiBreadcrumbs, Link, Typography } from '@mui/material'
import { useLocation, Link as RouterLink } from 'react-router-dom'
import { NavigateNext } from '@mui/icons-material'

interface BreadcrumbItem {
  label: string
  href?: string
}

export default function Breadcrumbs() {
  const location = useLocation()
  
  const generateBreadcrumbs = (): BreadcrumbItem[] => {
    const pathnames = location.pathname.split('/').filter((x) => x)
    const breadcrumbs: BreadcrumbItem[] = [
      { label: 'Dashboard', href: '/' }
    ]

    pathnames.forEach((name, index) => {
      const routeTo = `/${pathnames.slice(0, index + 1).join('/')}`
      const isLast = index === pathnames.length - 1
      
      // Convert route names to readable labels
      const label = name
        .split('-')
        .map(word => word.charAt(0).toUpperCase() + word.slice(1))
        .join(' ')
      
      if (isLast) {
        breadcrumbs.push({ label })
      } else {
        breadcrumbs.push({ label, href: routeTo })
      }
    })

    return breadcrumbs
  }

  const breadcrumbs = generateBreadcrumbs()

  return (
    <MuiBreadcrumbs
      separator={<NavigateNext fontSize="small" />}
      className="mb-4"
    >
      {breadcrumbs.map((breadcrumb, index) => {
        const isLast = index === breadcrumbs.length - 1
        
        if (isLast) {
          return (
            <Typography 
              key={breadcrumb.label}
              color="text.primary"
              className="font-medium"
            >
              {breadcrumb.label}
            </Typography>
          )
        }
        
        return (
          <Link
            key={breadcrumb.label}
            component={RouterLink}
            to={breadcrumb.href || '#'}
            sx={{
              color: 'text.secondary',
              textDecoration: 'none',
              transition: 'color 0.2s',
              '&:hover': {
                color: 'primary.main'
              }
            }}
          >
            {breadcrumb.label}
          </Link>
        )
      })}
    </MuiBreadcrumbs>
  )
}
