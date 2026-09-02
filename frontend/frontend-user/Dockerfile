# syntax=docker/dockerfile:1
# Build context must be frontend/ (the npm workspace root), e.g.:
#   docker build -f frontend/frontend-user/Dockerfile -t readora/frontend-user frontend

FROM node:20-alpine AS build
WORKDIR /app

# Copy every workspace's package.json first (plus the lockfile) so `npm ci` — which resolves the
# whole workspace, not just this app — caches independently of source changes in any one package.
COPY package.json package-lock.json ./
COPY shared-ui/package.json shared-ui/package.json
COPY frontend-user/package.json frontend-user/package.json
COPY frontend-admin/package.json frontend-admin/package.json
COPY frontend-delivery-agent/package.json frontend-delivery-agent/package.json
RUN npm ci

COPY . .
# Baked in at build time (Vite inlines VITE_* vars into the static bundle) —
# override with --build-arg if the gateway isn't reachable at localhost:8080.
ARG VITE_API_BASE_URL=http://localhost:8080
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build --workspace=frontend-user

FROM nginx:1.27-alpine AS runtime
COPY --from=build /app/frontend-user/dist /usr/share/nginx/html
COPY frontend-user/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
