# Multi-stage build for Ticket Service

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy parent POM and all modules (Maven requires all declared modules to exist)
COPY pom.xml .
COPY common common/
COPY ticket-service ticket-service/
COPY payment-service payment-service/
COPY notification-service notification-service/
COPY user-service user-service/
COPY audit-service audit-service/

# Build the application
RUN mvn clean package -pl ticket-service -am -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the built JAR from build stage
COPY --from=build /app/ticket-service/target/*.jar app.jar

# Install curl for healthcheck
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Expose port
EXPOSE 8081

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
