# Build frontend
FROM node:20-alpine AS frontend
WORKDIR /app
COPY frontend/package*.json ./
RUN npm ci
COPY frontend ./
RUN npm run build:prod -- --output-path /tmp/frontend-dist

# Build backend
FROM maven:3.9.6-eclipse-temurin-17 AS backend
WORKDIR /workspace
COPY backend/pom.xml backend/pom.xml
RUN mvn -f backend/pom.xml dependency:go-offline
COPY backend/src backend/src
COPY --from=frontend /tmp/frontend-dist/browser/ backend/src/main/resources/static/
RUN mvn -f backend/pom.xml clean package -DskipTests

# Runtime image
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=backend /workspace/backend/target/*.jar app.jar
EXPOSE 9080
ENTRYPOINT ["java","-jar","/app/app.jar"]
