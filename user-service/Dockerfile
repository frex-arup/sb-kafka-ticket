FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY common common/
COPY ticket-service ticket-service/
COPY payment-service payment-service/
COPY notification-service notification-service/
COPY user-service user-service/
COPY audit-service audit-service/
RUN mvn clean package -pl user-service -am -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/user-service/target/*.jar app.jar
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
