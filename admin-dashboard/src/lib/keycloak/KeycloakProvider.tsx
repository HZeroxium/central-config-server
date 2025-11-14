import React from "react";
import { ReactKeycloakProvider } from "@react-keycloak/web";
import { keycloak } from "./keycloakConfig";
import { CircularProgress, Box } from "@mui/material";

interface KeycloakProviderProps {
  children: React.ReactNode;
}

const LoadingComponent: React.FC = () => (
  <Box
    display="flex"
    justifyContent="center"
    alignItems="center"
    minHeight="100vh"
  >
    <CircularProgress />
  </Box>
);

const KeycloakProvider: React.FC<KeycloakProviderProps> = ({ children }) => {
  const initOptions = {
    onLoad: "login-required",
    // Enable check login iframe for better session management
    // This helps with "restart login" cookie issues
    checkLoginIframe: true,
    checkLoginIframeInterval: 30,
    // Use PKCE for security
    pkceMethod: "S256",
    // Enable logging for debugging
    enableLogging: true,
    // Timeout for initialization
    messageReceiveTimeout: 10000,
    // Flow type
    flow: "standard",
  };

  return (
    <ReactKeycloakProvider
      authClient={keycloak}
      initOptions={initOptions}
      LoadingComponent={<LoadingComponent />}
      onEvent={(event, error) => {
        console.log("Keycloak event:", event, error);
        if (event === "onAuthError") {
          console.error("Keycloak auth error:", error);
        }
        if (event === "onAuthSuccess") {
          console.log("Keycloak authentication successful");
        }
        if (event === "onReady") {
          console.log("Keycloak is ready");
        }
        if (event === "onInitError") {
          console.error("Keycloak initialization error:", error);
        }
      }}
      onTokens={(tokens) => {
        console.log("Keycloak tokens updated:", {
          token: tokens.token ? "present" : "missing",
          refreshToken: tokens.refreshToken ? "present" : "missing",
          idToken: tokens.idToken ? "present" : "missing",
        });
      }}
    >
      {children}
    </ReactKeycloakProvider>
  );
};

export default KeycloakProvider;
