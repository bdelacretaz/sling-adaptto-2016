# check Sling status
# If the HAproxy is setup correctly this should always
# output the same Sling ID line, and no "failed" messages
# (or maybe just briefly when new Sling instances appear,
# we're not strictly zero downtime so far)
HOST=bravo.example.com
while true
do 
  curl -D - -s -u admin:admin http://$HOST/system/console/status-slingsettings  | egrep "DEFAULT BACKEND|Sling&nbsp;ID&nbsp;" || echo "request/grep failed"
  sleep 0.1
done
