#!/bin/bash
# Script that displays the current Sling processor roles of our prototype
# Used for live demos
. $(dirname $0)/docker-helpers.sh

dockerLog docker_reddr | while read line 
do
	echo $line | sed 's/.*returned role *//' | cut -d',' -f1 
done