#!/bin/bash
# send simulated logs to graylog server
TARGET=graylog
DELAY=15
PORT=12201
CONTAINER_ID=$(head -1 < /etc/hosts | cut -f2)
echo "Starting simulated log messages to $TARGET:$PORT, every $DELAY seconds"
while true
do
	NOW=$(date)
	echo -e "{\"version\": \"1.1\",\"host\":\"$SLING_DOMAIN\",\"short_message\":\"Hello from $SLING_DOMAIN/$CONTAINER_ID at $NOW\",\"full_message\":\"Just an example log\n\to simulate what Sling would send\",\"level\":1,\"_user_id\":42}\0" \
	| nc -w 1 $TARGET $PORT
	sleep $DELAY
done