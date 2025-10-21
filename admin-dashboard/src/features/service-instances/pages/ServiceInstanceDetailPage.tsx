import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Box, Button, Alert } from '@mui/material';
import { ArrowBack as BackIcon } from '@mui/icons-material';
import { PageHeader } from '@components/common/PageHeader';
import { SkeletonLoader } from '@components/common/SkeletonLoader';
import { useGetServiceInstanceByIdQuery } from '../api';
import { InstanceDetailCard } from '../components/InstanceDetailCard';

const ServiceInstanceDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  
  const { data: instance, isLoading, error } = useGetServiceInstanceByIdQuery(id!, {
    skip: !id,
  });

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
