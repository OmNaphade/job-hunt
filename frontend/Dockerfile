FROM node:22-alpine AS build
WORKDIR /workspace
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ .

# Baked in at build time (Vite inlines env vars into the bundle, they can't be
# changed at container start like a backend service's env vars can). Left blank,
# VITE_API_BASE_URL makes every API call relative to whatever origin the page is
# served from — Caddy then reverse-proxies /api/* to api-gateway on that same
# origin, so the browser never makes a cross-origin request and CORS never applies.
ARG VITE_API_BASE_URL=""
ARG VITE_ALLOW_SELF_REGISTER="true"
ARG VITE_ZIPKIN_URL=""
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_ALLOW_SELF_REGISTER=$VITE_ALLOW_SELF_REGISTER
ENV VITE_ZIPKIN_URL=$VITE_ZIPKIN_URL
RUN npm run build

FROM caddy:2-alpine
COPY --from=build /workspace/dist /srv
COPY frontend/Caddyfile /etc/caddy/Caddyfile
EXPOSE 80
