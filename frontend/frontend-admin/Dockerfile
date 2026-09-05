FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
COPY shared-ui/package.json shared-ui/package.json
COPY frontend-admin/package.json frontend-admin/package.json
# npm ci strictly follows package-lock.json, which was generated on macOS and only has a
# materialized entry for @rollup/rollup-darwin-arm64 — it fails to pull the linux-arm64-musl
# binary this Alpine container actually needs (a well-known npm optional-deps bug, npm/cli#4828).
# npm install re-resolves platform-specific optional deps properly while still respecting every
# version already pinned in the lockfile.
RUN npm install

COPY shared-ui shared-ui
COPY frontend-admin frontend-admin

ARG VITE_API_BASE_URL=http://localhost:8080
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build --workspace=frontend-admin

FROM nginx:1.27-alpine AS runtime
COPY --from=build /app/frontend-admin/dist /usr/share/nginx/html
COPY frontend-admin/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
