FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
COPY shared-ui/package.json shared-ui/package.json
COPY frontend-admin/package.json frontend-admin/package.json
RUN npm ci

COPY shared-ui shared-ui
COPY frontend-admin frontend-admin

ARG VITE_API_BASE_URL=http://localhost:8080
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build --workspace=frontend-admin

FROM nginx:1.27-alpine AS runtime
COPY --from=build /app/frontend-admin/dist /usr/share/nginx/html
COPY frontend-admin/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
