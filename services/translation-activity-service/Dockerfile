# Build from the payment-platform REPO ROOT, not this directory:
#   docker build -f services/translation-activity-service/Dockerfile -t translation-activity-service:local .
# Needs the whole reactor in context so payment-common resolves as a local Maven
# module. For a real CI pipeline, publish payment-common to your internal Maven repo
# (Nexus/Artifactory/GitHub Packages) as its own release step first, then this build
# only needs services/translation-activity-service/ as context and a plain `mvn -pl translation-activity-service package` —
# see README "Building payment-common independently."

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY payment-common payment-common
COPY services/translation-activity-service services/translation-activity-service
RUN mvn -q -pl services/translation-activity-service -am package -DskipTests

FROM eclipse-temurin:21-jre-jammy
RUN useradd --system --create-home --shell /usr/sbin/nologin appuser
USER appuser
WORKDIR /app
COPY --from=build /workspace/services/translation-activity-service/target/*.jar app.jar

EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
