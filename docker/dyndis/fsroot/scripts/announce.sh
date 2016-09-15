#!/bin/bash
# announce this instance to etcd if that's available
# used for the Sling adaptTo 2016 demo
export SLING_ROLE=${SLING_ROLE-dyndis}

function announce() {
  export CONTAINER_ID=`hostname`
  export IP=`grep $CONTAINER_ID /etc/hosts | cut -f1`
  export URL="http://etcd:4001/v2/keys/sling/instances/`hostname`"
  echo "Announcing $IP:$SLING_PORT to $URL"
  # TODO need json escapes
  curl -s ${URL} -XPUT -d value="{\"ip\":\"$IP\",\"port\":\"$SLING_PORT\",\"role\":\"$SLING_ROLE\"}"
  echo
}

/scripts/wait-for-it.sh etcd:4001 && announce