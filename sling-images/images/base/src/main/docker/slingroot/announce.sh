#!/bin/bash
# announce this instance to etcd
# TODO wait if etcd server not ready
export ACTION=${1-PUT}
export CONTAINER_ID=`hostname`
export IP=`grep $CONTAINER_ID /etc/hosts | cut -f1`
export URL="http://etcd:4001/v2/keys/sling/instances/`hostname`"
echo "Announcing ($ACTION) $IP:$SLING_PORT to $URL"

# TODO need json escapes
curl -s ${URL} -X$ACTION -d value="{\"ip\":\"$IP\",\"port\":\"$SLING_PORT\",\"role\":\"$SLING_ROLE\"}"
