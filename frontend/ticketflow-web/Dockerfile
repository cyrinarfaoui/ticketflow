# Image de PRODUCTION du frontend (le développement utilise Dockerfile.dev)

# ---- Étape 1 : compilation Angular ----
FROM node:24-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npx ng build --configuration production

# ---- Étape 2 : serveur web nginx, sans Node ni code source ----
FROM nginx:1.29-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/ticketflow-web/browser /usr/share/nginx/html
EXPOSE 80
