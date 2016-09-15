FROM ubuntu:16.04
MAINTAINER bdelacretaz@apache.org

RUN apt-get update
RUN apt-get install -y apache2 lua5.1 luarocks curl
RUN apt-get clean
RUN rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

RUN luarocks install luasocket

RUN ln /etc/apache2/mods-available/proxy.load /etc/apache2/mods-enabled/ \
  && ln /etc/apache2/mods-available/proxy_http.load /etc/apache2/mods-enabled/ \
  && ln /etc/apache2/mods-available/lua.load /etc/apache2/mods-enabled/

COPY fsroot /
WORKDIR /scripts
RUN chmod +x *.sh
CMD ./setup.sh && service apache2 start && ./announce.sh && tail -F  /var/log/apache2/error.log
