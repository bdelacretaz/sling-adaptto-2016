FROM haproxy
MAINTAINER bdelacretaz@apache.org

RUN apt-get update && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*
    
RUN curl -L https://github.com/kelseyhightower/confd/releases/download/v0.7.1/confd-0.7.1-linux-amd64 -o /usr/local/bin/confd
RUN chmod +x /usr/local/bin/confd
    
COPY fsroot /

CMD ["/usr/local/bin/confd","-interval=2","-verbose","--config-file=/etc/confd/conf.d/haproxy.toml","-node=http://etcd:4001"]
