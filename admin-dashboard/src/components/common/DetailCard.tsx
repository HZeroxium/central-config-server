import React from 'react';
import { Card, CardContent, CardHeader, Typography, Box, Divider, type SxProps, type Theme } from '@mui/material';

interface DetailCardProps {
  title?: string;
  subtitle?: string;
  children: React.ReactNode;
  actions?: React.ReactNode;
  sx?: SxProps<Theme>;
}

export const DetailCard: React.FC<DetailCardProps> = ({
  title,
  subtitle,
  children,
  actions,
  sx = {},
}) => {
  return (
    <Card sx={{ height: '100%', ...sx }}>
      {(title || actions) && (
        <CardHeader
          title={title && <Typography variant="h6" fontWeight={600}>{title}</Typography>}
          subheader={subtitle && <Typography variant="body2" color="text.secondary">{subtitle}</Typography>}
          action={actions}
          sx={{
            pb: 1,
            '& .MuiCardHeader-content': {
              minWidth: 0,
            },
          }}
        />
      )}
      {(title || actions) && <Divider />}
      <CardContent sx={{ pt: title || actions ? 2 : 3 }}>
        <Box>{children}</Box>
      </CardContent>
    </Card>
  );
};

export default DetailCard;
