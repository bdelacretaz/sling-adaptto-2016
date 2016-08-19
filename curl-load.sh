#!/bin/bash
# Generate load on a Sling instance by creating and reading back content
export HOST=$1
export BASE="$HOST/tmp/test-$$-"
echo "Creating and reading back content under $BASE"
while true
do
	export ID="$(date +%s)"
	export URL="$BASE$ID"
	curl -s -u admin:admin -X POST -F"id=$ID" $URL > /dev/null
	echo -n ""$URL :""
	curl -s -u admin:admin ${URL}.json
	echo
done	
	