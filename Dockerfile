FROM adoptopenjdk:8-jre-hotspot as builder
WORKDIR application
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} formplayer.jar
RUN java -Djarmode=layertools -jar formplayer.jar extract

FROM adoptopenjdk:8-jre-hotspot
LABEL maintainer="Dimagi <devops@dimagi.com>"
WORKDIR application

RUN set -eux; \
    apt-get update; \
    apt-get install -y --no-install-recommends \
    # required to provide `route` command for custom entrypoint
    net-tools; \
    rm -rf /var/lib/apt/lists/*

COPY --from=builder application/dependencies/ ./
COPY --from=builder application/snapshot-dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/application/ ./

COPY scripts/docker_entrypoint.sh /entrypoint
RUN chmod +x /entrypoint
ENTRYPOINT ["/entrypoint"]
CMD ["java", "org.springframework.boot.loader.JarLauncher"]
