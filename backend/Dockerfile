# ==============================================================================
# Stage 1: Build stage
# ==============================================================================
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Cache Maven dependencies by copying pom.xml first
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copy source tree and build executable jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==============================================================================
# Stage 2: Production Runtime stage
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Create a dedicated non-root user and group
RUN addgroup -S devflow && adduser -S devflow -G devflow

# Copy executable jar from builder stage with appropriate permissions
COPY --from=builder --chown=devflow:devflow /build/target/backend-0.0.1-SNAPSHOT.jar app.jar

# Switch to non-root user
USER devflow:devflow

# Expose Spring Boot application port
EXPOSE 8080

# Container JVM optimization parameters
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Container healthcheck using Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT:-8080}/actuator/health || exit 1

# Launch with exec so the JVM process becomes PID 1 and receives termination signals directly
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
