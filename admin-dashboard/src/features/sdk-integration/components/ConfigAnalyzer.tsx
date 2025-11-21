/**
 * Component for visually analyzing configuration structure.
 */

import {
  Box,
  Card,
  CardContent,
  Typography,
  Chip,
  List,
  ListItem,
  ListItemText,
  Alert,
} from '@mui/material';
import type { ConfigAnalysisResponse } from '../types';

interface ConfigAnalyzerProps {
  analysisResult: ConfigAnalysisResponse | null;
}

export function ConfigAnalyzer({ analysisResult }: ConfigAnalyzerProps) {
  if (!analysisResult) {
    return null;
  }

  return (
    <Card>
      <CardContent>
        <Typography variant="h6" gutterBottom>
          Configuration Analysis
        </Typography>

        {/* Validation Results */}
        {analysisResult.validation && (
          <Box sx={{ mb: 3 }}>
            <Typography variant="subtitle2" gutterBottom>
              Validation Status: {analysisResult.validation.valid ? 'Valid' : 'Invalid'}
            </Typography>
            {analysisResult.validation.errors && analysisResult.validation.errors.length > 0 && (
              <Alert severity="error" sx={{ mt: 1 }}>
                <Typography variant="body2" component="strong">Errors:</Typography>
                <List dense>
                  {analysisResult.validation.errors.map((error, idx) => (
                    <ListItem key={idx}>
                      <ListItemText primary={error || 'Unknown error'} />
                    </ListItem>
                  ))}
                </List>
              </Alert>
            )}
            {analysisResult.validation.warnings && analysisResult.validation.warnings.length > 0 && (
              <Alert severity="warning" sx={{ mt: 1 }}>
                <Typography variant="body2" component="strong">Warnings:</Typography>
                <List dense>
                  {analysisResult.validation.warnings.map((warning, idx) => (
                    <ListItem key={idx}>
                      <ListItemText primary={warning || 'Unknown warning'} />
                    </ListItem>
                  ))}
                </List>
              </Alert>
            )}
          </Box>
        )}

        {/* Property Group Suggestions */}
        {analysisResult.propertyGroupSuggestions && analysisResult.propertyGroupSuggestions.length > 0 && (
          <Box sx={{ mb: 3 }}>
            <Typography variant="subtitle2" gutterBottom>
              Suggested Property Groups:
            </Typography>
            <List dense>
              {analysisResult.propertyGroupSuggestions.map((group, idx) => (
                <ListItem key={idx}>
                  <ListItemText
                    primary={
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <Typography variant="body2">{group.className || 'Unnamed Class'}</Typography>
                        <Chip label={group.prefix || 'N/A'} size="small" />
                        <Chip label={`${group.propertyCount || 0} properties`} size="small" variant="outlined" />
                      </Box>
                    }
                    secondary={`Properties: ${group.properties?.join(', ') || 'None'}`}
                  />
                </ListItem>
              ))}
            </List>
          </Box>
        )}

        {/* Linting Issues */}
        {analysisResult.lintingIssues && analysisResult.lintingIssues.length > 0 && (
          <Box>
            <Typography variant="subtitle2" gutterBottom>
              Linting Issues:
            </Typography>
            {analysisResult.lintingIssues.map((issue, idx) => (
              <Alert
                key={idx}
                severity={
                  issue.severity === 'ERROR' ? 'error' :
                  issue.severity === 'WARN' ? 'warning' : 'info'
                }
                sx={{ mt: 1 }}
              >
                <Typography variant="body2" component="strong">{issue.message || 'Unknown issue'}</Typography>
                {issue.suggestion && (
                  <Typography variant="body2" sx={{ mt: 0.5 }}>
                    Suggestion: {issue.suggestion}
                  </Typography>
                )}
              </Alert>
            ))}
          </Box>
        )}
      </CardContent>
    </Card>
  );
}

