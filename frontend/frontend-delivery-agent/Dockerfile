FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
COPY shared-ui/package.json shared-ui/package.json
COPY frontend-delivery-agent/package.json frontend-delivery-agent/package.json
RUN npm ci

COPY shared-ui shared-ui
COPY frontend-delivery-agent frontend-delivery-agent

ARG VITE_API_BASE_URL=http://localhost:8080
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build --workspace=frontend-delivery-agent

FROM nginx:1.27-alpine AS runtime
COPY --from=build /app/frontend-delivery-agent/dist /usr/share/nginx/html
COPY frontend-delivery-agent/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
