FROM haproxy:1.5.14
MAINTAINER bdelacretaz@apache.org

RUN apt-get update && \
    apt-get install -y curl && \
    apt-get install -y nano && \
    rm -rf /var/lib/apt/lists/*
    
# TODO add m5 check like
# ENV HAPROXY_MD5 ad9d7262b96ba85a0f8c6acc6cb9edde
# echo "${HAPROXY_MD5}  haproxy.tar.gz" | md5sum -c
RUN curl -L https://github.com/kelseyhightower/confd/releases/download/v0.7.1/confd-0.7.1-linux-amd64 -o /usr/local/bin/confd
RUN chmod +x /usr/local/bin/confd
    
COPY fsroot /
CMD ["/bin/bash","/start.sh"]