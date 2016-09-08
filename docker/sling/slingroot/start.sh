#!/bin/bash
export JAVA_OPTS="-Xmx512m"
export SLING_OPTS="-Dsling.run.modes=oak_mongo,oak -Doak.mongo.uri=mongodb://mongo:27017 -Doak.mongo.db=${SLING_DB} -p ${SLING_PORT}"

export PROF_DIR=/sling-volume/profiler/$(hostname)
export PROF_FILE=$PROF_DIR/log.hpl
mkdir -p $PROF_DIR
export PROF="-agentpath:/honest-profiler/liblagent.so=interval=7,logPath=$PROF_FILE"
echo Adding honest-profiler options: $PROF
export JAVA_OPTS="$JAVA_OPTS $PROF"

echo Starting Sling in `pwd`
echo JAVA_OPTS="$JAVA_OPTS"
echo SLING_OPTS=$SLING_OPTS

/bin/bash ./announce.sh
/bin/bash ./log-simulation.sh &

java $JAVA_OPTS -jar launchpad.jar $SLING_OPTS
