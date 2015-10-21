#!/bin/bash
NODE=http://etcd:4001

# WARNING make sure this is consistent between start.sh and reload.sh
haproxy -D -f /etc/haproxy/haproxy.cfg -p /var/haproxy/haproxy.pid -sf $(cat /var/haproxy/haproxy.pid)

/usr/local/bin/confd -interval=2 -verbose --config-file=/etc/confd/conf.d/haproxy.toml -node=$NODE
