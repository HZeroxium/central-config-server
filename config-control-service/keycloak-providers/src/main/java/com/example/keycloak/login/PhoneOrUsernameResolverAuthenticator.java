package com.example.keycloak.login;

import com.example.keycloak.util.PhoneNumberUtil;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Stream;

/**
 * Authenticator that resolves username or phone number input.
 * If the input is detected as a phone number, it looks up the user by phone attribute
 * and replaces the username field with the actual username for the next step.
 */
public class PhoneOrUsernameResolverAuthenticator implements Authenticator {
    
    private static final Logger logger = LoggerFactory.getLogger(PhoneOrUsernameResolverAuthenticator.class);
    
    @Override
    public void authenticate(AuthenticationFlowContext context) {
        // GET (render form) 
        logger.debug("authenticate() pass-through for login page rendering");
        context.success();
    }
    
    private UserModel findUserByPhone(AuthenticationFlowContext context, String normalizedPhone) {
        RealmModel realm = context.getRealm();
        KeycloakSession session = context.getSession();
        
        try {
            // Search for user by phone attribute
            Stream<UserModel> users = session.users()
                .searchForUserByUserAttributeStream(realm, "phone", normalizedPhone);
            
            return users.findFirst().orElse(null);
            
        } catch (Exception e) {
            logger.error("Error searching for user by phone: {}", normalizedPhone, e);
            return null;
        }
    }
    
    @Override
    public void action(AuthenticationFlowContext context) {
        var formData = context.getHttpRequest().getDecodedFormParameters();
        var rawInput = formData != null ? formData.getFirst("username") : null;

        if (rawInput == null || rawInput.trim().isEmpty()) {
            logger.debug("No username input provided on POST; continue");
            context.success();
            return;
        }

        String trimmedInput = rawInput.trim();
        logger.debug("POST login input: {}", trimmedInput);

        if (PhoneNumberUtil.isPhone(trimmedInput)) {
            logger.info("Detected phone number input on POST: {}", trimmedInput);
            try {
                String normalizedPhone = PhoneNumberUtil.normalize(trimmedInput);
                logger.debug("Normalized phone: {}", normalizedPhone);

                UserModel user = findUserByPhone(context, normalizedPhone);
                if (user != null) {
                    String actualUsername = user.getUsername();
                    logger.info("Resolved phone {} to username {}", normalizedPhone, actualUsername);

                    // Gắn user vào context cho các step sau
                    context.setUser(user);
                    // Ghi đè lại trường username để UsernamePasswordForm xử lý chuẩn
                    formData.putSingle("username", actualUsername);
                } else {
                    logger.warn("No user found with phone number: {}", normalizedPhone);
                }
            } catch (Exception e) {
                logger.error("Error resolving phone number: {}", trimmedInput, e);
            }
        } else {
            logger.debug("Input appears to be username: {}", trimmedInput);
        }

        // Tiếp tục đến UsernamePasswordForm
        context.success();
    }
    
    @Override
    public boolean requiresUser() {
        return false; // We don't need the user to be authenticated yet
    }
    
    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true; // Always configured - no user-specific configuration needed
    }
    
    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // No required actions needed
    }
    
    @Override
    public void close() {
        // No resources to close
    }
}
