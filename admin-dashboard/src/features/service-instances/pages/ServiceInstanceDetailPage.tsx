import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Box, Button, Alert } from '@mui/material';
import { ArrowBack as BackIcon } from '@mui/icons-material';
import { PageHeader } from '@components/common/PageHeader';
import { SkeletonLoader } from '@components/common/SkeletonLoader';
import { useFindByIdServiceInstance } from '@lib/api/hooks';
import { InstanceDetailCard } from '../components/InstanceDetailCard';

const ServiceInstanceDetailPage: React.FC = () => {
  const { serviceName, instanceId } = useParams<{ serviceName: string; instanceId: string }>();
  const navigate = useNavigate();
  
  const { data: instanceResponse, isLoading, error } = useFindByIdServiceInstance(
    serviceName!,
    instanceId!,
    {
      query: {
        enabled: !!serviceName && !!instanceId,
      },
    }
  );

  const instance = instanceResponse as any; // TODO: Fix API type generation

  const handleBack = () => {
    navigate('/instances');
  };

  if (isLoading) {
    return <SkeletonLoader variant="page" />;
  }

  if (error || !instance) {
    return (
      <Box>
        <PageHeader
          title="Service Instance Details"
          actions={
            <Button variant="outlined" startIcon={<BackIcon />} onClick={handleBack}>
              Back to Instances
            </Button>
          }
        />
        <Alert severity="error">
          Failed to load service instance. {error ? 'Please try again.' : 'Instance not found.'}
        </Alert>
      </Box>
    );
  }

  return (
    <Box>
      <PageHeader
        title={`Instance: ${instance.instanceId}`}
        actions={
          <Button variant="outlined" startIcon={<BackIcon />} onClick={handleBack}>
            Back to Instances
          </Button>
        }
      />
      
      <InstanceDetailCard instance={instance} />
    </Box>
  );
};

export default ServiceInstanceDetailPage;
