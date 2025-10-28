import React from "react";
import { useNavigate } from "react-router-dom";
import {
  Avatar,
  IconButton,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
  Divider,
  Typography,
} from "@mui/material";
import {
  Person as PersonIcon,
  Settings as SettingsIcon,
  Logout as LogoutIcon,
} from "@mui/icons-material";
import { useAuth } from "@features/auth/context";

export const UserMenu: React.FC = () => {
  const navigate = useNavigate();
  const { logout, userInfo } = useAuth();
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const handleLogout = () => {
    handleMenuClose();
    logout();
  };

  const handleProfile = () => {
    handleMenuClose();
    navigate("/profile");
  };

  const handleSettings = () => {
    handleMenuClose();
    // TODO: Navigate to settings page when implemented
  };

  if (!userInfo) return null;

  const getInitials = (firstName: string, lastName: string) => {
    return `${firstName?.charAt(0) || ""}${
      lastName?.charAt(0) || ""
    }`.toUpperCase();
  };

  return (
    <>
      <IconButton onClick={handleMenuOpen} size="small" sx={{ ml: 2 }}>
        <Avatar sx={{ width: 32, height: 32, bgcolor: "primary.main" }}>
          {getInitials(userInfo.firstName, userInfo.lastName)}
        </Avatar>
      </IconButton>
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleMenuClose}
        anchorOrigin={{
          vertical: "bottom",
          horizontal: "right",
        }}
        transformOrigin={{
          vertical: "top",
          horizontal: "right",
        }}
      >
        <MenuItem disabled>
          <ListItemText
            primary={
              <Typography variant="subtitle2" fontWeight={600}>
                {userInfo.firstName} {userInfo.lastName}
              </Typography>
            }
            secondary={
              <Typography variant="caption" color="text.secondary">
                {userInfo.email}
              </Typography>
            }
          />
        </MenuItem>
        <Divider />
        <MenuItem onClick={handleProfile}>
          <ListItemIcon>
            <PersonIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Profile</ListItemText>
        </MenuItem>
        <MenuItem onClick={handleSettings}>
          <ListItemIcon>
            <SettingsIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Settings</ListItemText>
        </MenuItem>
        <Divider />
        <MenuItem onClick={handleLogout}>
          <ListItemIcon>
            <LogoutIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Logout</ListItemText>
        </MenuItem>
      </Menu>
    </>
  );
};

export default UserMenu;
