# keycloak/Dockerfile
FROM alpine:latest

# Install curl and jq for JSON processing
RUN apk add --no-cache curl jq

# Copy initialization files
COPY keycloak-realm-export.json /tmp/
COPY keycloak-init.sh /tmp/

# Make script executable
RUN chmod +x /tmp/keycloak-init.sh

# Set working directory
WORKDIR /tmp

# Default environment variables (overridable at runtime)
ENV KEYCLOAK_API_URL=http://keycloak:8080
ENV KEYCLOAK_HEALTH_URL=http://keycloak:9000
ENV KEYCLOAK_ADMIN=admin
ENV KEYCLOAK_ADMIN_PASSWORD=admin

# Run initialization script
CMD ["/tmp/keycloak-init.sh"]
