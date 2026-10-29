# Multi-stage build for API Gateway
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /build

# Copy parent pom and all service poms
COPY pom.xml ./parent-pom.xml
COPY api-gateway/pom.xml ./api-gateway-pom.xml
COPY api-gateway/src ./src

# Install parent POM first so it's available locally
RUN apk add --no-cache maven && \
    mvn install:install-file \
      -Dfile=parent-pom.xml \
      -DpomFile=parent-pom.xml \
      -DgroupId=goldmansachs \
      -DartifactId=Event-Driven-Order-Pipeline \
      -Dversion=1.0.0-SNAPSHOT \
      -Dpackaging=pom && \
    mv api-gateway-pom.xml pom.xml && \
    mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/target/api-gateway-*.jar app.jar

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8000/actuator/health || exit 1

EXPOSE 8000
ENTRYPOINT ["java", "-jar", "app.jar"]
