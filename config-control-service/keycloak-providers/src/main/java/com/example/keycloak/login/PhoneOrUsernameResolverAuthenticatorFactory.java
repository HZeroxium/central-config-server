package com.example.keycloak.login;

import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

/**
 * Factory for PhoneOrUsernameResolverAuthenticator.
 * Registers the authenticator with Keycloak's SPI system.
 */
public class PhoneOrUsernameResolverAuthenticatorFactory implements AuthenticatorFactory {
    
    public static final String PROVIDER_ID = "phone-or-username-resolver";
    
    @Override
    public String getDisplayType() {
        return "Phone or Username Resolver";
    }
    
    @Override
    public String getReferenceCategory() {
        return "browser";
    }
    
    @Override
    public boolean isConfigurable() {
        return false; // No configuration needed
    }
    
    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[]{
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.ALTERNATIVE,
            AuthenticationExecutionModel.Requirement.DISABLED
        };
    }
    
    @Override
    public boolean isUserSetupAllowed() {
        return false; // No user setup needed
    }
    
    @Override
    public String getHelpText() {
        return "Resolves phone number input to username for authentication";
    }
    
    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of(); // No configuration properties
    }
    
    @Override
    public Authenticator create(KeycloakSession session) {
        return new PhoneOrUsernameResolverAuthenticator();
    }
    
    @Override
    public void init(org.keycloak.Config.Scope config) {
        // No initialization needed
    }
    
    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No post-initialization needed
    }
    
    @Override
    public void close() {
        // No cleanup needed
    }
    
    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
