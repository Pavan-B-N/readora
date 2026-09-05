# syntax=docker/dockerfile:1
# Build context must be services/ (the Maven reactor root), e.g.:
#   docker build -f services/api-gateway/Dockerfile -t readora/api-gateway services

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy every module pom.xml first so dependency resolution caches independently
# of source changes in any single module.
COPY pom.xml .
COPY shared-core/pom.xml shared-core/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY catalog-service/pom.xml catalog-service/pom.xml
COPY commerce-service/pom.xml commerce-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY mcp-server/pom.xml mcp-server/pom.xml
COPY ai-service/pom.xml ai-service/pom.xml
COPY delivery-agent-service/pom.xml delivery-agent-service/pom.xml
RUN mvn -o -q -pl api-gateway -am dependency:go-offline || true

COPY . .
RUN mvn -q -pl api-gateway -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -g 1000 -S spring && adduser -u 1000 -S spring -G spring
WORKDIR /app
COPY --from=build /workspace/api-gateway/target/*.jar app.jar
COPY otel-agent.jar /app/otel-agent.jar
RUN chmod 644 /app/otel-agent.jar
ENV OTEL_SERVICE_NAME=api-gateway
USER spring
EXPOSE 8080
EXPOSE 9000
ENTRYPOINT ["java", "-javaagent:/app/otel-agent.jar", "-jar", "/app/app.jar"]
