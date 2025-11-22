package com.example.control.application.service;

import com.example.control.infrastructure.config.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Builds commit messages for GitHub config file updates.
 * <p>
 * Creates standardized commit messages with template and optional custom message.
 * Format: "Update config for {displayName} ({profile}) by {username}"
 * If custom message provided: "{template}\n\n{customMessage}"
 * </p>
 *
 * @author Config Control Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommitMessageBuilder {

    private static final String TEMPLATE = "Update config for %s (%s) by %s";

    /**
     * Build commit message from template and optional custom message.
     *
     * @param displayName   the service display name
     * @param profile       the environment profile
     * @param userContext   the user context (for username)
     * @param customMessage optional custom message to append
     * @return formatted commit message
     */
    public String build(String displayName, String profile, UserContext userContext, String customMessage) {
        String username = userContext.getUsername() != null 
                ? userContext.getUsername() 
                : userContext.getUserId();

        String templateMessage = String.format(TEMPLATE, displayName, profile, username);

        if (customMessage != null && !customMessage.isBlank()) {
            String fullMessage = templateMessage + "\n\n" + customMessage.trim();
            log.debug("Built commit message with custom message: {}", fullMessage);
            return fullMessage;
        }

        log.debug("Built commit message from template: {}", templateMessage);
        return templateMessage;
    }
}

