# keycloak/Dockerfile
FROM alpine:latest

# Install curl and jq for JSON processing
RUN apk add --no-cache curl jq

# Copy initialization files
COPY keycloak-realm-export.json /tmp/
COPY keycloak-init.sh /tmp/
COPY keycloak-functions.sh /tmp/
COPY user-profile-attributes.json /tmp/
COPY roles.json /tmp/
COPY groups.json /tmp/
COPY users.json /tmp/
COPY scope-assignments.json /tmp/
COPY client-scopes/ /tmp/client-scopes/
COPY mappers/ /tmp/mappers/
COPY clients/ /tmp/clients/

# Make scripts executable
RUN chmod +x /tmp/keycloak-init.sh /tmp/keycloak-functions.sh

# Set working directory
WORKDIR /tmp

# Default environment variables (overridable at runtime)
ENV KEYCLOAK_API_URL=http://keycloak:8080
ENV KEYCLOAK_HEALTH_URL=http://keycloak:9000
ENV KEYCLOAK_ADMIN=admin
ENV KEYCLOAK_ADMIN_PASSWORD=admin

# Run initialization script
CMD ["/tmp/keycloak-init.sh"]
