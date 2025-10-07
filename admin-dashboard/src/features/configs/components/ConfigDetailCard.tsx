import { Card, CardContent, Typography, Box, Button, Chip, IconButton, Tooltip, Accordion, AccordionSummary, AccordionDetails } from '@mui/material'
import { ContentCopy as Copy, Check, ExpandMore } from '@mui/icons-material'
import { useState } from 'react'
import { Highlight, themes } from 'prism-react-renderer'
import type { ConfigEnvironmentResponse } from '@lib/api/types'

export default function ConfigDetailCard({ env }: { env: ConfigEnvironmentResponse }) {
  const [copiedProperty, setCopiedProperty] = useState<string | null>(null)

  const copyToClipboard = async (text: string, propertyKey?: string) => {
    try {
      await navigator.clipboard.writeText(text)
      if (propertyKey) {
        setCopiedProperty(propertyKey)
        setTimeout(() => setCopiedProperty(null), 2000)
      }
    } catch (err) {
      console.error('Failed to copy: ', err)
    }
  }

  const renderPropertyValue = (value: any, key: string) => {
    const jsonString = JSON.stringify(value, null, 2)
    const isCopied = copiedProperty === key

    return (
      <Box className="relative group">
        <Highlight
          theme={themes.vsDark}
          code={jsonString}
          language="json"
        >
          {({ className, style, tokens, getLineProps, getTokenProps }) => (
            <pre className={`${className} p-4 rounded-lg overflow-x-auto text-sm`} style={style}>
              {tokens.map((line, i) => (
                <div key={i} {...getLineProps({ line })}>
                  {line.map((token, key) => (
                    <span key={key} {...getTokenProps({ token })} />
                  ))}
                </div>
              ))}
            </pre>
          )}
        </Highlight>
        
        <Tooltip title={isCopied ? "Copied!" : "Copy to clipboard"}>
          <IconButton
            size="small"
            sx={{
              position: 'absolute',
              top: 8,
              right: 8,
              opacity: 0,
              transition: 'opacity 0.2s',
              bgcolor: 'rgba(0,0,0,0.7)',
              '&:hover': {
                bgcolor: 'rgba(0,0,0,0.85)'
              },
              '.group:hover &': {
                opacity: 1
              }
            }}
            onClick={() => copyToClipboard(jsonString, key)}
          >
            {isCopied ? <Check sx={{ color: 'success.main' }} /> : <Copy sx={{ color: 'grey.300' }} />}
          </IconButton>
        </Tooltip>
      </Box>
    )
  }

  return (
    <Box className="space-y-6">
      <Card className="card">
        <CardContent>
          <Box className="flex items-center justify-between mb-4">
            <Typography variant="h6" className="font-semibold">
              Configuration: {env.name}
            </Typography>
            <Button
              size="small"
              startIcon={copiedProperty === 'full-config' ? <Check /> : <Copy />}
              onClick={() => copyToClipboard(JSON.stringify(env, null, 2), 'full-config')}
              sx={{
                color: 'text.secondary',
                '&:hover': {
                  color: 'text.primary'
                }
              }}
            >
              {copiedProperty === 'full-config' ? 'Copied!' : 'Copy All'}
            </Button>
          </Box>
          
          <Box className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Box>
              <Typography variant="body2" sx={{ color: 'text.secondary', mb: 1 }}>Profiles</Typography>
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                {env.profiles?.map((profile) => (
                  <Chip key={profile} label={profile} size="small" color="primary" />
                )) || <Chip label="default" size="small" />}
              </Box>
            </Box>
            
            <Box>
              <Typography variant="body2" sx={{ color: 'text.secondary', mb: 1 }}>Label</Typography>
              <Typography variant="body1" sx={{ fontWeight: 500, color: 'text.primary' }}>{env.label || 'default'}</Typography>
            </Box>
            
            <Box>
              <Typography variant="body2" sx={{ color: 'text.secondary', mb: 1 }}>Version</Typography>
              <Typography variant="body1" sx={{ fontWeight: 500, color: 'text.primary' }}>{env.version || '-'}</Typography>
            </Box>
            
            <Box>
              <Typography variant="body2" sx={{ color: 'text.secondary', mb: 1 }}>State</Typography>
              <Chip 
                label={env.state || 'unknown'} 
                size="small" 
                color={env.state === 'success' ? 'success' : 'default'}
              />
            </Box>
          </Box>
        </CardContent>
      </Card>

      <Box>
        <Typography variant="h6" className="font-semibold mb-4">
          Property Sources
        </Typography>
        
        <Box className="space-y-4">
          {env.propertySources?.map((ps, index) => (
            <Accordion key={ps.name} className="card">
              <AccordionSummary expandIcon={<ExpandMore />}>
                <Box className="flex items-center justify-between w-full mr-4">
                  <Typography variant="subtitle1" className="font-medium">
                    {ps.name}
                  </Typography>
                  <Button
                    size="small"
                    startIcon={copiedProperty === `source-${index}` ? <Check /> : <Copy />}
                    onClick={(e) => {
                      e.stopPropagation()
                      copyToClipboard(JSON.stringify(ps.source, null, 2), `source-${index}`)
                    }}
                    sx={{
                      color: 'text.secondary',
                      '&:hover': {
                        color: 'text.primary'
                      }
                    }}
                  >
                    {copiedProperty === `source-${index}` ? 'Copied!' : 'Copy All'}
                  </Button>
                </Box>
              </AccordionSummary>
              <AccordionDetails>
                <Box className="space-y-2">
                  {Object.entries(ps.source || {}).map(([key, value]) => (
                    <Box key={key} sx={{ border: 1, borderColor: 'divider', borderRadius: 2, overflow: 'hidden' }}>
                      <Box sx={{ bgcolor: 'action.hover', px: 2, py: 1.5, borderBottom: 1, borderColor: 'divider' }}>
                        <Typography variant="body2" sx={{ fontWeight: 500, color: 'text.primary' }}>
                          {key}
                        </Typography>
                      </Box>
                      <Box sx={{ position: 'relative' }}>
                        {renderPropertyValue(value, `${ps.name}-${key}`)}
                      </Box>
                    </Box>
                  ))}
                </Box>
              </AccordionDetails>
            </Accordion>
          ))}
        </Box>
      </Box>
    </Box>
  )
}