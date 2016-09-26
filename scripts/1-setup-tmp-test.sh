#!/bin/bash
curl -Fsling:resourceType=test http://${H}/tmp/test
curl -X POST http://${H}/cluster/routing/scripts/test
export W=fileserver
echo $W > /tmp/1 && curl -T /tmp/1 http://${H}/cluster/routing/scripts/test/json.routing

echo
echo "********************************************************************"
echo "Contents of http://${H}/cluster/routing/scripts/test/json.routing :"
curl -s http://${H}/cluster/routing/scripts/test/json.routing
