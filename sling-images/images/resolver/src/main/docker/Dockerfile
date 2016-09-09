# Dockerfile for our Sling adaptTo 2016 launchpad runnable jar
# Filtered by the Maven resources plugin before building the image
FROM java:8
COPY ${project.build.finalName}.jar /opt/sling/launchpad.jar
WORKDIR /opt/sling/

# HTTP and debugging ports
EXPOSE 8080
EXPOSE 30303

# Activate JVM debugger
ENV JAVA_OPTS -Xmx512m  -XX:MaxPermSize=128m -agentlib:jdwp=transport=dt_socket,address=30303,server=y,suspend=n

ENV SLING_OPTS ''
CMD exec java $JAVA_OPTS -jar launchpad.jar $SLING_OPTS
