# Build from the payment-platform REPO ROOT, not this directory:
#   docker build -f services/inbound-normalization-workflow-service/Dockerfile -t inbound-normalization-workflow-service:local .
# Needs the whole reactor in context so payment-common resolves as a local Maven
# module. For a real CI pipeline, publish payment-common to your internal Maven repo
# (Nexus/Artifactory/GitHub Packages) as its own release step first, then this build
# only needs services/inbound-normalization-workflow-service/ as context and a plain `mvn -pl inbound-normalization-workflow-service package` —
# see README "Building payment-common independently."

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY payment-common payment-common
COPY services/inbound-normalization-workflow-service services/inbound-normalization-workflow-service
RUN mvn -q -pl services/inbound-normalization-workflow-service -am package -DskipTests

FROM eclipse-temurin:21-jre-jammy
RUN useradd --system --create-home --shell /usr/sbin/nologin appuser
USER appuser
WORKDIR /app
COPY --from=build /workspace/services/inbound-normalization-workflow-service/target/*.jar app.jar

EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
