import React from "react";
import { Box, Typography, Avatar, Skeleton } from "@mui/material";
import { useFindByIdIamUser } from "@lib/api/hooks";

interface UserInfoDisplayProps {
  userId: string;
  mode?: "compact" | "full";
  fallback?: string;
}

export const UserInfoDisplay: React.FC<UserInfoDisplayProps> = ({
  userId,
  mode = "compact",
  fallback,
}) => {
  const {
    data: user,
    isLoading,
    error,
  } = useFindByIdIamUser(userId, {
    query: {
      enabled: !!userId,
      staleTime: 5 * 60 * 1000, // Cache for 5 minutes
      retry: 1,
    },
  });

  const getInitials = (
    firstName?: string,
    lastName?: string,
    username?: string
  ): string => {
    if (firstName && lastName) {
      return `${firstName.charAt(0)}${lastName.charAt(0)}`.toUpperCase();
    }
    if (username) {
      return username.charAt(0).toUpperCase();
    }
    return userId.charAt(0).toUpperCase();
  };

  const getDisplayName = (): string => {
    if (user?.firstName && user?.lastName) {
      return `${user.firstName} ${user.lastName}`;
    }
    if (user?.username) {
      return user.username;
    }
    return fallback || userId;
  };

  if (isLoading) {
    return (
      <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
        <Skeleton
          variant="circular"
          width={mode === "compact" ? 24 : 32}
          height={mode === "compact" ? 24 : 32}
        />
        <Skeleton variant="text" width={100} />
      </Box>
    );
  }

  if (error || !user) {
    return (
      <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
        <Avatar
          sx={{
            width: mode === "compact" ? 24 : 32,
            height: mode === "compact" ? 24 : 32,
            fontSize: mode === "compact" ? "0.75rem" : "0.875rem",
          }}
        >
          {(fallback || userId).charAt(0).toUpperCase()}
        </Avatar>
        <Typography variant={mode === "compact" ? "body2" : "body1"}>
          {fallback || userId}
        </Typography>
      </Box>
    );
  }

  if (mode === "compact") {
    return (
      <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
        <Avatar
          sx={{
            width: 24,
            height: 24,
            fontSize: "0.75rem",
            bgcolor: "primary.main",
          }}
        >
          {getInitials(user.firstName, user.lastName, user.username)}
        </Avatar>
        <Typography variant="body2" noWrap>
          {getDisplayName()}
        </Typography>
      </Box>
    );
  }

  return (
    <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
      <Avatar
        sx={{
          width: 32,
          height: 32,
          bgcolor: "primary.main",
        }}
      >
        {getInitials(user.firstName, user.lastName, user.username)}
      </Avatar>
      <Box>
        <Typography variant="body1" fontWeight={500}>
          {getDisplayName()}
        </Typography>
        {user.username && (
          <Typography variant="caption" color="text.secondary">
            @{user.username}
          </Typography>
        )}
        {user.email && (
          <Typography variant="caption" color="text.secondary" display="block">
            {user.email}
          </Typography>
        )}
      </Box>
    </Box>
  );
};
