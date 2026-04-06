# Build stage
FROM maven:3.8-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy project files
COPY pom.xml .
COPY src/ src/
COPY config/ config/
COPY plugins/ plugins/

# Build the project
RUN mvn clean package -DskipTests -q

# Runtime stage
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Install Docker CLI for sandbox functionality
RUN apt-get update && apt-get install -y \
    docker.io \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy built JAR from builder
COPY --from=builder /build/target/JarvisTerminal.jar .
COPY --from=builder /build/config/ config/
COPY --from=builder /build/plugins/ plugins/
COPY --from=builder /build/src/main/resources/logback.xml .

# Create logs directory
RUN mkdir -p logs

# Expose port (optional, for future enhancements)
EXPOSE 9999

# Set entrypoint
ENTRYPOINT ["java", "-jar", "JarvisTerminal.jar"]
