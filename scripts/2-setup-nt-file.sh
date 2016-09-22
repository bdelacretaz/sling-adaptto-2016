#!/bin/bash
curl -F"sling:processorRole=fileserver" http://${H}/cluster/routing/methods/PUT
echo fileserver > /tmp/$$
curl -X POST http://${H}/cluster/routing/scripts/nt/file
curl -T /tmp/$$ http://${H}/cluster/routing/scripts/nt/file/GET.routing
rm -f /tmp/$$
