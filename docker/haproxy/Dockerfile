FROM haproxy:1.5.14
MAINTAINER bdelacretaz@apache.org

# TODO nano and procps are for debugging only
RUN apt-get update && \
    apt-get install -y curl && \
    apt-get install -y nano && \
    apt-get install -y procps && \
    apt-get install -y inotify-tools && \
    rm -rf /var/lib/apt/lists/*

ENV CONFD_MD5=e026e6807fbda9c1aba7bb068b22d8e4
RUN \
  curl -L https://github.com/kelseyhightower/confd/releases/download/v0.11.0/confd-0.11.0-linux-amd64 -o /usr/local/bin/confd && \
  echo "${CONFD_MD5}  /usr/local/bin/confd" | md5sum -c && \
  chmod +x /usr/local/bin/confd

COPY fsroot /
RUN chmod +x /*.sh
CMD ["/bin/bash","/start.sh"]
