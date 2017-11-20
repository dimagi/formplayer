FROM openjdk:7-jre-slim
LABEL maintainer="Dimagi <devops@dimagi.com>"

WORKDIR /formplayer

# Using wget instead of ADD to download formplayer because the connection got
# dropped repeatedly in testing, and wget -c handles that gracefully
RUN apt-get update && apt-get install -y wget
RUN wget -c -O /formplayer/formplayer.jar \
    https://jenkins.dimagi.com/job/formplayer/lastStableBuild/artifact/build/libs/formplayer.jar

ADD config/application.docker.properties /formplayer/application.properties

EXPOSE 8080

CMD java -jar formplayer.jar
