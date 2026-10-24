# ==============================================================================
# Stage 1: Build stage
# ==============================================================================
FROM node:20-alpine AS builder

WORKDIR /app

# Accept configurable backend API URL at build time (defaults to local host access)
ARG VITE_API_BASE_URL=http://localhost:8080
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

# Copy package manifests and install deterministic dependencies
COPY package.json package-lock.json ./
RUN npm ci

# Copy application source files
COPY . .

# Run production build producing optimized static bundles in dist/
RUN npm run build

# ==============================================================================
# Stage 2: Production Web Server stage
# ==============================================================================
FROM nginx:1.27-alpine AS runner

# Remove default nginx boilerplate configuration
RUN rm -rf /etc/nginx/conf.d/* /usr/share/nginx/html/*

# Copy custom Nginx configuration supporting SPA routing and security headers
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy compiled production artifacts from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Expose HTTP port
EXPOSE 80

# Container healthcheck targeting the dedicated /health endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:80/health || exit 1

# Launch Nginx in foreground mode
CMD ["nginx", "-g", "daemon off;"]
