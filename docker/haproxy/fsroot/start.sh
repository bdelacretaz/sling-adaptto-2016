#!/bin/bash
NODE=http://etcd:4001

# WARNING make sure this is consistent between start.sh and reload.sh
haproxy -D -f /etc/haproxy/haproxy.cfg -p /var/haproxy/haproxy.pid -sf $(cat /var/haproxy/haproxy.pid)

# save parameters for reload.sh
echo SLING_ROLE_REGEXP="$SLING_ROLE_REGEXP"
echo $SLING_ROLE_REGEXP > /tmp/sling-role-regexp.txt

# log changes to our backends config
export CFG="/tmp/haproxy-tmp.cfg"
> $CFG
while true
do
  inotifywait -e close_write $CFG
  echo "***** $CFG changed *****"
  sleep 1
  cat $CFG
  echo "************************"
done &

/usr/local/bin/confd -interval=2 --config-file=/etc/confd/conf.d/haproxy.toml -node=$NODE
