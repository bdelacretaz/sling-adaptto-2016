# Dockerfile for our Sling adaptTo 2016 Sling base image
# Filtered by the Maven resources plugin before building the image
FROM java:8
COPY slingroot /opt/sling
WORKDIR /opt/sling/
RUN chmod +x *.sh

# HTTP and debugging ports
EXPOSE 8080
EXPOSE 30303

CMD ["/bin/bash","./start.sh"]
