#!/bin/bash
export JAVA_OPTS="-Xmx512m"
export SLING_OPTS="-Dsling.run.modes=oak_mongo,oak -Doak.mongo.uri=mongodb://mongo:27017 -Doak.mongo.db=${SLING_DB} -p ${SLING_PORT}"

echo Starting Sling in `pwd`
echo JAVA_OPTS=$JAVA_OPTS
echo SLING_OPTS=$SLING_OPTS

/bin/bash ./announce.sh
/bin/bash ./log-simulation.sh &

java $JAVA_OPTS -jar org.apache.sling.launchpad.jar $SLING_OPTS
