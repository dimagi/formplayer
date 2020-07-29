FROM openjdk:8-jre-slim
LABEL maintainer="Dimagi <devops@dimagi.com>"

# Environment variable WEB_HOST is used for setting where Django is
# running; either in another container ("web") or on the Docker host
# machine ("dockerhost").
#
# /formplayer/set_webhost adds the IP address of the host machine to
# /etc/hosts and sets the hostname "webhost" as an alias to HQ.

WORKDIR /formplayer

# Using wget instead of ADD to download formplayer because the connection got
# dropped repeatedly in testing, and wget -c handles that gracefully
RUN apt-get update && apt-get install -y wget
RUN wget -c -O formplayer.jar \
    https://jenkins.dimagi.com/job/formplayer/lastStableBuild/artifact/build/libs/formplayer.jar

COPY config/application.docker.properties /formplayer/application.properties
COPY config/set_webhost /formplayer/

EXPOSE 8010

CMD ./set_webhost $WEB_HOST && java -jar formplayer.jar
