import { Card, CardContent, Stack, TextField, Button, Typography, Box} from '@mui/material'
import Grid from '@mui/material/Grid'
import { useState, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { History, Search } from '@mui/icons-material'

export default function ConfigListPage() {
  const [application, setApplication] = useState('sample-service')
  const [profile, setProfile] = useState('dev')
  const [label, setLabel] = useState('')
  const navigate = useNavigate()

  // Mock recent searches - in real app this would come from localStorage or API
  const recentSearches = useMemo(() => [
    { application: 'user-service', profile: 'prod', label: 'v1.2.3' },
    { application: 'config-server', profile: 'dev', label: '' },
    { application: 'gateway-service', profile: 'staging', label: 'latest' },
    { application: 'sample-service', profile: 'dev', label: '' },
  ], [])

  const go = () => {
    const path = `/configs/${encodeURIComponent(application)}/${encodeURIComponent(profile)}`
    const search = label ? `?label=${encodeURIComponent(label)}` : ''
    navigate(path + search)
  }

  const handleRecentSearch = (search: typeof recentSearches[0]) => {
    setApplication(search.application)
    setProfile(search.profile)
    setLabel(search.label)
  }

  return (
    <Box sx={{ p: { xs: 2, sm: 3, md: 4 } }}>
      <Typography variant="h4" sx={{ mb: { xs: 3, sm: 4 } }}>
        Configuration Explorer
      </Typography>
      
      <Grid container spacing={{ xs: 2, sm: 3 }}>
        <Grid size={{ xs: 12, lg: 8 }}>
          <Card sx={{ mb: { xs: 2, sm: 3 } }}>
            <CardContent sx={{ p: { xs: 2, sm: 3 } }}>
              <Typography variant="h6" sx={{ mb: 3, display: 'flex', alignItems: 'center' }}>
                <Search sx={{ mr: 1 }} />
                Search Configuration
              </Typography>
              
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                <TextField 
                  label="Application" 
                  size="small" 
                  value={application} 
                  onChange={(e) => setApplication(e.target.value)}
                  sx={{ flex: 1 }}
                  placeholder="e.g., sample-service"
                />
                <TextField 
                  label="Profile" 
                  size="small" 
                  value={profile} 
                  onChange={(e) => setProfile(e.target.value)}
                  sx={{ flex: 1 }}
                  placeholder="e.g., dev"
                />
                <TextField 
                  label="Label (optional)" 
                  size="small" 
                  value={label} 
                  onChange={(e) => setLabel(e.target.value)}
                  sx={{ flex: 1 }}
                  placeholder="e.g., v1.2.3"
                />
                <Button variant="contained" onClick={go} className="btn-primary">
                  View Config
                </Button>
              </Stack>
            </CardContent>
          </Card>
        </Grid>
        
        <Grid size={{ xs: 12, lg: 4 }}>
          <Card>
            <CardContent sx={{ p: { xs: 2, sm: 3 } }}>
              <Typography variant="h6" sx={{ mb: 3, display: 'flex', alignItems: 'center' }}>
                <History sx={{ mr: 1 }} />
                Recent Searches
              </Typography>
              
              <Stack spacing={2}>
                {recentSearches.map((search, index) => (
                  <Box 
                    key={index}
                    sx={{ 
                      p: 2, 
                      border: 1, 
                      borderColor: 'divider', 
                      borderRadius: 1,
                      cursor: 'pointer',
                      transition: 'all 0.2s',
                      '&:hover': {
                        bgcolor: 'action.hover',
                        borderColor: 'primary.main'
                      }
                    }}
                    onClick={() => handleRecentSearch(search)}
                  >
                    <Typography variant="body2" sx={{ fontWeight: 500, mb: 0.5 }}>
                      {search.application}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      Profile: {search.profile}
                      {search.label && ` • Label: ${search.label}`}
                    </Typography>
                  </Box>
                ))}
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
      
      {/* <Box sx={{ mt: { xs: 3, sm: 4 } }}>
        <Grid container spacing={{ xs: 2, sm: 3 }}>
          <Grid size={{ xs: 12, sm: 6 }}>
            <Card>
              <CardContent sx={{ p: { xs: 2, sm: 3 } }}>
                <Typography variant="h6" sx={{ mb: 3, fontWeight: 600 }}>
                  Quick Applications
                </Typography>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                  {quickApplications.map((app) => (
                    <Chip
                      key={app}
                      label={app}
                      size="small"
                      variant="outlined"
                      onClick={() => setApplication(app)}
                      sx={{
                        cursor: 'pointer',
                        transition: 'all 0.2s',
                        '&:hover': {
                          bgcolor: 'primary.50',
                          borderColor: 'primary.300'
                        }
                      }}
                    />
                  ))}
                </Box>
              </CardContent>
            </Card>
          </Grid>
          
          <Grid size={{ xs: 12, sm: 6 }}>
            <Card>
              <CardContent sx={{ p: { xs: 2, sm: 3 } }}>
                <Typography variant="h6" sx={{ mb: 3, fontWeight: 600 }}>
                  Quick Profiles
                </Typography>
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                  {quickProfiles.map((prof) => (
                    <Chip
                      key={prof}
                      label={prof}
                      size="small"
                      variant="outlined"
                      onClick={() => setProfile(prof)}
                      sx={{
                        cursor: 'pointer',
                        transition: 'all 0.2s',
                        '&:hover': {
                          bgcolor: 'primary.50',
                          borderColor: 'primary.300'
                        }
                      }}
                    />
                  ))}
                </Box>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </Box> */}
    </Box>
  )
}