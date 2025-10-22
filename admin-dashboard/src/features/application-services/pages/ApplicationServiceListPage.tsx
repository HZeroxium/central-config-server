import React, { useState } from 'react';
import { Box, Button, TextField, Alert } from '@mui/material';
import { Add as AddIcon } from '@mui/icons-material';
import { PageHeader } from '@components/common/PageHeader';
import { ApplicationServiceTable } from '../components/ApplicationServiceTable';
import { ApplicationServiceForm } from '../components/ApplicationServiceForm';
import { ConfirmDialog } from '@components/common/ConfirmDialog';
import {
  useFindAllApplicationServices,
  useCreateApplicationService,
  useDeleteApplicationService,
} from '@lib/api/hooks';
import type { ApplicationService, CreateApplicationServiceFormData } from '../types';
import { usePermissions } from '@features/auth/hooks/usePermissions';

export const ApplicationServiceListPage: React.FC = () => {
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [search, setSearch] = useState('');
  const [formOpen, setFormOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [selectedService, setSelectedService] = useState<ApplicationService | null>(null);

  const { isSysAdmin } = usePermissions();

  const {
    data: servicesResponse,
    isLoading,
    error,
    refetch,
  } = useFindAllApplicationServices(
    {
      filter: search ? { search } : undefined,
      pageable: { page, size: pageSize },
    },
    {
      query: {
        staleTime: 30000, // 30 seconds
      },
    }
  );

  const createServiceMutation = useCreateApplicationService();
  const deleteServiceMutation = useDeleteApplicationService();

  // Get the page data from the API response
  const pageData = servicesResponse;
  const services = (pageData?.content || []) as ApplicationService[];
  const totalElements = pageData?.totalElements || 0;
  const totalPages = pageData?.totalPages || 0;

  const handleCreateService = async (data: CreateApplicationServiceFormData) => {
    try {
      await createServiceMutation.mutateAsync({ data: data });
      setFormOpen(false);
      refetch();
    } catch (error) {
      console.error('Failed to create service:', error);
    }
  };

  const handleDeleteService = async () => {
    if (!selectedService) return;
    
    try {
      await deleteServiceMutation.mutateAsync({ id: selectedService.id });
      setDeleteDialogOpen(false);
      setSelectedService(null);
      refetch();
    } catch (error) {
      console.error('Failed to delete service:', error);
    }
  };

  const handleViewService = (service: ApplicationService) => {
    // Navigate to service detail page
    console.log('View service:', service.id);
  };

  const handleEditService = (service: ApplicationService) => {
    setSelectedService(service);
    setFormOpen(true);
  };

  const handleDeleteClick = (service: ApplicationService) => {
    setSelectedService(service);
    setDeleteDialogOpen(true);
  };

  const handleShareService = (service: ApplicationService) => {
    // Navigate to service detail page with share tab
    console.log('Share service:', service.id);
  };


  return (
    <Box>
      <PageHeader
        title="Application Services"
        subtitle="Manage your application services and their configurations"
        actions={
          isSysAdmin && (
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => setFormOpen(true)}
            >
              Create Service
            </Button>
          )
        }
      />

      {(error as any) && (
        <Alert severity="error" sx={{ mb: 3 }}>
          Failed to load application services. Please try again.
        </Alert>
      )}

      <Box sx={{ mb: 3 }}>
        <TextField
          fullWidth
          placeholder="Search services by name, ID, or team..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          sx={{ maxWidth: 400 }}
        />
      </Box>

      <ApplicationServiceTable
        services={services}
        loading={isLoading}
        onView={handleViewService}
        onEdit={handleEditService}
        onDelete={handleDeleteClick}
        onShare={handleShareService}
      />

      {/* Create/Edit Form */}
      <ApplicationServiceForm
        open={formOpen}
        mode={selectedService ? 'edit' : 'create'}
        initialData={selectedService as any}
        onSubmit={handleCreateService as any}
        onClose={() => {
          setFormOpen(false);
          setSelectedService(null);
        }}
        loading={createServiceMutation.isPending}
      />

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        open={deleteDialogOpen}
        title="Delete Application Service"
        message={`Are you sure you want to delete "${selectedService?.displayName}"? This action cannot be undone.`}
        confirmText="Delete"
        confirmColor="error"
        onConfirm={handleDeleteService}
        onCancel={() => {
          setDeleteDialogOpen(false);
          setSelectedService(null);
        }}
        loading={deleteServiceMutation.isPending}
      />
    </Box>
  );
};

export default ApplicationServiceListPage;
