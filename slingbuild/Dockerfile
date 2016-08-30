# Dockerfile for our Sling adaptTo 2016 launchpad runnable jar
#
# Build with 
#    mvn clean install
#    docker build -t sling-at16 .
#
FROM java:8
COPY launchpad/target/launchpad-0.0.1-SNAPSHOT.jar /opt/sling/launchpad.jar
WORKDIR /opt/sling/

# HTTP and debugging ports
EXPOSE 8080
EXPOSE 30303

# Without and with JVM debugger
#ENV JAVA_OPTS -Xmx512m
ENV JAVA_OPTS -Xmx512m  -XX:MaxPermSize=128m -agentlib:jdwp=transport=dt_socket,address=30303,server=y,suspend=n

ENV SLING_OPTS ''
CMD exec java $JAVA_OPTS -jar launchpad.jar $SLING_OPTS
