import React, { useState, useEffect } from 'react';
import {
  Drawer,
  Box,
  Typography,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  FormControlLabel,
  Checkbox,
  Button,
  Chip,
  Autocomplete,
  Divider,
} from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import { Person as PersonIcon, Group as GroupIcon } from '@mui/icons-material';
import { useFindAllApplicationServices } from '@lib/api/hooks';
import { useErrorHandler } from '../../../hooks/useErrorHandler';
import type { ServiceShare, CreateServiceShareRequest, Permission, Environment } from '../types';
import { PERMISSIONS, ENVIRONMENTS, PERMISSION_LABELS, ENVIRONMENT_LABELS } from '../types';

interface ShareFormDrawerProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (data: CreateServiceShareRequest) => void;
  share?: ServiceShare | null;
}

export const ShareFormDrawer: React.FC<ShareFormDrawerProps> = ({
  open,
  onClose,
  onSubmit,
  share,
}) => {
  const [formData, setFormData] = useState<CreateServiceShareRequest>({
    serviceId: '',
    grantedTo: 'USER',
    grantedToId: '',
    permissions: [],
    environments: [],
    expiresAt: undefined,
    note: '',
  });

  const [selectedPermissions, setSelectedPermissions] = useState<Permission[]>([]);
  const [selectedEnvironments, setSelectedEnvironments] = useState<Environment[]>([]);
  const [expiryDate, setExpiryDate] = useState<Date | null>(null);

  const { handleError } = useErrorHandler();

  // Fetch services for autocomplete
  const { data: servicesResponse } = useFindAllApplicationServices(
    { filter: undefined, pageable: { page: 0, size: 1000 } },
    { query: { enabled: open } }
  );

  const services = servicesResponse?.content || [];
  
  // Mock data for teams and users - in real implementation, these would come from API
  const teams = [
    { id: 'team1', name: 'Development Team', type: 'TEAM' as const },
    { id: 'team2', name: 'QA Team', type: 'TEAM' as const },
    { id: 'team3', name: 'DevOps Team', type: 'TEAM' as const },
  ];
  
  const users = [
    { id: 'user1', name: 'John Doe', type: 'USER' as const },
    { id: 'user2', name: 'Jane Smith', type: 'USER' as const },
    { id: 'user3', name: 'Bob Johnson', type: 'USER' as const },
  ];

  // Initialize form data when editing
  useEffect(() => {
    if (share) {
      setFormData({
        serviceId: share.serviceId,
        grantedTo: share.grantedTo,
        grantedToId: share.grantedToId,
        permissions: share.permissions as Permission[],
        environments: share.environments as Environment[],
        expiresAt: share.expiresAt,
        note: '',
      });
      setSelectedPermissions(share.permissions as Permission[]);
      setSelectedEnvironments(share.environments as Environment[]);
      setExpiryDate(share.expiresAt ? new Date(share.expiresAt) : null);
    } else {
      // Reset form for new share
      setFormData({
        serviceId: '',
        grantedTo: 'USER',
        grantedToId: '',
        permissions: [],
        environments: [],
        expiresAt: undefined,
        note: '',
      });
      setSelectedPermissions([]);
      setSelectedEnvironments([]);
      setExpiryDate(null);
    }
  }, [share, open]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!formData.serviceId || !formData.grantedToId || selectedPermissions.length === 0 || selectedEnvironments.length === 0) {
      handleError(new Error('Please fill in all required fields'));
      return;
    }

    onSubmit({
      ...formData,
      permissions: selectedPermissions,
      environments: selectedEnvironments,
      expiresAt: expiryDate?.toISOString(),
    });
  };

  const handleGrantedToChange = (value: 'TEAM' | 'USER') => {
    setFormData(prev => ({
      ...prev,
      grantedTo: value,
      grantedToId: '', // Reset selection when changing type
    }));
  };

  const getGrantedToOptions = () => {
    if (formData.grantedTo === 'TEAM') {
      return teams.map(team => ({
        id: team.id,
        name: team.name,
        type: 'TEAM' as const,
      }));
    } else {
      return users.map(user => ({
        id: user.id,
        name: `${user.firstName} ${user.lastName}`,
        type: 'USER' as const,
      }));
    }
  };

  const getGrantedToName = () => {
    const options = getGrantedToOptions();
    const selected = options.find(option => option.id === formData.grantedToId);
    return selected?.name || '';
  };

  return (
    <LocalizationProvider dateAdapter={AdapterDateFns}>
      <Drawer
        anchor="right"
        open={open}
        onClose={onClose}
        PaperProps={{
          sx: { width: 600, p: 3 }
        }}
      >
        <Box>
          <Typography variant="h6" gutterBottom>
            {share ? 'Edit Service Share' : 'Grant Service Access'}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            Configure access permissions for a service
          </Typography>

          <form onSubmit={handleSubmit}>
            {/* Service Selection */}
            <FormControl fullWidth sx={{ mb: 3 }}>
              <InputLabel>Service *</InputLabel>
              <Select
                value={formData.serviceId}
                onChange={(e) => setFormData(prev => ({ ...prev, serviceId: e.target.value }))}
                label="Service *"
                required
              >
                {services.map((service: any) => (
                  <MenuItem key={service.id} value={service.id}>
                    {service.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            {/* Grant Type */}
            <FormControl fullWidth sx={{ mb: 3 }}>
              <InputLabel>Grant To *</InputLabel>
              <Select
                value={formData.grantedTo}
                onChange={(e) => handleGrantedToChange(e.target.value as 'TEAM' | 'USER')}
                label="Grant To *"
                required
              >
                <MenuItem value="USER">
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <PersonIcon fontSize="small" />
                    User
                  </Box>
                </MenuItem>
                <MenuItem value="TEAM">
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <GroupIcon fontSize="small" />
                    Team
                  </Box>
                </MenuItem>
              </Select>
            </FormControl>

            {/* User/Team Selection */}
            <Autocomplete
              options={getGrantedToOptions()}
              getOptionLabel={(option) => option.name}
              value={getGrantedToOptions().find(option => option.id === formData.grantedToId) || null}
              onChange={(_, value) => setFormData(prev => ({ ...prev, grantedToId: value?.id || '' }))}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label={`Select ${formData.grantedTo.toLowerCase()}`}
                  required
                  sx={{ mb: 3 }}
                />
              )}
              renderOption={(props, option) => (
                <li {...props}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    {option.type === 'TEAM' ? (
                      <GroupIcon fontSize="small" color="primary" />
                    ) : (
                      <PersonIcon fontSize="small" color="action" />
                    )}
                    {option.name}
                  </Box>
                </li>
              )}
            />

            <Divider sx={{ my: 3 }} />

            {/* Permissions */}
            <Typography variant="subtitle1" gutterBottom>
              Permissions *
            </Typography>
            <Box sx={{ mb: 3 }}>
              {Object.entries(PERMISSIONS).map(([key, permission]) => (
                <FormControlLabel
                  key={key}
                  control={
                    <Checkbox
                      checked={selectedPermissions.includes(permission)}
                      onChange={(e) => {
                        if (e.target.checked) {
                          setSelectedPermissions(prev => [...prev, permission]);
                        } else {
                          setSelectedPermissions(prev => prev.filter(p => p !== permission));
                        }
                      }}
                    />
                  }
                  label={PERMISSION_LABELS[permission]}
                />
              ))}
            </Box>

            {/* Selected Permissions Display */}
            {selectedPermissions.length > 0 && (
              <Box sx={{ mb: 3 }}>
                <Typography variant="body2" color="text.secondary" gutterBottom>
                  Selected permissions:
                </Typography>
                <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
                  {selectedPermissions.map((permission) => (
                    <Chip
                      key={permission}
                      label={PERMISSION_LABELS[permission]}
                      size="small"
                      onDelete={() => setSelectedPermissions(prev => prev.filter(p => p !== permission))}
                    />
                  ))}
                </Box>
              </Box>
            )}

            <Divider sx={{ my: 3 }} />

            {/* Environments */}
            <Typography variant="subtitle1" gutterBottom>
              Environments *
            </Typography>
            <Box sx={{ mb: 3 }}>
              {Object.entries(ENVIRONMENTS).map(([key, environment]) => (
                <FormControlLabel
                  key={key}
                  control={
                    <Checkbox
                      checked={selectedEnvironments.includes(environment)}
                      onChange={(e) => {
                        if (e.target.checked) {
                          setSelectedEnvironments(prev => [...prev, environment]);
                        } else {
                          setSelectedEnvironments(prev => prev.filter(e => e !== environment));
                        }
                      }}
                    />
                  }
                  label={ENVIRONMENT_LABELS[environment]}
                />
              ))}
            </Box>

            {/* Selected Environments Display */}
            {selectedEnvironments.length > 0 && (
              <Box sx={{ mb: 3 }}>
                <Typography variant="body2" color="text.secondary" gutterBottom>
                  Selected environments:
                </Typography>
                <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
                  {selectedEnvironments.map((environment) => (
                    <Chip
                      key={environment}
                      label={ENVIRONMENT_LABELS[environment]}
                      size="small"
                      onDelete={() => setSelectedEnvironments(prev => prev.filter(e => e !== environment))}
                    />
                  ))}
                </Box>
              </Box>
            )}

            <Divider sx={{ my: 3 }} />

            {/* Expiry Date */}
            <Typography variant="subtitle1" gutterBottom>
              Expiry Date (Optional)
            </Typography>
            <DatePicker
              value={expiryDate}
              onChange={setExpiryDate}
              slotProps={{
                textField: {
                  fullWidth: true,
                  sx: { mb: 3 },
                  helperText: "Leave empty for no expiration"
                }
              }}
            />

            {/* Note */}
            <TextField
              label="Note (Optional)"
              multiline
              rows={3}
              value={formData.note}
              onChange={(e) => setFormData(prev => ({ ...prev, note: e.target.value }))}
              fullWidth
              sx={{ mb: 3 }}
              helperText="Add a note about why this access is being granted"
            />

            {/* Action Buttons */}
            <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
              <Button onClick={onClose} variant="outlined">
                Cancel
              </Button>
              <Button
                type="submit"
                variant="contained"
                disabled={!formData.serviceId || !formData.grantedToId || selectedPermissions.length === 0 || selectedEnvironments.length === 0}
              >
                {share ? 'Update Share' : 'Grant Access'}
              </Button>
            </Box>
          </form>
        </Box>
      </Drawer>
    </LocalizationProvider>
  );
};