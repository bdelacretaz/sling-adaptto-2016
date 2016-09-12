#!/bin/bash
export JAVA_OPTS="-Xmx512m"
export SLING_OPTS="-Dsling.run.modes=oak_mongo,oak -Doak.mongo.uri=mongodb://mongo:27017 -Doak.mongo.db=${SLING_DB} -p ${SLING_PORT}"

echo Starting Sling in `pwd`
echo JAVA_OPTS=$JAVA_OPTS
echo SLING_OPTS=$SLING_OPTS

# Wait for required services, announce Sling instance to etcd and start Sling
/bin/bash ./wait-for-it.sh etcd:4001 \
&& /bin/bash ./wait-for-it.sh mongo:27017 \
&& /bin/bash ./announce.sh \
&& java $JAVA_OPTS -jar launchpad.jar $SLING_OPTS
