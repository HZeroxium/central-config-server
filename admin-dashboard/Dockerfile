# Multi-stage Dockerfile for admin-dashboard
# Stage 1: Build
# Note: Build context should be project root to access config-control-service/spec/openapi.json
FROM node:24-alpine AS build

WORKDIR /app

# Copy package files first for better layer caching
COPY admin-dashboard/package*.json ./
COPY admin-dashboard/orval.config.js ./

# Install dependencies
RUN npm ci

# Copy OpenAPI spec from config-control-service (needed for API generation)
# and place it where orval expects it (../config-control-service/spec/openapi.json relative to admin-dashboard)
COPY config-control-service/spec/openapi.json /tmp/openapi.json
RUN mkdir -p ../config-control-service/spec && \
    cp /tmp/openapi.json ../config-control-service/spec/openapi.json

# Copy admin-dashboard source code (excluding node_modules via .dockerignore)
COPY admin-dashboard/ .

# Build arguments for environment variables (Vite requires build-time vars)
ARG VITE_API_BASE_URL=http://config-control-service:8080
ARG VITE_KEYCLOAK_URL=http://keycloak:8080
ARG VITE_KEYCLOAK_REALM=config-control
ARG VITE_KEYCLOAK_CLIENT_ID=admin-dashboard

# Set environment variables for build
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_KEYCLOAK_URL=$VITE_KEYCLOAK_URL
ENV VITE_KEYCLOAK_REALM=$VITE_KEYCLOAK_REALM
ENV VITE_KEYCLOAK_CLIENT_ID=$VITE_KEYCLOAK_CLIENT_ID

# Generate API, compile TypeScript, and build
# Note: npm run build already includes generate:api, but we run it explicitly for clarity
RUN npm run build

# Stage 2: Serve with nginx
FROM nginx:alpine

# Copy nginx configuration
COPY admin-dashboard/nginx.conf /etc/nginx/conf.d/default.conf

# Copy built assets from build stage
COPY --from=build /app/dist /usr/share/nginx/html

# Expose port 80
EXPOSE 80

# Healthcheck
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1

# Start nginx
CMD ["nginx", "-g", "daemon off;"]

