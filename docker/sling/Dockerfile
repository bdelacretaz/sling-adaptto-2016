FROM sling-at16:latest
MAINTAINER bdelacretaz@apache.org

COPY slingroot /opt/sling
WORKDIR /opt/sling/
EXPOSE 8080

# We would need a unique folder name
# for each Sling instance to do this
# VOLUME /opt/sling/sling

CMD ["/bin/bash","./start.sh"]
