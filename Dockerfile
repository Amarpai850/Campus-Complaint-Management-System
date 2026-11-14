# ===================================================================
# Multi-Stage Dockerfile for Campus Complaint Management System
# Optimized for Cloud Deployment (Render, Railway, Fly.io, AWS)
# ===================================================================

# Stage 1: Build JAR using Maven and Temurin JDK 17
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build

# Copy Maven POM and source code
COPY pom.xml .
COPY src ./src

# Build production artifact without running integration tests during Docker build
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal Production Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create non-privileged user for container security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy built JAR from builder stage
COPY --from=builder /build/target/complaint-management-1.0.0.jar app.jar

# Switch to non-root user
USER appuser

# Expose standard port (Render/Railway map dynamic $PORT automatically)
EXPOSE 8080

# Production defaults
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod

# Container-aware JVM memory configuration (prevents OOM on 512MB RAM cloud tiers)
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
