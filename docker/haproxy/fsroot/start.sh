#!/bin/bash

# Using the etcd hostname didn't work on some networks, not sure why
ETCD_IP=$(ping etcd -c 1 | head -1 | cut -d'(' -f2 | cut -d')' -f1)
NODE=http://$ETCD_IP:4001
echo "Will use $NODE to connect to etcd"

# WARNING make sure this is consistent between start.sh and reload.sh
haproxy -D -f /etc/haproxy/haproxy.cfg -p /var/haproxy/haproxy.pid -sf $(cat /var/haproxy/haproxy.pid)

# save SLING_ env for other scripts
set | grep SLING_ > /tmp/SLING_ENV.sh
echo "SLING_ environment:"
cat /tmp/SLING_ENV.sh

# log changes of interesting files
./logchanges.sh /etc/haproxy/haproxy.cfg

# confd watches etcd and triggers reload.sh upon changes
/usr/local/bin/confd -interval=2 --config-file=/etc/confd/conf.d/haproxy.toml -node=$NODE
