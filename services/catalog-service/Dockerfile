# syntax=docker/dockerfile:1
# Build context must be services/ (the Maven reactor root), e.g.:
#   docker build -f services/catalog-service/Dockerfile -t readora/catalog-service services

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
RUN mvn -o -q -pl catalog-service -am dependency:go-offline || true

COPY . .
RUN mvn -q -pl catalog-service -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY --from=build /workspace/catalog-service/target/*.jar app.jar
COPY otel-agent.jar /app/otel-agent.jar
RUN chmod 644 /app/otel-agent.jar
ENV OTEL_SERVICE_NAME=catalog-service
USER spring
EXPOSE 8083
EXPOSE 9000
ENTRYPOINT ["java", "-javaagent:/app/otel-agent.jar", "-jar", "/app/app.jar"]
