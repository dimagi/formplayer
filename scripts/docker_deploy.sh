#!/bin/bash -e

DOCKER_IMAGE="dimagi/formplayer"
TARGET_IMAGE_LATEST="${DOCKER_IMAGE}:latest"
docker build -t $DOCKER_IMAGE .

echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin

docker tag ${DOCKER_IMAGE} ${TARGET_IMAGE_LATEST}
docker push ${TARGET_IMAGE_LATEST}
