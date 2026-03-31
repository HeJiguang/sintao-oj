FROM node:22-alpine AS deps
WORKDIR /workspace/frontend

COPY frontend/package.json frontend/package-lock.json frontend/tsconfig.base.json ./
COPY frontend/apps/web/package.json apps/web/package.json
COPY frontend/apps/app/package.json apps/app/package.json
COPY frontend/apps/admin/package.json apps/admin/package.json
COPY frontend/packages/api/package.json packages/api/package.json
COPY frontend/packages/config/package.json packages/config/package.json
COPY frontend/packages/tokens/package.json packages/tokens/package.json
COPY frontend/packages/ui/package.json packages/ui/package.json

RUN npm ci

FROM deps AS builder
WORKDIR /workspace/frontend

COPY frontend/apps ./apps
COPY frontend/packages ./packages

RUN npm run build:admin

FROM node:22-alpine AS runner
WORKDIR /workspace/frontend

ENV NODE_ENV=production

COPY --from=builder /workspace/frontend ./

EXPOSE 4002

CMD ["npm", "run", "start", "-w", "@aioj/admin", "--", "--hostname", "0.0.0.0", "--port", "4002"]
