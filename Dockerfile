# Build stage
FROM gradle:8-jdk21 AS build
WORKDIR /app

# Copy gradle files
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Copy source
COPY src ./src

# Copy generated JOOQ code (must be generated locally first)
COPY target ./target

# Build application (skip tasks that require Docker/DB)
RUN gradle clean build -x test -x composeUp -x update -x generateJooq --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Create non-root user
RUN addgroup -g 1001 appuser && \
    adduser -D -u 1001 -G appuser appuser && \
    chown -R appuser:appuser /app

USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    # "-Dserver.address=0.0.0.0", \
    # "-Dserver.port=8080", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
