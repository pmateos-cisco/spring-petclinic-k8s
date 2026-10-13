# Generic Dockerfile used to containerize the already-built Spring Boot jars
# for local deployment to the kind cluster. The upstream project normally
# builds images with Cloud Native Buildpacks (see Makefile / README), but
# `pack` isn't installed here, so this lightweight Dockerfile is used instead.
#
# Build example:
#   docker build --build-arg JAR_FILE=spring-petclinic-api-gateway/target/spring-petclinic-api-gateway-2.3.3.jar \
#                -t spring-petclinic-k8s/api-gateway:local .
FROM eclipse-temurin:17-jre-jammy

RUN useradd --no-create-home --shell /usr/sbin/nologin appuser

WORKDIR /app

ARG JAR_FILE
COPY --chown=appuser:appuser ${JAR_FILE} app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
