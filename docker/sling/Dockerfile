FROM sling-at16:latest
MAINTAINER bdelacretaz@apache.org

# TODO netcat was temporary used for graylog logs simulation
# RUN apt-get update && apt-get install -y netcat && rm -rf /var/lib/apt/lists/*

COPY slingroot /opt/sling
WORKDIR /opt/sling/
EXPOSE 8080

# We would need a unique folder name
# for each Sling instance to do this
# VOLUME /opt/sling/sling

CMD ["/bin/bash","./start.sh"]
