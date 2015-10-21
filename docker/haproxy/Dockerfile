FROM haproxy:1.5.14
MAINTAINER bdelacretaz@apache.org

RUN apt-get update && \
    apt-get install -y curl && \
    apt-get install -y nano && \
    rm -rf /var/lib/apt/lists/*

ENV CONFD_MD5=1bc02600fa2dc33f9cdaf7b1fd189cfd
RUN \
  curl -L https://github.com/kelseyhightower/confd/releases/download/v0.7.1/confd-0.7.1-linux-amd64 -o /usr/local/bin/confd && \
  echo "${CONFD_MD5}  /usr/local/bin/confd" | md5sum -c && \
  chmod +x /usr/local/bin/confd
    
COPY fsroot /
CMD ["/bin/bash","/start.sh"]