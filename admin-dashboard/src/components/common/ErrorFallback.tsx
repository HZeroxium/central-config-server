import { Alert, AlertTitle, Button, Stack } from '@mui/material'

export default function ErrorFallback({ title = 'Something went wrong', message, onRetry }: { title?: string; message?: string; onRetry?: () => void }) {
  return (
    <Stack spacing={2}>
      <Alert severity="error">
        <AlertTitle>{title}</AlertTitle>
        {message || 'Please try again later.'}
      </Alert>
      {onRetry && (
        <Button variant="contained" color="primary" onClick={onRetry} sx={{ alignSelf: 'start' }}>
          Retry
        </Button>
      )}
    </Stack>
  )
}


