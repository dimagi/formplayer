#!/bin/bash -e

DOCKER_IMAGE="dimagi/formplayer"
TARGET_IMAGE_LATEST="${DOCKER_IMAGE}:latest"
docker build -t $DOCKER_IMAGE .

docker login -u $DOCKER_USER -p $DOCKER_PASSWORD

docker tag ${DOCKER_IMAGE} ${TARGET_IMAGE_LATEST}
docker push ${TARGET_IMAGE_LATEST}
