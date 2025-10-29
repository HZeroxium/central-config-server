package com.example.control.infrastructure.config.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Custom argument resolver for injecting UserContext directly into controller methods.
 * <p>
 * This resolver automatically extracts UserContext from JWT tokens, eliminating
 * the need for manual UserContext.fromJwt(jwt) calls in controllers.
 * </p>
 * <p>
 * Usage:
 * <pre>
 * // Before
 * public ResponseEntity<?> method(@AuthenticationPrincipal Jwt jwt) {
 *     UserContext ctx = UserContext.fromJwt(jwt);
 *     ...
 * }
 *
 * // After
 * public ResponseEntity<?> method(UserContext userContext) {
 *     ...
 * }
 * </pre>
 */
@Slf4j
public class UserContextArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return UserContext.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) throws Exception {

        // Get the JWT from the authentication principal
        Object principal = webRequest.getUserPrincipal();

        if (principal instanceof Jwt jwt) {
            log.debug("Resolving UserContext from JWT for parameter: {}", parameter.getParameterName());
            return UserContext.fromJwt(jwt);
        }

        log.warn("Unable to resolve UserContext - principal is not a JWT: {}",
                principal != null ? principal.getClass().getSimpleName() : "null");
        return null;
    }
}
