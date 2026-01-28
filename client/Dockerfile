# Stage 1: Build stage
FROM node:20-alpine AS build

WORKDIR /app

# Copy package files first for better caching
COPY package*.json ./

# Optional: Ensure npm is up to date, but usually not needed for standard builds
RUN npm install -g npm@latest

RUN npm install
COPY . .
RUN npm run build

# Stage 2: Production stage (Nginx)
FROM nginx:stable-alpine

# Copy the built files from Vite's default 'dist' folder to Nginx's serve folder
COPY --from=build /app/dist /usr/share/nginx/html

# EXPOSE is mostly for documentation
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]