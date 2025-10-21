import React, { useState } from 'react';
import { Box, Button, TextField, Alert } from '@mui/material';
import { Add as AddIcon } from '@mui/icons-material';
import { PageHeader } from '@components/common/PageHeader';
import { ApplicationServiceTable } from '../components/ApplicationServiceTable';
import { ApplicationServiceForm } from '../components/ApplicationServiceForm';
import { ConfirmDialog } from '@components/common/ConfirmDialog';
import {
  useGetApplicationServicesQuery,
  useCreateApplicationServiceMutation,
  useDeleteApplicationServiceMutation,
} from '../api';
import type { ApplicationService, CreateApplicationServiceFormData } from '../types';
import { usePermissions } from '@features/auth/hooks/usePermissions';

export const ApplicationServiceListPage: React.FC = () => {
  const [page] = useState(0);
  const [pageSize] = useState(10);
  const [search, setSearch] = useState('');
  const [formOpen, setFormOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [selectedService, setSelectedService] = useState<ApplicationService | null>(null);

  const { canEditService } = usePermissions();

  const {
    data: servicesData,
    isLoading,
    error,
  } = useGetApplicationServicesQuery({
    filter: { search },
    page,
    size: pageSize,
  });

  const [createService, { isLoading: createLoading }] = useCreateApplicationServiceMutation();
  const [deleteService, { isLoading: deleteLoading }] = useDeleteApplicationServiceMutation();

  const handleCreateService = async (data: CreateApplicationServiceFormData) => {
    try {
      await createService(data as any).unwrap();
      setFormOpen(false);
    } catch (error) {
      console.error('Failed to create service:', error);
    }
  };

  const handleDeleteService = async () => {
    if (!selectedService) return;
    
    try {
      await deleteService(selectedService.id).unwrap();
      setDeleteDialogOpen(false);
      setSelectedService(null);
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

  const services = servicesData?.content || [];

  return (
    <Box>
      <PageHeader
        title="Application Services"
        subtitle="Manage your application services and their configurations"
        actions={
          canEditService && (
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

      {error && (
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
        onEdit={canEditService ? handleEditService : undefined}
        onDelete={canEditService ? handleDeleteClick : undefined}
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
        loading={createLoading}
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
        loading={deleteLoading}
      />
    </Box>
  );
};

export default ApplicationServiceListPage;
