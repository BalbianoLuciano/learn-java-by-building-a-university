# Local image of the web app (production is served by Cloudflare Pages).
# Build context: the repository root.
FROM node:24-alpine AS build
WORKDIR /repo
RUN corepack enable
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
COPY apps/web/package.json apps/web/
COPY packages/contracts/package.json packages/contracts/
RUN pnpm install --frozen-lockfile
COPY packages/contracts packages/contracts
COPY content/i18n content/i18n
COPY apps/web apps/web
# Where the browser reaches the api from.
ARG VITE_API_URL=http://localhost:8080
ENV VITE_API_URL=$VITE_API_URL
RUN pnpm --filter @ljbu/web build

FROM nginxinc/nginx-unprivileged:stable-alpine
COPY apps/web/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /repo/apps/web/dist /usr/share/nginx/html
EXPOSE 8080
