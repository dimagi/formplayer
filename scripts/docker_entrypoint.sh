#!/bin/bash
# Trick to get host.docker.internal working on Linux Docker
# From https://dev.to/bufferings/access-host-from-a-docker-container-4099
HOST_DOMAIN="host.docker.internal"
ping -q -c1 $HOST_DOMAIN > /dev/null 2>&1
if [ $? -ne 0 ]; then
    HOST_IP=$(route | awk 'FNR==3 {print $2}')
    echo -e "$HOST_IP\t$HOST_DOMAIN" >> /etc/hosts
fi

cd /application

trap 'kill $PID' SIGINT SIGTERM

$@ &

PID=$!
BASH_PID=$$
wait $PID
