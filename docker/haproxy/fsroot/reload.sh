#!/bin/bash
# called when confd detects changes
IN=/tmp/backends.txt
OUT=/etc/haproxy/haproxy.cfg
BASE=/etc/haproxy/haproxy-base.cfg
TMP=/tmp/haproxy-tmp.cfg

# TODO might need better handling of duplicate domains
# for now we just sort -u them
grep -v '^ *$' < $IN  | sort | awk -F# '{ print "server " $1 " " $2 ":" $3 " check" }' | sort -u > $TMP

# TODO only reload if $TMP changed
echo "servers list updated, reloading haproxy"
cat $BASE $TMP > $OUT
haproxy -D -f /etc/haproxy/haproxy.cfg