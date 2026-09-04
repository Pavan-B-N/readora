FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
COPY shared-ui/package.json shared-ui/package.json
COPY frontend-user/package.json frontend-user/package.json
RUN npm ci

COPY shared-ui shared-ui
COPY frontend-user frontend-user

ARG VITE_API_BASE_URL=http://localhost:8080
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build --workspace=frontend-user

FROM nginx:1.27-alpine AS runtime
COPY --from=build /app/frontend-user/dist /usr/share/nginx/html
COPY frontend-user/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
