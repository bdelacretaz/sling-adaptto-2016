#!/bin/bash
NODE=http://etcd:4001
haproxy -D -f /etc/haproxy/haproxy.cfg
/usr/local/bin/confd -interval=2 -debug --config-file=/etc/confd/conf.d/haproxy.toml -node=$NODE
