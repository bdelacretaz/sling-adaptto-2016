#!/bin/bash
# announce this instance to etcd
# TODO wait if etcd server not ready
export IP=`/sbin/ip route|awk '/default/ { print $3 }'`
export URL="http://etcd:4001/v2/keys/sling/instances/`hostname`"
echo "Announcing IP $IP and port $SLING_PORT to $URL"

# TODO need json escapes
curl ${URL} -XPUT -d value="{\"ip\":\"$IP\",\"port\":\"$SLING_PORT\",\"domain\":\"$SLING_DOMAIN\"}"
