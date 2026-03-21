# ============================================================
# Multi-stage build for NLQ Spring Boot app
# Stage 1: Maven build / Stage 2: Run
# ============================================================

# --- Build stage ---
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
# 先下載依賴 (利用 Docker cache) download dependencies first (leverage cache)
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# --- Run stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
